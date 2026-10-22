package com.gestion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class CreateUtilisateurDTO {

    @NotBlank(message = "Le nom d'utilisateur est obligatoire")
    @Size(min = 3, max = 100, message = "Le nom d'utilisateur doit contenir entre 3 et 100 caractères")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Le nom d'utilisateur ne peut contenir que des lettres, chiffres, points, tirets et underscores")
    private String username;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 8, max = 72, message = "Le mot de passe doit contenir entre 8 et 72 caractères")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Le mot de passe doit contenir au moins une lettre et un chiffre")
    private String password;

    @NotNull(message = "Le rôle est obligatoire")
    @Positive(message = "L'identifiant du rôle doit être positif")
    private Integer roleId;

    @Positive(message = "L'identifiant de l'employé doit être positif")
    private Integer employeId;

    public CreateUtilisateurDTO() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getRoleId() {
        return roleId;
    }

    public void setRoleId(Integer roleId) {
        this.roleId = roleId;
    }

    public Integer getEmployeId() {
        return employeId;
    }

    public void setEmployeId(Integer employeId) {
        this.employeId = employeId;
    }
}
