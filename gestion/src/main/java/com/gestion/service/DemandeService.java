package com.gestion.service;

import com.gestion.dto.CreateDemandeDTO;
import com.gestion.dto.DemandeCreateRequestDTO;
import com.gestion.exception.RessourceIntrouvableException;
import com.gestion.model.Demande;
import com.gestion.model.DemandeStatus;
import com.gestion.model.Departement;
import com.gestion.model.Employe;
import com.gestion.model.Motif;
import com.gestion.model.Poste;
import com.gestion.model.Utilisateur;
import com.gestion.repository.DemandeRepository;
import com.gestion.repository.DemandeStatusRepository;
import com.gestion.repository.DepartementRepository;
import com.gestion.repository.EmployeRepository;
import com.gestion.repository.MotifRepository;
import com.gestion.repository.PosteRepository;
import com.gestion.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import com.gestion.dto.DemandeResponseDTO;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DemandeService {

    private final DemandeRepository demandeRepository;
    private final PosteRepository posteRepository;
    private final DepartementRepository departementRepository;
    private final MotifRepository motifRepository;
    private final EmployeRepository employeRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final DemandeStatusService demandeStatusService;

    @Transactional
    public Demande creer(CreateDemandeDTO dto, Utilisateur demandeur) {
        Demande demande = Demande.builder()
                .poste(trouverReference(posteRepository.findById(dto.getPosteId()), "poste"))
                .departement(trouverReference(departementRepository.findById(dto.getDepartementId()), "département"))
                .motif(trouverReference(motifRepository.findById(dto.getMotifId()), "motif"))
                .demandeur(trouverReference(utilisateurRepository.findById(demandeur.getIdUser()), "demandeur"))
                .demandeStatus(trouverStatutInitial())
                .empRemplace(dto.getEmpRemplaceId() == null
                        ? null
                        : trouverReference(employeRepository.findById(dto.getEmpRemplaceId()), "employé remplacé"))
                .budget(dto.getBudget())
                .dateSouhaite(dto.getDateSouhaite())
                .dateCreation(LocalDateTime.now())
                .build();

        return demandeRepository.save(demande);
    }


    @Transactional
    public Demande creer(DemandeCreateRequestDTO request, Utilisateur demandeur) {
        CreateDemandeDTO dto = new CreateDemandeDTO();

        dto.setPosteId(request.idPoste());
        dto.setDepartementId(request.idDept());
        dto.setMotifId(request.idMotif());
        dto.setBudget(request.budget());
        dto.setDateSouhaite(request.dateSouhaite());

        return creer(dto, demandeur);
    }


    private DemandeStatus trouverStatutInitial() {
        return demandeStatusService.trouverStatutInitial()
                .orElseThrow(() -> new RessourceIntrouvableException(
                        "Statut initial introuvable : « " + demandeStatusService.getLibelleStatutInitial() + " »"));
    }

    private <T> T trouverReference(Optional<T> ressource, String nom) {
        return ressource.orElseThrow(() -> new RessourceIntrouvableException("Référence introuvable : " + nom));
    }


    @Transactional(readOnly = true)
        public List<DemandeResponseDTO> listerToutes() {
            return demandeRepository.findAllByOrderByDateCreationDesc()
                    .stream()
                    .map(d -> new DemandeResponseDTO(
                            d.getIdDemande(),
                            d.getPoste() != null ? d.getPoste().getIdPoste() : null,
                            d.getDepartement() != null ? d.getDepartement().getIdDept() : null,
                            d.getMotif() != null ? d.getMotif().getIdMotif() : null,
                            d.getBudget(),
                            d.getDateCreation(),
                            d.getDateSouhaite(),
                            d.getDemandeStatus() != null
                                    ? d.getDemandeStatus().getLibelleStatus() : null
                    ))
                    .toList();
        }


    @Transactional(readOnly = true)
    public DemandeResponseDTO trouverParId(Integer id) {
        Demande d = demandeRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException(
                        "Demande introuvable : " + id));

        return new DemandeResponseDTO(
                d.getIdDemande(),
                d.getPoste() != null ? d.getPoste().getIdPoste() : null,
                d.getDepartement() != null ? d.getDepartement().getIdDept() : null,
                d.getMotif() != null ? d.getMotif().getIdMotif() : null,
                d.getBudget(),
                d.getDateCreation(),
                d.getDateSouhaite(),
                d.getDemandeStatus() != null ? d.getDemandeStatus().getLibelleStatus() : null
        );
    }

}