package com.sirh.backend.modules.offboarding.service;

import com.sirh.backend.modules.offboarding.dto.CreateExitRequest;
import com.sirh.backend.modules.offboarding.dto.CreateEmployeeResignationRequest;
import com.sirh.backend.modules.dossier.service.EmployeeDocumentStorageService;
import com.sirh.backend.modules.notifications.service.NotificationCenterService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

@Service
public class ExitManagementService {
    private final JdbcTemplate jdbc;
    private final NotificationCenterService notifications;
    private final EmployeeDocumentStorageService storage;
    public ExitManagementService(JdbcTemplate jdbc, NotificationCenterService notifications, EmployeeDocumentStorageService storage) { this.jdbc = jdbc; this.notifications = notifications; this.storage = storage; }

    @Transactional(readOnly = true)
    public List<Map<String,Object>> list(Integer actorId, boolean managerScope) {
        requireRole(actorId, managerScope ? "manager" : "rh");
        if (managerScope) return jdbc.queryForList("""
            SELECT d.id,d.employe_id AS "employeId",e.matricule,COALESCE(u.full_name,u.username) AS "nomEmploye",
                   d.contrat_id AS "contratId",c.numero AS "numeroContrat",m.code AS "motifCode",m.libelle AS motif,
                   st.code AS "statutCode",st.libelle AS statut,d.date_notification AS "dateNotification",
                   d.dernier_jour_travaille AS "dernierJourTravaille",d.commentaire,d.created_at AS "creeAt"
            FROM dossiers_sortie d JOIN employes e ON e.id=d.employe_id JOIN users u ON u.id=e.user_id
            JOIN contrats c ON c.id=d.contrat_id JOIN motifs_sortie m ON m.id=d.motif_id JOIN statuts_sortie st ON st.id=d.statut_id
            JOIN postes p ON p.id=e.poste_id
            WHERE p.departement_id=(SELECT mp.departement_id FROM employes me JOIN postes mp ON mp.id=me.poste_id WHERE me.user_id=?)
            ORDER BY d.created_at DESC,d.id DESC
            """, actorId);
        String scope = managerScope ? "AND p.departement_id=(SELECT mp.departement_id FROM employes me JOIN postes mp ON mp.id=me.poste_id WHERE me.user_id=?)" : "";
        return jdbc.queryForList(query(scope));
    }

    @Transactional(readOnly = true)
    public List<Map<String,Object>> ownList(Integer actorId) {
        requireEmployeeRole(actorId);
        Integer employeeId=jdbc.query("SELECT id FROM employes WHERE user_id=?",rs->rs.next()?rs.getInt(1):null,actorId);
        if(employeeId==null)return List.of();
        return jdbc.queryForList(query("AND d.employe_id=?"),employeeId);
    }

    @Transactional(readOnly = true)
    public List<Map<String,Object>> employeeList(Integer actorId,Integer employeeId) {
        requireRole(actorId,"rh");
        return jdbc.queryForList(query("AND d.employe_id=?"),employeeId);
    }

    @Transactional(readOnly = true)
    public List<Map<String,Object>> options(Integer actorId) {
        requireRole(actorId, "manager");
        return jdbc.queryForList("""
            SELECT e.id AS "employeeId",e.matricule,COALESCE(u.full_name,u.username) AS "employeeName",
                   c.id AS "contractId",c.numero AS "contractNumber",p.intitule AS poste
            FROM employes e JOIN users u ON u.id=e.user_id JOIN postes p ON p.id=e.poste_id
            JOIN candidatures ca ON ca.candidat_id=e.user_id JOIN contrats c ON c.candidature_id=ca.id
            JOIN statuts_contrat st ON st.id=c.statut_id AND st.code='ACTIF'
            WHERE p.departement_id=(SELECT mp.departement_id FROM employes me JOIN postes mp ON mp.id=me.poste_id WHERE me.user_id=?)
            ORDER BY u.full_name,e.matricule
            """, actorId);
    }

