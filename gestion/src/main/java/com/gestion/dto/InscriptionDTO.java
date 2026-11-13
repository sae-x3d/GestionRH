package com.gestion.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class InscriptionDTO {

    @NotBlank(message = "Le nom d'utilisateur est obligatoire")
    @Size(min = 3, max = 100, message = "Le nom d'utilisateur doit contenir entre 3 et 100 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Le nom d'utilisateur ne peut contenir que des lettres, chiffres, points, tirets et underscores")
    private String username;

    @NotBlank(message = "L'adresse email est obligatoire")
    @Email(message = "L'adresse email n'est pas valide")
    @Size(max = 150, message = "L'adresse email ne peut pas depasser 150 caracteres")
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 8, max = 72, message = "Le mot de passe doit contenir entre 8 et 72 caracteres")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Le mot de passe doit contenir au moins une lettre et un chiffre")
    private String password;

    @NotBlank(message = "La confirmation du mot de passe est obligatoire")
    private String confirmation;

    @Positive(message = "L'identifiant de l'offre doit etre positif")
    private Integer offreId;

    public boolean motsDePasseIdentiques() {
        return password != null && password.equals(confirmation);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmation() {
        return confirmation;
    }

    public void setConfirmation(String confirmation) {
        this.confirmation = confirmation;
    }

    public Integer getOffreId() {
        return offreId;
    }

    public void setOffreId(Integer offreId) {
        this.offreId = offreId;
    }
}
