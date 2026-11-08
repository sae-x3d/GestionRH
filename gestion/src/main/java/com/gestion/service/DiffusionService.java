package com.gestion.service;

import com.gestion.enums.StatutDiffusion;
import com.gestion.enums.TypeDiffusion;
import com.gestion.exception.RessourceIntrouvableException;
import com.gestion.model.DiffusionOffre;
import com.gestion.model.OffreEmploi;
import com.gestion.repository.DiffusionOffreRepository;
import com.gestion.repository.OffreEmploiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DiffusionService {

    private final DiffusionOffreRepository diffusionRepository;
    private final OffreEmploiRepository offreRepository;

    @Transactional
    public List<DiffusionOffre> diffuser(Integer idOffre, Collection<TypeDiffusion> types,
                                         String canal, String reference) {
        OffreEmploi offre = offreRepository.findById(idOffre)
                .orElseThrow(() -> new RessourceIntrouvableException("Offre introuvable : " + idOffre));
        if (types == null || types.isEmpty()) {
            throw new IllegalStateException("Selectionnez au moins un canal de diffusion.");
        }
        List<DiffusionOffre> diffusions = new ArrayList<>();
        for (TypeDiffusion type : types) {
            diffusions.add(diffusionRepository.save(DiffusionOffre.builder()
                    .offre(offre)
                    .type(type)
                    .canal(texteOuNull(canal))
                    .statut(StatutDiffusion.PUBLIEE)
                    .dateDiffusion(LocalDateTime.now())
                    .reference(texteOuNull(reference))
                    .build()));
        }
        return diffusions;
    }

    @Transactional
    public void cloturer(Integer idDiffusion) {
        DiffusionOffre diffusion = diffusionRepository.findById(idDiffusion)
                .orElseThrow(() -> new RessourceIntrouvableException(
                        "Diffusion introuvable : " + idDiffusion));
        diffusion.setStatut(StatutDiffusion.CLOTUREE);
        diffusionRepository.save(diffusion);
    }

    @Transactional(readOnly = true)
    public List<DiffusionOffre> listerParOffre(Integer idOffre) {
        return diffusionRepository.findByOffreIdOffreOrderByDateDiffusionDesc(idOffre);
    }

    private String texteOuNull(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur.trim();
    }
}