    @Transactional
    public Map<String,Object> create(Integer actorId, CreateExitRequest r) {
        requireRole(actorId, "manager");
        if (r.dernierJourTravaille().isBefore(r.dateNotification())) throw bad("Le dernier jour travaillé ne peut pas précéder la notification.");
        List<Integer> employeeRows = jdbc.query("""
            SELECT e.id FROM employes e JOIN postes p ON p.id=e.poste_id
            WHERE e.id=? AND p.departement_id=(SELECT mp.departement_id FROM employes me JOIN postes mp ON mp.id=me.poste_id WHERE me.user_id=?)
              AND EXISTS (SELECT 1 FROM user_roles ur JOIN roles ro ON ro.id=ur.role_id WHERE ur.user_id=? AND LOWER(ro.role_name)='manager')
            """, (rs, rowNum) -> rs.getInt(1), r.employeeId(), actorId, actorId);
        Integer employee = employeeRows.stream().findFirst().orElse(null);
        if (employee == null) throw forbidden("Ce salarié ne relève pas de votre département.");
        List<Integer> contractRows = jdbc.query("""
            SELECT c.id FROM contrats c JOIN candidatures ca ON ca.id=c.candidature_id
            JOIN employes e ON e.user_id=ca.candidat_id JOIN statuts_contrat st ON st.id=c.statut_id
            WHERE c.id=? AND e.id=? AND st.code='ACTIF' FOR UPDATE
            """, (rs, rowNum) -> rs.getInt(1), r.contractId(), employee);
        Integer contract = contractRows.stream().findFirst().orElse(null);
        if (contract == null) throw conflict("Le contrat sélectionné n’est pas actif ou n’appartient pas à ce salarié.");
        if (!exists("SELECT EXISTS(SELECT 1 FROM motifs_sortie m JOIN types_initiative_sortie i ON i.id=m.initiative_id WHERE m.code=? AND m.actif=TRUE AND i.code IN ('SALARIE','ECHEANCE'))", r.motifCode())) throw bad("Un manager peut transmettre uniquement une démission déclarée ou une échéance de CDD. Les décisions de l’employeur relèvent d’une procédure RH distincte.");
        if (exists("SELECT EXISTS(SELECT 1 FROM dossiers_sortie d JOIN statuts_sortie s ON s.id=d.statut_id WHERE d.contrat_id=? AND s.terminal=FALSE)", contract)) throw conflict("Une demande de sortie est déjà en cours pour ce contrat.");
        if ("FIN_CDD".equals(r.motifCode()) && !exists("SELECT EXISTS(SELECT 1 FROM contrats c JOIN types_contrat tc ON tc.id=c.type_contrat_id WHERE c.id=? AND c.date_fin=? AND UPPER(tc.code) IN ('CDD','CDD_RENOUVELABLE'))", contract, r.dernierJourTravaille())) throw bad("Une échéance ne peut être déclarée que pour un CDD et doit correspondre à sa date de fin enregistrée.");
        Integer id = jdbc.queryForObject("""
            INSERT INTO dossiers_sortie(employe_id,contrat_id,motif_id,statut_id,initie_par_id,date_notification,dernier_jour_travaille,commentaire)
            SELECT ?,?,?,s.id,?,?,?,? FROM statuts_sortie s WHERE s.code='SOUMISE_RH' RETURNING id
            """, Integer.class, employee, contract, lookup("motifs_sortie", r.motifCode()), actorId,
                r.dateNotification(), r.dernierJourTravaille(), r.commentaire());
        logStatus(id, null, "SOUMISE_RH", actorId, "Demande transmise par le manager");
        notifications.notifyRole("rh", "DOSSIER_SORTIE", "Départ à instruire", "Une demande de départ a été transmise pour contrôle RH.", "/espace/rh/sorties");
        return detail(id);
    }

