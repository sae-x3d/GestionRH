package com.sirh.backend.modules.recrutement.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import com.sirh.backend.modules.auth.entity.AuthUserPrincipal;
import com.sirh.backend.modules.recrutement.dto.PublicOfferResponse;
import com.sirh.backend.modules.recrutement.dto.StaffOfferResponse;
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
    public List<StaffOfferResponse> listForStaff(AuthUserPrincipal principal) {
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Connexion requise.");
        if (!principal.isEnabled() || !"APPROVED".equalsIgnoreCase(principal.getAccountValidationStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Un compte actif et validé est requis.");
        }
        List<String> roles = principal.getRoles().stream().map(role -> role.toLowerCase(Locale.ROOT)).toList();
        boolean managerOnly = roles.contains("manager") && !roles.contains("rh") && !roles.contains("administration");
        if (!managerOnly && !roles.contains("rh") && !roles.contains("administration")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès réservé aux RH, managers et à l’administration.");
        }
        List<Object> parameters = new ArrayList<>();
        String departmentFilter = "";
        if (managerOnly) {
            List<String> departments = principal.getDepartmentCodes();
            if (departments.isEmpty()) return List.of();
            departmentFilter = " AND d.code IN (" + String.join(",", java.util.Collections.nCopies(departments.size(), "?")) + ")";
            parameters.addAll(departments);
        }
        String sql = """
            SELECT o.id, o.titre, o.description, o.competences, p.intitule AS poste, d.nom AS departement,
                   dr.budget_salaire_base AS budget, dr.effectif, dr.effectif_recrute,
                   GREATEST(dr.effectif - dr.effectif_recrute, 0)::integer AS effectif_restant,
                   COUNT(DISTINCT c.id)::integer AS nombre_candidatures,
                   CASE WHEN so.code = 'PUBLIEE' AND (o.date_cloture < CURRENT_DATE OR dr.effectif_recrute >= dr.effectif)
                        THEN 'CLOTUREE' ELSE so.code END AS statut_code,
                   CASE WHEN so.code = 'PUBLIEE' AND (o.date_cloture < CURRENT_DATE OR dr.effectif_recrute >= dr.effectif)
                        THEN 'Clôturée' ELSE so.libelle END AS statut,
                   o.date_publication, o.date_cloture
            FROM offres_emploi o JOIN statuts_offre_emploi so ON so.id = o.statut_id
            JOIN demandes_recrutement dr ON dr.id = o.demande_id
            JOIN postes p ON p.id = dr.poste_id JOIN departements d ON d.id = dr.departement_id
            LEFT JOIN candidatures c ON c.offre_id = o.id
            WHERE so.code <> 'BROUILLON'
            """ + departmentFilter + """
            GROUP BY o.id, p.intitule, d.nom, dr.budget_salaire_base, dr.effectif, dr.effectif_recrute,
                     so.code, so.libelle
            ORDER BY o.date_publication DESC NULLS LAST, o.id DESC
            """;
        return jdbc.query(sql, (row, index) -> new StaffOfferResponse(
            row.getInt("id"), row.getString("titre"), row.getString("description"), row.getString("competences"),
            row.getString("poste"), row.getString("departement"), row.getBigDecimal("budget"), row.getInt("effectif"),
            row.getInt("effectif_recrute"), row.getInt("effectif_restant"), row.getInt("nombre_candidatures"),
            row.getString("statut_code"), row.getString("statut"), row.getObject("date_publication", LocalDate.class),
            row.getObject("date_cloture", LocalDate.class)), parameters.toArray());
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
