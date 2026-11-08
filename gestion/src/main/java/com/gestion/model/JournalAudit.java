package com.gestion.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "journal_audit", indexes = {
        @Index(name = "idx_audit_date", columnList = "date_action"),
        @Index(name = "idx_audit_utilisateur", columnList = "username")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JournalAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_audit")
    private Integer idAudit;

    @Column(name = "username", length = 100)
    private String username;

    @Column(name = "methode", length = 10)
    private String methode;

    @Column(name = "chemin", length = 500)
    private String chemin;

    @Column(name = "entite", length = 100)
    private String entite;

    @Column(name = "id_entite", length = 50)
    private String idEntite;

    @Column(name = "details", length = 4000)
    private String details;

    @Column(name = "adresse_ip", length = 60)
    private String adresseIp;

    @Column(name = "date_action", nullable = false)
    private LocalDateTime dateAction;
}
