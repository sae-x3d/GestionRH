package com.gestion.controller;

import com.gestion.model.Demande;
import com.gestion.repository.DemandeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DemandeFluxCompletTests {

    private static final Pattern JETON_CSRF = Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"");

    @LocalServerPort
    private int port;

    @Autowired
    private DemandeRepository demandeRepository;

    @Test
    void parcoursCompletConnexionSaisieEnregistrement() throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        String base = "http://localhost:" + port;

        // 1. Page de connexion
        HttpResponse<String> pageConnexion = client.send(
                HttpRequest.newBuilder(URI.create(base + "/connexion")).GET().build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertThat(pageConnexion.statusCode()).isEqualTo(200);

        // 2. Authentification
        String jetonConnexion = extraireJetonCsrf(pageConnexion.body());
        HttpResponse<String> connexion = client.send(
                HttpRequest.newBuilder(URI.create(base + "/connexion"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(
                                "username=manager.it&password=MotDePasse2026&_csrf=" + encodage(jetonConnexion)))
                        .build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertThat(connexion.statusCode()).isEqualTo(302);
        assertThat(connexion.headers().firstValue("Location").orElseThrow())
                .endsWith("/demandes");

        // 3. Formulaire alimenté par la base
        HttpResponse<String> formulaire = client.send(
                HttpRequest.newBuilder(URI.create(base + "/demandes/nouvelle")).GET().build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertThat(formulaire.statusCode()).isEqualTo(200);
        assertThat(formulaire.body())
                .contains("manager.it")
                .contains("Développeur Backend")
                .contains("Informatique")
                .contains("Création de poste")
                .contains("En attente");

        // 4. Envoi du formulaire
        long nombreDemandesAvant = demandeRepository.count();
        String jetonFormulaire = extraireJetonCsrf(formulaire.body());
        HttpResponse<String> envoi = client.send(
                HttpRequest.newBuilder(URI.create(base + "/demandes"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(
                                "posteId=1&departementId=1&motifId=3&budget=3500000&dateSouhaite=2030-11-01T09:00&_csrf="
                                        + encodage(jetonFormulaire)))
                        .build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertThat(envoi.statusCode()).isEqualTo(302);
        URI destination = URI.create(envoi.headers().firstValue("Location").orElseThrow());
        assertThat(destination.getPath()).matches("/demandes/\\d+");

        // 5. Page de détail de la demande créée
        HttpResponse<String> confirmation = client.send(
                HttpRequest.newBuilder(URI.create(base + destination.getPath())).GET().build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertThat(confirmation.statusCode()).isEqualTo(200);
        assertThat(confirmation.body())
                .contains("enregistrée avec le statut")
                .contains("Développeur Backend")
                .contains("manager.it")
                .contains("Demande n°");

        // 6. Trace en base
        assertThat(demandeRepository.count()).isEqualTo(nombreDemandesAvant + 1);
        Demande demande = demandeRepository.findAll().stream()
                .max(Comparator.comparing(Demande::getIdDemande))
                .orElseThrow();
        assertThat(demande.getDemandeur().getIdUser()).isEqualTo(3);
        assertThat(demande.getDemandeStatus().getLibelleStatus()).isEqualTo("En attente");
        assertThat(demande.getBudget()).isEqualTo(3500000);

        demandeRepository.delete(demande);
    }

    private String extraireJetonCsrf(String html) {
        Matcher matcher = JETON_CSRF.matcher(html);
        assertThat(matcher.find()).as("Jeton CSRF présent dans la page").isTrue();
        return matcher.group(1);
    }

    private String encodage(String valeur) throws IOException {
        return URLEncoder.encode(valeur, StandardCharsets.UTF_8);
    }
}
