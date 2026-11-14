package com.sirh.backend.modules.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Keeps the registration profile column synchronized on existing local databases. */
@Entity
@Table(name = "users")
public class UserFullNameEntity {
    @Id
    private Integer id;

    @Column(name = "full_name", length = 150)
    private String fullName;

    protected UserFullNameEntity() { }
}
