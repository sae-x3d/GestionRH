package com.sirh.backend.modules.recrutement.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import com.sirh.backend.modules.recrutement.dto.PublicOfferResponse;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PublicOfferService {
    private static final String SELECT = """
        SELECT o.id, o.titre, o.description, o.competences, p.intitule AS poste,
               d.nom AS departement, dr.budget_salaire_base AS budget, dr.effectif,
               GREATEST(dr.effectif - dr.effectif_recrute, 0)::integer AS effectif_restant,
               o.date_publication, o.date_cloture
        FROM offres_emploi o
        JOIN statuts_offre_emploi so ON so.id = o.statut_id AND so.code = 'PUBLIEE'
        JOIN demandes_recrutement dr ON dr.id = o.demande_id
        JOIN postes p ON p.id = dr.poste_id
        JOIN departements d ON d.id = dr.departement_id
        WHERE o.date_cloture >= CURRENT_DATE
          AND dr.effectif > dr.effectif_recrute
        """;

    private final JdbcTemplate jdbc;

    public PublicOfferService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Transactional(readOnly = true)
    public List<PublicOfferResponse> list() {
        return jdbc.query(SELECT + " ORDER BY o.date_publication DESC, o.id DESC", (result, row) -> map(
            result.getInt("id"), result.getString("titre"), result.getString("description"),
            result.getString("competences"), result.getString("poste"), result.getString("departement"),
            result.getBigDecimal("budget"), result.getInt("effectif"), result.getInt("effectif_restant"),
            result.getObject("date_publication", LocalDate.class), result.getObject("date_cloture", LocalDate.class)));
    }

    @Transactional(readOnly = true)
    public PublicOfferResponse get(Integer id) {
        return jdbc.query(SELECT + " AND o.id = ?", (result, row) -> map(
            result.getInt("id"), result.getString("titre"), result.getString("description"),
            result.getString("competences"), result.getString("poste"), result.getString("departement"),
            result.getBigDecimal("budget"), result.getInt("effectif"), result.getInt("effectif_restant"),
            result.getObject("date_publication", LocalDate.class), result.getObject("date_cloture", LocalDate.class)), id)
            .stream().findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Offre introuvable ou clôturée."));
    }

    private PublicOfferResponse map(Integer id, String titre, String description, String competences,
            String poste, String departement, BigDecimal budget, Integer effectif, Integer effectifRestant,
            LocalDate datePublication, LocalDate dateCloture) {
        return new PublicOfferResponse(id, titre, description, competences, poste,
            departement, budget, effectif, effectifRestant, datePublication, dateCloture);
    }
}
