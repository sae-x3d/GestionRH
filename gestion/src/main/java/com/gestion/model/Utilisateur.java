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

    @Column(name = "prenom", length = 100)
    private String prenom;

    @Column(name = "nom", length = 100)
    private String nom;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "telephone", length = 30)
    private String telephone;

    @Column(name = "username", length = 100)
    private String username;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;
}
