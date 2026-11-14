package com.sirh.backend.modules.candidatures.dto;

import java.util.List;

public record CandidatureOptionsResponse(List<Option> typesParcours, List<Option> niveauxEtude) {
    public record Option(String code, String libelle) { }
}
