package com.sirh.backend.modules.paie.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Payroll calculation and period lifecycle. Values and dated rates always come from PostgreSQL. */
@Service
public class PayrollProcessingService {
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private final JdbcTemplate jdbc;

    public PayrollProcessingService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> periods(Integer actorId) {
        requireRh(actorId);
        return jdbc.queryForList("""
            SELECT p.id, p.date_debut AS "dateDebut", p.date_fin AS "dateFin", s.code AS "statutCode",
                   s.libelle AS statut, p.created_at AS "creeAt", p.valide_at AS "valideAt", p.cloture_at AS "clotureAt",
                   COUNT(b.id)::integer AS "nombreBulletins", COALESCE(SUM(b.salaire_brut),0) AS "totalBrut",
                   COALESCE(SUM(b.net_a_payer),0) AS "totalNet", COALESCE(SUM(b.total_charges_patronales),0) AS "totalCharges"
            FROM periodes_paie p JOIN statuts_periode_paie s ON s.id=p.statut_id
            LEFT JOIN bulletins_paie b ON b.periode_id=p.id
            GROUP BY p.id,s.code,s.libelle ORDER BY p.date_debut DESC,p.id DESC
            """);
    }

    @Transactional
    public Map<String, Object> createPeriod(Integer actorId, LocalDate start, LocalDate end) {
        requireRh(actorId);
        if (start == null || end == null || end.isBefore(start)) throw bad("Choisissez une période avec des dates cohérentes.");
        if (!start.equals(start.withDayOfMonth(1)) || !end.equals(end.withDayOfMonth(end.lengthOfMonth())))
            throw bad("Une période de paie doit couvrir un mois civil complet.");
        Integer overlap = jdbc.queryForObject("""
            SELECT COUNT(*) FROM periodes_paie WHERE daterange(date_debut,date_fin,'[]') && daterange(?,?,'[]')
            """, Integer.class, start, end);
        if (overlap != null && overlap > 0) throw conflict("Une période de paie existe déjà pour tout ou partie de ce mois.");
        Integer id = jdbc.queryForObject("""
            INSERT INTO periodes_paie(date_debut,date_fin,statut_id,cree_par_id)
            SELECT ?,?,id,? FROM statuts_periode_paie WHERE code='BROUILLON' RETURNING id
            """, Integer.class, start, end, actorId);
        attachPendingSanctions(id, start, end);
        return period(actorId, id);
    }

    void attachPendingSanctions(int periodId, LocalDate start, LocalDate end) {
        List<Map<String, Object>> pending = jdbc.queryForList("""
            SELECT rs.id AS "retenueId", rs.sanction_id AS "sanctionId", i.employe_id AS "employeId",
                   rs.date_debut AS "dateDebut", rs.date_fin AS "dateFin", rs.nombre_jours AS "nombreJours",
                   rs.taux_journalier AS "tauxJournalier", rs.montant, s.decide_par_id AS "decideParId"
            FROM retenues_sanction rs
            JOIN sanctions s ON s.id = rs.sanction_id
            JOIN procedures_disciplinaires pr ON pr.id = s.procedure_id
            JOIN incidents i ON i.id = pr.incident_id
            WHERE rs.variable_paie_id IS NULL AND rs.date_debut >= ? AND rs.date_fin <= ?
            ORDER BY rs.id
            """, start, end);
        for (Map<String, Object> row : pending) {
            BigDecimal montant = decimal(row.get("montant"));
            BigDecimal taux = decimal(row.get("tauxJournalier"));
            int jours = ((Number) row.get("nombreJours")).intValue();
            String source = "Mise à pied #" + row.get("sanctionId") + " du " + row.get("dateDebut")
                + " au " + row.get("dateFin") + " : " + jours + " j × " + taux.toPlainString()
                + " Ar (reprise à l’ouverture de la période)";
            Integer variableId = jdbc.queryForObject("""
                INSERT INTO variables_paie
                    (periode_id, employe_id, rubrique_id, quantite, montant_unitaire, montant,
                     source_detail, statut_id, saisi_par_id, valide_par_id, valide_at,
                     motif_decision, sanction_id)
                SELECT ?, ?, r.id, ?, ?, ?, ?, sv.id, ?, ?, CURRENT_TIMESTAMP, ?, ?
                FROM rubriques_paie r, statuts_variable_paie sv
                WHERE r.code = 'RETENUE_MISE_A_PIED' AND sv.code = 'VALIDEE'
                RETURNING id
                """, Integer.class, periodId, row.get("employeId"), jours, taux, montant, source,
                row.get("decideParId"), row.get("decideParId"),
                "Retenue automatique - sanction #" + row.get("sanctionId"), row.get("sanctionId"));
            jdbc.update("UPDATE retenues_sanction SET variable_paie_id = ? WHERE id = ?",
                variableId, row.get("retenueId"));
        }
    }

    @Transactional(readOnly = true)
    public Map<String,Object> variableOptions(Integer actorId,Integer periodId){
        requireRh(actorId);Map<String,Object> p=period(actorId,periodId);
        if(!List.of("BROUILLON","CALCULEE").contains(p.get("statutCode")))throw conflict("La période est déjà validée.");
        LocalDate start=localDate(p.get("dateDebut")),end=localDate(p.get("dateFin"));
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("employees",jdbc.queryForList("""
            SELECT DISTINCT e.id, e.matricule, COALESCE(u.full_name,u.username) AS nom
            FROM employes e JOIN users u ON u.id=e.user_id JOIN candidatures ca ON ca.candidat_id=e.user_id
            JOIN contrats c ON c.candidature_id=ca.id JOIN statuts_contrat sc ON sc.id=c.statut_id AND sc.code='ACTIF'
            WHERE c.date_debut<=? AND (c.date_fin IS NULL OR c.date_fin>=?) ORDER BY e.matricule
            """,end,start));
        result.put("rubriques",jdbc.queryForList("""
            SELECT r.id,r.code,r.libelle,c.code AS categorie FROM rubriques_paie r
            JOIN categories_rubrique_paie c ON c.id=r.categorie_id
            JOIN modes_calcul_rubrique_paie m ON m.id=r.mode_calcul_id
            WHERE r.actif=TRUE AND r.saisie_manuelle_autorisee=TRUE AND r.date_debut<=?
              AND (r.date_fin IS NULL OR r.date_fin>=?) AND c.code IN ('GAIN','RETENUE_SALARIALE')
            ORDER BY c.code,r.libelle
            """,end,start));
        result.put("variables",jdbc.queryForList("""
            SELECT v.id,e.matricule,COALESCE(u.full_name,u.username) AS nom,r.libelle AS rubrique,
              c.code AS categorie,v.montant,v.source_detail AS "sourceDetail",s.code AS "statutCode",s.libelle AS statut,
              v.saisi_par_id AS "saisiParId",v.motif_decision AS "motifDecision"
            FROM variables_paie v JOIN employes e ON e.id=v.employe_id JOIN users u ON u.id=e.user_id
            JOIN rubriques_paie r ON r.id=v.rubrique_id JOIN categories_rubrique_paie c ON c.id=r.categorie_id
            JOIN statuts_variable_paie s ON s.id=v.statut_id WHERE v.periode_id=? ORDER BY v.id DESC
            """,periodId));
        return result;
    }

