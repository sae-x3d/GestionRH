package com.gestion.service;

import com.gestion.dto.CreerContratDTO;
import com.gestion.model.*;
import com.gestion.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;

@Service @RequiredArgsConstructor
public class ContratService {
    private final ContratRepository contratRepository;
    private final CandidatureRepository candidatureRepository;
    private final EmployeRepository employeRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ProvenanceRepository provenanceRepository;
    private final ContratPdfService pdfService;

    @Transactional(readOnly=true)
    public List<Contrat> contratsCandidat(Integer userId) { return contratRepository.findByCandidatureUtilisateurIdUserOrderByDateGenerationDesc(userId); }
    @Transactional(readOnly=true)
    public List<Contrat> contratsRh() { return contratRepository.findAll(); }
    @Transactional(readOnly=true)
    public Contrat trouver(Integer id) { return contratRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Contrat introuvable.")); }
    @Transactional(readOnly=true)
    public Contrat contratCandidature(Integer id) { return contratRepository.findByCandidatureIdCandidature(id).orElse(null); }

    @Transactional
    public Contrat generer(Integer candidatureId, CreerContratDTO dto) {
        Candidature candidature = candidatureRepository.findByIdAvecDetails(candidatureId)
                .orElseThrow(() -> new IllegalArgumentException("Candidature introuvable."));
        if (!"ACCEPTEE".equalsIgnoreCase(candidature.getStatut().getLibelle()))
            throw new IllegalStateException("La candidature doit etre acceptee par RH avant l'emission du contrat.");
        if (contratRepository.findByCandidatureIdCandidature(candidatureId).isPresent())
            throw new IllegalStateException("Un contrat existe deja pour cette candidature.");
        String type = dto.getTypeContrat().trim().toUpperCase();
        if (!List.of("CDI", "CDD").contains(type)) throw new IllegalStateException("Type de contrat invalide.");
        if (dto.getDateFin() != null && !dto.getDateFin().isAfter(dto.getDateEffet())) throw new IllegalStateException("La date de fin doit etre apres la date d'effet.");
        if ("CDD".equals(type) && (dto.getDateFin()==null || dto.getMotifCdd()==null || dto.getMotifCdd().isBlank()))
            throw new IllegalStateException("Pour un CDD, le terme et son motif doivent etre precises.");
        if ("CDD".equals(type) && (Period.between(dto.getDateEffet(), dto.getDateFin()).toTotalMonths() < 1
                || Period.between(dto.getDateEffet(), dto.getDateFin()).toTotalMonths() > 24))
            throw new IllegalStateException("La duree d'un CDD doit etre comprise entre un mois et deux ans.");
        if ("CDI".equals(type) && dto.getDateFin()!=null) throw new IllegalStateException("Un CDI ne comporte pas de date de fin.");
        var offre = candidature.getOffre();
        if (offre.getSalaire()==null || offre.getSalaire()<=0) throw new IllegalStateException("Le salaire valide de l'offre est manquant.");
        Contrat c = Contrat.builder().candidature(candidature).typeContrat(type)
                .nomSalarie(dto.getNomSalarie().trim()).adresseSalarie(dto.getAdresseSalarie().trim())
                .nomEmployeur(dto.getNomEmployeur().trim()).adresseEmployeur(dto.getAdresseEmployeur().trim())
                .fonction(offre.getTitre()).categorieProfessionnelle(dto.getCategorieProfessionnelle().trim())
                .indiceClassification(dto.getIndiceClassification().trim()).salaire(offre.getSalaire())
                .dureeTravail(dto.getDureeTravail().trim()).lieuTravail(dto.getLieuTravail().trim())
                .dateEffet(dto.getDateEffet()).dateFin(dto.getDateFin()).motifCdd(dto.getMotifCdd())
                .statut("EN_ATTENTE_ACCEPTATION").dateGeneration(LocalDateTime.now()).build();
        c.setPdf(pdfService.generer(c));
        return contratRepository.save(c);
    }

    @Transactional
    public Contrat repondre(Integer contratId, Integer userId, boolean accepte) {
        Contrat c = contratRepository.findById(contratId).orElseThrow(() -> new IllegalArgumentException("Contrat introuvable."));
        if (!c.getCandidature().getUtilisateur().getIdUser().equals(userId)) throw new IllegalStateException("Ce contrat ne vous appartient pas.");
        if (!"EN_ATTENTE_ACCEPTATION".equals(c.getStatut())) throw new IllegalStateException("Ce contrat a deja ete traite.");
        if (!accepte) { c.setStatut("REFUSE"); return contratRepository.save(c); }
        Utilisateur user = utilisateurRepository.verrouillerParId(userId).orElseThrow(() -> new IllegalStateException("Candidat introuvable."));
        Employe emp = user.getEmploye();
        if (emp == null) {
            var demande = c.getCandidature().getOffre().getDemande();
            Provenance provenance = provenanceRepository.findAll().stream().filter(p -> "Recrutement externe".equalsIgnoreCase(p.getLibelle())).findFirst()
                    .orElseThrow(() -> new IllegalStateException("La provenance 'Recrutement externe' manque dans la base."));
            emp = Employe.builder().nomEmp(c.getNomSalarie()).dateEmbauche(c.getDateEffet().atStartOfDay())
                    .poste(demande.getPoste()).departement(demande.getDepartement()).salaireEmp(c.getSalaire()).provenance(provenance).build();
            emp = employeRepository.save(emp);
            user.setEmploye(emp); utilisateurRepository.save(user);
        }
        c.setEmploye(emp); c.setDateAcceptation(LocalDateTime.now()); c.setStatut("ACCEPTE");
        return contratRepository.save(c);
    }
}
