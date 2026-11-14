package com.sirh.backend.modules.recrutement.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.sirh.backend.modules.organisation.entity.DepartementEntity;
import com.sirh.backend.modules.organisation.entity.PosteEntity;
import com.sirh.backend.modules.organisation.repository.DepartementRepository;
import com.sirh.backend.modules.organisation.repository.PosteRepository;
import com.sirh.backend.modules.auth.repository.AuthUserRepository;
import com.sirh.backend.modules.notifications.service.NotificationCenterService;
import com.sirh.backend.modules.recrutement.dto.CreateDemandeRequest;
import com.sirh.backend.modules.recrutement.entity.CircuitValidationEntity;
import com.sirh.backend.modules.recrutement.entity.MotifRecrutementEntity;
import com.sirh.backend.modules.recrutement.entity.StatutDemandeEntity;
import com.sirh.backend.modules.recrutement.repository.CircuitValidationRepository;
import com.sirh.backend.modules.recrutement.repository.DemandeRecrutementRepository;
import com.sirh.backend.modules.recrutement.repository.MotifRecrutementRepository;
import com.sirh.backend.modules.recrutement.repository.StatutDemandeRepository;
import com.sirh.backend.modules.recrutement.repository.ValidationDemandeRepository;
import com.sirh.backend.modules.recrutement.repository.EtapeCircuitRepository;
import com.sirh.backend.modules.recrutement.repository.DecisionValidationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ManagerRecrutementServiceTest {
    private DepartementRepository departements;
    private PosteRepository postes;
    private MotifRecrutementRepository motifs;
    private StatutDemandeRepository statuts;
    private CircuitValidationRepository circuits;
    private DemandeRecrutementRepository demandes;
    private ValidationDemandeRepository validations;
    private EtapeCircuitRepository etapes;
    private DecisionValidationRepository decisions;
    private AuthUserRepository users;
    private NotificationCenterService notificationCenter;
    private ManagerRecrutementService service;

    @BeforeEach
    void setUp() {
        departements = mock(DepartementRepository.class);
        postes = mock(PosteRepository.class);
        motifs = mock(MotifRecrutementRepository.class);
        statuts = mock(StatutDemandeRepository.class);
        circuits = mock(CircuitValidationRepository.class);
        demandes = mock(DemandeRecrutementRepository.class);
        validations = mock(ValidationDemandeRepository.class);
        etapes = mock(EtapeCircuitRepository.class);
        decisions = mock(DecisionValidationRepository.class);
        users = mock(AuthUserRepository.class);
        notificationCenter = mock(NotificationCenterService.class);
        service = new ManagerRecrutementService(departements, postes, motifs, statuts, circuits, demandes,
            validations, etapes, decisions, users, notificationCenter);

        var department = mock(DepartementEntity.class);
        when(department.getId()).thenReturn(12);
        when(departements.findActiveForEmployeeUser(5)).thenReturn(List.of(department));

        var post = mock(PosteEntity.class);
        when(post.getId()).thenReturn(30);
        when(post.getDepartementId()).thenReturn(12);
        when(post.isActif()).thenReturn(true);
        when(post.getIntitule()).thenReturn("Poste existant");
        when(post.getSalaireBaseMin()).thenReturn(new BigDecimal("500.00"));
        when(post.getSalaireBaseMax()).thenReturn(new BigDecimal("1000.00"));
        when(postes.findById(30)).thenReturn(Optional.of(post));

        var motif = mock(MotifRecrutementEntity.class);
        when(motif.getId()).thenReturn(2);
        when(motif.getLibelle()).thenReturn("Renforcement");
        when(motifs.findById(2)).thenReturn(Optional.of(motif));

        var status = mock(StatutDemandeEntity.class);
        when(status.getId()).thenReturn(4);
        when(status.getLibelle()).thenReturn("Soumise");
        when(statuts.findByCode("A_VALIDER_RH")).thenReturn(Optional.of(status));

        var circuit = mock(CircuitValidationEntity.class);
        when(circuit.getId()).thenReturn(1);
        when(circuit.isActif()).thenReturn(true);
        when(circuits.findFirstByActifTrue()).thenReturn(Optional.of(circuit));
        when(demandes.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void submitsRequestForAnExistingPostAndKeepsItInSubmittedStatus() {
        var response = service.submit(5, request("750.00"));

        assertEquals("Poste existant", response.poste());
        assertEquals("Renforcement", response.motif());
        assertEquals("Soumise", response.statut());
        assertEquals(new BigDecimal("750.00"), response.budgetSalaireBase());
        verify(demandes).save(any());
    }

    @Test
    void rejectsBudgetBelowThePostsSalaryRange() {
        assertThrows(ResponseStatusException.class, () -> service.submit(5, request("499.99")));
        verify(demandes, never()).save(any());
    }

    @Test
    void rejectsBudgetAboveThePostsSalaryRange() {
        assertThrows(ResponseStatusException.class, () -> service.submit(5, request("1000.01")));
        verify(demandes, never()).save(any());
    }

    private CreateDemandeRequest request(String budget) {
        return new CreateDemandeRequest(30, 2, 1, new BigDecimal(budget), "Besoin justifié", LocalDate.now().plusDays(30));
    }
}
