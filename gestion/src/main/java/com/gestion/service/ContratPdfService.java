package com.gestion.service;

import com.gestion.model.Contrat;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class ContratPdfService {
    public byte[] generer(Contrat c) {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4); doc.addPage(page);
            try (PDPageContentStream s = new PDPageContentStream(doc, page)) {
                float y = 790;
                y = line(s, "CONTRAT DE TRAVAIL " + c.getTypeContrat(), 50, y, 18, true) - 26;
                y = line(s, "Document genere le " + DateTimeFormatter.ofPattern("dd/MM/yyyy").format(c.getDateGeneration().toLocalDate()), 50, y, 10, false) - 24;
                y = section(s, "ENTRE LES SOUSSIGNES", y);
                y = paragraph(s, "Employeur : " + c.getNomEmployeur() + " — " + c.getAdresseEmployeur(), y);
                y = paragraph(s, "Salarie(e) : " + c.getNomSalarie() + " — " + c.getAdresseSalarie(), y) - 12;
                y = section(s, "CONDITIONS D'EMPLOI", y);
                y = paragraph(s, "Fonction : " + c.getFonction(), y);
                y = paragraph(s, "Categorie professionnelle : " + c.getCategorieProfessionnelle(), y);
                y = paragraph(s, "Indice minimum de classification : " + c.getIndiceClassification(), y);
                y = paragraph(s, "Remuneration brute : " + NumberFormat.getIntegerInstance(Locale.FRANCE).format(c.getSalaire()) + " Ariary par mois", y);
                y = paragraph(s, "Duree du travail : " + c.getDureeTravail(), y);
                y = paragraph(s, "Lieu de travail : " + c.getLieuTravail(), y);
                y = paragraph(s, "Date d'effet : " + c.getDateEffet().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), y);
                if (c.getDateFin() != null) y = paragraph(s, "Terme : " + c.getDateFin().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), y);
                if (c.getMotifCdd() != null && !c.getMotifCdd().isBlank()) y = paragraph(s, "Motif du CDD : " + c.getMotifCdd(), y);
                y -= 14;
                y = paragraph(s, "Le present contrat est soumis au Code du travail de Madagascar (loi n° 2024-014).", y);
                y = paragraph(s, "Il est etabli en deux exemplaires. Chaque partie conserve un exemplaire apres signature.", y) - 28;
                y = line(s, "Fait a ____________________, le ____ / ____ / ______", 50, y, 11, false) - 42;
                line(s, "L'EMPLOYEUR                                         LE / LA SALARIE(E)", 50, y, 11, true);
                line(s, "Signature precedee de la mention                 Signature precedee de la mention", 50, y-18, 9, false);
                line(s, "Lu et approuve                                         Lu et approuve", 50, y-32, 9, false);
            }
            doc.save(out); return out.toByteArray();
        } catch (IOException e) { throw new IllegalStateException("Impossible de generer le PDF du contrat.", e); }
    }
    private float section(PDPageContentStream s, String t, float y) throws IOException { return line(s, t, 50, y, 12, true)-18; }
    private float paragraph(PDPageContentStream s, String t, float y) throws IOException {
        String remaining = t;
        while (remaining.length() > 92) {
            int cut = remaining.lastIndexOf(' ', 92);
            if (cut < 1) cut = 92;
            line(s, remaining.substring(0, cut), 50, y, 11, false);
            remaining = remaining.substring(cut).trim(); y -= 16;
        }
        line(s, remaining, 50, y, 11, false);
        return y - 18;
    }
    private float line(PDPageContentStream s, String t, float x, float y, int size, boolean bold) throws IOException {
        var font = new PDType1Font(bold ? Standard14Fonts.FontName.HELVETICA_BOLD : Standard14Fonts.FontName.HELVETICA);
        String safe = t.replace("’", "'").replace("—", "-").replace("°", "o")
                .replace('\u202F', ' ').replace('\u00A0', ' ')
                .replace("é", "e").replace("è", "e").replace("ê", "e").replace("à", "a")
                .replace("ù", "u").replace("ç", "c").replace("ô", "o").replace("î", "i").replace("É", "E");
        s.beginText(); s.setFont(font, size); s.newLineAtOffset(x,y); s.showText(safe); s.endText(); return y;
    }
}
