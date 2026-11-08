package com.gestion.service;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Limitation locale par IP et adresse cible; les identifiants ne sont pas conserves en clair. */
@Component
public class PasswordResetRateLimiter {
    private static final Duration FENETRE = Duration.ofMinutes(15);
    private final ConcurrentHashMap<String, Fenetre> compteurs = new ConcurrentHashMap<>();
    private final AtomicInteger appels = new AtomicInteger();

    public boolean autoriser(String identifiant, int maximum) {
        Instant maintenant = Instant.now();
        String cle;
        try {
            cle = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(identifiant.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
        Fenetre fenetre = compteurs.compute(cle, (k, courante) -> {
            if (courante == null || courante.debut.plus(FENETRE).isBefore(maintenant))
                return new Fenetre(maintenant, 1);
            return new Fenetre(courante.debut, courante.nombre + 1);
        });
        // Nettoyage periodique : ne pas parcourir toute la table pour chaque requete.
        if (appels.incrementAndGet() % 256 == 0)
            compteurs.entrySet().removeIf(e -> e.getValue().debut.plus(FENETRE.multipliedBy(2)).isBefore(maintenant));
        return fenetre.nombre <= maximum;
    }

    private record Fenetre(Instant debut, int nombre) { }
}
