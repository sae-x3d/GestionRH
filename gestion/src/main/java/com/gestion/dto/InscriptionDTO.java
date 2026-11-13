package com.gestion.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class InscriptionDTO {

<<<<<<< HEAD
    @NotBlank(message = "Le prenom est obligatoire")
    @Size(max = 100, message = "Le prenom ne peut pas depasser 100 caracteres")
    private String prenom;

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 100, message = "Le nom ne peut pas depasser 100 caracteres")
    private String nom;

    @NotBlank(message = "L'adresse email est obligatoire")
    @Email(message = "L'adresse email n'est pas valide")
    @Size(max = 255, message = "L'adresse email ne peut pas depasser 255 caracteres")
    private String email;

    @NotBlank(message = "Le telephone est obligatoire")
    @Size(min = 6, max = 30, message = "Le telephone doit contenir entre 6 et 30 caracteres")
    @Pattern(regexp = "^[0-9+ .()\\-]+$", message = "Le telephone contient des caracteres invalides")
    private String telephone;

=======
>>>>>>> origin/dev-correction
    @NotBlank(message = "Le nom d'utilisateur est obligatoire")
    @Size(min = 3, max = 100, message = "Le nom d'utilisateur doit contenir entre 3 et 100 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Le nom d'utilisateur ne peut contenir que des lettres, chiffres, points, tirets et underscores")
    private String username;

<<<<<<< HEAD
=======
    @NotBlank(message = "L'adresse email est obligatoire")
    @Email(message = "L'adresse email n'est pas valide")
    @Size(max = 150, message = "L'adresse email ne peut pas depasser 150 caracteres")
    private String email;

>>>>>>> origin/dev-correction
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

<<<<<<< HEAD
    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
=======
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
>>>>>>> origin/dev-correction
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

<<<<<<< HEAD
    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

=======
>>>>>>> origin/dev-correction
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
