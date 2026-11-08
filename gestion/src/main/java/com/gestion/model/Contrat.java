package com.gestion.model;

import com.gestion.enums.NatureContrat;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "contrat", indexes = {
        @Index(name = "idx_contrat_candidature", columnList = "id_candidature"),
        @Index(name = "idx_contrat_statut", columnList = "statut")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Contrat {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_contrat") private Integer idContrat;
    @ManyToOne(optional = false) @JoinColumn(name = "id_candidature", nullable = false)
    private Candidature candidature;
    @ManyToOne @JoinColumn(name = "id_emp") private Employe employe;
    @Column(name = "numero_contrat", unique = true, length = 50) private String numeroContrat;
    @Column(name = "type_contrat", nullable = false, length = 20) private String typeContrat;
    @Column(name = "nom_salarie", nullable = false, length = 200) private String nomSalarie;
    @Column(name = "adresse_salarie", nullable = false, length = 500) private String adresseSalarie;
    @Column(name = "nom_employeur", nullable = false, length = 200) private String nomEmployeur;
    @Column(name = "adresse_employeur", nullable = false, length = 500) private String adresseEmployeur;
    @Column(name = "fonction", nullable = false, length = 200) private String fonction;
    @Column(name = "categorie_professionnelle", nullable = false, length = 150) private String categorieProfessionnelle;
    @Column(name = "indice_classification", nullable = false, length = 100) private String indiceClassification;
    @Column(name = "salaire", nullable = false) private Integer salaire;
    @Column(name = "duree_travail", nullable = false, length = 150) private String dureeTravail;
    @Column(name = "lieu_travail", nullable = false, length = 200) private String lieuTravail;
    @Column(name = "date_effet", nullable = false) private LocalDate dateEffet;
    @Column(name = "date_fin") private LocalDate dateFin;
    @Column(name = "duree_essai_mois") private Integer dureeEssaiMois;
    @Column(name = "date_fin_essai") private LocalDate dateFinEssai;
    @ManyToOne @JoinColumn(name = "id_contrat_precedent") private Contrat contratPrecedent;
    @Column(name = "motif_cdd", length = 500) private String motifCdd;
    @Column(name = "lieu_signature", length = 200) private String lieuSignature;
    @Column(name = "date_signature") private LocalDate dateSignature;
    @Enumerated(EnumType.STRING)
    @Column(name = "nature", length = 30)
    private NatureContrat nature;
    @Column(name = "numero_avenant")
    private Integer numeroAvenant;
    @Column(name = "alerte_fin_envoyee", nullable = false)
    private boolean alerteFinEnvoyee;
    @Column(name = "alerte_essai_envoyee", nullable = false)
    private boolean alerteEssaiEnvoyee;
    @Column(name = "commentaire", length = 1000)
    private String commentaire;
    @Column(name = "statut", nullable = false, length = 30) private String statut;
    @Column(name = "date_generation") private LocalDateTime dateGeneration;
    @Column(name = "date_acceptation") private LocalDateTime dateAcceptation;
    @Column(name = "pdf", columnDefinition = "bytea") private byte[] pdf;
}
