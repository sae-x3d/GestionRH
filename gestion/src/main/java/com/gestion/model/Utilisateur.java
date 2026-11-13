package com.gestion.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "utilisateur")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_user")
    private Integer idUser;

    @ManyToOne
    @JoinColumn(name = "id_emp")
    private Employe employe;

    @ManyToOne
    @JoinColumn(name = "id_role")
    private Role role;

    @Column(name = "username", length = 100)
    private String username;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "security_version", nullable = false)
    @Builder.Default
    private Integer securityVersion = 0;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "actif", nullable = false, columnDefinition = "boolean default true")
    @Builder.Default
    private boolean actif = true;
}
