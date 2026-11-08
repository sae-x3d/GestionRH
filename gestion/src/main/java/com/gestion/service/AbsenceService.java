package com.gestion.service;

import com.gestion.enums.StatutAbsence;
import com.gestion.enums.TypeAbsence;
import com.gestion.enums.TypeJourPlanning;
import com.gestion.exception.AccesRefuseException;
import com.gestion.exception.RessourceIntrouvableException;
import com.gestion.model.Absence;
import com.gestion.model.Employe;
import com.gestion.model.Utilisateur;
import com.gestion.repository.AbsenceRepository;
import com.gestion.repository.EmployeRepository;
import com.gestion.repository.PlanningJourRepository;
import com.gestion.repository.DemandeCongeRepository;
import com.gestion.repository.DepartementRepository;
import com.gestion.enums.StatutDemandeConge;
import com.gestion.service.StockageDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.nio.file.Path;

@Service @RequiredArgsConstructor
public class AbsenceService {
    @Value("${app.conges.seuil-absents-equipe:0.30}")
    private double seuilAbsentsEquipe = 0.30;
    private final AbsenceRepository absenceRepository;
    private final EmployeRepository employeRepository;
    private final PlanningJourRepository planningJourRepository;
    private final DemandeCongeRepository demandeCongeRepository;
    private final DepartementRepository departementRepository;
    private final StockageDocumentService stockageDocumentService;
    private final AuditService auditService;

    @Transactional
    public Absence soumettre(Employe employe, TypeAbsence type, LocalDate debut, LocalDate fin, MultipartFile justificatif) {
        if (employe == null || type == null || debut == null || fin == null || fin.isBefore(debut))
            throw new IllegalStateException("Salarie, type et periode d'absence valides obligatoires.");
        if (justificatif == null || justificatif.isEmpty())
            throw new IllegalStateException("Un justificatif est obligatoire pour une absence.");
        if (employe.getDepartement() != null && employe.getDepartement().getIdDept() != null)
            departementRepository.verrouillerPourReservation(employe.getDepartement().getIdDept());
        employeRepository.verrouillerPourDemande(employe.getIdEmp());
        if (demandeCongeRepository.existsChevauchement(employe.getIdEmp(), debut, fin,
                List.of(StatutDemandeConge.SOUMISE, StatutDemandeConge.VALIDEE_MANAGER,
                        StatutDemandeConge.ATTENTE_DIRECTION, StatutDemandeConge.VALIDEE_RH)))
            throw new IllegalStateException("Une demande de conge chevauche cette absence.");
        if (absenceRepository.existsByEmployeIdEmpAndStatutAndDateDebutLessThanEqualAndDateFinGreaterThanEqual(
                employe.getIdEmp(), StatutAbsence.SOUMISE, fin, debut)
                || absenceRepository.existsByEmployeIdEmpAndStatutAndDateDebutLessThanEqualAndDateFinGreaterThanEqual(
                employe.getIdEmp(), StatutAbsence.VALIDEE_RH, fin, debut))
            throw new IllegalStateException("Une absence existe deja sur cette periode.");
        verifierSeuilAbsentsEquipe(employe, debut, fin);
        String chemin = stockageDocumentService.enregistrer(justificatif, "ABSENCE", employe.getIdEmp());
        Absence absence = absenceRepository.save(Absence.builder().employe(employe).typeAbsence(type)
                .dateDebut(debut).dateFin(fin).justificatifChemin(chemin)
                .justificatifNom(justificatif.getOriginalFilename()).statut(StatutAbsence.SOUMISE)
                .dateCreation(LocalDateTime.now()).build());
        auditService.enregistrer("employe:" + employe.getIdEmp(), "CREATION", "/absences", "Absence",
                String.valueOf(absence.getIdAbsence()), "avant={}; apres={type:" + type + ",dateDebut:" + debut
                        + ",dateFin:" + fin + ",statut:" + absence.getStatut() + "}", null);
        return absence;
    }

