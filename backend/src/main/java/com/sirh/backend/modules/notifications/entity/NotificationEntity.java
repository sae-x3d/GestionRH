package com.sirh.backend.modules.notifications.entity;

import java.time.OffsetDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
@Table(name = "notifications")
public class NotificationEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "destinataire_id", nullable = false)
    private Integer destinataireId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "type_id", nullable = false)
    private NotificationTypeEntity type;
    @Column(nullable = false, length = 150)
    private String titre;
    @Column(nullable = false, length = 500)
    private String resume;
    @Column(nullable = false, length = 300)
    private String lien;
    @Column(name = "lue_at")
    private OffsetDateTime lueAt;
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected NotificationEntity() { }
    public Integer getId() { return id; }
    public String getType() { return type.getCode(); }
    public String getTitre() { return titre; }
    public String getResume() { return resume; }
    public String getLien() { return lien; }
    public OffsetDateTime getLueAt() { return lueAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void markRead() { this.lueAt = OffsetDateTime.now(); }
}
