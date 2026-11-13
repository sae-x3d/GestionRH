package com.gestion.service;

import com.gestion.enums.StatutSortie;
import com.gestion.enums.TypeSortie;
import com.gestion.exception.RessourceIntrouvableException;
import com.gestion.model.ChecklistSortie;
import com.gestion.model.Employe;
import com.gestion.model.SortieEmploye;
import com.gestion.model.Utilisateur;
import com.gestion.repository.ChecklistSortieRepository;
import com.gestion.repository.EmployeRepository;
import com.gestion.repository.SortieEmployeRepository;
import com.gestion.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SortieService {

    private static final List<String[]> CHECKLIST_DEPART = List.of(
            new String[]{"Restitution du materiel informatique", "IT"},
            new String[]{"Desactivation des acces informatiques", "IT"},
            new String[]{"Restitution du badge et des cles", "RH"},
            new String[]{"Solde de tout compte et documents de fin de contrat", "PAIE"},
            new String[]{"Archivage du dossier administratif", "RH"});

    private final SortieEmployeRepository sortieRepository;
    private final EmployeRepository employeRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ChecklistSortieRepository checklistRepository;
    private final EmployeService employeService;
    private final NotificationService notificationService;
    private final HistoriqueCarriereService historiqueCarriereService;

    @Transactional
    public SortieEmploye creer(Integer idEmp, TypeSortie typeSortie, LocalDate dateSortie,
                               String motif, Integer preavisJours, String commentaire,
                               Utilisateur traitePar) {
        Employe employe = employeRepository.findById(idEmp)
                .orElseThrow(() -> new RessourceIntrouvableException("Employe introuvable : " + idEmp));
        if (typeSortie == null || dateSortie == null) {
            throw new IllegalStateException("Le type et la date de sortie sont obligatoires.");
        }
        SortieEmploye sortie = SortieEmploye.builder()
                .employe(employe)
                .typeSortie(typeSortie)
                .dateSortie(dateSortie)
                .motif(texteOuNull(motif))
                .preavisJours(preavisJours)
                .statut(StatutSortie.EN_COURS)
                .commentaire(texteOuNull(commentaire))
                .traitePar(traitePar)
                .dateCreation(LocalDateTime.now())
                .build();
        return sortieRepository.save(sortie);
    }

    @Transactional
    public SortieEmploye valider(Integer idSortie, Integer soldeToutCompte, Utilisateur utilisateur) {
        SortieEmploye sortie = trouver(idSortie);
        if (sortie.getStatut() != StatutSortie.EN_COURS) {
            throw new IllegalStateException("Cette sortie a deja ete traitee.");
        }
        sortie.setStatut(StatutSortie.VALIDEE);
        sortie.setSoldeToutCompte(soldeToutCompte);
        sortie.setDateValidation(LocalDateTime.now());
        sortie.setTraitePar(utilisateur);
        Employe employe = sortie.getEmploye();
        employe.setDateDepart(sortie.getDateSortie().atStartOfDay());
        employe.setStatut("SORTI");
        employeRepository.save(employe);
        employeService.rafraichirStatutPoste(employe.getPoste());

        desactiverComptes(employe);
        creerChecklistDepart(sortie);
        historiqueCarriereService.enregistrer(employe, employe.getPoste(), employe.getDepartement(),
                null, sortie.getDateSortie(), "SORTIE", utilisateur);
        notifierChecklist(sortie);
        return sortieRepository.save(sortie);
    }

    @Transactional
    public ChecklistSortie marquerChecklist(Integer idChecklist, boolean fait, String commentaire) {
        ChecklistSortie item = checklistRepository.findById(idChecklist)
                .orElseThrow(() -> new RessourceIntrouvableException("Tache de sortie introuvable."));
        item.setFait(fait);
        item.setDateFait(fait ? LocalDateTime.now() : null);
        item.setCommentaire(texteOuNull(commentaire));
        return checklistRepository.save(item);
    }

    @Transactional
    public SortieEmploye annuler(Integer idSortie) {
        SortieEmploye sortie = trouver(idSortie);
        if (sortie.getStatut() == StatutSortie.VALIDEE) {
            throw new IllegalStateException("Une sortie validee ne peut pas etre annulee.");
        }
        sortie.setStatut(StatutSortie.ANNULEE);
        return sortieRepository.save(sortie);
    }

    @Transactional(readOnly = true)
    public List<SortieEmploye> lister() {
        return sortieRepository.findAllByOrderByDateCreationDesc();
    }

    @Transactional(readOnly = true)
    public List<SortieEmploye> listerParEmploye(Integer idEmp) {
        return sortieRepository.findByEmployeIdEmpOrderByDateCreationDesc(idEmp);
    }

    @Transactional(readOnly = true)
    public List<ChecklistSortie> checklist(Integer idSortie) {
        return checklistRepository.findBySortieIdSortieOrderByResponsableAscLibelleAsc(idSortie);
    }

    @Transactional(readOnly = true)
    public SortieEmploye trouver(Integer id) {
        return sortieRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Sortie introuvable : " + id));
    }

    private void desactiverComptes(Employe employe) {
        for (Utilisateur compte : employe.getUtilisateurs()) {
            if (compte.isActif()) {
                compte.setActif(false);
                utilisateurRepository.save(compte);
            }
        }
    }

    private void creerChecklistDepart(SortieEmploye sortie) {
        if (checklistRepository.existsBySortieIdSortie(sortie.getIdSortie())) {
            return;
        }
        List<ChecklistSortie> items = CHECKLIST_DEPART.stream()
                .map(ligne -> ChecklistSortie.builder()
                        .sortie(sortie)
                        .libelle(ligne[0])
                        .responsable(ligne[1])
                        .fait(false)
                        .build())
                .toList();
        checklistRepository.saveAll(items);
    }

    private void notifierChecklist(SortieEmploye sortie) {
        for (Utilisateur gestionnaire : utilisateurRepository.trouverGestionnairesCandidatures()) {
            notificationService.envoyer(gestionnaire, "SORTIE",
                    "Checklist de depart a traiter pour " + sortie.getEmploye().getNomEmp() + ".",
                    "/sorties");
        }
    }

    private String texteOuNull(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur.trim();
    }
}
