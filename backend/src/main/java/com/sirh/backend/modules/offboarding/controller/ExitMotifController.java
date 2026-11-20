package com.sirh.backend.modules.offboarding.controller;

import java.util.List;
import java.util.Map;
import com.sirh.backend.modules.auth.entity.AuthUserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExitMotifController {
    private final JdbcTemplate jdbc;
    public ExitMotifController(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @GetMapping("/api/sorties/motifs")
    public List<Map<String,Object>> motifs(@AuthenticationPrincipal AuthUserPrincipal actor) {
        if (actor == null) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED);
        boolean manager = actor != null && actor.getRoles().stream().anyMatch(role -> "manager".equalsIgnoreCase(role));
        boolean rh = actor.getRoles().stream().anyMatch(role -> "rh".equalsIgnoreCase(role));
        if (!manager && !rh) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Accès réservé aux managers et aux ressources humaines.");
        if (manager) {
            return jdbc.queryForList("""
                SELECT m.code,m.libelle FROM motifs_sortie m
                JOIN types_initiative_sortie i ON i.id=m.initiative_id
                WHERE m.actif=TRUE AND i.code IN ('SALARIE','ECHEANCE') ORDER BY m.ordre,m.libelle
                """);
        }
        return jdbc.queryForList("SELECT code,libelle FROM motifs_sortie WHERE actif=TRUE ORDER BY ordre,libelle");
    }
}
