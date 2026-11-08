package com.gestion.service;

import com.gestion.enums.TypeMouvementCarriere;
import com.gestion.model.Departement;
import com.gestion.model.Employe;
import com.gestion.model.HistoriqueCarriere;
import com.gestion.model.Poste;
import com.gestion.model.Utilisateur;
import com.gestion.repository.HistoriqueCarriereRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HistoriqueCarriereService {

    private final HistoriqueCarriereRepository historiqueCarriereRepository;

    @Transactional
    public HistoriqueCarriere enregistrer(Employe employe, Poste poste, Departement departement,
                                          Employe manager, LocalDate dateEffet, String motif,
                                          Utilisateur auteur) {
        if (employe == null) {
            return null;
        }
        TypeMouvementCarriere mouvement = convertir(motif);
        HistoriqueCarriere historique = HistoriqueCarriere.builder()
                .employe(employe)
                .poste(poste)
                .departement(departement)
                .manager(manager)
                .dateEffet(dateEffet == null ? LocalDate.now() : dateEffet)
                .motif(mouvement)
                .auteur(auteur)
                .dateCreation(LocalDateTime.now())
                .build();
        return historiqueCarriereRepository.save(historique);
    }

    @Transactional(readOnly = true)
    public List<HistoriqueCarriere> lister(Integer idEmp) {
        return historiqueCarriereRepository.findByEmployeIdEmpOrderByDateEffetDesc(idEmp);
    }

    private TypeMouvementCarriere convertir(String motif) {
        if (motif == null) {
            return TypeMouvementCarriere.AFFECTATION;
        }
        try {
            return TypeMouvementCarriere.valueOf(motif.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return TypeMouvementCarriere.AFFECTATION;
        }
    }
}