    @Transactional
    public Map<String,Object> createVariable(Integer actorId,Integer periodId,Integer employeeId,Integer rubricId,BigDecimal amount,String source){
        requireRh(actorId);Map<String,Object> p=lockPeriod(periodId);
        if(!"BROUILLON".equals(p.get("statutCode")))throw conflict("Les variables doivent être saisies avant le calcul de la période.");
        if(employeeId==null||rubricId==null||amount==null||amount.signum()<=0)throw bad("Sélectionnez un salarié, une rubrique et un montant positif.");
        if(source==null||source.trim().length()<8||source.trim().length()>1000)throw bad("Ajoutez une justification de 8 à 1000 caractères.");
        LocalDate start=localDate(p.get("dateDebut")),end=localDate(p.get("dateFin"));
        Integer eligible=jdbc.queryForObject("""
            SELECT COUNT(*) FROM employes e JOIN candidatures ca ON ca.candidat_id=e.user_id JOIN contrats c ON c.candidature_id=ca.id
            JOIN statuts_contrat sc ON sc.id=c.statut_id AND sc.code='ACTIF'
            WHERE e.id=? AND c.date_debut<=? AND (c.date_fin IS NULL OR c.date_fin>=?)
            """,Integer.class,employeeId,end,start);
        if(eligible==null||eligible==0)throw bad("Le salarié n’a pas de contrat actif sur cette période.");
        Integer manual=jdbc.queryForObject("""
            SELECT COUNT(*) FROM rubriques_paie r JOIN categories_rubrique_paie c ON c.id=r.categorie_id
            WHERE r.id=? AND r.actif=TRUE AND r.saisie_manuelle_autorisee=TRUE AND c.code IN ('GAIN','RETENUE_SALARIALE')
            """,Integer.class,rubricId);
        if(manual==null||manual==0)throw bad("Cette rubrique ne permet pas de saisie manuelle.");
        jdbc.update("""
            INSERT INTO variables_paie(periode_id,employe_id,rubrique_id,quantite,montant_unitaire,montant,source_detail,statut_id,saisi_par_id)
            SELECT ?,?,?,1,?,?,?,id,? FROM statuts_variable_paie WHERE code='A_VALIDER'
            """,periodId,employeeId,rubricId,amount,amount,source.trim(),actorId);
        return variableOptions(actorId,periodId);
    }

