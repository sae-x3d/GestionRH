package com.gestion.service;

import com.gestion.dto.AccreditationForm;
import com.gestion.exception.AccesRefuseException;
import com.gestion.model.Accreditation;
import com.gestion.model.Candidature;
import com.gestion.model.Utilisateur;
import com.gestion.repository.AccreditationRepository;
import com.gestion.repository.CandidatureRepository;
import com.gestion.repository.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccreditationServiceTests {

    @Mock private AccreditationRepository accreditationRepository;
    @Mock private CandidatureRepository candidatureRepository;
    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private ValidationWorkflowService workflowService;
    @Mock private StockageDocumentService stockageDocumentService;
    @Mock private MultipartFile document;

    private AccreditationService service;

    @BeforeEach
    void setUp() {
        service = new AccreditationService(accreditationRepository, candidatureRepository,
                utilisateurRepository, workflowService, stockageDocumentService);
    }

    @Test
    void unCandidatNePeutPasDeclarerUneAccreditationPourUnAutreUtilisateur() {
        Utilisateur candidat = utilisateur(9, "user.simple");
        AccreditationForm form = form(null);
        form.setIdUtilisateur(42);

        assertThrows(AccesRefuseException.class,
                () -> service.soumettre(form, document, candidat));

        verify(accreditationRepository, never()).save(any(Accreditation.class));
    }

    @Test
    void leRhNePeutPasRattacherLaCandidatureDUnAutreTitulaire() {
        Utilisateur rh = utilisateur(2, "rh.sophie");
        Utilisateur titulaire = utilisateur(9, "user.simple");
        Utilisateur autre = utilisateur(10, "finance.claire");
        when(workflowService.estRh(rh)).thenReturn(true);
        when(utilisateurRepository.findById(9)).thenReturn(Optional.of(titulaire));
        Candidature candidature = Candidature.builder()
                .idCandidature(7)
                .utilisateur(autre)
                .build();
        when(candidatureRepository.findByIdAvecDetails(7)).thenReturn(Optional.of(candidature));
        AccreditationForm form = form(7);
        form.setIdUtilisateur(9);

        assertThrows(AccesRefuseException.class,
                () -> service.soumettre(form, document, rh));

        verify(accreditationRepository, never()).save(any(Accreditation.class));
    }

    @Test
    void leTitulairePeutDeclarerSaPropreAccreditation() {
        Utilisateur candidat = utilisateur(9, "user.simple");
        Candidature candidature = Candidature.builder()
                .idCandidature(7)
                .utilisateur(candidat)
                .build();
        when(candidatureRepository.findByIdAvecDetails(7)).thenReturn(Optional.of(candidature));
        when(stockageDocumentService.enregistrer(any(), anyString(), anyInt())).thenReturn("acc.pdf");
        when(document.getOriginalFilename()).thenReturn("acc.pdf");
        when(accreditationRepository.save(any(Accreditation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Accreditation accreditation = service.soumettre(form(7), document, candidat);

        verify(accreditationRepository).save(eq(accreditation));
        verify(workflowService).ouvrir(any(), any(), any(), eq(candidat), anyString(), anyString());
    }

    private AccreditationForm form(Integer idCandidature) {
        AccreditationForm form = new AccreditationForm();
        form.setIdCandidature(idCandidature);
        form.setTypeAccreditation("Certification comptable");
        form.setOrganisme("Ordre des experts");
        form.setDateObtention(LocalDate.of(2026, 1, 15));
        return form;
    }

    private Utilisateur utilisateur(Integer id, String username) {
        return Utilisateur.builder().idUser(id).username(username).build();
    }
}
