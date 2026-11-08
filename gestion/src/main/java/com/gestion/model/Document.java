package com.gestion.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "document", indexes = {
        @Index(name = "idx_document_onboarding", columnList = "id_onboarding")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_document")
    private Integer idDocument;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_onboarding", nullable = false)
    private Onboarding onboarding;

    @Column(name = "type_document", nullable = false, length = 80)
    private String typeDocument;

    @Column(name = "nom_original", nullable = false, length = 255)
    private String nomOriginal;

    @Column(name = "chemin", nullable = false, length = 500)
    private String chemin;

    @Column(name = "date_upload", nullable = false)
    private LocalDateTime dateUpload;

    @Column(name = "valide", nullable = false)
    private boolean valide;

    @Column(name = "commentaire", length = 500)
    private String commentaire;
}
