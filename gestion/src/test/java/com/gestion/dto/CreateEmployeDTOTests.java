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

class CreateEmployeDTOTests {

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
    void employeValide_neDeclencheAucuneViolation() {
        Set<ConstraintViolation<CreateEmployeDTO>> violations = validator.validate(employeValide());

        assertThat(violations).isEmpty();
    }

    @Test
    void nomVide_declencheUneViolationSurNomEmp() {
        CreateEmployeDTO dto = employeValide();
        dto.setNomEmp("   ");

        assertThat(champsEnErreur(dto)).contains("nomEmp");
    }

    @Test
    void nomTropLong_declencheUneViolationSurNomEmp() {
        CreateEmployeDTO dto = employeValide();
        dto.setNomEmp("a".repeat(256));

        assertThat(champsEnErreur(dto)).contains("nomEmp");
    }

    @Test
    void dateEmbaucheFuture_declencheUneViolation() {
        CreateEmployeDTO dto = employeValide();
        dto.setDateEmbauche(LocalDateTime.now().plusDays(1));

        assertThat(champsEnErreur(dto)).contains("dateEmbauche");
    }

    @Test
    void dateEmbaucheNulle_declencheUneViolation() {
        CreateEmployeDTO dto = employeValide();
        dto.setDateEmbauche(null);

        assertThat(champsEnErreur(dto)).contains("dateEmbauche");
    }

    @Test
    void dateDepartAvantEmbauche_declencheUneViolationSurLaCoherenceDesDates() {
        CreateEmployeDTO dto = employeValide();
        dto.setDateEmbauche(LocalDateTime.now().minusDays(10));
        dto.setDateDepart(LocalDateTime.now().minusDays(20));

        assertThat(champsEnErreur(dto)).contains("dateDepartCoherente");
    }

    @Test
    void dateDepartAbsente_estAcceptee() {
        CreateEmployeDTO dto = employeValide();
        dto.setDateDepart(null);

        assertThat(validator.validate(dto)).isEmpty();
    }

    @Test
    void salaireNegatif_declencheUneViolation() {
        CreateEmployeDTO dto = employeValide();
        dto.setSalaireEmp(-1);

        assertThat(champsEnErreur(dto)).contains("salaireEmp");
    }

    @Test
    void salaireNul_declencheUneViolation() {
        CreateEmployeDTO dto = employeValide();
        dto.setSalaireEmp(null);

        assertThat(champsEnErreur(dto)).contains("salaireEmp");
    }

    @Test
    void identifiantsFkNuls_declenchentUneViolation() {
        CreateEmployeDTO dto = employeValide();
        dto.setPosteId(null);
        dto.setDepartementId(null);
        dto.setProvenanceId(null);

        assertThat(champsEnErreur(dto)).contains("posteId", "departementId", "provenanceId");
    }

    @Test
    void identifiantsFkNegatifs_declenchentUneViolation() {
        CreateEmployeDTO dto = employeValide();
        dto.setPosteId(0);
        dto.setDepartementId(-3);
        dto.setProvenanceId(-1);

        assertThat(champsEnErreur(dto)).contains("posteId", "departementId", "provenanceId");
    }

    private CreateEmployeDTO employeValide() {
        CreateEmployeDTO dto = new CreateEmployeDTO();
        dto.setNomEmp("Dupont");
        dto.setDateEmbauche(LocalDateTime.now().minusMonths(6));
        dto.setPosteId(1);
        dto.setDepartementId(2);
        dto.setSalaireEmp(3200);
        dto.setProvenanceId(3);
        return dto;
    }

    private Set<String> champsEnErreur(CreateEmployeDTO dto) {
        return validator.validate(dto).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }
}
