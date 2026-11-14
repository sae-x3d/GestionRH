package com.sirh.backend.modules.candidatures.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ContractDocumentService {
    private static final Charset PDF_TEXT = Charset.forName("windows-1252");
    private static final int PAGE_WIDTH = 595;
    private static final int PAGE_HEIGHT = 842;
    private static final int LEFT = 55;
    private static final int RIGHT = 540;
    private static final int BOTTOM = 58;
    private final Path root;

    public ContractDocumentService(@Value("${sirh.storage.root:datas}") String configuredRoot) {
        Path configured = Path.of(configuredRoot);
        Path working = Path.of("").toAbsolutePath().normalize();
        if (configured.isAbsolute()) {
            this.root = configured.normalize();
        } else {
            Path local = working.resolve(configured).normalize();
            Path parent = working.resolve("..").resolve(configured).normalize();
            this.root = Files.isDirectory(local) ? local : Files.isDirectory(parent) ? parent : local;
        }
    }

    public String generate(Map<String, Object> contract) {
        String number = value(contract, "numero");
        String relative = "contrats/contrat_" + number.replaceAll("[^A-Za-z0-9._-]", "_") + ".pdf";
        Path file = checked(relative);
        try {
            Path directory = file.getParent();
            Files.createDirectories(directory);
            if (!directory.toRealPath().startsWith(root.toAbsolutePath().normalize())) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Dossier de contrats invalide.");
            }
            Path temporary = Files.createTempFile(directory, "contrat-", ".tmp");
            try {
                Files.write(temporary, renderPdf(contract));
                try {
                    Files.move(temporary, file, java.nio.file.StandardCopyOption.ATOMIC_MOVE,
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                } catch (java.nio.file.AtomicMoveNotSupportedException exception) {
                    Files.move(temporary, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temporary);
            }
            return relative;
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Le PDF du contrat n’a pas pu être enregistré.");
        }
    }

    public byte[] read(String relative) {
        if (relative == null || !relative.matches("contrats/contrat_[A-Za-z0-9._-]+[.]pdf")) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "PDF du contrat introuvable.");
        }
        Path file = checked(relative);
        try {
            Path realRoot = root.toRealPath();
            Path realFile = file.toRealPath();
            if (!realFile.startsWith(realRoot) || !Files.isRegularFile(realFile)
                    || Files.size(realFile) > 5L * 1024 * 1024) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "PDF du contrat introuvable.");
            }
            return Files.readAllBytes(realFile);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "PDF du contrat introuvable.");
        }
    }

    private Path checked(String relative) {
        Path file = root.resolve(relative).normalize();
        if (!file.startsWith(root.toAbsolutePath().normalize())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "PDF du contrat introuvable.");
        }
        return file;
    }

    private byte[] renderPdf(Map<String, Object> c) throws IOException {
        Page page = new Page();
        page.heading("CONTRAT DE TRAVAIL");
        page.center("Référence " + value(c, "numero") + "  |  Généré le " + value(c, "genere_le"));
        page.paragraph("Le présent contrat est conclu entre l’organisation recruteuse, représentée par son service des ressources humaines, et la personne salariée identifiée ci-dessous. Les parties conviennent des dispositions suivantes, sous réserve des textes légaux et conventionnels applicables.");

        page.section("1. Identification des parties");
        page.fact("Salarié(e)", value(c, "salarie"));
        page.fact("Adresse électronique", value(c, "email"));
        page.fact("Date de naissance", value(c, "date_naissance"));
        page.fact("Matricule salarié", value(c, "matricule"));

        page.section("2. Emploi et affectation");
        page.paragraph("La personne salariée est engagée en qualité de " + value(c, "poste") + " au sein du département " + value(c, "departement") + ", à la suite de l’offre « " + value(c, "offre") + " ». Elle exerce les missions compatibles avec cet emploi et sa qualification qui lui sont confiées par l’employeur.");

        page.section("3. Nature, durée et prise d’effet");
        page.paragraph("Le présent contrat est conclu sous la forme " + value(c, "type_contrat") + ". Il prend effet le " + value(c, "date_debut") + " et se poursuit conformément aux dispositions applicables à ce type de contrat.");
        page.section("4. Période d’essai");
        page.paragraph(value(c, "periode_essai"));

        page.section("5. Rémunération de base et primes");
        page.paragraph("La rémunération mensuelle brute de base convenue est de " + value(c, "salaire_base") + ". Ce montant constitue le salaire de base contractuel, avant prélèvements obligatoires.");
        page.paragraph("Des primes ou autres éléments variables de rémunération peuvent s’y ajouter selon les dispositifs applicables, les conditions d’éligibilité et les décisions de l’employeur. Ils sont distincts du salaire de base et ne sont pas inclus dans le montant ci-dessus.");

        page.section("6. Éléments déclarés au questionnaire d’embauche");
        page.fact("Prétention de salaire de base", value(c, "salaire_base_souhaite"));
        page.fact("Date de début souhaitée", value(c, "date_debut_souhaitee"));
        page.fact("Type de contrat souhaité", value(c, "type_contrat_souhaite"));
        page.fact("Salaire proposé par le candidat", value(c, "salaire_base_propose_candidat"));
        page.fact("Date proposée par le candidat", value(c, "date_debut_proposee_candidat"));
        page.fact("Type proposé par le candidat", value(c, "type_contrat_propose_candidat"));
        page.fact("Commentaire du candidat", value(c, "commentaire_candidat"));
        page.fact("Règles de l’entreprise prises en connaissance", value(c, "regles_acceptees"));
        page.paragraph("Les conditions finales du présent contrat sont celles acceptées par les parties et indiquées aux articles précédents.");

        page.section("7. Organisation du travail");
        page.paragraph("Le lieu de travail, les horaires, la durée du travail, les modalités de présence et les éventuelles règles de télétravail sont communiqués par l’employeur et appliqués conformément aux textes en vigueur.");
        page.section("8. Obligations professionnelles et confidentialité");
        page.paragraph("La personne salariée s’engage à exécuter ses fonctions avec diligence, à respecter les consignes professionnelles, les règles de sécurité, le règlement intérieur ainsi que les politiques de protection des données et de confidentialité. Les informations non publiques obtenues dans le cadre de ses fonctions sont protégées selon les règles applicables.");
        page.section("9. Congés et absences");
        page.paragraph("Les droits à congés et les modalités de déclaration des absences sont déterminés par la législation applicable et les procédures internes communiquées au salarié.");
        page.section("10. Modification et fin du contrat");
        page.paragraph("Toute modification substantielle fait l’objet d’un écrit signé par les parties. La rupture du contrat intervient selon les formes, délais et motifs prévus par la législation applicable.");
        page.section("11. Dispositions finales");
        page.paragraph("Le présent document reprend les informations enregistrées dans le dossier RH. Il est établi en deux exemplaires et soumis à la signature des parties.");
        page.ensure(105);
        page.signature("Pour l’employeur\nRessources humaines");
        page.signature("La personne salariée\n" + value(c, "salarie"));

        return encodePdf(page.finishAll());
    }

    private byte[] encodePdf(List<byte[]> streams) throws IOException {
        int pageCount = streams.size();
        int objectCount = 4 + pageCount * 2;
        List<byte[]> objects = new ArrayList<>(objectCount + 1);
        objects.add(new byte[0]);
        objects.add(ascii("<< /Type /Catalog /Pages 2 0 R >>"));
        StringBuilder kids = new StringBuilder("<< /Type /Pages /Count ").append(pageCount).append(" /Kids [");
        for (int i = 0; i < pageCount; i++) kids.append(6 + i * 2).append(" 0 R ");
        kids.append("] >>");
        objects.add(ascii(kids.toString()));
        objects.add(ascii("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>"));
        objects.add(ascii("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>"));
        for (int i = 0; i < pageCount; i++) {
            byte[] stream = streams.get(i);
            int streamId = 5 + i * 2;
            int pageId = streamId + 1;
            ByteArrayOutputStream streamObject = new ByteArrayOutputStream();
            streamObject.write(ascii("<< /Length " + stream.length + " >>\nstream\n"));
            streamObject.write(stream);
            streamObject.write(ascii("\nendstream"));
            objects.add(streamObject.toByteArray());
            objects.add(ascii("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + PAGE_WIDTH + " " + PAGE_HEIGHT
                + "] /Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> /Contents " + streamId + " 0 R >>"));
        }

        ByteArrayOutputStream pdf = new ByteArrayOutputStream();
        pdf.write(ascii("%PDF-1.4\n%âãÏÓ\n"));
        List<Integer> offsets = new ArrayList<>();
        offsets.add(0);
        for (int id = 1; id < objects.size(); id++) {
            offsets.add(pdf.size());
            pdf.write(ascii(id + " 0 obj\n"));
            pdf.write(objects.get(id));
            pdf.write(ascii("\nendobj\n"));
        }
        int xref = pdf.size();
        pdf.write(ascii("xref\n0 " + objects.size() + "\n0000000000 65535 f \n"));
        for (int id = 1; id < offsets.size(); id++) pdf.write(ascii(String.format(java.util.Locale.ROOT, "%010d 00000 n \n", offsets.get(id))));
        pdf.write(ascii("trailer\n<< /Size " + objects.size() + " /Root 1 0 R >>\nstartxref\n" + xref + "\n%%EOF\n"));
        return pdf.toByteArray();
    }

    private byte[] ascii(String value) { return value.getBytes(StandardCharsets.ISO_8859_1); }

    private String value(Map<String, Object> c, String key) {
        Object value = c.get(key);
        return value == null ? "Non renseigné" : value.toString();
    }

    private String pdfString(String value) {
        byte[] bytes = value.getBytes(PDF_TEXT);
        StringBuilder escaped = new StringBuilder();
        for (byte item : bytes) {
            int b = item & 0xff;
            if (b == '(' || b == ')' || b == '\\') escaped.append('\\').append((char) b);
            else if (b < 32 || b > 126) escaped.append('\\').append(String.format(java.util.Locale.ROOT, "%03o", b));
            else escaped.append((char) b);
        }
        return escaped.toString();
    }

    private final class Page {
        private StringBuilder content = new StringBuilder();
        private final List<StringBuilder> streams = new ArrayList<>(List.of(content));
        private int y = 778;
        private void heading(String text) { text(text, 18, true, 25); line(); }
        private void center(String text) { text(text, 9, false, 17); line(); }
        private void section(String text) { ensure(30); y -= 5; text(text, 12, true, 18); }
        private void fact(String label, String value) { paragraph(label + " : " + value); }
        private void paragraph(String text) {
            for (String wrapped : wrap(text, 92)) {
                ensure(17);
                text(wrapped, 10, false, 14);
            }
            y -= 4;
        }
        private void text(String text, int size, boolean bold, int leading) {
            content.append("BT /").append(bold ? "F2" : "F1").append(' ').append(size)
                .append(" Tf 1 0 0 1 ").append(LEFT).append(' ').append(y)
                .append(" Tm (").append(pdfString(text)).append(") Tj ET\n");
            y -= leading;
        }
        private void ensure(int required) {
            if (y - required < BOTTOM) {
                content = new StringBuilder();
                streams.add(content);
                y = 778;
            }
        }
        private void line() { content.append("0.55 0.68 0.60 RG ").append(LEFT).append(' ').append(y + 5).append(" m ").append(RIGHT).append(' ').append(y + 5).append(" l S\n"); }
        private void signature(String text) {
            ensure(90);
            content.append("0.45 0.52 0.48 RG ").append(LEFT).append(' ').append(y - 8).append(" m ").append(LEFT + 210).append(' ').append(y - 8).append(" l S\n");
            for (String row : text.split("\\n")) text(row, 9, false, 13);
            y -= 45;
        }
        private List<byte[]> finishAll() {
            List<byte[]> finished = new ArrayList<>(streams.size());
            for (int i = 0; i < streams.size(); i++) {
                StringBuilder stream = streams.get(i);
                stream.append("BT /F1 8 Tf 1 0 0 1 ").append(RIGHT - 42).append(" 30 Tm (Page ")
                    .append(i + 1).append(" / ").append(streams.size()).append(") Tj ET\n");
                finished.add(stream.toString().getBytes(StandardCharsets.ISO_8859_1));
            }
            return finished;
        }
    }

    private List<String> wrap(String text, int width) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split("\\s+")) {
            if (!line.isEmpty() && line.length() + word.length() + 1 > width) {
                lines.add(line.toString()); line.setLength(0);
            }
            if (!line.isEmpty()) line.append(' ');
            line.append(word);
        }
        if (!line.isEmpty()) lines.add(line.toString());
        return lines;
    }
}
