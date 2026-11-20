package com.sirh.backend.modules.paie.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

@Service
public class PayrollPayslipService {
    private static final String BULLETIN_SELECT = """
        SELECT b.id, b.periode_id AS "periodeId", b.employe_id AS "employeId",
               b.matricule_snapshot AS matricule, b.nom_snapshot AS nom,
               b.categorie_snapshot AS categorie, b.fonction_snapshot AS fonction,
               b.salaire_base_contractuel AS "salaireBaseContractuel",
               b.salaire_base_proratise AS "salaireBaseProratise", b.salaire_brut AS "salaireBrut",
               b.revenu_imposable AS "revenuImposable", b.total_retenues_salariales AS "totalRetenues",
               b.salaire_net AS "salaireNet", b.total_charges_patronales AS "chargesPatronales",
               b.net_a_payer AS "netAPayer", b.ordre_paiement AS "ordrePaiement",
               s.code AS "statutCode", s.libelle AS statut, p.date_debut AS "dateDebut", p.date_fin AS "dateFin",
               b.created_at AS "calculeAt", b.valide_at AS "valideAt"
        FROM bulletins_paie b JOIN periodes_paie p ON p.id = b.periode_id
        JOIN statuts_bulletin_paie s ON s.id = b.statut_id
        """;

    private final JdbcTemplate jdbc;