    @Transactional
    public Map<String,Object> employeeResignation(Integer actorId,CreateEmployeeResignationRequest request) {
        requireEmployeeRole(actorId);
        if(request.dernierJourTravaille().isBefore(request.dateNotification()))throw bad("Le dernier jour travaillé ne peut pas précéder la notification.");
        Integer employeeId=jdbc.query("SELECT id FROM employes WHERE user_id=?",rs->rs.next()?rs.getInt(1):null,actorId);
        if(employeeId==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Aucune fiche salarié n’est associée à ce compte.");
        Integer contractId=jdbc.query("""
            SELECT c.id FROM contrats c JOIN candidatures ca ON ca.id=c.candidature_id
            JOIN statuts_contrat sc ON sc.id=c.statut_id AND sc.code='ACTIF'
            WHERE c.id=? AND ca.candidat_id=? FOR UPDATE
            """,rs->rs.next()?rs.getInt(1):null,request.contractId(),actorId);
        if(contractId==null)throw conflict("Le contrat sélectionné n’est plus actif ou ne vous appartient pas.");
        return createCase(actorId,employeeId,contractId,"DEMISSION",request.dateNotification(),request.dernierJourTravaille(),request.commentaire(),"Demande de démission transmise par le salarié.");
    }

    @Transactional
    public Map<String,Object> employerDismissal(Integer actorId,CreateExitRequest request) {
        requireRole(actorId,"rh");
        if(!"LICENCIEMENT".equals(request.motifCode()))throw bad("Cette action crée uniquement un dossier de licenciement.");
        if(request.commentaire()==null||request.commentaire().isBlank())throw bad("Ajoutez le motif factuel et les références de la décision RH.");
        if(request.dernierJourTravaille().isBefore(request.dateNotification()))throw bad("Le dernier jour travaillé ne peut pas précéder la notification.");
        Integer contractId=jdbc.query("""
            SELECT c.id FROM contrats c JOIN candidatures ca ON ca.id=c.candidature_id
            JOIN employes e ON e.user_id=ca.candidat_id JOIN statuts_contrat sc ON sc.id=c.statut_id AND sc.code='ACTIF'
            WHERE c.id=? AND e.id=? FOR UPDATE
            """,rs->rs.next()?rs.getInt(1):null,request.contractId(),request.employeeId());
        if(contractId==null)throw conflict("Le contrat actif ne correspond pas au salarié sélectionné.");
        if(!exists("SELECT EXISTS(SELECT 1 FROM motifs_sortie m JOIN types_initiative_sortie i ON i.id=m.initiative_id WHERE m.code='LICENCIEMENT' AND m.actif=TRUE AND i.code='EMPLOYEUR')"))throw conflict("Le motif de licenciement n’est pas configuré.");
        return createCase(actorId,request.employeeId(),contractId,"LICENCIEMENT",request.dateNotification(),request.dernierJourTravaille(),request.commentaire(),"Dossier de licenciement ouvert par les ressources humaines.");
    }

    private Map<String,Object> createCase(Integer actorId,Integer employeeId,Integer contractId,String motifCode,
            LocalDate notification,LocalDate lastDay,String comment,String historyMessage) {
        if(exists("SELECT EXISTS(SELECT 1 FROM dossiers_sortie d JOIN statuts_sortie s ON s.id=d.statut_id WHERE d.contrat_id=? AND s.terminal=FALSE)",contractId))
            throw conflict("Un dossier de sortie est déjà en cours pour ce contrat.");
        Integer id=jdbc.queryForObject("""
            INSERT INTO dossiers_sortie(employe_id,contrat_id,motif_id,statut_id,initie_par_id,date_notification,dernier_jour_travaille,commentaire)
            SELECT ?,?,m.id,s.id,?,?,?,? FROM motifs_sortie m CROSS JOIN statuts_sortie s
            WHERE m.code=? AND s.code='SOUMISE_RH' RETURNING id
            """,Integer.class,employeeId,contractId,actorId,notification,lastDay,comment==null?"":comment.trim(),motifCode);
        logStatus(id,null,"SOUMISE_RH",actorId,historyMessage);
        notifications.notifyRole("rh","DOSSIER_SORTIE","Départ à instruire","Un dossier de sortie a été transmis au service RH.","/espace/rh/sorties");
        return detail(id);
    }

    /** Produces a reviewable gross settlement draft from the paid payroll cutoff and leave ledger. */
    @Transactional
    public Map<String,Object> prepareSettlement(Integer actorId, Integer exitId) {
        requireRole(actorId, "rh");
        List<Map<String,Object>> exits = jdbc.queryForList("""
            SELECT d.id,d.employe_id,d.contrat_id,d.dernier_jour_travaille,d.statut_id,st.code AS statut_code,
                   c.salaire_base_mensuel,c.date_debut AS contrat_debut,e.matricule
            FROM dossiers_sortie d JOIN statuts_sortie st ON st.id=d.statut_id
            JOIN contrats c ON c.id=d.contrat_id JOIN employes e ON e.id=d.employe_id
            WHERE d.id=? FOR UPDATE
            """, exitId);
        if (exits.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Dossier de sortie introuvable.");
        Map<String,Object> exit = exits.get(0);
        if (!List.of("SOUMISE_RH", "EN_PREPARATION").contains(exit.get("statut_code"))) throw conflict("Cette demande ne peut plus être préparée.");
        jdbc.update("DELETE FROM lignes_solde_sortie WHERE dossier_sortie_id=?", exitId);
        LocalDate lastDay = (LocalDate) exit.get("dernier_jour_travaille");
        LocalDate contractStart = (LocalDate) exit.get("contrat_debut");
        Integer employeeId = ((Number) exit.get("employe_id")).intValue();
        Map<String,Object> cutoff = jdbc.queryForMap("""
            SELECT MAX(p.date_fin) AS date_fin FROM bulletins_paie b JOIN periodes_paie p ON p.id=b.periode_id
            JOIN statuts_bulletin_paie sb ON sb.id=b.statut_id
            WHERE b.employe_id=? AND p.date_fin<? AND sb.code='PAYE'
            """, employeeId, lastDay.plusDays(1));
        LocalDate lastPaid = (LocalDate) cutoff.get("date_fin");
        LocalDate salaryFrom = lastPaid == null ? contractStart : lastPaid.plusDays(1);
        if (salaryFrom.isBefore(contractStart)) salaryFrom = contractStart;
        if (!salaryFrom.isAfter(lastDay)) {
            BigDecimal days = BigDecimal.valueOf(ChronoUnit.DAYS.between(salaryFrom, lastDay) + 1);
            BigDecimal daily = decimal(exit.get("salaire_base_mensuel")).divide(BigDecimal.valueOf(30), 4, RoundingMode.HALF_UP);
            addLine(exitId, "SALAIRE_DU", "Salaire de base restant dû (brut)", days, daily,
                    days.multiply(daily).setScale(0, RoundingMode.HALF_UP),
                    "Base mensuelle ÷ 30 × jours calendaires du " + salaryFrom + " au " + lastDay
                    + ". Dernière période de paie PAYE terminée le " + (lastPaid == null ? "aucune" : lastPaid));
        }
        BigDecimal leave = jdbc.queryForObject("""
            SELECT COALESCE(SUM(ms.jours),0) FROM mouvements_solde_conge ms
            JOIN types_conge tc ON tc.id=ms.type_conge_id
            WHERE ms.employe_id=? AND tc.code='CONGES_PAYES'
            """, BigDecimal.class, employeeId);
        if (leave != null && leave.signum() > 0) {
            BigDecimal daily = decimal(exit.get("salaire_base_mensuel")).divide(BigDecimal.valueOf(30), 4, RoundingMode.HALF_UP);
            addLine(exitId, "CONGES_NON_PRIS", "Indemnité compensatrice de congés non pris (à vérifier)", leave,
                    daily, leave.multiply(daily).setScale(0, RoundingMode.HALF_UP),
                    "Solde issu du journal des mouvements CONGES_PAYES; contrôler les droits acquis et les périodes avant validation.");
        }
        // Les autres éléments (préavis, indemnités, retenues et cotisations) sont ajoutés
        // uniquement à partir d'une décision/document source vérifiable par RH.
        jdbc.update("UPDATE dossiers_sortie SET statut_id=(SELECT id FROM statuts_sortie WHERE code='STC_A_VERIFIER'), stc_prepare_at=CURRENT_TIMESTAMP, updated_at=CURRENT_TIMESTAMP WHERE id=?", exitId);
        logStatus(exitId, "EN_PREPARATION", "STC_A_VERIFIER", actorId, "Brouillon du STC préparé depuis paie et registre de congés; éléments légaux complémentaires à vérifier.");
        notifyInitiator(exitId, "Votre dossier de sortie est en préparation du décompte STC.", "/espace/manager/equipe/sorties");
        return detail(exitId);
    }

    @Transactional
    public Map<String,Object> validate(Integer actorId, Integer exitId, BigDecimal netAmount, String netJustification) {
        requireRole(actorId, "rh");
        Map<String,Object> row = detail(exitId);
        if (!"STC_A_VERIFIER".equals(row.get("statutCode"))) throw conflict("Préparez d’abord le décompte du STC.");
        if (netAmount == null || netAmount.signum() < 0 || netJustification == null || netJustification.isBlank()) throw bad("Saisissez le net valide et sa base de verification.");
        Integer missing = jdbc.queryForObject("""
            SELECT COUNT(*) FROM lignes_solde_sortie WHERE dossier_sortie_id=? AND (montant IS NULL OR detail IS NULL OR BTRIM(detail)='')
            """, Integer.class, exitId);
        if (missing != null && missing > 0) throw conflict("Chaque ligne du STC doit avoir un montant et une justification.");
        jdbc.update("UPDATE dossiers_sortie SET statut_id=(SELECT id FROM statuts_sortie WHERE code='STC_VALIDEE'),valide_par_id=?,validee_at=CURRENT_TIMESTAMP,montant_net_valide=?,net_verifie_par_id=?,net_verifie_at=CURRENT_TIMESTAMP,controle_net_detail=?,updated_at=CURRENT_TIMESTAMP WHERE id=?", actorId, netAmount, actorId, netJustification.trim(), exitId);
        logStatus(exitId, "STC_A_VERIFIER", "STC_VALIDEE", actorId, "Décompte contrôlé par RH; paiement et remise des pièces restent à confirmer.");
        notifyInitiator(exitId, "Le décompte STC a été contrôlé par les RH.", "/espace/manager/equipe/sorties");
        return detail(exitId);
    }

    @Transactional
    public Map<String,Object> confirmPayment(Integer actorId, Integer exitId, String reference, LocalDate date, BigDecimal amount) {
        requireRole(actorId, "rh");
        if (reference == null || reference.isBlank() || reference.length() > 120 || date == null || amount == null || amount.signum() < 0) throw bad("La date, la reference et le montant paye sont obligatoires.");
        Map<String,Object> row = detail(exitId);
        if (!"STC_VALIDEE".equals(row.get("statutCode"))) throw conflict("Le STC doit être validé avant l’enregistrement du paiement.");
        BigDecimal approvedNet = decimal(row.get("montantNetValide"));
        if (approvedNet.compareTo(amount) != 0) throw conflict("Le montant paye doit correspondre au net valide: " + approvedNet + " Ar.");
        jdbc.update("UPDATE dossiers_sortie SET statut_id=(SELECT id FROM statuts_sortie WHERE code='STC_PAYE'),paiement_reference=?,paiement_date=?,paiement_confirme_par_id=?,updated_at=CURRENT_TIMESTAMP WHERE id=?", reference.trim(), date, actorId, exitId);
        logStatus(exitId, "STC_VALIDEE", "STC_PAYE", actorId, "Paiement enregistré : " + reference.trim());
        notifyInitiator(exitId, "Le paiement du solde de sortie a été enregistré.", "/espace/manager/equipe/sorties");
        return detail(exitId);
    }

    @Transactional
    public Map<String,Object> generateExitDocuments(Integer actorId, Integer exitId) {
        requireRole(actorId, "rh");
        Map<String,Object> row = detail(exitId);
        if (!"STC_PAYE".equals(row.get("statutCode"))) throw conflict("Les documents sont generes apres validation et paiement du STC.");
        Integer employeeId = ((Number) row.get("employeId")).intValue();
        Map<String,Object> identity = jdbc.queryForList("""
            SELECT e.matricule,COALESCE(u.full_name,u.username) AS nom,c.numero AS contrat,p.intitule AS poste,
                   dep.nom AS departement,c.date_debut AS debut,c.date_fin AS fin
            FROM dossiers_sortie ds JOIN employes e ON e.id=ds.employe_id JOIN users u ON u.id=e.user_id
            JOIN contrats c ON c.id=ds.contrat_id JOIN postes p ON p.id=e.poste_id JOIN departements dep ON dep.id=p.departement_id
            WHERE ds.id=?
            """, exitId).stream().findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Dossier de sortie introuvable."));
        String matricule = safe(identity.get("matricule"));
        String certName = "certificat-travail-" + matricule + ".pdf";
        Integer certificateId = saveEmployeePdf(employeeId, actorId, exitId, "CERTIFICAT_TRAVAIL", certName, List.of(
            "CERTIFICAT DE TRAVAIL", "", "Je certifie que " + safe(identity.get("nom")) + ", matricule " + matricule + ",",
            "a exerce les fonctions de " + safe(identity.get("poste")) + " au departement " + safe(identity.get("departement")) + ".",
            "Periode d'emploi : du " + identity.get("debut") + " au " + row.get("dernierJourTravaille") + ".",
            "Contrat de reference : " + safe(identity.get("contrat")) + ".", "",
            "Certificat genere le " + LocalDate.now() + " et remis a l'interesse pour servir et valoir ce que de droit."));
        List<String> text = new java.util.ArrayList<>();
        text.add("DECOMPTE DE SORTIE ET SOLDE DE TOUT COMPTE");
        text.add("Dossier " + exitId + " | Contrat " + safe(identity.get("contrat")) + " | Matricule " + matricule);
        text.add("Salarie : " + safe(identity.get("nom")) + " | Dernier jour : " + row.get("dernierJourTravaille"));
        text.add(""); text.add("ELEMENTS CONTROLES (montants en Ariary)");
        for (Map<String,Object> line : (List<Map<String,Object>>) row.get("lignesStc"))
            text.add(safe(line.get("libelle")) + " : " + line.get("montant") + " Ar - " + safe(line.get("detail")));
        text.add(""); text.add("Total brut provisoire des gains : " + row.get("totalStcBrut") + " Ar");
        text.add("Retenues documentees : " + row.get("totalRetenuesProvisoires") + " Ar");
        text.add("NET VALIDE PAR LES RH : " + row.get("montantNetValide") + " Ar");
        text.add("Base de controle RH : " + safe(row.get("controleNetDetail")));
        text.add("Paiement declare : " + row.get("datePaiement") + " | Reference : " + safe(row.get("referencePaiement")));
        text.add(""); text.add("Les justificatifs, la validation RH et la reference de paiement sont conserves au dossier.");
        Integer stcId = saveEmployeePdf(employeeId, actorId, exitId, "DECOMPTE_STC", "solde-tout-compte-" + matricule + ".pdf", text);
        jdbc.update("UPDATE dossiers_sortie SET stc_document_employe_id=?,certificat_document_employe_id=?,updated_at=CURRENT_TIMESTAMP WHERE id=?", stcId, certificateId, exitId);
        return detail(exitId);
    }

    private Integer saveEmployeePdf(Integer employeeId, Integer actorId, Integer exitId, String code, String fileName, List<String> lines) {
        byte[] bytes = renderExitPdf(lines);
        EmployeeDocumentStorageService.StoredDocument stored = storage.storeGeneratedPdf(employeeId, fileName, bytes);
        Integer typeId = lookup("types_documents_employe", code);
        Integer documentId = jdbc.queryForList("SELECT id FROM documents_employe WHERE employe_id=? AND type_document_id=? ORDER BY id DESC LIMIT 1", Integer.class, employeeId, typeId).stream().findFirst().orElse(null);
        if (documentId == null) documentId = jdbc.queryForObject("INSERT INTO documents_employe(employe_id,type_document_id,ajoute_par_id) VALUES(?,?,?) RETURNING id", Integer.class, employeeId, typeId, actorId);
        Integer version = jdbc.queryForObject("SELECT COALESCE(MAX(numero_version),0)+1 FROM versions_documents_employe WHERE document_id=?", Integer.class, documentId);
        jdbc.update("INSERT INTO versions_documents_employe(document_id,numero_version,cle_stockage,nom_fichier,type_mime,taille_octets,date_emission,ajoute_par_id) VALUES(?,?,?,?,?,?,CURRENT_DATE,?)", documentId, version, stored.key(), stored.fileName(), stored.mimeType(), stored.size(), actorId);
        Integer exitTypeId = lookup("types_documents_sortie", code);
        jdbc.update("INSERT INTO documents_sortie(dossier_sortie_id,type_document_id,document_employe_id,genere_par_id) VALUES(?,?,?,?) ON CONFLICT(dossier_sortie_id,type_document_id) DO UPDATE SET document_employe_id=EXCLUDED.document_employe_id,genere_par_id=EXCLUDED.genere_par_id,genere_at=CURRENT_TIMESTAMP,remis_at=NULL", exitId, exitTypeId, documentId, actorId);
        return documentId;
    }

    private byte[] renderExitPdf(List<String> lines) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDPage page = new PDPage(PDRectangle.A4); document.addPage(page);
            PDPageContentStream stream = new PDPageContentStream(document, page);
            float y = 780;
            for (int i = 0; i < lines.size(); i++) {
                for (String line : wrap(ascii(lines.get(i)), i == 0 ? 82 : 112)) {
                    if (y < 55) { stream.close(); page = new PDPage(PDRectangle.A4); document.addPage(page); stream = new PDPageContentStream(document, page); y = 780; }
                    stream.beginText(); stream.setFont(i == 0 ? bold : regular, i == 0 ? 14 : 9); stream.newLineAtOffset(48, y); stream.showText(line); stream.endText();
                    y -= i == 0 ? 30 : 15;
                }
            }
            stream.close(); document.save(output); return output.toByteArray();
        } catch (IOException exception) { throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Le PDF de sortie n'a pas pu etre genere."); }
    }

    private static String ascii(Object value) {
        return Normalizer.normalize(value == null ? "" : value.toString(), Normalizer.Form.NFD).replaceAll("\\p{M}+", "").replaceAll("[^\\x20-\\x7E]", " ");
    }

    private static List<String> wrap(String text, int maxLength) {
        if (text.isBlank()) return List.of("");
        List<String> result = new java.util.ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split("\\s+")) {
            if (line.length() > 0 && line.length() + 1 + word.length() > maxLength) {
                result.add(line.toString());
                line.setLength(0);
            }
            while (word.length() > maxLength) {
                if (line.length() > 0) { result.add(line.toString()); line.setLength(0); }
                result.add(word.substring(0, maxLength));
                word = word.substring(maxLength);
            }
            if (line.length() > 0) line.append(' ');
            line.append(word);
        }
        if (line.length() > 0) result.add(line.toString());
        return result;
    }

