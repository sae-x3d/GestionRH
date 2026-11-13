package com.gestion.service;

import com.gestion.model.Employe;
import com.gestion.model.HistoriqueSalaire;
import com.gestion.repository.ChangementSensibleRepository;
import com.gestion.repository.EmployeRepository;
import com.gestion.repository.HistoriqueSalaireRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DossierAdministratifServiceTests {
    @Mock private EmployeRepository employeRepository;
    @Mock private HistoriqueSalaireRepository historiqueSalaireRepository;
    @Mock private ChangementSensibleRepository changementRepository;
    @Mock private HistoriqueCarriereService historiqueCarriereService;
    @Mock private AuditService auditService;

    @Test void traceLeSalaireAvantEtApresSaModification() {
        Employe employe = Employe.builder().idEmp(4).nomEmp("Test").salaireEmp(500000).build();
        when(employeRepository.findById(4)).thenReturn(Optional.of(employe));
        when(historiqueSalaireRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        DossierAdministratifService service = new DossierAdministratifService(employeRepository,
                historiqueSalaireRepository, changementRepository, historiqueCarriereService, auditService);

        service.changerSalaire(4, 600000, LocalDate.of(2026, 12, 1), "Promotion", null);

        assertEquals(600000, employe.getSalaireEmp());
        ArgumentCaptor<String> details = ArgumentCaptor.forClass(String.class);
        verify(auditService).enregistrer(eq("system"), eq("SALAIRE"), eq("/employes/4"),
                eq("Employe"), eq("4"), details.capture(), isNull());
        assertEquals(true, details.getValue().contains("avant={salaire:500000}"));
        assertEquals(true, details.getValue().contains("apres={salaire:600000"));
    }
}
