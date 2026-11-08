package com.gestion.service;

import com.gestion.exception.RessourceIntrouvableException;
import com.gestion.model.DocumentEmploye;
import com.gestion.model.Employe;
import com.gestion.repository.DocumentEmployeRepository;
import com.gestion.repository.EmployeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentEmployeService {

    private final DocumentEmployeRepository documentRepository;
    private final EmployeRepository employeRepository;
    private final StockageDocumentService stockageDocumentService;

    @Transactional
    public DocumentEmploye ajouter(Integer idEmp, String typeDocument, LocalDate dateEmission,
                                   LocalDate dateValidite, String commentaire, MultipartFile fichier) {
        Employe employe = employeRepository.findById(idEmp)
                .orElseThrow(() -> new RessourceIntrouvableException("Employe introuvable : " + idEmp));
        if (fichier == null || fichier.isEmpty()) {
            throw new IllegalStateException("Le fichier du document est obligatoire.");
        }
        if (typeDocument == null || typeDocument.isBlank()) {
            throw new IllegalStateException("Le type de document est obligatoire.");
        }
        String nom = stockageDocumentService.enregistrer(fichier, "EMP", idEmp);
        DocumentEmploye document = DocumentEmploye.builder()
                .employe(employe)
                .typeDocument(typeDocument.trim())
                .nomOriginal(fichier.getOriginalFilename())
                .chemin(nom)
                .dateEmission(dateEmission)
                .dateValidite(dateValidite)
                .commentaire(texteOuNull(commentaire))
                .dateUpload(LocalDateTime.now())
                .build();
        return documentRepository.save(document);
    }

    @Transactional(readOnly = true)
    public List<DocumentEmploye> listerParEmploye(Integer idEmp) {
        return documentRepository.findByEmployeIdEmpOrderByDateUploadDesc(idEmp);
    }

    @Transactional(readOnly = true)
    public List<DocumentEmploye> listerTous() {
        return documentRepository.findAllByOrderByDateUploadDesc();
    }

    @Transactional(readOnly = true)
    public DocumentEmploye trouver(Integer id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Document introuvable : " + id));
    }

    @Transactional(readOnly = true)
    public List<DocumentEmploye> documentsExpirant(int jours) {
        LocalDate aujourdHui = LocalDate.now();
        return documentRepository.findByDateValiditeBetweenAndAlerteExpirationEnvoyeeFalse(
                aujourdHui, aujourdHui.plusDays(jours));
    }

    public Path chemin(DocumentEmploye document) {
        return stockageDocumentService.chemin(document.getChemin());
    }

    private String texteOuNull(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur.trim();
    }
}