    @Transactional
    public Map<String,Object> markDocumentsRemitted(Integer actorId, Integer exitId) {
        requireRole(actorId, "rh");
        Map<String,Object> row = detail(exitId);
        if (!"STC_PAYE".equals(row.get("statutCode"))) throw conflict("Le STC doit être payé avant la remise des pièces de sortie.");
        Integer missing = jdbc.queryForObject("""
            SELECT COUNT(*) FROM types_documents_sortie t WHERE t.obligatoire=TRUE AND NOT EXISTS
            (SELECT 1 FROM documents_sortie d WHERE d.dossier_sortie_id=? AND d.type_document_id=t.id AND d.document_employe_id IS NOT NULL)
            """, Integer.class, exitId);
        if (missing != null && missing > 0) throw conflict("Générez et rattachez tous les documents de sortie obligatoires avant de confirmer leur remise.");
        jdbc.update("UPDATE documents_sortie SET remis_at=COALESCE(remis_at,CURRENT_TIMESTAMP) WHERE dossier_sortie_id=?", exitId);
        jdbc.update("UPDATE dossiers_sortie SET statut_id=(SELECT id FROM statuts_sortie WHERE code='DOCUMENTS_REMIS'),updated_at=CURRENT_TIMESTAMP WHERE id=?", exitId);
        logStatus(exitId, "STC_PAYE", "DOCUMENTS_REMIS", actorId, "Remise des documents de sortie confirmée par RH.");
        return detail(exitId);
    }

