package com.sirh.backend.modules.notifications.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "types_notifications")
public class NotificationTypeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(nullable = false, unique = true, length = 40)
    private String code;
    @Column(nullable = false, length = 100)
    private String libelle;

    protected NotificationTypeEntity() { }
    public Integer getId() { return id; }
    public String getCode() { return code; }
    public String getLibelle() { return libelle; }
}
