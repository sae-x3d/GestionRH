package com.gestion.service;

import com.gestion.model.DocumentRequis;
import com.gestion.model.DocumentEmploye;
import com.gestion.model.Employe;
import com.gestion.model.Role;
import com.gestion.model.Utilisateur;
import com.gestion.repository.ContratRepository;
import com.gestion.repository.DocumentEmployeRepository;
import com.gestion.repository.DocumentRequisRepository;
import com.gestion.repository.EmployeRepository;
import com.gestion.repository.UtilisateurRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.anyList;

@ExtendWith(MockitoExtension.class)
class AlerteDocumentsRequisTests {
    @Mock private ContratRepository contratRepository;
    @Mock private DocumentEmployeRepository documentEmployeRepository;
    @Mock private DocumentRequisRepository documentRequisRepository;
    @Mock private EmployeRepository employeRepository;
    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private NotificationService notificationService;

    @Test void verifieTousLesTypesDeDocumentsActifsConfigurables() {
        AlerteService service = new AlerteService(contratRepository, documentEmployeRepository,
                documentRequisRepository, employeRepository, utilisateurRepository, notificationService);
        Utilisateur rh = Utilisateur.builder().idUser(1).role(Role.builder().nomRole("RH").build()).build();
        Employe employe = Employe.builder().idEmp(5).nomEmp("Salariée Test").build();
        when(utilisateurRepository.findAll()).thenReturn(List.of(rh));
        when(documentRequisRepository.findByActifTrueOrderByTypeDocumentAsc()).thenReturn(List.of(
                DocumentRequis.builder().typeDocument("RIB").actif(true).build(),
                DocumentRequis.builder().typeDocument("DIPLOME").actif(true).build()));
        when(employeRepository.findByDateDepartIsNullOrderByNomEmpAsc()).thenReturn(List.of(employe));
        when(documentEmployeRepository.existsByEmployeIdEmpAndTypeDocumentIgnoreCase(5, "RIB")).thenReturn(false);
        when(documentEmployeRepository.existsByEmployeIdEmpAndTypeDocumentIgnoreCase(5, "DIPLOME")).thenReturn(true);

        assertEquals(1, service.alerterDossiersIncomplets());

        verify(notificationService).envoyer(eq(rh), eq("DOSSIER"),
                contains("RIB"), eq("/employes/5"));
    }

    @Test void initialiseLesDocumentsParDefautQuandLaConfigurationEstVide() {
        AlerteService service = new AlerteService(contratRepository, documentEmployeRepository,
                documentRequisRepository, employeRepository, utilisateurRepository, notificationService);
        when(documentRequisRepository.count()).thenReturn(0L);
        when(documentRequisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.initialiserDocumentsRequis();

        ArgumentCaptor<DocumentRequis> captor = ArgumentCaptor.forClass(DocumentRequis.class);
        verify(documentRequisRepository, times(3)).save(captor.capture());
        assertEquals(List.of("PIECE_IDENTITE", "RIB", "CONTRAT_SIGNE"),
                captor.getAllValues().stream().map(DocumentRequis::getTypeDocument).toList());
    }

    @Test void envoieUneAlertePourUnDocumentExpirant() {
        Employe employe = Employe.builder().idEmp(8).build();
        DocumentEmploye doc = DocumentEmploye.builder().employe(employe).typeDocument("CARTE").
                dateValidite(java.time.LocalDate.now().plusDays(5)).build();
        Utilisateur user = Utilisateur.builder().idUser(9).build();
        when(documentEmployeRepository.findByDateValiditeBetweenAndAlerteExpirationEnvoyeeFalse(
                any(), any())).thenReturn(List.of(doc));
        when(utilisateurRepository.findByEmployeIdEmp(8)).thenReturn(java.util.Optional.of(user));
        AlerteService service = new AlerteService(contratRepository, documentEmployeRepository,
                documentRequisRepository, employeRepository, utilisateurRepository, notificationService);

        assertEquals(1, service.alerterDocumentsExpirants());
        verify(notificationService).envoyer(eq(user), eq("DOCUMENT"), contains("expire"),
                eq("/employes/8/documents"));
        assertEquals(true, doc.isAlerteExpirationEnvoyee());
    }
}
