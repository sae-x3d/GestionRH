package com.gestion.service;

import com.gestion.dto.MenuLien;
import com.gestion.dto.MenuSection;
import com.gestion.model.Departement;
import com.gestion.model.Employe;
import com.gestion.model.Role;
import com.gestion.model.Utilisateur;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NavigationServiceTests {

    private final NavigationService navigationService = new NavigationService();

    @Test
    void leMenuAdministrateurContientLePilotageEtLeRecrutementSansPostulationCandidat() {
        List<String> libelles = libelles(menu("Administrateur", "/tableau-de-bord"));

        assertThat(libelles).contains("Tableau de bord", "Demandes", "Nouvelle demande",
                "Validation Direction", "Offres d'emploi", "Gérer les offres", "Candidatures RH",
                "Questionnaire contrat", "Mon profil");
        assertThat(libelles).doesNotContain("Accueil", "Validation Finance", "Validation RH",
                "Mes candidatures", "Postuler", "Mes contrats", "Notifications");
    }

    @Test
    void leMenuRhGereLesCandidaturesSansEspaceCandidat() {
        List<String> libelles = libelles(menu("RH", "/demandes"));

        assertThat(libelles).contains("Tableau de bord", "Demandes", "Validation RH",
                "Offres d'emploi", "Gérer les offres", "Candidatures RH", "Questionnaire contrat",
                "Mon profil");
        assertThat(libelles).doesNotContain("Accueil", "Validation Direction", "Validation Finance",
                "Nouvelle demande", "Mes candidatures", "Postuler", "Mes contrats", "Notifications");
    }

    @Test
    void unManagerHorsFinanceNeVoitPasLaValidationFinance() {
        Utilisateur manager = utilisateur("Manager", departement("Informatique"));

        assertThat(libelles(navigationService.construire(manager, "/demandes")))
                .contains("Tableau de bord", "Mes demandes", "Nouvelle demande", "Offres d'emploi",
                        "Mes candidatures", "Mes contrats", "Mon profil")
                .doesNotContain("Accueil", "Validation Finance", "Candidatures RH",
                        "Gérer les offres", "Postuler", "Notifications");
    }

    @Test
    void unManagerDuDepartementFinanceVoitLaValidationFinance() {
        Utilisateur manager = utilisateur("Manager", departement("Finance"));

        assertThat(libelles(navigationService.construire(manager, "/demandes")))
                .contains("Mes demandes", "Nouvelle demande", "Validation Finance");
    }

    @Test
    void unEmployeNeVoitNiNouvelleDemandeNiGestionDesOffres() {
        List<String> libelles = libelles(menu("Employe", "/demandes"));

        assertThat(libelles).contains("Tableau de bord", "Mes demandes", "Offres d'emploi",
                "Mes candidatures", "Mes contrats", "Mon profil");
        assertThat(libelles).doesNotContain("Accueil", "Nouvelle demande", "Gérer les offres",
                "Validation RH", "Candidatures RH", "Postuler", "Notifications");
    }

    @Test
    void unCandidatNaQueSonEspace() {
        List<String> libelles = libelles(menu("Utilisateur", "/offres"));

        assertThat(libelles).contains("Tableau de bord", "Offres d'emploi", "Mes candidatures",
                "Mon profil");
        assertThat(libelles).doesNotContain("Accueil", "Demandes", "Validation Finance",
                "Mes contrats", "Candidatures RH", "Postuler", "Notifications");
    }

    @Test
    void aucunMenuNeProposeDeLienDirectDePostulation() {
        for (String role : List.of("Administrateur", "RH", "Manager", "Employe", "Finance", "Utilisateur")) {
            assertThat(urls(menu(role, "/")))
                    .as("menu du role " + role)
                    .doesNotContain("/candidatures/postuler");
        }
    }

    @Test
    void leLienLePlusPrecisEstActif() {
        List<MenuSection> menu = menu("RH", "/demandes/validation-rh/12");

        assertThat(lien(menu, "Validation RH").actif()).isTrue();
        assertThat(lien(menu, "Demandes").actif()).isFalse();
    }

    @Test
    void leDetailDuneOffreActiveLeLienOffres() {
        List<MenuSection> menu = menu("Utilisateur", "/offres/8");

        assertThat(lien(menu, "Offres d'emploi").actif()).isTrue();
    }

    @Test
    void leDetailDUneCandidatureRhActiveCandidaturesRh() {
        List<MenuSection> menu = menu("RH", "/candidatures/rh/8");

        assertThat(lien(menu, "Candidatures RH").actif()).isTrue();
    }

    private List<MenuSection> menu(String role, String uri) {
        return navigationService.construire(utilisateur(role, null), uri);
    }

    private Utilisateur utilisateur(String role, Departement departement) {
        Employe employe = departement == null ? null : Employe.builder().departement(departement).build();
        return Utilisateur.builder()
                .username("test")
                .role(Role.builder().nomRole(role).build())
                .employe(employe)
                .build();
    }

    private Departement departement(String nom) {
        return Departement.builder().nomDept(nom).build();
    }

    private List<String> libelles(List<MenuSection> sections) {
        return sections.stream()
                .flatMap(section -> section.liens().stream())
                .map(MenuLien::libelle)
                .toList();
    }

    private List<String> urls(List<MenuSection> sections) {
        return sections.stream()
                .flatMap(section -> section.liens().stream())
                .map(MenuLien::url)
                .toList();
    }

    private MenuLien lien(List<MenuSection> sections, String libelle) {
        return sections.stream()
                .flatMap(section -> section.liens().stream())
                .filter(lien -> lien.libelle().equals(libelle))
                .findFirst()
                .orElseThrow();
    }
}
