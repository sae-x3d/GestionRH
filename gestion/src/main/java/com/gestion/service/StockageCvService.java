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

@Service
public class StockageCvService {

    private static final List<String> EXTENSIONS_AUTORISEES = List.of("pdf", "doc", "docx");

    private final Path dossierCv;

    public StockageCvService(@Value("${app.candidature.dossier-cv:uploads/cv}") String dossierCv) {
        this.dossierCv = Paths.get(dossierCv).toAbsolutePath().normalize();
    }

    public String enregistrer(MultipartFile fichier, String username, Integer idCandidature) {
        if (fichier == null || fichier.isEmpty()) {
            throw new IllegalStateException("Le CV est obligatoire.");
        }

        String extension = extraireExtension(fichier.getOriginalFilename());
        if (!EXTENSIONS_AUTORISEES.contains(extension)) {
            throw new IllegalStateException("Format de CV non autorise (pdf, doc ou docx).");
        }

        String nomFichier = "CV_" + username + "_" + idCandidature + "." + extension;
        try {
            Files.createDirectories(dossierCv);
            Path cible = dossierCv.resolve(nomFichier).normalize();
            if (!cible.getParent().equals(dossierCv)) {
                throw new IllegalStateException("Nom de fichier invalide.");
            }
            Files.copy(fichier.getInputStream(), cible, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new RessourceIntrouvableException("Echec de l'enregistrement du CV : " + ex.getMessage());
        }
        return nomFichier;
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
