package com.gestion.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class CreateUtilisateurDTOTests {

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
    void utilisateurValide_neDeclencheAucuneViolation() {
        assertThat(validator.validate(utilisateurValide())).isEmpty();
    }

    @Test
    void usernameVide_declencheUneViolation() {
        CreateUtilisateurDTO dto = utilisateurValide();
        dto.setUsername("  ");

        assertThat(champsEnErreur(dto)).contains("username");
    }

    @Test
    void usernameTropCourt_declencheUneViolation() {
        CreateUtilisateurDTO dto = utilisateurValide();
        dto.setUsername("ab");

        assertThat(champsEnErreur(dto)).contains("username");
    }

    @Test
    void usernameTropLong_declencheUneViolation() {
        CreateUtilisateurDTO dto = utilisateurValide();
        dto.setUsername("a".repeat(101));

        assertThat(champsEnErreur(dto)).contains("username");
    }

    @Test
    void usernameAvecCaractereInterdit_declencheUneViolation() {
        CreateUtilisateurDTO dto = utilisateurValide();
        dto.setUsername("jean dupont!");

        assertThat(champsEnErreur(dto)).contains("username");
    }

    @Test
    void motDePasseTropCourt_declencheUneViolation() {
        CreateUtilisateurDTO dto = utilisateurValide();
        dto.setPassword("Abc1234");

        assertThat(champsEnErreur(dto)).contains("password");
    }

    @Test
    void motDePasseTropLong_declencheUneViolation() {
        CreateUtilisateurDTO dto = utilisateurValide();
        dto.setPassword("a".repeat(73));

        assertThat(champsEnErreur(dto)).contains("password");
    }

    @Test
    void motDePasseSansChiffre_declencheUneViolation() {
        CreateUtilisateurDTO dto = utilisateurValide();
        dto.setPassword("motdepasse");

        assertThat(champsEnErreur(dto)).contains("password");
    }

    @Test
    void motDePasseSansLettre_declencheUneViolation() {
        CreateUtilisateurDTO dto = utilisateurValide();
        dto.setPassword("12345678");

        assertThat(champsEnErreur(dto)).contains("password");
    }

    @Test
    void roleNul_declencheUneViolation() {
        CreateUtilisateurDTO dto = utilisateurValide();
        dto.setRoleId(null);

        assertThat(champsEnErreur(dto)).contains("roleId");
    }

    @Test
    void roleNegatif_declencheUneViolation() {
        CreateUtilisateurDTO dto = utilisateurValide();
        dto.setRoleId(-1);

        assertThat(champsEnErreur(dto)).contains("roleId");
    }

    @Test
    void employeIdNegatif_declencheUneViolation() {
        CreateUtilisateurDTO dto = utilisateurValide();
        dto.setEmployeId(-1);

        assertThat(champsEnErreur(dto)).contains("employeId");
    }

    @Test
    void employeIdAbsent_estAccepte() {
        CreateUtilisateurDTO dto = utilisateurValide();
        dto.setEmployeId(null);

        assertThat(validator.validate(dto)).isEmpty();
    }

    private CreateUtilisateurDTO utilisateurValide() {
        CreateUtilisateurDTO dto = new CreateUtilisateurDTO();
        dto.setUsername("j.dupont");
        dto.setPassword("Secret2026");
        dto.setRoleId(1);
        return dto;
    }

    private Set<String> champsEnErreur(CreateUtilisateurDTO dto) {
        return validator.validate(dto).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }
}
