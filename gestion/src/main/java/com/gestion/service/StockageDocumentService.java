package com.gestion.service;

import com.gestion.exception.RessourceIntrouvableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class StockageDocumentService {

    private static final List<String> EXTENSIONS_AUTORISEES = List.of("pdf", "jpg", "jpeg", "png");

    private final Path dossierDocuments;

    public StockageDocumentService(@Value("${app.document.dossier:uploads/documents}") String dossierDocuments) {
        this.dossierDocuments = Paths.get(dossierDocuments).toAbsolutePath().normalize();
    }

    public String enregistrer(MultipartFile fichier, String prefixe, Integer idObjet) {
        if (fichier == null || fichier.isEmpty()) {
            throw new IllegalStateException("Le document justificatif est obligatoire.");
        }
        String extension = extraireExtension(fichier.getOriginalFilename());
        if (!EXTENSIONS_AUTORISEES.contains(extension)) {
            throw new IllegalStateException("Format de document non autorise (pdf, jpg, jpeg ou png).");
        }
        String nomFichier = nettoyer(prefixe) + "_" + idObjet + "_"
                + UUID.randomUUID().toString().substring(0, 8) + "." + extension;
        try {
            Files.createDirectories(dossierDocuments);
            Path cible = dossierDocuments.resolve(nomFichier).normalize();
            if (!cible.getParent().equals(dossierDocuments)) {
                throw new IllegalStateException("Nom de fichier invalide.");
            }
            Files.copy(fichier.getInputStream(), cible, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new RessourceIntrouvableException(
                    "Echec de l'enregistrement du document : " + ex.getMessage());
        }
        return nomFichier;
    }

    public Path chemin(String nomFichier) {
        if (nomFichier == null || nomFichier.isBlank()) {
            throw new RessourceIntrouvableException("Document introuvable.");
        }
        Path cible = dossierDocuments.resolve(nomFichier).normalize();
        if (!cible.getParent().equals(dossierDocuments)) {
            throw new RessourceIntrouvableException("Document introuvable.");
        }
        return cible;
    }

    private String nettoyer(String valeur) {
        if (valeur == null || valeur.isBlank()) {
            return "DOC";
        }
        return valeur.replaceAll("[^A-Za-z0-9_-]", "_");
    }

    private String extraireExtension(String nomFichier) {
        if (nomFichier == null) {
            return "";
        }
        int point = nomFichier.lastIndexOf('.');
        if (point < 0 || point == nomFichier.length() - 1) {
            return "";
        }
        return nomFichier.substring(point + 1).toLowerCase(Locale.ROOT);
    }
}