    @Transactional
    public Map<String,Object> finalizeExit(Integer actorId, Integer exitId) {
        requireRole(actorId, "rh");
        Map<String,Object> row = detail(exitId);
        if (!"DOCUMENTS_REMIS".equals(row.get("statutCode"))) throw conflict("Le paiement et la remise des documents doivent être confirmés avant la clôture.");
        Integer outstanding = jdbc.queryForObject("""
            SELECT COUNT(*) FROM restitutions_materiel r JOIN statuts_restitution_materiel s ON s.id=r.statut_restitution_id
            WHERE r.dossier_sortie_id=? AND s.code NOT IN ('RESTITUE','NON_CONCERNE')
            """, Integer.class, exitId);
        if (outstanding != null && outstanding > 0) throw conflict("Du matériel reste à restituer ou à régulariser.");
        Integer employeeId = ((Number) row.get("employeId")).intValue();
        Integer contractId = ((Number) row.get("contratId")).intValue();
        LocalDate lastDay = (LocalDate) row.get("dernierJourTravaille");
        jdbc.update("UPDATE contrats SET statut_id=(SELECT id FROM statuts_contrat WHERE code='TERMINE'),date_fin=?,updated_at=CURRENT_TIMESTAMP WHERE id=?", lastDay, contractId);
        jdbc.update("UPDATE situation_administrative_employe SET statut_id=(SELECT id FROM statuts_administratifs_employe WHERE code='SORTI'),date_effet=?,motif='Sortie finalisée après paiement, remise des pièces et contrôle du matériel',modifie_par_id=?,updated_at=CURRENT_TIMESTAMP WHERE employe_id=?", lastDay, actorId, employeeId);
        jdbc.update("INSERT INTO historique_statut_employe(employe_id,statut_id,date_effet,motif,acteur_id) SELECT ?,id,?,'Sortie finalisée après paiement, remise des pièces et contrôle du matériel',? FROM statuts_administratifs_employe WHERE code='SORTI'", employeeId, lastDay, actorId);
        jdbc.update("UPDATE users SET is_suspended=TRUE,updated_at=CURRENT_TIMESTAMP WHERE id=(SELECT user_id FROM employes WHERE id=?)", employeeId);
        jdbc.update("UPDATE dossiers_sortie SET statut_id=(SELECT id FROM statuts_sortie WHERE code='FINALISEE'),sortie_finalisee_at=CURRENT_TIMESTAMP,updated_at=CURRENT_TIMESTAMP WHERE id=?", exitId);
        logStatus(exitId, "DOCUMENTS_REMIS", "FINALISEE", actorId, "Sortie clôturée; contrat terminé et accès suspendu.");
        notifications.notifyUser(jdbc.queryForObject("SELECT user_id FROM employes WHERE id=?", Integer.class, employeeId), "SORTIE_FINALISEE", "Votre départ est finalisé", "Votre contrat est terminé et les documents de sortie sont archivés.", "/espace/profil");
        notifyInitiator(exitId, "Le dossier de sortie est clôturé.", "/espace/manager/equipe/sorties");
        return detail(exitId);
    }

