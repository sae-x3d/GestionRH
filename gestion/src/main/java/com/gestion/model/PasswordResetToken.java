package com.gestion.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_token", indexes = @Index(name = "idx_password_reset_expiration", columnList = "expires_at"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PasswordResetToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reset") private Integer idReset;
    @ManyToOne(optional = false) @JoinColumn(name = "id_user", nullable = false)
    private Utilisateur utilisateur;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;
    @Column(name = "created_at", nullable = false) private LocalDateTime dateCreation;
    @Column(name = "expires_at", nullable = false) private LocalDateTime dateExpiration;
    @Column(name = "used_at") private LocalDateTime dateUtilisation;
}