    private void verifierSeuilAbsentsEquipe(Employe employe, LocalDate debut, LocalDate fin) {
        if (employe.getDepartement() == null || employe.getDepartement().getIdDept() == null) return;
        Integer dept = employe.getDepartement().getIdDept();
        long effectif = employeRepository.countByDepartementIdDeptAndDateDepartIsNull(dept);
        if (effectif == 0) return;
        for (LocalDate jour = debut; !jour.isAfter(fin); jour = jour.plusDays(1)) {
            java.util.Set<Integer> absents = new java.util.HashSet<>(demandeCongeRepository.employesAbsentsEquipe(
                    dept, jour, List.of(StatutDemandeConge.SOUMISE, StatutDemandeConge.VALIDEE_MANAGER,
                            StatutDemandeConge.ATTENTE_DIRECTION, StatutDemandeConge.VALIDEE_RH)));
            absents.addAll(absenceRepository.employesAbsentsEquipe(dept, jour,
                    List.of(StatutAbsence.SOUMISE, StatutAbsence.VALIDEE_RH)));
            absents.add(employe.getIdEmp());
            int plafond = Math.max(1, (int) Math.ceil(effectif * Math.max(0, Math.min(1, seuilAbsentsEquipe))));
            if (absents.size() > plafond)
                throw new IllegalStateException("Le seuil d'absents autorise dans l'equipe serait depasse le " + jour + ".");
        }
    }

    @Transactional
    public Absence decider(Integer id, boolean accepter, Utilisateur validateur) {
        if (!"RH".equals(role(validateur)) && !"ADMINISTRATEUR".equals(role(validateur)))
            throw new AccesRefuseException("Seule la RH peut traiter une absence.");
        Absence absence = absenceRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Absence introuvable."));
        if (absence.getStatut() != StatutAbsence.SOUMISE) throw new IllegalStateException("Absence deja traitee.");
        absence.setStatut(accepter ? StatutAbsence.VALIDEE_RH : StatutAbsence.REFUSEE);
        absence.setValidePar(validateur);
        absence.setDateValidation(LocalDateTime.now());
        if (accepter) {
            for (LocalDate d = absence.getDateDebut(); !d.isAfter(absence.getDateFin()); d = d.plusDays(1)) {
                planningJourRepository.findByPlanningEmployeIdEmpAndDateJour(absence.getEmploye().getIdEmp(), d)
                        .ifPresent(j -> { j.setTypeJour(TypeJourPlanning.CONGE); j.setHeureDebut(null);
                            j.setHeureFin(null); j.setPauseMinutes(null); planningJourRepository.save(j); });
            }
        }
        return absenceRepository.save(absence);
    }

    @Transactional(readOnly = true)
    public List<Absence> lister(Employe employe, Utilisateur utilisateur) {
        boolean rh = "RH".equals(role(utilisateur)) || "ADMINISTRATEUR".equals(role(utilisateur));
        if (rh) return absenceRepository.findAll();
        return employe == null ? List.of() : absenceRepository.findByEmployeIdEmpOrderByDateDebutDesc(employe.getIdEmp());
    }

    @Transactional(readOnly = true)
    public Path justificatif(Integer id, Utilisateur user) {
        Absence absence = absenceRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Absence introuvable."));
        boolean rh = "RH".equals(role(user)) || "ADMINISTRATEUR".equals(role(user));
        if (!rh && (user == null || user.getEmploye() == null
                || !absence.getEmploye().getIdEmp().equals(user.getEmploye().getIdEmp())))
            throw new AccesRefuseException("Acces refuse au justificatif d'absence.");
        return stockageDocumentService.chemin(absence.getJustificatifChemin());
    }

    private String role(Utilisateur user) { return user == null || user.getRole() == null ? ""
            : user.getRole().getNomRole().replace("ROLE_", "").toUpperCase(); }
}