    @Transactional
    public Map<String,Object> cancel(Integer actorId, Integer exitId, String motif) {
        requireRole(actorId, "rh");
        if (motif == null || motif.isBlank()) throw bad("Un motif d’annulation est obligatoire.");
        Map<String,Object> current = detail(exitId);
        if (List.of("FINALISEE", "ANNULEE").contains(current.get("statutCode"))) throw conflict("Cette sortie est déjà finalisée ou annulée.");
        if (List.of("STC_VALIDEE", "STC_PAYE", "DOCUMENTS_REMIS").contains(current.get("statutCode"))) throw conflict("Une sortie dont le décompte est validé ne peut plus être annulée automatiquement. Faites corriger le dossier par RH avec une régularisation tracée.");
        jdbc.update("UPDATE dossiers_sortie SET statut_id=(SELECT id FROM statuts_sortie WHERE code='ANNULEE'),motif_annulation=?,updated_at=CURRENT_TIMESTAMP WHERE id=?", motif.trim(), exitId);
        logStatus(exitId, (String)current.get("statutCode"), "ANNULEE", actorId, motif.trim());
        return detail(exitId);
    }

    private String query(String scope) { return """
        SELECT d.id,d.employe_id AS "employeId",e.matricule,COALESCE(u.full_name,u.username) AS "nomEmploye",
               d.contrat_id AS "contratId",c.numero AS "numeroContrat",m.code AS "motifCode",m.libelle AS motif,
               st.code AS "statutCode",st.libelle AS statut,d.date_notification AS "dateNotification",
               d.dernier_jour_travaille AS "dernierJourTravaille",d.commentaire,
               COALESCE((SELECT SUM(l.montant) FROM lignes_solde_sortie l JOIN types_ligne_solde_sortie t ON t.id=l.type_ligne_id JOIN categories_rubrique_paie c ON c.id=t.nature_id WHERE l.dossier_sortie_id=d.id AND c.code='GAIN'),0) AS "totalStcBrut",
               COALESCE((SELECT SUM(l.montant) FROM lignes_solde_sortie l JOIN types_ligne_solde_sortie t ON t.id=l.type_ligne_id JOIN categories_rubrique_paie c ON c.id=t.nature_id WHERE l.dossier_sortie_id=d.id AND c.code='RETENUE_SALARIALE'),0) AS "totalRetenuesProvisoires",
               d.paiement_reference AS "referencePaiement",d.paiement_date AS "datePaiement",
               d.montant_net_valide AS "montantNetValide",d.controle_net_detail AS "controleNetDetail",
               (SELECT COUNT(*) FROM documents_sortie ds JOIN types_documents_sortie td ON td.id=ds.type_document_id WHERE ds.dossier_sortie_id=d.id AND td.obligatoire=TRUE AND ds.document_employe_id IS NOT NULL) AS "documentsGeneres",
               d.created_at AS "creeAt"
        FROM dossiers_sortie d JOIN employes e ON e.id=d.employe_id JOIN users u ON u.id=e.user_id
        JOIN contrats c ON c.id=d.contrat_id JOIN motifs_sortie m ON m.id=d.motif_id JOIN statuts_sortie st ON st.id=d.statut_id
        JOIN postes p ON p.id=e.poste_id WHERE TRUE
        """ + scope + " ORDER BY d.created_at DESC,d.id DESC"; }

