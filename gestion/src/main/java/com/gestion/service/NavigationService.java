package com.gestion.service;

import com.gestion.dto.MenuLien;
import com.gestion.dto.MenuSection;
import com.gestion.model.Utilisateur;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class NavigationService {

    private static final String ROLE_ADMINISTRATEUR = "ADMINISTRATEUR";
    private static final String ROLE_RH = "RH";
    private static final String ROLE_MANAGER = "MANAGER";
    private static final String ROLE_EMPLOYE = "EMPLOYE";
    private static final String ROLE_FINANCE = "FINANCE";
    private static final String ROLE_UTILISATEUR = "UTILISATEUR";
    private static final String ROLE_DIRECTION = "DIRECTION";
    private static final String ROLE_IT = "IT";
    private static final String ROLE_PAIE = "PAIE";

    public List<MenuSection> construire(Utilisateur utilisateur, String uri) {
        if (utilisateur == null) {
            return List.of();
        }

        List<MenuSection> sections = switch (role(utilisateur)) {
            case ROLE_ADMINISTRATEUR -> menuAdministrateur();
            case ROLE_RH -> menuRh();
            case ROLE_MANAGER -> menuManager(estFinance(utilisateur));
            case ROLE_EMPLOYE -> menuEmploye(estFinance(utilisateur));
            case ROLE_FINANCE -> menuFinance();
            case ROLE_DIRECTION -> menuDirection();
            case ROLE_IT -> menuIt();
            case ROLE_PAIE -> menuPaie();
            case ROLE_UTILISATEUR -> menuUtilisateur();
            default -> menuUtilisateur();
        };

        return marquerActif(sections, uri);
    }

    private List<MenuSection> menuAdministrateur() {
        return List.of(
                section("Général",
                        lien("Tableau de bord", "/tableau-de-bord")),
                section("Demandes",
                        lien("Demandes", "/demandes"),
                        lien("Nouvelle demande", "/demandes/nouvelle"),
                        lien("Validation Direction", "/demandes/validation/direction")),
                section("Recrutement",
                        lien("Offres d'emploi", "/offres"),
                        lien("Gérer les offres", "/offres/gestion"),
                        lien("Candidatures RH", "/candidatures/rh"),
                        lien("Questionnaire contrat", "/contrats/questionnaire"),
                        lien("Accreditations", "/accreditations"),
                        lien("Onboarding", "/onboarding"),
                        lien("Validations contrats / accreditations", "/validations/bloc4")),
                section("Conges",
                        lien("Mes conges", "/conges"),
                        lien("Validation conges", "/conges/validation/rh"),
                        lien("Calendrier", "/conges/calendrier"),
                        lien("Referentiels conges", "/conges/parametres")),
                section("Temps de travail",
                        lien("Planning", "/planning"),
                        lien("Pointage equipe", "/pointage/equipe"),
                        lien("Anomalies", "/pointage/anomalies"),
                        lien("Heures supplementaires", "/heures-sup"),
                        lien("Cloture paie", "/paie/cloture")),
                section("Gestion RH",
                        lien("Salaries", "/employes"),
                        lien("Changements sensibles", "/employes/changements"),
                        lien("Sorties", "/sorties"),
                        lien("Paie", "/paie"),
                        lien("Reporting", "/reporting"),
                        lien("Journal d'audit", "/audit")),
                section("Compte",
                        lien("Mon profil", "/profil"))
        );
    }

    private List<MenuSection> menuRh() {
        return List.of(
                section("Général",
                        lien("Tableau de bord", "/tableau-de-bord")),
                section("Demandes",
                        lien("Demandes", "/demandes"),
                        lien("Validation RH", "/demandes/validation-rh")),
                section("Recrutement",
                        lien("Offres d'emploi", "/offres"),
                        lien("Gérer les offres", "/offres/gestion"),
                        lien("Candidatures RH", "/candidatures/rh"),
                        lien("Questionnaire contrat", "/contrats/questionnaire"),
                        lien("Accreditations", "/accreditations"),
                        lien("Onboarding", "/onboarding"),
                        lien("Validations contrats / accreditations", "/validations/bloc4")),
                section("Conges",
                        lien("Mes conges", "/conges"),
                        lien("Validation manager", "/conges/validation/manager"),
                        lien("Validation RH", "/conges/validation/rh"),
                        lien("Calendrier", "/conges/calendrier"),
                        lien("Referentiels conges", "/conges/parametres")),
                section("Temps de travail",
                        lien("Planning", "/planning"),
                        lien("Pointage equipe", "/pointage/equipe"),
                        lien("Anomalies", "/pointage/anomalies"),
                        lien("Heures supplementaires", "/heures-sup"),
                        lien("Cloture paie", "/paie/cloture")),
                section("Gestion RH",
                        lien("Salaries", "/employes"),
                        lien("Changements sensibles", "/employes/changements"),
                        lien("Sorties", "/sorties"),
                        lien("Paie", "/paie"),
                        lien("Reporting", "/reporting")),
                section("Compte",
                        lien("Mon profil", "/profil"))
        );
    }

    private List<MenuSection> menuManager(boolean finance) {
        List<MenuSection> sections = new ArrayList<>();
        sections.add(section("Général",
                lien("Tableau de bord", "/tableau-de-bord")));
        sections.add(section("Demandes",
                lien("Mes demandes", "/demandes"),
                lien("Nouvelle demande", "/demandes/nouvelle")));
        sections.add(section("Validation",
                lien("Validations a traiter", "/validations/bloc4"),
                lien("Onboarding equipe", "/onboarding"),
                lien("Accreditations equipe", "/accreditations")));
        if (finance) {
            sections.add(section("Validation",
                    lien("Validation Finance", "/demandes/validation/finance")));
        }
        sections.add(section("Recrutement",
                lien("Offres d'emploi", "/offres"),
                lien("Mes candidatures", "/candidatures")));
        sections.add(section("Conges",
                lien("Mes conges", "/conges"),
                lien("Validation equipe", "/conges/validation/manager"),
                lien("Calendrier", "/conges/calendrier")));
        sections.add(section("Temps de travail",
                lien("Planning", "/planning"),
                lien("Pointage equipe", "/pointage/equipe"),
                lien("Anomalies", "/pointage/anomalies"),
                lien("Mes heures sup", "/heures-sup")));
        sections.add(section("Contrats",
                lien("Mes contrats", "/contrats"),
                lien("Mon integration", "/mon-onboarding"),
                lien("Mes accreditations", "/mes-accreditations")));
        sections.add(section("Compte",
                lien("Mon dossier", "/mon-dossier"),
                lien("Mon profil", "/profil")));
        return sections;
    }

    private List<MenuSection> menuEmploye(boolean finance) {
        List<MenuSection> sections = new ArrayList<>();
        sections.add(section("Général",
                lien("Tableau de bord", "/tableau-de-bord")));
        sections.add(section("Demandes",
                lien("Mes demandes", "/demandes")));
        if (finance) {
            sections.add(section("Validation",
                    lien("Validation Finance", "/demandes/validation/finance")));
        }
        sections.add(section("Recrutement",
                lien("Offres d'emploi", "/offres"),
                lien("Mes candidatures", "/candidatures")));
        sections.add(section("Conges",
                lien("Mes conges", "/conges"),
                lien("Calendrier", "/conges/calendrier")));
        sections.add(section("Temps de travail",
                lien("Mon pointage", "/pointage"),
                lien("Mes heures sup", "/heures-sup")));
        sections.add(section("Contrats",
                lien("Mes contrats", "/contrats"),
                lien("Mon integration", "/mon-onboarding"),
                lien("Mes accreditations", "/mes-accreditations")));
        sections.add(section("Compte",
                lien("Mon dossier", "/mon-dossier"),
                lien("Mon profil", "/profil")));
        return sections;
    }

    private List<MenuSection> menuFinance() {
        return List.of(
                section("Général",
                        lien("Tableau de bord", "/tableau-de-bord")),
                section("Validation",
                        lien("Validation Finance", "/demandes/validation/finance")),
                section("Paie",
                        lien("Cloture paie", "/paie/cloture"),
                        lien("Inscriptions paie", "/paie")),
                section("Recrutement",
                        lien("Offres d'emploi", "/offres"),
                        lien("Mes accreditations", "/mes-accreditations")),
                section("Compte",
                        lien("Mon profil", "/profil"))
        );
    }

    private List<MenuSection> menuDirection() {
        return List.of(
                section("Général",
                        lien("Tableau de bord", "/tableau-de-bord"),
                        lien("Reporting", "/reporting")),
                section("Pilotage",
                        lien("Demandes", "/demandes"),
                        lien("Validation Direction", "/demandes/validation/direction"),
                        lien("Salaries", "/employes"),
                        lien("Sorties", "/sorties")),
                section("Compte",
                        lien("Mon profil", "/profil"))
        );
    }

    private List<MenuSection> menuIt() {
        return List.of(
                section("Général",
                        lien("Tableau de bord", "/tableau-de-bord")),
                section("Integrations",
                        lien("Onboarding", "/onboarding"),
                        lien("Mes taches IT", "/onboarding")),
                section("Compte",
                        lien("Mon profil", "/profil"))
        );
    }

    private List<MenuSection> menuPaie() {
        return List.of(
                section("Général",
                        lien("Tableau de bord", "/tableau-de-bord")),
                section("Paie",
                        lien("Inscriptions paie", "/paie"),
                        lien("Cloture paie", "/paie/cloture"),
                        lien("Salaries", "/employes"),
                        lien("Reporting", "/reporting")),
                section("Compte",
                        lien("Mon profil", "/profil"))
        );
    }

    private List<MenuSection> menuUtilisateur() {
        return List.of(
                section("Général",
                        lien("Tableau de bord", "/tableau-de-bord")),
                section("Recrutement",
                        lien("Offres d'emploi", "/offres"),
                        lien("Mes candidatures", "/candidatures"),
                        lien("Mes accreditations", "/mes-accreditations")),
                section("Compte",
                        lien("Mon profil", "/profil"))
        );
    }

    private MenuSection section(String titre, MenuLien... liens) {
        return new MenuSection(titre, List.of(liens));
    }

    private MenuLien lien(String libelle, String url) {
        return new MenuLien(libelle, url);
    }

    private List<MenuSection> marquerActif(List<MenuSection> sections, String uri) {
        String chemin = uri == null ? "" : uri;
        String meilleur = null;

        for (MenuSection section : sections) {
            for (MenuLien lien : section.liens()) {
                if (correspond(chemin, lien.url())
                        && (meilleur == null || lien.url().length() > meilleur.length())) {
                    meilleur = lien.url();
                }
            }
        }

        if (meilleur == null) {
            return sections;
        }

        String actif = meilleur;
        return sections.stream()
                .map(section -> new MenuSection(section.titre(), section.liens().stream()
                        .map(lien -> lien.avecActif(lien.url().equals(actif)))
                        .toList()))
                .toList();
    }

    private boolean correspond(String uri, String url) {
        if ("/".equals(url)) {
            return "/".equals(uri);
        }
        return uri.equals(url) || uri.startsWith(url + "/");
    }

    private boolean estFinance(Utilisateur utilisateur) {
        if (ROLE_FINANCE.equals(role(utilisateur))) {
            return true;
        }
        return utilisateur.getEmploye() != null
                && utilisateur.getEmploye().getDepartement() != null
                && "Finance".equalsIgnoreCase(utilisateur.getEmploye().getDepartement().getNomDept());
    }

    private String role(Utilisateur utilisateur) {
        if (utilisateur.getRole() == null || utilisateur.getRole().getNomRole() == null) {
            return "";
        }
        return Normalizer.normalize(utilisateur.getRole().getNomRole(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT);
    }
}
