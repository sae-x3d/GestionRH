package com.gestion.service;

import com.gestion.model.DroitConge;
import com.gestion.model.JourFerie;
import com.gestion.model.RegimeTravail;
import com.gestion.model.TypeConge;
import com.gestion.repository.DroitCongeRepository;
import com.gestion.repository.JourFerieRepository;
import com.gestion.repository.RegimeTravailRepository;
import com.gestion.repository.TypeCongeRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CongeReferentielService {

    private static final Logger log = LoggerFactory.getLogger(CongeReferentielService.class);

    private final TypeCongeRepository typeCongeRepository;
    private final JourFerieRepository jourFerieRepository;
    private final RegimeTravailRepository regimeTravailRepository;
    private final DroitCongeRepository droitCongeRepository;

    @Transactional(readOnly = true)
    public List<TypeConge> types() {
        return typeCongeRepository.findAllByOrderByLibelleAsc();
    }

    @Transactional(readOnly = true)
    public List<TypeConge> typesActifs() {
        return typeCongeRepository.findByActifTrueOrderByLibelleAsc();
    }

    @Transactional(readOnly = true)
    public List<JourFerie> joursFeries() {
        return jourFerieRepository.findAllByOrderByDateJourFerieAsc();
    }

    @Transactional(readOnly = true)
    public List<RegimeTravail> regimes() {
        return regimeTravailRepository.findAllByOrderByLibelleAsc();
    }

    @Transactional(readOnly = true)
    public List<DroitConge> droits() {
        return droitCongeRepository.findAllByOrderByAncienneteMinMoisAsc();
    }

    @Transactional
    public TypeConge ajouterType(String code, String libelle, boolean decompteSolde,
                                 boolean justificatifRequis) {
        return ajouterType(code, libelle, decompteSolde, justificatifRequis, 0, null);
    }

    @Transactional
    public TypeConge ajouterType(String code, String libelle, boolean decompteSolde,
                                 boolean justificatifRequis, Integer preavisJours,
                                 BigDecimal dureeMaxJours) {
        return ajouterType(code, libelle, decompteSolde, justificatifRequis, preavisJours, dureeMaxJours, false);
    }

    @Transactional
    public TypeConge ajouterType(String code, String libelle, boolean decompteSolde,
                                 boolean justificatifRequis, Integer preavisJours,
                                 BigDecimal dureeMaxJours, Boolean validationDirection) {
        return ajouterType(code, libelle, decompteSolde, justificatifRequis, preavisJours,
                dureeMaxJours, validationDirection, null);
    }

    @Transactional
    public TypeConge ajouterType(String code, String libelle, boolean decompteSolde,
                                 boolean justificatifRequis, Integer preavisJours,
                                 BigDecimal dureeMaxJours, Boolean validationDirection,
                                 BigDecimal directionAPartirJours) {
        if (code == null || code.isBlank() || libelle == null || libelle.isBlank()) {
            throw new IllegalStateException("Le code et le libelle du type de conge sont obligatoires.");
        }
        if (preavisJours == null || preavisJours < 0 || (dureeMaxJours != null && dureeMaxJours.signum() <= 0)
                || (directionAPartirJours != null && directionAPartirJours.signum() <= 0)) {
            throw new IllegalStateException("Le preavis doit etre positif ou nul et la duree maximale strictement positive.");
        }
        if (typeCongeRepository.findByCodeIgnoreCase(code.trim()).isPresent()) {
            throw new IllegalStateException("Un type de conge avec ce code existe deja.");
        }
        return typeCongeRepository.save(TypeConge.builder()
                .code(code.trim().toUpperCase())
                .libelle(libelle.trim())
                .decompteSolde(decompteSolde)
                .justificatifRequis(justificatifRequis)
                .preavisJours(preavisJours)
                .dureeMaxJours(dureeMaxJours)
                .validationDirection(validationDirection)
                .directionAPartirJours(directionAPartirJours)
                .actif(true)
                .build());
    }

    @Transactional
    public JourFerie ajouterJourFerie(LocalDate date, String libelle) {
        if (date == null || libelle == null || libelle.isBlank()) {
            throw new IllegalStateException("La date et le libelle du jour ferie sont obligatoires.");
        }
        if (jourFerieRepository.existsByDateJourFerie(date)) {
            throw new IllegalStateException("Un jour ferie existe deja a cette date.");
        }
        return jourFerieRepository.save(JourFerie.builder()
                .dateJourFerie(date)
                .libelle(libelle.trim())
                .build());
    }

    @Transactional
    public RegimeTravail ajouterRegime(String libelle, BigDecimal heuresHebdomadaires, String joursOuvres) {
        if (libelle == null || libelle.isBlank()) {
            throw new IllegalStateException("Le libelle du regime est obligatoire.");
        }
        return regimeTravailRepository.save(RegimeTravail.builder()
                .libelle(libelle.trim())
                .heuresHebdomadaires(heuresHebdomadaires)
                .joursOuvres(joursOuvres == null || joursOuvres.isBlank() ? "LUN,MAR,MER,JEU,VEN" : joursOuvres)
                .actif(true)
                .build());
    }

    @Transactional
    public DroitConge ajouterDroit(Integer idTypeConge, Integer ancienneteMinMois, BigDecimal joursParAn) {
        TypeConge type = typeCongeRepository.findById(idTypeConge)
                .orElseThrow(() -> new IllegalStateException("Type de conge introuvable."));
        if (joursParAn == null || joursParAn.signum() < 0) {
            throw new IllegalStateException("Le nombre de jours par an doit etre positif.");
        }
        return droitCongeRepository.save(DroitConge.builder()
                .typeConge(type)
                .ancienneteMinMois(ancienneteMinMois == null ? 0 : ancienneteMinMois)
                .joursParAn(joursParAn)
                .build());
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initialiserReferentiels() {
        if (typeCongeRepository.count() == 0) {
            TypeConge paye = typeCongeRepository.save(TypeConge.builder().code("CONGE_PAYE")
                    .libelle("Conges payes").decompteSolde(true).build());
            typeCongeRepository.save(TypeConge.builder().code("RTT").libelle("RTT")
                    .decompteSolde(true).build());
            typeCongeRepository.save(TypeConge.builder().code("MALADIE").libelle("Absence maladie")
                    .decompteSolde(false).justificatifRequis(true).build());
            typeCongeRepository.save(TypeConge.builder().code("MATERNITE").libelle("Conge maternite")
                    .decompteSolde(false).justificatifRequis(true).build());
            typeCongeRepository.save(TypeConge.builder().code("SANS_SOLDE").libelle("Conge sans solde")
                    .decompteSolde(false).build());
            droitCongeRepository.save(DroitConge.builder().typeConge(paye)
                    .ancienneteMinMois(0).joursParAn(new BigDecimal("30.0")).build());
            log.info("Referentiels de conges initialises.");
        }
        for (String codeAbsence : List.of("MALADIE", "MATERNITE")) {
            typeCongeRepository.findByCodeIgnoreCase(codeAbsence).ifPresent(type -> {
                type.setActif(false);
                typeCongeRepository.save(type);
            });
        }
        if (regimeTravailRepository.count() == 0) {
            regimeTravailRepository.save(RegimeTravail.builder().libelle("Temps plein 35h")
                    .heuresHebdomadaires(new BigDecimal("35.0"))
                    .joursOuvres("LUN,MAR,MER,JEU,VEN").build());
        }
        if (jourFerieRepository.count() == 0) {
            int annee = LocalDate.now().getYear();
            jourFerieRepository.save(JourFerie.builder().dateJourFerie(LocalDate.of(annee, 1, 1))
                    .libelle("Jour de l'an").build());
            jourFerieRepository.save(JourFerie.builder().dateJourFerie(LocalDate.of(annee, 5, 1))
                    .libelle("Fete du travail").build());
            jourFerieRepository.save(JourFerie.builder().dateJourFerie(LocalDate.of(annee, 12, 25))
                    .libelle("Noel").build());
        }
    }
}
