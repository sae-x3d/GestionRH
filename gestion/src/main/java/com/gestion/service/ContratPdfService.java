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
import java.text.Normalizer;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class ContratPdfService {

    private static final float MARGE = 52;
    private static final float LARGEUR = PDRectangle.A4.getWidth() - MARGE * 2;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final PDType1Font NORMAL = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDType1Font GRAS = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    public byte[] generer(Contrat contrat) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {
            PdfWriter pdf = new PdfWriter(document);
            String statut = "ACCEPTE".equalsIgnoreCase(contrat.getStatut())
                    ? "CONTRAT DE TRAVAIL " : "PROPOSITION DE CONTRAT DE TRAVAIL ";
            pdf.titre(statut + contrat.getTypeContrat() + " - " + contrat.getNumeroContrat());
            if (!"ACCEPTE".equalsIgnoreCase(contrat.getStatut())) {
                pdf.texte("En attente de l'acceptation du/de la salarie(e).", 10);
            } else if (contrat.getDateAcceptation() != null) {
                pdf.texte("Acceptation enregistree le " + DATE.format(contrat.getDateAcceptation().toLocalDate()), 10);
            }
            pdf.texte("Document genere le " + DATE.format(contrat.getDateGeneration().toLocalDate()), 9);
            pdf.espace(12);
            pdf.section("ENTRE LES SOUSSIGNES");
            pdf.paragraphe("L'employeur : " + contrat.getNomEmployeur() + ", dont l'adresse est "
                    + contrat.getAdresseEmployeur() + ".");
            pdf.paragraphe("Et le/la salarie(e) : " + contrat.getNomSalarie() + ", demeurant a "
                    + contrat.getAdresseSalarie() + ".");
            pdf.paragraphe("Les parties conviennent des conditions de travail suivantes.");

            pdf.section("POSTE ET CLASSIFICATION");
            pdf.paragraphe("Fonction : " + contrat.getFonction() + ".");
            pdf.paragraphe("Categorie professionnelle : " + contrat.getCategorieProfessionnelle() + ".");
            pdf.paragraphe("Indice minimum de classification : " + contrat.getIndiceClassification() + ".");
            pdf.paragraphe("Lieu de travail : " + contrat.getLieuTravail() + ".");

            pdf.section("DUREE ET DATE D'EFFET");
            if ("CDD".equalsIgnoreCase(contrat.getTypeContrat())) {
                pdf.paragraphe("Le present contrat est conclu a duree determinee, du "
                        + DATE.format(contrat.getDateEffet()) + " au " + DATE.format(contrat.getDateFin()) + ".");
                pdf.paragraphe("Motif et nature temporaire du besoin : " + contrat.getMotifCdd() + ".");
            } else {
                pdf.paragraphe("Le present contrat est conclu a duree indeterminee a compter du "
                        + DATE.format(contrat.getDateEffet()) + ".");
            }

            pdf.section("REMUNERATION ET TEMPS DE TRAVAIL");
            pdf.paragraphe("Salaire mensuel brut : "
                    + NumberFormat.getIntegerInstance(Locale.FRANCE).format(contrat.getSalaire())
                    + " Ariary, payable mensuellement selon les dispositions applicables.");
            pdf.paragraphe("Duree du travail : " + contrat.getDureeTravail() + ".");
            pdf.paragraphe("Les conditions d'execution du travail sont soumises au Code du travail malgache, "
                    + "a la convention collective applicable et aux dispositions plus favorables convenues entre les parties.");

            pdf.section("PRISE D'EFFET ET EXEMPLAIRES");
            pdf.paragraphe("Le contrat prend effet a la date indiquee ci-dessus, sous reserve de son acceptation "
                    + "par les deux parties. Il est etabli en deux exemplaires originaux, un pour chaque partie.");
            pdf.espace(18);
            String lieuSignature = contrat.getLieuSignature() == null || contrat.getLieuSignature().isBlank()
                    ? "______________________________" : contrat.getLieuSignature();
            pdf.paragraphe("Fait a " + lieuSignature + ", le ____ / ____ / ______");
            pdf.espace(12);
            pdf.paragraphe("L'EMPLOYEUR                                             LE / LA SALARIE(E)");
            pdf.paragraphe("Nom : _____________________________       Nom : _____________________________");
            pdf.paragraphe("Signature precedee de la mention                  Signature precedee de la mention");
            pdf.paragraphe("Lu et approuve                                            Lu et approuve");
            pdf.fermer();
            document.save(sortie);
            return sortie.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de generer le PDF du contrat.", e);
        }
    }

    private static final class PdfWriter {
        private final PDDocument document;
        private PDPageContentStream contenu;
        private float y;

        private PdfWriter(PDDocument document) throws IOException {
            this.document = document;
            nouvellePage();
        }

        private void titre(String texte) throws IOException {
            ecrire(texte, GRAS, 15);
            espace(7);
        }

        private void section(String texte) throws IOException {
            espace(8);
            ecrire(texte, GRAS, 11);
            espace(3);
        }

        private void paragraphe(String texte) throws IOException {
            String restant = nettoyer(texte);
            while (!restant.isEmpty()) {
                verifierPlace(14);
                int fin = dernierMotDansLargeur(restant, NORMAL, 10.5f, LARGEUR);
                ecrire(restant.substring(0, fin), NORMAL, 10.5f);
                restant = restant.substring(fin).stripLeading();
            }
            espace(5);
        }

        private void texte(String texte, float taille) throws IOException {
            ecrire(texte, NORMAL, taille);
        }

        private void ecrire(String texte, PDType1Font police, float taille) throws IOException {
            String propre = nettoyer(texte);
            verifierPlace(taille + 5);
            contenu.beginText();
            contenu.setFont(police, taille);
            contenu.newLineAtOffset(MARGE, y);
            contenu.showText(propre);
            contenu.endText();
            y -= taille + 5;
        }

        private void espace(float hauteur) {
            y -= hauteur;
        }

        private void verifierPlace(float hauteur) throws IOException {
            if (y - hauteur < 48) nouvellePage();
        }

        private int dernierMotDansLargeur(String texte, PDType1Font police, float taille,
                                          float largeur) throws IOException {
            int fin = texte.length();
            while (fin > 1 && police.getStringWidth(texte.substring(0, fin)) / 1000 * taille > largeur) {
                fin--;
            }
            if (fin < texte.length()) {
                int espace = texte.lastIndexOf(' ', fin);
                if (espace > 0) fin = espace;
            }
            return Math.max(1, fin);
        }

        private void nouvellePage() throws IOException {
            if (contenu != null) contenu.close();
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            contenu = new PDPageContentStream(document, page);
            y = PDRectangle.A4.getHeight() - 52;
        }

        private void fermer() throws IOException {
            if (contenu != null) contenu.close();
        }
    }

    private static String nettoyer(String valeur) {
        return Normalizer.normalize(valeur == null ? "" : valeur, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace("’", "'").replace("—", "-").replace("°", "o")
                .replace('\u202F', ' ').replace('\u00A0', ' ');
    }
}
