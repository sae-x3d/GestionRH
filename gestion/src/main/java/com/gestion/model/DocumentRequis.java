package com.gestion.model;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "document_requis", uniqueConstraints = @UniqueConstraint(columnNames = "type_document"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DocumentRequis {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id_document_requis")
    private Integer idDocumentRequis;
    @Column(name = "type_document", nullable = false, length = 100) private String typeDocument;
    @Column(name = "actif", nullable = false) @Builder.Default private boolean actif = true;
}
