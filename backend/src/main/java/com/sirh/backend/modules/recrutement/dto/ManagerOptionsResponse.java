package com.sirh.backend.modules.recrutement.dto;

import java.math.BigDecimal;
import java.util.List;

public record ManagerOptionsResponse(DepartmentOption departement, List<PosteOption> postes, List<ReferenceOption> motifs) {
    public record DepartmentOption(Integer id, String code, String nom) { }
    public record PosteOption(Integer id, String intitule, BigDecimal salaireBaseMin, BigDecimal salaireBaseMax,
            Integer effectifAutorise, Integer effectifActif, Integer postesVacantsDisponibles, Integer effectifMinimal) { }
    public record ReferenceOption(Integer id, String code, String libelle) { }
}