    private Map<String,Object> detail(Integer id) {
        List<Map<String,Object>> rows = jdbc.queryForList("SELECT * FROM (" + query("") + ") q WHERE id=?", id);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Dossier de sortie introuvable.");
        Map<String,Object> data = rows.get(0);
        data.put("lignesStc", jdbc.queryForList("SELECT t.code,l.libelle,l.quantite,l.montant_unitaire AS \"montantUnitaire\",l.montant,l.detail FROM lignes_solde_sortie l JOIN types_ligne_solde_sortie t ON t.id=l.type_ligne_id WHERE l.dossier_sortie_id=? ORDER BY t.ordre,l.id", id));
        data.put("historique", jdbc.queryForList("""
            SELECT h.created_at AS date,h.commentaire,COALESCE(u.full_name,u.username,'Système') AS acteur,COALESCE(s.libelle,'Créée') AS statut
            FROM historique_sortie h LEFT JOIN users u ON u.id=h.acteur_id LEFT JOIN statuts_sortie s ON s.id=h.nouveau_statut_id
            WHERE h.dossier_sortie_id=? ORDER BY h.id
            """, id));
        return data;
    }

    private void addLine(Integer id,String code,String label,BigDecimal quantity,BigDecimal rate,BigDecimal amount,String detail) {
        jdbc.update("INSERT INTO lignes_solde_sortie(dossier_sortie_id,type_ligne_id,libelle,quantite,montant_unitaire,montant,detail) SELECT ?,id,?,?,?,?,? FROM types_ligne_solde_sortie WHERE code=?",id,label,quantity,rate,amount,detail,code);
    }
    private void logStatus(Integer id,String from,String to,Integer actor,String note) {
        jdbc.update("INSERT INTO historique_sortie(dossier_sortie_id,ancien_statut_id,nouveau_statut_id,acteur_id,commentaire) SELECT ?,a.id,b.id,?,? FROM (SELECT id FROM statuts_sortie WHERE code IS NOT DISTINCT FROM ?) a FULL JOIN (SELECT id FROM statuts_sortie WHERE code=?) b ON TRUE", id, actor, note, from, to);
    }
    private void notifyInitiator(Integer exitId,String message,String link) {
        Map<String,Object> initiator=jdbc.queryForMap("""
            SELECT d.initie_par_id AS user_id,m.code AS motif,
                   (SELECT h.commentaire FROM historique_sortie h WHERE h.dossier_sortie_id=d.id ORDER BY h.id LIMIT 1) AS origine
            FROM dossiers_sortie d JOIN motifs_sortie m ON m.id=d.motif_id WHERE d.id=?
            """,exitId);
        Integer userId=((Number)initiator.get("user_id")).intValue();
        String origin=String.valueOf(initiator.get("origine"));
        String destination=switch(String.valueOf(initiator.get("motif"))){
            case "DEMISSION" -> origin.toLowerCase(java.util.Locale.ROOT).contains("manager")?"/espace/manager/equipe/sorties":"/espace/employe/contrat";
            case "LICENCIEMENT" -> "/espace/rh/sorties";
            default -> link;
        };
        notifications.notifyUser(userId,"DOSSIER_SORTIE","Mise à jour d’un dossier de sortie",message,destination);
    }
    private boolean exists(String sql,Object... args) { return Boolean.TRUE.equals(jdbc.queryForObject(sql,Boolean.class,args)); }
    private static String safe(Object value) { return value == null ? "-" : value.toString(); }
    private int lookup(String table,String code) { return jdbc.queryForObject("SELECT id FROM " + table + " WHERE code=?",Integer.class,code); }
    private void requireRole(Integer user,String role) { if(!exists("SELECT EXISTS(SELECT 1 FROM user_roles ur JOIN roles r ON r.id=ur.role_id JOIN users u ON u.id=ur.user_id JOIN statuts_validation_compte v ON v.id=u.administrative_validation_status_id WHERE u.id=? AND u.is_suspended=FALSE AND v.code='APPROVED' AND LOWER(r.role_name)=?)",user,role)) throw forbidden("Accès réservé à un compte actif du rôle " + role + "."); }
    private void requireEmployeeRole(Integer user) { if(!exists("SELECT EXISTS(SELECT 1 FROM user_roles ur JOIN roles r ON r.id=ur.role_id JOIN users u ON u.id=ur.user_id JOIN statuts_validation_compte v ON v.id=u.administrative_validation_status_id WHERE u.id=? AND u.is_suspended=FALSE AND v.code='APPROVED' AND LOWER(r.role_name) IN ('employe','manager'))",user)) throw forbidden("Accès réservé au salarié concerné."); }
    private static BigDecimal decimal(Object value) { return value instanceof BigDecimal b ? b : new BigDecimal(value.toString()); }
    private ResponseStatusException bad(String m) { return new ResponseStatusException(HttpStatus.BAD_REQUEST,m); }
    private ResponseStatusException forbidden(String m) { return new ResponseStatusException(HttpStatus.FORBIDDEN,m); }
    private ResponseStatusException conflict(String m) { return new ResponseStatusException(HttpStatus.CONFLICT,m); }
}