    @Transactional
    public Map<String,Object> decideVariable(Integer actorId,Integer periodId,Integer variableId,boolean approve,String reason){
        requireRh(actorId);Map<String,Object> p=lockPeriod(periodId);
        if(!"BROUILLON".equals(p.get("statutCode")))throw conflict("La période doit rester en brouillon pour traiter les variables.");
        if(reason==null||reason.isBlank()||reason.trim().length()>500)throw bad("Indiquez le motif de la décision (500 caractères maximum).");
        Map<String,Object> v=jdbc.query("SELECT saisi_par_id,statut_id FROM variables_paie WHERE id=? AND periode_id=? FOR UPDATE",rs->{if(!rs.next())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Variable de paie introuvable.");Map<String,Object> m=new LinkedHashMap<>();m.put("saisiParId",rs.getInt("saisi_par_id"));m.put("statutId",rs.getInt("statut_id"));return m;},variableId,periodId);
        if(((Number)v.get("saisiParId")).intValue()==actorId)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Un autre compte RH doit valider la variable saisie.");
        Integer pending=jdbc.queryForObject("SELECT COUNT(*) FROM statuts_variable_paie WHERE id=? AND code='A_VALIDER'",Integer.class,v.get("statutId"));
        if(pending==null||pending==0)throw conflict("Cette variable a déjà été traitée.");
        jdbc.update("""
            UPDATE variables_paie SET statut_id=(SELECT id FROM statuts_variable_paie WHERE code=?),valide_par_id=?,valide_at=CURRENT_TIMESTAMP,motif_decision=? WHERE id=?
            """,approve?"VALIDEE":"REFUSEE",actorId,reason.trim(),variableId);
        return variableOptions(actorId,periodId);
    }

    @Transactional(readOnly = true)
    public Map<String,Object> advanceOptions(Integer actorId,Integer periodId){
        Map<String,Object> options=variableOptions(actorId,periodId);
        options.put("avances",jdbc.queryForList("""
            SELECT a.id,e.matricule,COALESCE(u.full_name,u.username) AS nom,a.date_avance AS "dateAvance",
              a.montant,a.motif,s.code AS "statutCode",s.libelle AS statut,a.saisi_par_id AS "saisiParId"
            FROM avances_salariales a JOIN employes e ON e.id=a.employe_id JOIN users u ON u.id=e.user_id
            JOIN statuts_avance_paie s ON s.id=a.statut_id WHERE a.periode_deduction_id=? ORDER BY a.id DESC
            """,periodId));
        return options;
    }

    @Transactional
    public Map<String,Object> createAdvance(Integer actorId,Integer periodId,Integer employeeId,LocalDate advanceDate,BigDecimal amount,String reason){
        requireRh(actorId);Map<String,Object> p=lockPeriod(periodId);
        if(!"BROUILLON".equals(p.get("statutCode")))throw conflict("Les avances doivent être saisies avant le calcul de la période.");
        if(employeeId==null||advanceDate==null||amount==null||amount.signum()<=0)throw bad("Sélectionnez un salarié, une date et un montant positif.");
        if(reason==null||reason.trim().length()<8||reason.trim().length()>500)throw bad("Indiquez un motif de 8 à 500 caractères.");
        LocalDate start=localDate(p.get("dateDebut")),end=localDate(p.get("dateFin"));
        if(advanceDate.isBefore(start)||advanceDate.isAfter(end))throw bad("La date de l’avance doit appartenir à la période de déduction.");
        Integer eligible=jdbc.queryForObject("""
            SELECT COUNT(*) FROM employes e JOIN candidatures ca ON ca.candidat_id=e.user_id JOIN contrats c ON c.candidature_id=ca.id
            JOIN statuts_contrat sc ON sc.id=c.statut_id AND sc.code='ACTIF'
            WHERE e.id=? AND c.date_debut<=? AND (c.date_fin IS NULL OR c.date_fin>=?)
            """,Integer.class,employeeId,end,start);
        if(eligible==null||eligible==0)throw bad("Le salarié n’a pas de contrat actif sur cette période.");
        jdbc.update("""
            INSERT INTO avances_salariales(employe_id,date_avance,montant,statut_id,periode_deduction_id,saisi_par_id,motif)
            SELECT ?,?,?,id,?,?,? FROM statuts_avance_paie WHERE code='A_VALIDER'
            """,employeeId,advanceDate,amount,periodId,actorId,reason.trim());
        return advanceOptions(actorId,periodId);
    }

    @Transactional
    public Map<String,Object> decideAdvance(Integer actorId,Integer periodId,Integer advanceId,boolean approve,String reason){
        requireRh(actorId);Map<String,Object> p=lockPeriod(periodId);
        if(!"BROUILLON".equals(p.get("statutCode")))throw conflict("La période doit rester en brouillon pour traiter les avances.");
        if(reason==null||reason.isBlank()||reason.trim().length()>500)throw bad("Indiquez un motif de décision.");
        Map<String,Object> a=jdbc.query("SELECT saisi_par_id,statut_id,motif FROM avances_salariales WHERE id=? AND periode_deduction_id=? FOR UPDATE",rs->{if(!rs.next())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Avance salariale introuvable.");Map<String,Object> m=new LinkedHashMap<>();m.put("saisiParId",rs.getInt("saisi_par_id"));m.put("statutId",rs.getInt("statut_id"));m.put("motif",rs.getString("motif"));return m;},advanceId,periodId);
        if(((Number)a.get("saisiParId")).intValue()==actorId)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Un autre compte RH doit traiter l’avance saisie.");
        Integer pending=jdbc.queryForObject("SELECT COUNT(*) FROM statuts_avance_paie WHERE id=? AND code='A_VALIDER'",Integer.class,a.get("statutId"));
        if(pending==null||pending==0)throw conflict("Cette avance a déjà été traitée.");
        jdbc.update("""
            UPDATE avances_salariales SET statut_id=(SELECT id FROM statuts_avance_paie WHERE code=?),valide_par_id=?,valide_at=CURRENT_TIMESTAMP,motif=? WHERE id=?
            """,approve?"VERSEE":"ANNULEE",actorId,a.get("motif")+" | Décision: "+reason.trim(),advanceId);
        return advanceOptions(actorId,periodId);
    }

    @Transactional
    public Map<String, Object> calculate(Integer actorId, Integer periodId) {
        requireRh(actorId);
        Map<String, Object> period = lockPeriod(periodId);
        String status = (String) period.get("statutCode");
        if (!List.of("BROUILLON", "CALCULEE").contains(status)) throw conflict("Cette période est déjà validée et ne peut plus être recalculée.");

        Integer pendingVariables=jdbc.queryForObject("""
            SELECT COUNT(*) FROM variables_paie v JOIN statuts_variable_paie s ON s.id=v.statut_id
            WHERE v.periode_id=? AND s.code='A_VALIDER'
            """,Integer.class,periodId);
        if(pendingVariables!=null&&pendingVariables>0)throw conflict("Traitez toutes les variables en attente avant de calculer la paie.");

        jdbc.update("DELETE FROM lignes_bulletin_paie WHERE bulletin_id IN (SELECT id FROM bulletins_paie WHERE periode_id=?)", periodId);
        jdbc.update("DELETE FROM bulletins_paie WHERE periode_id=?", periodId);
        jdbc.update("UPDATE variables_paie SET statut_id=(SELECT id FROM statuts_variable_paie WHERE code='VALIDEE'), valide_par_id=NULL, valide_at=NULL WHERE periode_id=? AND statut_id=(SELECT id FROM statuts_variable_paie WHERE code='INTEGREE')", periodId);

        LocalDate start = localDate(period.get("dateDebut")), end = localDate(period.get("dateFin"));
        List<Map<String, Object>> employees = jdbc.queryForList("""
            SELECT e.id AS "employeId",e.matricule,COALESCE(u.full_name,u.username) AS nom,p.intitule AS fonction,
                   c.id AS "contratId",c.salaire_base_mensuel AS salaire,c.date_debut AS "contratDebut",c.date_fin AS "contratFin",
                   COALESCE(cat.libelle,'Non catégorisé') AS categorie,COALESCE(reg.code,'GENERAL') AS regimeCode,
                   COALESCE(reg.id,(SELECT id FROM regimes_paie WHERE code='GENERAL')) AS "regimeId"
            FROM employes e JOIN users u ON u.id=e.user_id JOIN postes p ON p.id=e.poste_id
            JOIN candidatures ca ON ca.candidat_id=e.user_id JOIN contrats c ON c.candidature_id=ca.id
            JOIN statuts_contrat sc ON sc.id=c.statut_id AND sc.code='ACTIF'
            LEFT JOIN LATERAL (SELECT ar.regime_id,ar.categorie_id FROM affectations_regime_paie ar
                WHERE ar.employe_id=e.id AND ar.date_debut<=? AND (ar.date_fin IS NULL OR ar.date_fin>=?)
                ORDER BY ar.date_debut DESC,ar.id DESC LIMIT 1) ar ON TRUE
            LEFT JOIN regimes_paie reg ON reg.id=ar.regime_id
            LEFT JOIN categories_professionnelles cat ON cat.id=ar.categorie_id
            WHERE c.date_debut<=? AND (c.date_fin IS NULL OR c.date_fin>=?)
            ORDER BY e.matricule
            """, end, start, end, start);
        if (employees.isEmpty()) throw conflict("Aucun salarié avec un contrat actif ne couvre cette période.");

        for (Map<String, Object> employee : employees) calculateEmployee(actorId, periodId, start, end, employee);
        jdbc.update("UPDATE periodes_paie SET statut_id=(SELECT id FROM statuts_periode_paie WHERE code='CALCULEE') WHERE id=?", periodId);
        return period(actorId, periodId);
    }

    @Transactional
    public Map<String, Object> validate(Integer actorId, Integer periodId) {
        requireRh(actorId);
        Map<String, Object> period = lockPeriod(periodId);
        if (!"CALCULEE".equals(period.get("statutCode"))) throw conflict("Calculez et vérifiez les bulletins avant de valider la période.");
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM bulletins_paie WHERE periode_id=?", Integer.class, periodId);
        if (count == null || count == 0) throw conflict("La période ne contient aucun bulletin à valider.");
        jdbc.update("UPDATE bulletins_paie SET statut_id=(SELECT id FROM statuts_bulletin_paie WHERE code='VALIDE'),valide_par_id=?,valide_at=CURRENT_TIMESTAMP WHERE periode_id=?", actorId, periodId);
        jdbc.update("UPDATE variables_paie SET statut_id=(SELECT id FROM statuts_variable_paie WHERE code='INTEGREE'),valide_par_id=?,valide_at=CURRENT_TIMESTAMP WHERE periode_id=? AND statut_id=(SELECT id FROM statuts_variable_paie WHERE code='VALIDEE')", actorId, periodId);
        jdbc.update("UPDATE avances_salariales SET statut_id=(SELECT id FROM statuts_avance_paie WHERE code='DEDUITE') WHERE periode_deduction_id=? AND statut_id=(SELECT id FROM statuts_avance_paie WHERE code='VERSEE')", periodId);
        jdbc.update("""
            INSERT INTO notifications(destinataire_id,type_id,titre,resume,lien)
            SELECT e.user_id,tn.id,'Bulletin de paie disponible',
              'Votre bulletin de paie pour la période du '||to_char(p.date_debut,'DD/MM/YYYY')||' au '||to_char(p.date_fin,'DD/MM/YYYY')||' est disponible.',
              '/espace/employe/paie'
            FROM bulletins_paie b JOIN periodes_paie p ON p.id=b.periode_id JOIN employes e ON e.id=b.employe_id
            CROSS JOIN types_notifications tn WHERE b.periode_id=? AND tn.code='BULLETIN_PAIE'
            """,periodId);
        jdbc.update("UPDATE periodes_paie SET statut_id=(SELECT id FROM statuts_periode_paie WHERE code='VALIDEE'),valide_par_id=?,valide_at=CURRENT_TIMESTAMP WHERE id=?", actorId, periodId);
        return period(actorId, periodId);
    }

    @Transactional
    public Map<String, Object> markPaid(Integer actorId, Integer periodId,String reference) {
        requireRh(actorId);
        Map<String, Object> period = lockPeriod(periodId);
        if (!"VALIDEE".equals(period.get("statutCode"))) throw conflict("Seule une période validée peut être marquée payée.");
        if(reference==null||reference.trim().length()<3||reference.trim().length()>80)throw bad("Indiquez une référence de paiement de 3 à 80 caractères.");
        jdbc.update("UPDATE bulletins_paie SET statut_id=(SELECT id FROM statuts_bulletin_paie WHERE code='PAYE'),ordre_paiement=? WHERE periode_id=?", reference.trim(),periodId);
        jdbc.update("UPDATE periodes_paie SET statut_id=(SELECT id FROM statuts_periode_paie WHERE code='PAYEE') WHERE id=?", periodId);
        return period(actorId, periodId);
    }

    @Transactional
    public Map<String, Object> close(Integer actorId, Integer periodId) {
        requireRh(actorId);
        Map<String, Object> period = lockPeriod(periodId);
        if (!"PAYEE".equals(period.get("statutCode"))) throw conflict("Marquez la période comme payée avant de la clôturer.");
        jdbc.update("UPDATE periodes_paie SET statut_id=(SELECT id FROM statuts_periode_paie WHERE code='CLOTUREE'),cloture_at=CURRENT_TIMESTAMP WHERE id=?", periodId);
        return period(actorId, periodId);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> periodBulletins(Integer actorId, Integer periodId) {
        requireRh(actorId);
        return jdbc.queryForList("""
            SELECT b.id,b.employe_id AS "employeId",b.matricule_snapshot AS matricule,b.nom_snapshot AS nom,
                   b.fonction_snapshot AS fonction,b.salaire_base_contractuel AS "salaireBase",b.salaire_brut AS brut,
                   b.revenu_imposable AS "revenuImposable",b.total_retenues_salariales AS retenues,
                   b.salaire_net AS "salaireNet",b.total_charges_patronales AS charges,b.net_a_payer AS net,
                   sb.code AS "statutCode",sb.libelle AS statut
            FROM bulletins_paie b JOIN statuts_bulletin_paie sb ON sb.id=b.statut_id
            WHERE b.periode_id=? ORDER BY b.matricule_snapshot
            """, periodId);
    }

    private void calculateEmployee(Integer actorId, Integer periodId, LocalDate start, LocalDate end, Map<String, Object> e) {
        int employeeId = ((Number)e.get("employeId")).intValue();
        int regimeId = ((Number)e.get("regimeId")).intValue();
        BigDecimal contractSalary = decimal(e.get("salaire"));
        LocalDate activeStart = max(start, localDate(e.get("contratDebut")));
        Object contractEndValue = e.get("contratFin");
        LocalDate activeEnd = contractEndValue == null ? end : min(end,localDate(contractEndValue));
        long periodDays = ChronoUnit.DAYS.between(start,end)+1, activeDays=ChronoUnit.DAYS.between(activeStart,activeEnd)+1;
        BigDecimal base = contractSalary.multiply(BigDecimal.valueOf(activeDays)).divide(BigDecimal.valueOf(periodDays),2,RoundingMode.HALF_UP);
        Map<String,Object> regime = jdbc.queryForMap("SELECT id,code FROM regimes_paie WHERE id=?",regimeId);
        BigDecimal referenceHours = parameter("HEURES_MENSUELLES_REFERENCE",regimeId,start,end);
        BigDecimal hourRate = referenceHours.signum()==0 ? ZERO : contractSalary.divide(referenceHours,6,RoundingMode.HALF_UP);
        List<Map<String,Object>> lines = new ArrayList<>();
        addLine(lines,"SALAIRE_BASE","Salaire de base","GAIN",base,BigDecimal.ONE,contractSalary,"Contrat #"+e.get("contratId"),null,null,null,null);

        addVariables(lines,periodId,employeeId,start,end);
        addConfiguredPremiums(lines,employeeId,start,end,contractSalary);
        addUnpaidAbsence(lines,employeeId,start,end,contractSalary);
        addOvertime(lines,employeeId,start,end,hourRate,parameter("HEURES_MENSUELLES_REFERENCE",regimeId,start,end));

        BigDecimal gross=computeGross(lines);
        BigDecimal cnapsBase=gross.min(parameter("SME_MENSUEL",regimeId,start,end).multiply(parameter("CNAPS_PLAFOND_MULTIPLICATEUR",null,start,end)));
        BigDecimal cnapsEmployee=cnapsBase.multiply(parameter("TAUX_CNAPS_SALARIAL",null,start,end)).setScale(0,RoundingMode.HALF_UP);
        BigDecimal healthEmployee=gross.multiply(parameter("TAUX_SANTE_SALARIAL",null,start,end)).setScale(0,RoundingMode.HALF_UP);
        BigDecimal taxable=gross.subtract(cnapsEmployee).subtract(healthEmployee).max(ZERO);
        BigDecimal irsa=calculateIrsa(taxable,start,end);
        BigDecimal cnapsEmployer=cnapsBase.multiply(parameter("TAUX_CNAPS_PATRONAL",regimeId,start,end)).setScale(0,RoundingMode.HALF_UP);
        BigDecimal healthEmployer=gross.multiply(parameter("TAUX_SANTE_PATRONAL",null,start,end)).setScale(0,RoundingMode.HALF_UP);
        addLine(lines,"CNAPS_SALARIAL","Cotisation CNaPS salariale","RETENUE_SALARIALE",cnapsEmployee,BigDecimal.ONE,cnapsBase,"Taux daté de la base, assiette plafonnée à 8 × SME",null,null,null,null);
        addLine(lines,"SANTE_SALARIAL","Cotisation santé salariale","RETENUE_SALARIALE",healthEmployee,BigDecimal.ONE,gross,"Taux daté de la base, assiette brute",null,null,null,null);
        addLine(lines,"IRSA","IRSA","RETENUE_SALARIALE",irsa,BigDecimal.ONE,taxable,"Tranches IRSA actives à la fin de la période",null,null,null,null);
        addLine(lines,"CNAPS_PATRONAL","CNaPS patronale","CHARGE_PATRONALE",cnapsEmployer,BigDecimal.ONE,cnapsBase,"Taux et plafond datés de la base",null,null,null,null);
        addLine(lines,"SANTE_PATRONAL","Cotisation santé patronale","CHARGE_PATRONALE",healthEmployer,BigDecimal.ONE,gross,"Taux daté de la base",null,null,null,null);

        BigDecimal employeeDeductions=sum(lines,"RETENUE_SALARIALE")
            .subtract(sumCode(lines,"RETENUE_ABSENCE")).subtract(sumCode(lines,"RETENUE_MISE_A_PIED")).max(ZERO);
        BigDecimal employerCharges=sum(lines,"CHARGE_PATRONALE");
        BigDecimal net=computeNet(gross,lines);
        BigDecimal advances=deductAdvances(lines,employeeId,periodId);
        BigDecimal baseDuMois=base.subtract(sumCode(lines,"RETENUE_ABSENCE"))
            .subtract(sumCode(lines,"RETENUE_MISE_A_PIED")).max(ZERO);
        if (advances.compareTo(net)>0) throw conflict("Le montant des avances à déduire dépasse le net du salarié "+e.get("matricule")+". Ajustez la déduction avant le calcul.");
        BigDecimal netPayable=net.subtract(advances).max(ZERO);
        int bulletinId=jdbc.queryForObject("""
            INSERT INTO bulletins_paie(periode_id,employe_id,contrat_id,matricule_snapshot,nom_snapshot,categorie_snapshot,
              fonction_snapshot,salaire_base_contractuel,salaire_base_proratise,salaire_brut,revenu_imposable,
              total_retenues_salariales,salaire_net,total_charges_patronales,net_a_payer,statut_id,calcule_par_id)
            SELECT ?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,id,? FROM statuts_bulletin_paie WHERE code='CALCULE' RETURNING id
            """,Integer.class,periodId,employeeId,e.get("contratId"),e.get("matricule"),e.get("nom"),e.get("categorie"),e.get("fonction"),contractSalary,baseDuMois,gross,taxable,employeeDeductions,net,employerCharges,netPayable,actorId);
        for (int i=0;i<lines.size();i++) persistLine(bulletinId, i+1, lines.get(i));
    }

    private void addVariables(List<Map<String,Object>> lines,int periodId,int employeeId,LocalDate start,LocalDate end) {
        List<Map<String,Object>> variables=jdbc.queryForList("""
            SELECT v.id,v.quantite,v.montant_unitaire,v.montant,v.source_detail,v.sanction_id,r.code,r.libelle,c.code AS categorie
            FROM variables_paie v JOIN statuts_variable_paie sv ON sv.id=v.statut_id AND sv.code='VALIDEE'
            JOIN rubriques_paie r ON r.id=v.rubrique_id AND r.actif=TRUE
            JOIN categories_rubrique_paie c ON c.id=r.categorie_id
            WHERE v.periode_id=? AND v.employe_id=? ORDER BY v.id
            """,periodId,employeeId);
        for(Map<String,Object> v:variables){
            if(!List.of("GAIN","RETENUE_SALARIALE").contains(v.get("categorie"))) continue;
            String code=(String)v.get("code");
            if("PRIME_PRODUCTION".equals(code)||"PRIME_QUALITE".equals(code)){
                String capCode="PRIME_PRODUCTION".equals(code)?"PRIME_PRODUCTION_PLAFOND":"PRIME_QUALITE_PLAFOND";
                BigDecimal cap=parameter(capCode,null,start,end);
                if(cap.signum()>0&&decimal(v.get("montant")).compareTo(cap)>0)throw conflict("La variable "+code+" dépasse le plafond daté de "+cap+" Ar.");
            }
            addLine(lines,(String)v.get("code"),(String)v.get("libelle"),(String)v.get("categorie"),decimal(v.get("montant")),decimal(v.get("quantite")),decimal(v.get("montant_unitaire")),(String)v.get("source_detail"),((Number)v.get("id")).intValue(),null,null,null,v.get("sanction_id")==null?null:((Number)v.get("sanction_id")).intValue());
        }
    }

    private void addConfiguredPremiums(List<Map<String,Object>> lines,int employeeId,LocalDate start,LocalDate end,BigDecimal salary){
        Map<String,Object> employee=jdbc.queryForMap("SELECT date_embauche,user_id FROM employes WHERE id=?",employeeId);
        LocalDate hire=localDate(employee.get("date_embauche"));
        int seniorityYears=Math.max(0,Period.between(hire,end).getYears());
        if(seniorityYears>0){
            BigDecimal rate=parameter("PRIME_ANCIENNETE_TAUX",null,start,end);
            BigDecimal amount=salary.multiply(rate).multiply(BigDecimal.valueOf(seniorityYears)).setScale(0,RoundingMode.HALF_UP);
            if(amount.signum()>0)addLine(lines,"PRIME_ANCIENNETE","Prime d’ancienneté","GAIN",amount,BigDecimal.valueOf(seniorityYears),salary,"Ancienneté révolue : "+seniorityYears+" année(s); taux daté en base",null,null,null,null);
        }
        Integer manager=jdbc.queryForObject("SELECT COUNT(*) FROM user_roles ur JOIN roles r ON r.id=ur.role_id WHERE ur.user_id=? AND LOWER(r.role_name)='manager'",Integer.class,employee.get("user_id"));
        if(manager!=null&&manager>0){
            BigDecimal amount=parameter("PRIME_FONCTION_MONTANT",null,start,end);
            if(amount.signum()>0)addLine(lines,"PRIME_FONCTION","Prime de fonction","GAIN",amount,BigDecimal.ONE,amount,"Fonction manager; montant daté en base",null,null,null,null);
        }
        BigDecimal attendancePremium=parameter("PRIME_ASSIDUITE_MENSUELLE",null,start,end);
        if(attendancePremium.signum()>0&&eligibleForAttendancePremium(employeeId,start,end))
            addLine(lines,"PRIME_ASSIDUITE","Prime d’assiduité","GAIN",attendancePremium,BigDecimal.ONE,attendancePremium,"Aucun retard, départ anticipé ni absence déductible; toutes les journées planifiées ont un pointage complet",null,null,null,null);
    }

    private boolean eligibleForAttendancePremium(int employeeId,LocalDate start,LocalDate end){
        Integer planned=jdbc.queryForObject("""
            SELECT COUNT(*) FROM jours_planning_employe j JOIN types_jour_planning t ON t.id=j.type_jour_id AND t.code='TRAVAIL'
            WHERE j.employe_id=? AND j.date_travail BETWEEN ? AND ?
            """,Integer.class,employeeId,start,end);
        if(planned==null||planned==0)return false;
        Integer complete=jdbc.queryForObject("""
            SELECT COUNT(DISTINCT s.id) FROM sessions_pointage s
            JOIN evenements_pointage a ON a.session_id=s.id JOIN types_evenement_pointage ta ON ta.id=a.type_id AND ta.code='ARRIVEE'
            JOIN evenements_pointage f ON f.session_id=s.id JOIN types_evenement_pointage tf ON tf.id=f.type_id AND tf.code='FIN_SERVICE'
            JOIN jours_planning_employe j ON j.id=s.jour_planning_id AND j.date_travail=s.date_service
            WHERE s.employe_id=? AND s.date_service BETWEEN ? AND ?
            """,Integer.class,employeeId,start,end);
        if(complete==null||complete<planned)return false;
        Integer anomalies=jdbc.queryForObject("""
            SELECT COUNT(*) FROM anomalies_pointage a JOIN sessions_pointage s ON s.id=a.session_id
            JOIN types_anomalie_pointage t ON t.id=a.type_id WHERE s.employe_id=? AND s.date_service BETWEEN ? AND ?
              AND t.code IN ('RETARD','DEPART_ANTICIPE')
            """,Integer.class,employeeId,start,end);
        Integer absences=jdbc.queryForObject("""
            SELECT COUNT(*) FROM absences a JOIN types_absence t ON t.id=a.type_absence_id
            WHERE a.employe_id=? AND t.retenue_paie=TRUE AND a.date_debut < (? + INTERVAL '1 day') AND a.date_fin >= ?
            """,Integer.class,employeeId,end,start);
        return (anomalies==null||anomalies==0)&&(absences==null||absences==0);
    }

    private void addUnpaidAbsence(List<Map<String,Object>> lines,int employeeId,LocalDate start,LocalDate end,BigDecimal salary) {
        Map<String,Object> absence=jdbc.queryForMap("""
            SELECT COUNT(DISTINCT d.jour)::integer AS jours,
                   COALESCE(string_agg(DISTINCT a.id::text, ', ' ORDER BY a.id::text), '') AS ids,
                   COALESCE(string_agg(DISTINCT ta.libelle, ', ' ORDER BY ta.libelle), '') AS motifs
            FROM absences a JOIN types_absence ta ON ta.id=a.type_absence_id AND ta.retenue_paie=TRUE
            CROSS JOIN LATERAL generate_series(GREATEST(a.date_debut::date,?::date),LEAST(a.date_fin::date,?::date),INTERVAL '1 day') d(jour)
            LEFT JOIN jours_planning_employe j ON j.employe_id=a.employe_id AND j.date_travail=d.jour::date
            LEFT JOIN types_jour_planning t ON t.id=j.type_jour_id
            WHERE a.employe_id=? AND a.date_debut < (? + INTERVAL '1 day') AND a.date_fin >= ?
              AND COALESCE(t.code,CASE WHEN EXTRACT(ISODOW FROM d.jour)<6 THEN 'TRAVAIL' ELSE 'REPOS' END) IN ('TRAVAIL','ABSENCE')
            """,start,end,employeeId,end,start);
        int days=((Number)absence.get("jours")).intValue();
        if(days<=0)return;
        BigDecimal dailyRate=salary.divide(BigDecimal.valueOf(30),2,RoundingMode.HALF_UP);
        BigDecimal amount=salary.multiply(BigDecimal.valueOf(days)).divide(BigDecimal.valueOf(30),0,RoundingMode.HALF_UP);
        addLine(lines,"RETENUE_ABSENCE","Retenue absence non rémunérée","RETENUE_SALARIALE",amount,BigDecimal.valueOf(days),dailyRate,
            "Absence(s) #"+absence.get("ids")+" · "+absence.get("motifs")+" · taux journalier salaire mensuel / 30",null,null,null,null);
    }

    private void addOvertime(List<Map<String,Object>> lines,int employeeId,LocalDate start,LocalDate end,
            BigDecimal hourlyRate,BigDecimal monthlyReferenceHours) {
        LocalDate weekStart=start.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        List<Map<String,Object>> sessions=jdbc.queryForList("""
            SELECT s.id,
              MIN(e.horodatage) FILTER (WHERE t.code='ARRIVEE') AS arrivee,
              MAX(e.horodatage) FILTER (WHERE t.code='FIN_SERVICE') AS fin_service,
              MIN(e.horodatage) FILTER (WHERE t.code='DEBUT_PAUSE') AS debut_pause,
              MAX(e.horodatage) FILTER (WHERE t.code='FIN_PAUSE') AS fin_pause
            FROM sessions_pointage s JOIN evenements_pointage e ON e.session_id=s.id
            JOIN types_evenement_pointage t ON t.id=e.type_id
            WHERE s.employe_id=? AND s.date_service BETWEEN ? AND ?
            GROUP BY s.id
            HAVING COUNT(*) FILTER (WHERE t.code='ARRIVEE')>0 AND COUNT(*) FILTER (WHERE t.code='FIN_SERVICE')>0
            ORDER BY MIN(e.horodatage),s.id
            """,employeeId,weekStart,end);
        List<WorkInterval> intervals=new ArrayList<>();
        for(Map<String,Object> session:sessions){
            if(session.get("arrivee")==null||session.get("fin_service")==null)continue;
            ZonedDateTime arrival=offsetDateTime(session.get("arrivee")).atZoneSameInstant(ZoneId.of("Indian/Antananarivo"));
            ZonedDateTime finish=offsetDateTime(session.get("fin_service")).atZoneSameInstant(ZoneId.of("Indian/Antananarivo"));
            if(!finish.isAfter(arrival))continue;
            int sessionId=((Number)session.get("id")).intValue();
            if(session.get("debut_pause")!=null&&session.get("fin_pause")!=null){
                ZonedDateTime breakStart=offsetDateTime(session.get("debut_pause")).atZoneSameInstant(arrival.getZone());
                ZonedDateTime breakEnd=offsetDateTime(session.get("fin_pause")).atZoneSameInstant(arrival.getZone());
                if(breakStart.isAfter(arrival)&&breakEnd.isBefore(finish)&&breakEnd.isAfter(breakStart)){
                    intervals.add(new WorkInterval(arrival,breakStart,sessionId));
                    intervals.add(new WorkInterval(breakEnd,finish,sessionId));
                    continue;
                }
            }
            intervals.add(new WorkInterval(arrival,finish,sessionId));
        }
        if(intervals.isEmpty())return;
        intervals.sort(java.util.Comparator.comparing(WorkInterval::start));
        BigDecimal firstTier=parameter("HEURES_SUP_SEUIL_1",null,start,end);
        BigDecimal overtimeLimit=parameter("HEURES_SUP_SEUIL_2",null,start,end);
        BigDecimal maxWeekly=parameterOrDefault("HEURES_MAX_SEMAINE",null,start,end,BigDecimal.valueOf(60));
        BigDecimal normalWeekly=monthlyReferenceHours.multiply(BigDecimal.valueOf(12))
            .divide(BigDecimal.valueOf(52),2,RoundingMode.HALF_UP);
        BigDecimal rateOne=BigDecimal.ONE.add(parameter("MAJORATION_HEURES_SUP_1",null,start,end));
        BigDecimal rateTwo=BigDecimal.ONE.add(parameter("MAJORATION_HEURES_SUP_2",null,start,end));
        BigDecimal sundayMultiplier=BigDecimal.ONE.add(parameter("MAJORATION_DIMANCHE",null,start,end));
        BigDecimal nightMultiplier=BigDecimal.ONE.add(parameter("MAJORATION_NUIT",null,start,end));
        int nightStart=parameter("HEURE_NUIT_DEBUT",null,start,end).intValue();
        int nightEnd=parameter("HEURE_NUIT_FIN",null,start,end).intValue();
        Map<String,Double> workedByWeek=new LinkedHashMap<>(),overtimeByWeek=new LinkedHashMap<>();
        BigDecimal overtimeAmount=ZERO,overtimeHours=ZERO,sundayAmount=ZERO,sundayHours=ZERO,nightAmount=ZERO,nightHours=ZERO;
        java.util.Set<Integer> overtimeSources=new java.util.LinkedHashSet<>(),sundaySources=new java.util.LinkedHashSet<>(),nightSources=new java.util.LinkedHashSet<>();
        for(WorkInterval interval:intervals){
            for(ZonedDateTime cursor=interval.start();cursor.isBefore(interval.end());){
                ZonedDateTime segmentEnd=cursor.plusMinutes(1);
                if(segmentEnd.isAfter(interval.end()))segmentEnd=interval.end();
                LocalDate workDate=cursor.toLocalDate();
                double hours=java.time.Duration.between(cursor,segmentEnd).toSeconds()/3600d;
                String week=weekKey(workDate);
                double worked=workedByWeek.getOrDefault(week,0d);
                double maxHours=maxWeekly.doubleValue();
                double nextWorked=worked+hours;
                if(nextWorked>maxHours+0.000001)throw conflict("Le pointage dépasse la limite de "+maxWeekly+" heures pour la semaine du "+workDate.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))+" (salarié "+employeeId+"). Corrigez les pointages avant le calcul.");
                double regular=Math.min(hours,Math.max(0d,normalWeekly.doubleValue()-worked));
                double overtime=hours-regular;
                double alreadyOvertime=overtimeByWeek.getOrDefault(week,0d);
                if(overtime>0&&alreadyOvertime+overtime>overtimeLimit.doubleValue()+0.000001)
                    throw conflict("Les heures supplémentaires dépassent "+overtimeLimit+" heures pour la semaine du "+workDate.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))+" (salarié "+employeeId+").");
                workedByWeek.put(week,nextWorked);
                if(overtime>0)overtimeByWeek.put(week,alreadyOvertime+overtime);

                if(!workDate.isBefore(start)&&!workDate.isAfter(end)){
                    boolean sunday=workDate.getDayOfWeek()==java.time.DayOfWeek.SUNDAY;
                    boolean night=cursor.getHour()>=nightStart||cursor.getHour()<nightEnd;
                    BigDecimal exceptionalRate=sunday?sundayMultiplier:(night?nightMultiplier:BigDecimal.ONE);
                    if(regular>0&&sunday){
                        sundayAmount=sundayAmount.add(hourlyRate.multiply(BigDecimal.valueOf(regular)).multiply(sundayMultiplier));
                        sundayHours=sundayHours.add(BigDecimal.valueOf(regular));sundaySources.add(interval.sessionId());
                    }else if(regular>0&&night){
                        nightAmount=nightAmount.add(hourlyRate.multiply(BigDecimal.valueOf(regular)).multiply(nightMultiplier));
                        nightHours=nightHours.add(BigDecimal.valueOf(regular));nightSources.add(interval.sessionId());
                    }
                    if(overtime>0){
                        double before=alreadyOvertime;
                        double tierOne=Math.min(overtime,Math.max(0d,firstTier.doubleValue()-before));
                        double tierTwo=overtime-tierOne;
                        BigDecimal multiplierOne=rateOne.max(exceptionalRate);
                        BigDecimal multiplierTwo=rateTwo.max(exceptionalRate);
                        overtimeAmount=overtimeAmount.add(hourlyRate.multiply(BigDecimal.valueOf(tierOne)).multiply(multiplierOne))
                            .add(hourlyRate.multiply(BigDecimal.valueOf(tierTwo)).multiply(multiplierTwo));
                        overtimeHours=overtimeHours.add(BigDecimal.valueOf(overtime));overtimeSources.add(interval.sessionId());
                    }
                }
                cursor=segmentEnd;
            }
        }
        if(overtimeAmount.signum()>0)addLine(lines,"HEURES_SUP","Heures supplémentaires","GAIN",overtimeAmount.setScale(0,RoundingMode.HALF_UP),overtimeHours,hourlyRate,
            "Pointages "+overtimeSources+" · au-delà de "+normalWeekly.stripTrailingZeros().toPlainString()+" h/semaine · tranches +30% puis +50% · plafond "+overtimeLimit+" h",null,overtimeSources.iterator().next(),null,null);
        if(sundayAmount.signum()>0)addLine(lines,"TRAVAIL_DIMANCHE","Travail du dimanche","GAIN",sundayAmount.setScale(0,RoundingMode.HALF_UP),sundayHours,hourlyRate,
            "Pointages "+sundaySources+" · travail du dimanche payé 2 fois le taux horaire; samedi non majoré",null,sundaySources.iterator().next(),null,null);
        if(nightAmount.signum()>0)addLine(lines,"TRAVAIL_NUIT","Travail de nuit","GAIN",nightAmount.setScale(0,RoundingMode.HALF_UP),nightHours,hourlyRate,
            "Pointages "+nightSources+" · heures entre "+nightStart+" h et "+nightEnd+" h payées 1,5 fois le taux horaire",null,nightSources.iterator().next(),null,null);
    }

    private record WorkInterval(ZonedDateTime start,ZonedDateTime end,int sessionId) { }
    private static String weekKey(LocalDate date){return date.get(WeekFields.ISO.weekBasedYear())+"-"+date.get(WeekFields.ISO.weekOfWeekBasedYear());}
    private record OvertimeDay(LocalDate date,String week,int sessionId,double hours,OffsetDateTime end) { }

    private BigDecimal deductAdvances(List<Map<String,Object>> lines,int employeeId,int periodId){
        List<Map<String,Object>> advances=jdbc.queryForList("""
            SELECT a.id,a.montant,a.motif FROM avances_salariales a JOIN statuts_avance_paie s ON s.id=a.statut_id
            WHERE a.employe_id=? AND a.periode_deduction_id=? AND s.code='VERSEE' ORDER BY a.id
            """,employeeId,periodId);
        BigDecimal total=ZERO;
        for(Map<String,Object> a:advances){ BigDecimal amount=decimal(a.get("montant")); total=total.add(amount);
            addLine(lines,"AVANCE","Remboursement avance salariale","RETENUE_SALARIALE",amount,BigDecimal.ONE,amount,"Avance #"+a.get("id")+" · "+a.get("motif"),null,null,null,((Number)a.get("id")).intValue()); }
        return total;
    }

    private BigDecimal calculateIrsa(BigDecimal taxable,LocalDate start,LocalDate end){
        List<Map<String,Object>> brackets=jdbc.queryForList("""
            SELECT borne_inferieure,borne_superieure,taux FROM tranches_irsa
            WHERE date_debut<=? AND (date_fin IS NULL OR date_fin>=?) ORDER BY borne_inferieure
            """,start,end);
        if(brackets.isEmpty()) throw conflict("Aucun barème IRSA en vigueur ne couvre cette période.");
        BigDecimal total=ZERO;
        for(Map<String,Object> b:brackets){
            BigDecimal lower=decimal(b.get("borne_inferieure")); Object upperValue=b.get("borne_superieure");
            BigDecimal upper=upperValue==null?taxable:decimal(upperValue).min(taxable);
            if(taxable.compareTo(lower)>0) total=total.add(upper.subtract(lower).max(ZERO).multiply(decimal(b.get("taux"))));
        }
        BigDecimal min=parameter("MINIMUM_IRSA",null,start,end);
        if(total.signum()>0 && total.compareTo(min)<0) total=min;
        return total.setScale(0,RoundingMode.HALF_UP);
    }

    private BigDecimal parameter(String code,Integer regimeId,LocalDate start,LocalDate end){
        List<BigDecimal> values=jdbc.query("""
            SELECT valeur FROM parametres_paie WHERE code=? AND date_debut<=? AND (date_fin IS NULL OR date_fin>=?)
              AND (regime_id=? OR regime_id IS NULL) ORDER BY (regime_id IS NULL),date_debut DESC,id DESC LIMIT 1
            """,(rs,row)->rs.getBigDecimal(1),code,start,end,regimeId);
        if(values.isEmpty()) throw conflict("Paramètre de paie manquant à la date de calcul : "+code+".");
        return values.get(0);
    }

    private BigDecimal parameterOrDefault(String code,Integer regimeId,LocalDate start,LocalDate end,BigDecimal fallback){
        List<BigDecimal> values=jdbc.query("""
            SELECT valeur FROM parametres_paie WHERE code=? AND date_debut<=? AND (date_fin IS NULL OR date_fin>=?)
              AND (regime_id=? OR regime_id IS NULL) ORDER BY (regime_id IS NULL),date_debut DESC,id DESC LIMIT 1
            """,(rs,row)->rs.getBigDecimal(1),code,start,end,regimeId);
        return values.isEmpty()?fallback:values.get(0);
    }

    private void addLine(List<Map<String,Object>> lines,String code,String label,String category,BigDecimal amount,BigDecimal quantity,BigDecimal base,String source,Integer variable,Integer session,Integer absence,Integer advance){
        addLine(lines,code,label,category,amount,quantity,base,source,variable,session,absence,advance,null);
    }
    private void addLine(List<Map<String,Object>> lines,String code,String label,String category,BigDecimal amount,BigDecimal quantity,BigDecimal base,String source,Integer variable,Integer session,Integer absence,Integer advance,Integer sanction){
        Map<String,Object> line=new LinkedHashMap<>();line.put("code",code);line.put("label",label);line.put("category",category);line.put("amount",amount.setScale(2,RoundingMode.HALF_UP));line.put("quantity",quantity);line.put("base",base);line.put("source",source);line.put("variable",variable);line.put("session",session);line.put("absence",absence);line.put("advance",advance);line.put("sanction",sanction);lines.add(line);
    }
    private void persistLine(int bulletinId,int order,Map<String,Object> l){
        BigDecimal base=decimal(l.get("base")),quantity=decimal(l.get("quantity")),amount=decimal(l.get("amount"));
        BigDecimal denominator=base.multiply(quantity);
        BigDecimal rate=denominator.signum()==0?null:amount.divide(denominator,6,RoundingMode.HALF_UP);
        jdbc.update("""
            INSERT INTO lignes_bulletin_paie(bulletin_id,rubrique_id,code_snapshot,libelle_snapshot,categorie_snapshot,quantite,base_calcul,taux_applique,montant,variable_id,session_pointage_id,absence_id,avance_id,sanction_id,source_snapshot,ordre)
            SELECT ?,r.id,?,?,?,?,?,?,?,?,?,?,?,?,?,? FROM rubriques_paie r WHERE r.code=?
            """,bulletinId,l.get("code"),l.get("label"),l.get("category"),l.get("quantity"),l.get("base"),rate,l.get("amount"),l.get("variable"),l.get("session"),l.get("absence"),l.get("advance"),l.get("sanction"),l.get("source"),order,l.get("code"));
    }
    private static BigDecimal sum(List<Map<String,Object>> lines,String category){return lines.stream().filter(l->category.equals(l.get("category"))).map(l->decimal(l.get("amount"))).reduce(ZERO,BigDecimal::add);}
    private static BigDecimal sumCode(List<Map<String,Object>> lines,String code){return lines.stream().filter(l->code.equals(l.get("code"))).map(l->decimal(l.get("amount"))).reduce(ZERO,BigDecimal::add);}
    static BigDecimal computeGross(List<Map<String,Object>> lines){
        return sum(lines,"GAIN").subtract(sumCode(lines,"RETENUE_ABSENCE")).subtract(sumCode(lines,"RETENUE_MISE_A_PIED")).max(ZERO);
    }
    static BigDecimal computeNet(BigDecimal gross,List<Map<String,Object>> lines){
        BigDecimal deductions=sum(lines,"RETENUE_SALARIALE");
        BigDecimal absence=sumCode(lines,"RETENUE_ABSENCE");
        BigDecimal miseAPied=sumCode(lines,"RETENUE_MISE_A_PIED");
        return gross.subtract(deductions.subtract(absence).subtract(miseAPied)).max(ZERO);
    }
    private static LocalDate localDate(Object value) {
        return value instanceof java.sql.Date date ? date.toLocalDate() : (LocalDate) value;
    }
    private static java.time.LocalTime localTime(Object value) {
        return value instanceof java.sql.Time time ? time.toLocalTime() : (java.time.LocalTime) value;
    }
    private static OffsetDateTime offsetDateTime(Object value) {
        return value instanceof java.sql.Timestamp timestamp
            ? timestamp.toInstant().atOffset(java.time.ZoneOffset.UTC) : (OffsetDateTime) value;
    }
    private static double hoursBetween(Object start,Object end){return java.time.Duration.between(offsetDateTime(start),offsetDateTime(end)).toSeconds()/3600d;}
    private Map<String,Object> lockPeriod(int id){return jdbc.query("""
        SELECT p.id,p.date_debut AS "dateDebut",p.date_fin AS "dateFin",s.code AS "statutCode",s.libelle AS statut
        FROM periodes_paie p JOIN statuts_periode_paie s ON s.id=p.statut_id WHERE p.id=? FOR UPDATE
        """,rs->{if(!rs.next())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Période de paie introuvable.");Map<String,Object> m=new LinkedHashMap<>();m.put("id",rs.getInt("id"));m.put("dateDebut",rs.getDate("dateDebut").toLocalDate());m.put("dateFin",rs.getDate("dateFin").toLocalDate());m.put("statutCode",rs.getString("statutCode"));m.put("statut",rs.getString("statut"));return m;},id);}
    private Map<String,Object> period(Integer actorId,int id){requireRh(actorId);Map<String,Object> result=jdbc.queryForMap("""
        SELECT p.id,p.date_debut AS "dateDebut",p.date_fin AS "dateFin",s.code AS "statutCode",s.libelle AS statut,
          COUNT(b.id)::integer AS "nombreBulletins",COALESCE(SUM(b.salaire_brut),0) AS "totalBrut",
          COALESCE(SUM(b.net_a_payer),0) AS "totalNet",COALESCE(SUM(b.total_charges_patronales),0) AS "totalCharges"
        FROM periodes_paie p JOIN statuts_periode_paie s ON s.id=p.statut_id LEFT JOIN bulletins_paie b ON b.periode_id=p.id
        WHERE p.id=? GROUP BY p.id,s.code,s.libelle
        """,id);
        result.put("dateDebut", localDate(result.get("dateDebut")));
        result.put("dateFin", localDate(result.get("dateFin")));
        return result;
    }
    private void requireRh(Integer actorId){Integer count=jdbc.queryForObject("""
        SELECT COUNT(*) FROM users u JOIN statuts_validation_compte sv ON sv.id=u.administrative_validation_status_id AND sv.code='APPROVED'
        JOIN user_roles ur ON ur.user_id=u.id JOIN roles r ON r.id=ur.role_id AND LOWER(r.role_name)='rh'
        WHERE u.id=? AND u.is_suspended=FALSE
        """,Integer.class,actorId);if(count==null||count==0)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Action réservée aux RH.");}
    private static BigDecimal decimal(Object value){return value instanceof BigDecimal b?b:value instanceof Number n?BigDecimal.valueOf(n.doubleValue()):ZERO;}
    private static LocalDate max(LocalDate a,LocalDate b){return a.isAfter(b)?a:b;} private static LocalDate min(LocalDate a,LocalDate b){return a.isBefore(b)?a:b;}
    private static ResponseStatusException bad(String m){return new ResponseStatusException(HttpStatus.BAD_REQUEST,m);} private static ResponseStatusException conflict(String m){return new ResponseStatusException(HttpStatus.CONFLICT,m);}
}
