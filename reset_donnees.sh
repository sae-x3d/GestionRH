#!/usr/bin/env bash
set -euo pipefail

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-gestionrh}"
DB_USERNAME="${DB_USERNAME:-postgres}"
DB_PASSWORD="${DB_PASSWORD:-3958}"

export PGPASSWORD="$DB_PASSWORD"

if [[ "${1:-}" != "--force" ]]; then
    printf "ATTENTION : toutes les données opérationnelles de démonstration de la base '%s' seront supprimées.\n" "$DB_NAME"
    read -r -p "Confirmer la remise à zéro ? (oui/non) " answer
    case "$answer" in
        oui|Oui|OUI|o|O) ;;
        *) echo "Annulé."; exit 1 ;;
    esac
fi

psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USERNAME" -d "$DB_NAME" -v ON_ERROR_STOP=1 <<'SQL'
TRUNCATE TABLE
    public.absence,
    public.accreditation,
    public.affectation,
    public.anomalie_pointage,
    public.candidature,
    public.changement_sensible,
    public.checklist_sortie,
    public.consommation_conge,
    public.contrat,
    public.demande,
    public.demande_conge,
    public.diffusion_offre,
    public.document,
    public.document_employe,
    public.emp,
    public.entretien,
    public.evaluation_candidat,
    public.heure_sup,
    public.historique_carriere,
    public.historique_salaire,
    public.inscription_paie,
    public.jour_travaille,
    public.journal_audit,
    public.notification,
    public.offre_emploi,
    public.onboarding,
    public.onboarding_tache,
    public.periode_paie,
    public.planning,
    public.planning_jour,
    public.pointage,
    public.question,
    public.reponse,
    public.solde_conge,
    public.sortie_employe,
    public.utilisateur,
    public.validation_dossier,
    public.validation_etape,
    public.variable_paie
RESTART IDENTITY CASCADE;
SQL

echo "Données de démonstration remises à zéro dans la base '$DB_NAME'."
echo "Les référentiels et paramètres sont conservés."
echo "Pour recharger les données de démonstration : ./insert_donnees.sh"
echo "Les fichiers téléversés dans gestion/uploads ne sont pas supprimés."
