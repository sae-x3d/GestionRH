package com.gestion.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "document_employe", indexes = {
        @Index(name = "idx_document_employe_emp", columnList = "id_emp"),
        @Index(name = "idx_document_employe_validite", columnList = "date_validite")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentEmploye {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_document_employe")
    private Integer idDocumentEmploye;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_emp", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Employe employe;

    @Column(name = "type_document", nullable = false, length = 100)
    private String typeDocument;

    @Column(name = "nom_original", nullable = false, length = 255)
    private String nomOriginal;

    @Column(name = "chemin", nullable = false, length = 500)
    private String chemin;

    @Column(name = "date_emission")
    private LocalDate dateEmission;

    @Column(name = "date_validite")
    private LocalDate dateValidite;

    @Column(name = "alerte_expiration_envoyee", nullable = false)
    private boolean alerteExpirationEnvoyee;

    @Column(name = "commentaire", length = 500)
    private String commentaire;

    @Column(name = "date_upload", nullable = false)
    private LocalDateTime dateUpload;
}
