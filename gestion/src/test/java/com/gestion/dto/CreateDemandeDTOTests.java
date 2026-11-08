package com.gestion.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class CreateDemandeDTOTests {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void initValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    void demandeValide_neDeclencheAucuneViolation() {
        assertThat(validator.validate(demandeValide())).isEmpty();
    }

    @Test
    void referencesNulles_declenchentUneViolation() {
        CreateDemandeDTO dto = demandeValide();
        dto.setPosteId(null);
        dto.setDepartementId(null);
        dto.setMotifId(null);

        assertThat(champsEnErreur(dto)).contains("posteId", "departementId", "motifId");
    }

    @Test
    void referencesNegatives_declenchentUneViolation() {
        CreateDemandeDTO dto = demandeValide();
        dto.setPosteId(0);
        dto.setDepartementId(-2);
        dto.setMotifId(-3);

        assertThat(champsEnErreur(dto)).contains("posteId", "departementId", "motifId");
    }

    @Test
    void budgetNegatif_declencheUneViolation() {
        CreateDemandeDTO dto = demandeValide();
        dto.setBudget(-100);

        assertThat(champsEnErreur(dto)).contains("budget");
    }

    @Test
    void budgetNul_estAccepte() {
        CreateDemandeDTO dto = demandeValide();
        dto.setBudget(null);

        assertThat(validator.validate(dto)).isEmpty();
    }

    @Test
    void dateSouhaiteePassee_declencheUneViolation() {
        CreateDemandeDTO dto = demandeValide();
        dto.setDateSouhaite(LocalDateTime.now().minusDays(1));

        assertThat(champsEnErreur(dto)).contains("dateSouhaite");
    }

    @Test
    void dateSouhaiteeAbsente_estAcceptee() {
        CreateDemandeDTO dto = demandeValide();
        dto.setDateSouhaite(null);

        assertThat(validator.validate(dto)).isEmpty();
    }

    @Test
    void empRemplaceNegatif_declencheUneViolation() {
        CreateDemandeDTO dto = demandeValide();
        dto.setEmpRemplaceId(-1);

        assertThat(champsEnErreur(dto)).contains("empRemplaceId");
    }

    @Test
    void demandeurEtStatutNeFontPasPartieDuFormulaire() {
        CreateDemandeDTO dto = demandeValide();

        assertThat(dto.getClass().getDeclaredFields())
                .extracting("name")
                .doesNotContain("demandeurId", "demandeStatusId");
    }

    private CreateDemandeDTO demandeValide() {
        CreateDemandeDTO dto = new CreateDemandeDTO();
        dto.setPosteId(1);
        dto.setDepartementId(2);
        dto.setMotifId(3);
        dto.setBudget(50000);
        dto.setDateSouhaite(LocalDateTime.now().plusMonths(2));
        return dto;
    }

    private Set<String> champsEnErreur(CreateDemandeDTO dto) {
        return validator.validate(dto).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }
}