    public PayrollPayslipService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> ownBulletins(Integer userId) {
        requireEmployee(userId);
        List<Integer> employees = jdbc.queryForList("SELECT id FROM employes WHERE user_id = ?", Integer.class, userId);
        if (employees.isEmpty()) return List.of();
        Integer employeeId = employees.get(0);
        return jdbc.queryForList(BULLETIN_SELECT + " WHERE b.employe_id = ? AND s.code IN ('VALIDE','PAYE') ORDER BY p.date_debut DESC, b.id DESC", employeeId);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> rhBulletins(Integer userId, Integer periodeId) {
        requireRole(userId, "rh");
        String sql = BULLETIN_SELECT + (periodeId == null ? " ORDER BY p.date_debut DESC, b.matricule_snapshot, b.id DESC" : " WHERE b.periode_id = ? ORDER BY b.matricule_snapshot, b.id DESC");
        return periodeId == null ? jdbc.queryForList(sql) : jdbc.queryForList(sql, periodeId);
    }

    @Transactional(readOnly = true)
    public byte[] ownPdf(Integer userId, Integer bulletinId) {
        requireEmployee(userId);
        List<Integer> employees = jdbc.queryForList("SELECT id FROM employes WHERE user_id = ?", Integer.class, userId);
        if (employees.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucun dossier salarié associé à ce compte.");
        Integer employeeId = employees.get(0);
        return pdfFor(bulletinId, employeeId, true);
    }

    @Transactional(readOnly = true)
    public byte[] rhPdf(Integer userId, Integer bulletinId) {
        requireRole(userId, "rh");
        return pdfFor(bulletinId, null, false);
    }

    @Transactional(readOnly = true)
    public byte[] rhMonthlyPayrollExcel(Integer userId, Integer periodId) {
        requireRole(userId, "rh");
        Map<String, Object> period = jdbc.query("SELECT p.id, p.date_debut AS \"dateDebut\", p.date_fin AS \"dateFin\", sp.libelle AS statut FROM periodes_paie p JOIN statuts_periode_paie sp ON sp.id=p.statut_id WHERE p.id=?",
            rs -> { if (!rs.next()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Période de paie introuvable.");
                Map<String,Object> row = new java.util.LinkedHashMap<>(); row.put("id",rs.getInt("id")); row.put("dateDebut",rs.getObject("dateDebut")); row.put("dateFin",rs.getObject("dateFin")); row.put("statut",rs.getString("statut")); return row; }, periodId);
        List<Map<String,Object>> bulletins = jdbc.queryForList(BULLETIN_SELECT + " WHERE b.periode_id=? ORDER BY b.matricule_snapshot,b.id", periodId);
        if (bulletins.isEmpty()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Aucun bulletin calculé pour cette période.");
        List<Map<String,Object>> lines = jdbc.queryForList("""
            SELECT b.id AS "bulletinId", b.matricule_snapshot AS matricule, b.nom_snapshot AS nom,
                   l.code_snapshot AS code, l.libelle_snapshot AS libelle, l.categorie_snapshot AS categorie,
                   l.quantite, l.base_calcul AS base, l.taux_applique AS taux, l.montant, l.source_snapshot AS source
            FROM lignes_bulletin_paie l JOIN bulletins_paie b ON b.id=l.bulletin_id
            WHERE b.periode_id=? ORDER BY b.matricule_snapshot,l.ordre,l.id
            """, periodId);
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            CellStyle title = workbook.createCellStyle(); Font titleFont=workbook.createFont(); titleFont.setBold(true); titleFont.setFontHeightInPoints((short)14); title.setFont(titleFont);
            CellStyle header = workbook.createCellStyle(); Font headerFont=workbook.createFont(); headerFont.setBold(true); header.setFont(headerFont); header.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex()); header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            CellStyle money = workbook.createCellStyle(); money.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));
            CellStyle decimal = workbook.createCellStyle(); decimal.setDataFormat(workbook.createDataFormat().getFormat("0.000"));

            Sheet summary=workbook.createSheet("Etat de paie");
            Row titleRow=summary.createRow(0); titleRow.createCell(0).setCellValue("ÉTAT DE PAIE — " + period.get("dateDebut") + " au " + period.get("dateFin")); titleRow.getCell(0).setCellStyle(title);
            Row statusRow=summary.createRow(1); statusRow.createCell(0).setCellValue("Statut de la période"); statusRow.createCell(1).setCellValue(String.valueOf(period.get("statut")));
            String[] headers={"Période début","Période fin","Statut bulletin","Matricule","Nom et prénom","Catégorie","Fonction","Salaire de base contractuel (Ar)","Salaire de base du mois après retenues d’absence (Ar)","Indemnités / primes (Ar)","Rappels (Ar)","Autres gains (Ar)","Retenues d’absence / mise à pied (Ar)","Heures supplémentaires (Ar)","Salaire brut (Ar)","CNaPS salarié (Ar)","OSTIE / santé salarié (Ar)","Revenu imposable (Ar)","IRSA (Ar)","Total retenues salariales (Ar)","Salaire net (Ar)","Avances déduites après net (Ar)","Net à payer (Ar)","CNaPS patronale (Ar)","Santé patronale (Ar)","Total charges patronales (Ar)","Référence / ordre de paiement"};
            Row h=summary.createRow(3); for(int i=0;i<headers.length;i++){h.createCell(i).setCellValue(headers[i]);h.getCell(i).setCellStyle(header);}
            int rowIndex=4;
            for(Map<String,Object> b:bulletins){List<Map<String,Object>> employeeLines=lines.stream().filter(l->b.get("id").equals(l.get("bulletinId"))).toList(); Row row=summary.createRow(rowIndex++); int c=0;
                set(row,c++,period.get("dateDebut")); set(row,c++,period.get("dateFin")); set(row,c++,b.get("statut")); set(row,c++,b.get("matricule")); set(row,c++,b.get("nom")); set(row,c++,b.get("categorie")); set(row,c++,b.get("fonction"));
                setMoney(row,c++,b.get("salaireBaseContractuel"),money); setMoney(row,c++,b.get("salaireBaseProratise"),money);
                setMoney(row,c++,sumGainPrefixes(employeeLines,List.of("PRIME_","TRAVAIL_DIMANCHE","TRAVAIL_NUIT")),money);
                setMoney(row,c++,sum(employeeLines,"GAIN","RAPPEL"),money);
                setMoney(row,c++,sumGainOther(employeeLines),money);
                setMoney(row,c++,sum(employeeLines,"RETENUE_SALARIALE","RETENUE_ABSENCE").add(sum(employeeLines,"RETENUE_SALARIALE","RETENUE_MISE_A_PIED")),money);
                setMoney(row,c++,sum(employeeLines,"GAIN","HEURES_SUP"),money); setMoney(row,c++,b.get("salaireBrut"),money);
                setMoney(row,c++,sum(employeeLines,"RETENUE_SALARIALE","CNAPS_SALARIAL"),money); setMoney(row,c++,sum(employeeLines,"RETENUE_SALARIALE","SANTE_SALARIAL"),money);
                setMoney(row,c++,b.get("revenuImposable"),money); setMoney(row,c++,sum(employeeLines,"RETENUE_SALARIALE","IRSA"),money); setMoney(row,c++,b.get("totalRetenues"),money);
                setMoney(row,c++,b.get("salaireNet"),money); setMoney(row,c++,sum(employeeLines,"RETENUE_SALARIALE","AVANCE"),money); setMoney(row,c++,b.get("netAPayer"),money);
                setMoney(row,c++,sum(employeeLines,"CHARGE_PATRONALE","CNAPS_PATRONAL"),money); setMoney(row,c++,sum(employeeLines,"CHARGE_PATRONALE","SANTE_PATRONAL"),money); setMoney(row,c++,b.get("chargesPatronales"),money); set(row,c,b.get("ordrePaiement"));
            }
            summary.createFreezePane(0,4); summary.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(3,Math.max(3,rowIndex-1),0,headers.length-1));
            for(int i=0;i<headers.length;i++) summary.setColumnWidth(i,Math.min(45,Math.max(16,headers[i].length()+3))*256);

            Sheet detail=workbook.createSheet("Détail des rubriques"); String[] detailHeaders={"Période début","Période fin","Matricule","Nom et prénom","Catégorie","Code rubrique","Rubrique","Type","Quantité","Base de calcul (Ar)","Taux","Montant (Ar)","Source / justification"};
            Row dh=detail.createRow(0); for(int i=0;i<detailHeaders.length;i++){dh.createCell(i).setCellValue(detailHeaders[i]);dh.getCell(i).setCellStyle(header);}
            int dr=1; for(Map<String,Object> l:lines){Row row=detail.createRow(dr++);int c=0;set(row,c++,period.get("dateDebut"));set(row,c++,period.get("dateFin"));set(row,c++,l.get("matricule"));set(row,c++,l.get("nom"));set(row,c++,l.get("categorie"));set(row,c++,l.get("code"));set(row,c++,l.get("libelle"));set(row,c++,l.get("categorie"));setDecimal(row,c++,l.get("quantite"),decimal);setMoney(row,c++,l.get("base"),money);setDecimal(row,c++,l.get("taux"),decimal);setMoney(row,c++,l.get("montant"),money);set(row,c,l.get("source"));}
            detail.createFreezePane(0,1); if(dr>1) detail.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0,dr-1,0,detailHeaders.length-1)); for(int i=0;i<detailHeaders.length;i++)detail.setColumnWidth(i,Math.min(55,Math.max(16,detailHeaders[i].length()+3))*256);
            workbook.write(output); return output.toByteArray();
        } catch(IOException exception){throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Impossible de générer l’état de paie Excel.",exception);}
    }

    private static BigDecimal sum(List<Map<String,Object>> lines,String category,String codePrefix){return lines.stream().filter(l->category.equals(l.get("categorie"))&&(codePrefix==null||String.valueOf(l.get("code")).startsWith(codePrefix))).map(l->l.get("montant") instanceof BigDecimal value?value:BigDecimal.ZERO).reduce(BigDecimal.ZERO,BigDecimal::add);}
    private static BigDecimal sumGainPrefixes(List<Map<String,Object>> lines,List<String> prefixes){return lines.stream().filter(l->"GAIN".equals(l.get("categorie"))&&prefixes.stream().anyMatch(prefix->String.valueOf(l.get("code")).startsWith(prefix))).map(l->l.get("montant") instanceof BigDecimal value?value:BigDecimal.ZERO).reduce(BigDecimal.ZERO,BigDecimal::add);}
    private static BigDecimal sumGainOther(List<Map<String,Object>> lines){return lines.stream().filter(l->"GAIN".equals(l.get("categorie"))&&!List.of("SALAIRE_BASE","HEURES_SUP","RAPPEL").contains(l.get("code"))&&!String.valueOf(l.get("code")).startsWith("PRIME_")&&!String.valueOf(l.get("code")).startsWith("TRAVAIL_")).map(l->l.get("montant") instanceof BigDecimal value?value:BigDecimal.ZERO).reduce(BigDecimal.ZERO,BigDecimal::add);}
    private static void set(Row row,int column,Object value){if(value==null)return;row.createCell(column).setCellValue(value instanceof java.sql.Date d?d.toLocalDate().toString():value.toString());}
    private static void setMoney(Row row,int column,Object value,CellStyle style){Cell cell=row.createCell(column);if(value instanceof Number n)cell.setCellValue(n.doubleValue());else if(value instanceof BigDecimal n)cell.setCellValue(n.doubleValue());cell.setCellStyle(style);}
    private static void setDecimal(Row row,int column,Object value,CellStyle style){setMoney(row,column,value,style);}

    private byte[] pdfFor(Integer bulletinId, Integer employeeId, boolean finalizedOnly) {
        StringBuilder condition = new StringBuilder(" WHERE b.id = ? ");
        List<Object> arguments = new ArrayList<>(); arguments.add(bulletinId);
        if (employeeId != null) { condition.append(" AND b.employe_id = ? "); arguments.add(employeeId); }
        if (finalizedOnly) condition.append(" AND s.code IN ('VALIDE','PAYE') ");
        Map<String, Object> bulletin = jdbc.query(BULLETIN_SELECT + condition,
            rs -> { if (!rs.next()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Bulletin introuvable ou non accessible.");
                var map = new java.util.LinkedHashMap<String, Object>();
                for (String key : List.of("id", "periodeId", "employeId", "matricule", "nom", "categorie", "fonction", "salaireBaseContractuel", "salaireBaseProratise", "salaireBrut", "revenuImposable", "totalRetenues", "salaireNet", "chargesPatronales", "netAPayer", "ordrePaiement", "statutCode", "statut", "dateDebut", "dateFin", "calculeAt", "valideAt")) map.put(key, rs.getObject(key));
                return map;
            }, arguments.toArray());
        List<Map<String, Object>> lines = jdbc.queryForList("""
            SELECT code_snapshot AS code, libelle_snapshot AS libelle, categorie_snapshot AS categorie,
                   quantite, base_calcul AS base, taux_applique AS taux, montant, source_snapshot AS source
            FROM lignes_bulletin_paie WHERE bulletin_id = ? ORDER BY ordre, id
            """, bulletinId);
        return renderPdf(bulletin, lines);
    }

    private byte[] renderPdf(Map<String, Object> b, List<Map<String, Object>> lines) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font italic = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);
            PageWriter writer = new PageWriter(document, regular, bold, italic);
            writer.heading("SIRH · BULLETIN DE PAIE", "Document individuel · Référence " + b.get("id"));
            writer.section("PÉRIODE DE PAIE");
            writer.text("Du " + date(b.get("dateDebut")) + " au " + date(b.get("dateFin")) + "     Statut : " + b.get("statut"));
            writer.section("SALARIÉ");
            writer.text("Nom : " + safe(b.get("nom")) + "     Matricule : " + safe(b.get("matricule")));
            writer.text("Fonction : " + safe(b.get("fonction")) + "     Catégorie : " + safe(b.get("categorie")));
            writer.section("RÉMUNÉRATIONS ET PRIMES");
            writer.columns("Rubrique", "Base / quantité", "Taux", "Montant");
            writeCategory(writer, lines, "GAIN");
            if(lines.stream().anyMatch(line -> List.of("RETENUE_ABSENCE","RETENUE_MISE_A_PIED").contains(line.get("code")))) {
                writer.section("DÉDUCTIONS AVANT CALCUL DU BRUT");
                writer.columns("Rubrique", "Base / quantité", "Taux", "Montant");
                writeCode(writer, lines, "RETENUE_ABSENCE");
                writeCode(writer, lines, "RETENUE_MISE_A_PIED");
            }
            writer.section("RETENUES SALARIALES");
            writer.columns("Rubrique", "Base / quantité", "Taux", "Montant");
            writeCategory(writer, lines, "RETENUE_SALARIALE");
            writer.section("CHARGES PATRONALES · INFORMATION");
            writer.columns("Rubrique", "Base / quantité", "Taux", "Montant");
            writeCategory(writer, lines, "CHARGE_PATRONALE");
            writer.section("RÉCAPITULATIF");
            writer.total("Salaire de base contractuel", money(b.get("salaireBaseContractuel")));
            writer.total("Salaire de base proratisé", money(b.get("salaireBaseProratise")));
            writer.total("Salaire brut", money(b.get("salaireBrut")));
            writer.total("Revenu imposable", money(b.get("revenuImposable")));
            writer.total("Retenues salariales", money(b.get("totalRetenues")));
            writer.total("Salaire net", money(b.get("salaireNet")));
            writer.total("Charges patronales (information)", money(b.get("chargesPatronales")));
            if(lines.stream().anyMatch(line -> "AVANCE".equals(line.get("code")))) {
                writer.section("AVANCES DÉDUITES APRÈS LE NET");
                writer.columns("Rubrique", "Base / quantité", "Taux", "Montant");
                writeCode(writer, lines, "AVANCE");
            }
            writer.highlight("NET À PAYER", money(b.get("netAPayer")));
            if (b.get("ordrePaiement") != null) writer.text("Référence de paiement : " + b.get("ordrePaiement"));
            writer.footer("Valeurs issues du bulletin et des rubriques archivés dans le SIRH. Généré le " + LocalDate.now() + ".");
            writer.close();
            document.save(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Impossible de générer le bulletin PDF.", exception);
        }
    }

    private void requireEmployee(Integer userId) { requireAnyRole(userId, List.of("employe", "manager")); }
    private void requireRole(Integer userId, String role) { requireAnyRole(userId, List.of(role)); }
    private void requireAnyRole(Integer userId, List<String> roles) {
        String placeholders = String.join(",", roles.stream().map(ignored -> "?").toList());
        List<Object> args = new ArrayList<>(List.of(userId)); args.addAll(roles);
        Integer count = jdbc.queryForObject("""
            SELECT COUNT(*) FROM users u JOIN statuts_validation_compte sv ON sv.id = u.administrative_validation_status_id
            JOIN user_roles ur ON ur.user_id = u.id JOIN roles r ON r.id = ur.role_id
            WHERE u.id = ? AND u.is_suspended = FALSE AND sv.code = 'APPROVED' AND LOWER(r.role_name) IN (""" + placeholders + ")", Integer.class, args.toArray());
        if (count == null || count == 0) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès au bulletin non autorisé.");
    }

    private static String safe(Object value) { return value == null || value.toString().isBlank() ? "—" : value.toString(); }
    private static void writeCategory(PageWriter writer, List<Map<String, Object>> lines, String category) throws IOException {
        for (Map<String, Object> line : lines) {
            if (!category.equalsIgnoreCase(safe(line.get("categorie")))) continue;
            if ("RETENUE_SALARIALE".equals(category) && List.of("AVANCE","RETENUE_ABSENCE","RETENUE_MISE_A_PIED").contains(line.get("code"))) continue;
            writer.row(safe(line.get("libelle")), baseAndQuantity(line), percent(line.get("taux")), money(line.get("montant")));
        }
    }
    private static void writeCode(PageWriter writer,List<Map<String,Object>> lines,String code)throws IOException{
        for(Map<String,Object> line:lines)if(code.equals(line.get("code")))writer.row(safe(line.get("libelle")),baseAndQuantity(line),percent(line.get("taux")),money(line.get("montant")));
    }
    private static String date(Object value) { return value instanceof LocalDate d ? d.toString() : safe(value); }
    private static String money(Object value) {
        if (!(value instanceof BigDecimal amount)) return "0 Ar";
        return NumberFormat.getNumberInstance(Locale.FRANCE).format(amount).replace('\u202f', ' ').replace('\u00a0', ' ') + " Ar";
    }
    private static String percent(Object value) { return value instanceof BigDecimal rate ? NumberFormat.getPercentInstance(Locale.FRANCE).format(rate) : "—"; }
    private static String baseAndQuantity(Map<String, Object> row) {
        Object base = row.get("base"), quantity = row.get("quantite");
        if (base == null && quantity == null) return "—";
        return (base == null ? "" : money(base) + " × ") + (quantity == null ? "" : quantity.toString());
    }

    private static final class PageWriter {
        private final PDDocument document;
        private final PDType1Font regular, bold, italic;
        private PDPageContentStream stream;
        private float y;
        PageWriter(PDDocument document, PDType1Font regular, PDType1Font bold, PDType1Font italic) throws IOException {
            this.document = document; this.regular = regular; this.bold = bold; this.italic = italic; newPage();
        }
        void heading(String title, String subtitle) throws IOException {
            stream.setNonStrokingColor(new java.awt.Color(31, 75, 62)); stream.addRect(0, 735, 595, 107); stream.fill();
            write(title, 42, 800, 19, bold, 255, 255, 255); write(subtitle, 42, 777, 9, regular, 226, 241, 233); y = 716;
        }
        void section(String label) throws IOException { ensure(32); y -= 15; write(label, 42, y, 9, bold, 36, 112, 94); y -= 15; }
        void text(String value) throws IOException { ensure(22); write(value, 42, y, 9, regular, 45, 57, 51); y -= 15; }
        void columns(String name, String base, String rate, String amount) throws IOException {
            ensure(22); stream.setNonStrokingColor(new java.awt.Color(237, 245, 240)); stream.addRect(38, y - 5, 519, 21); stream.fill();
            write(name, 44, y, 8, bold, 43, 75, 61); write(base, 325, y, 8, bold, 43, 75, 61);
            write(rate, 425, y, 8, bold, 43, 75, 61); write(amount, 492, y, 8, bold, 43, 75, 61); y -= 19;
        }
        void row(String name, String base, String rate, String amount) throws IOException {
            ensure(20); write(trim(name, 44), 44, y, 8, regular, 45, 57, 51); write(trim(base, 16), 325, y, 8, regular, 45, 57, 51);
            write(rate, 425, y, 8, regular, 45, 57, 51); write(amount, 492, y, 8, regular, 45, 57, 51); y -= 15;
        }
        void total(String name, String amount) throws IOException {
            ensure(20); write(name, 325, y, 8, regular, 45, 57, 51); write(amount, 492, y, 8, bold, 45, 57, 51); y -= 15;
        }
        void highlight(String name, String amount) throws IOException {
            ensure(38); stream.setNonStrokingColor(new java.awt.Color(31, 75, 62)); stream.addRect(38, y - 10, 519, 30); stream.fill();
            write(name, 48, y, 10, bold, 255, 255, 255); write(amount, 480, y, 10, bold, 255, 255, 255); y -= 40;
        }
        void footer(String value) throws IOException { ensure(30); write(value, 42, 32, 7, italic, 112, 124, 117); }
        void close() throws IOException { if (stream != null) { stream.close(); stream = null; } }
        private void ensure(float space) throws IOException { if (y - space < 48) newPage(); }
        private void newPage() throws IOException { if (stream != null) stream.close(); PDPage page = new PDPage(PDRectangle.A4); document.addPage(page); stream = new PDPageContentStream(document, page); y = 790; }
        private void write(String value, float x, float y, int size, PDType1Font font, int r, int g, int b) throws IOException {
            stream.setNonStrokingColor(new java.awt.Color(r, g, b)); stream.beginText(); stream.setFont(font, size); stream.newLineAtOffset(x, y);
            stream.showText(toWinAnsi(value)); stream.endText();
        }
        private static String trim(String value, int max) { return value.length() <= max ? value : value.substring(0, max - 1) + "…"; }
        private static String toWinAnsi(String value) { return value.replace('\u202f', ' ').replace('\u00a0', ' ').replace("—", "-").replace("·", "|").replace("×", "x").replace("…", "...").replace("’", "'").replace("œ", "oe").replace("Œ", "OE"); }
    }
}
