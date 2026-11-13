# Deploiement base de donnees

Les scripts de migration sont conserves dans `src/main/resources/db/patches/` et ne sont pas executes automatiquement par l'application. Les appliquer une seule fois, dans l'ordre chronologique, avant de mettre a jour l'application :

```sh
psql "$PGDATABASE" -v ON_ERROR_STOP=1 -f src/main/resources/db/patches/20261106_type_conge_rules.sql
psql "$PGDATABASE" -v ON_ERROR_STOP=1 -f src/main/resources/db/patches/20261106_security_audit.sql
psql "$PGDATABASE" -v ON_ERROR_STOP=1 -f src/main/resources/db/patches/20261106_security_sessions.sql
```

Configurer `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER` et `PGPASSWORD` pour la connexion `psql` (ou passer les options correspondantes). `DB_URL` de l'application utilise, lui, la forme JDBC.

Pour un environnement de production, definir au minimum `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` et `APP_URL` (URL publique HTTPS de l'application). Pour envoyer les courriels, definir `APP_MAIL_ENABLED=true`, `APP_MAIL_FROM`, `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_SMTP_AUTH=true` et `MAIL_SMTP_STARTTLS=true` selon les exigences du fournisseur SMTP. Ne pas activer `app.security.dev-reset-link` en production. Le lien de reinitialisation n'est affiche dans la page que lorsque le profil Spring `dev` est actif.

Le parametre `spring.jpa.hibernate.ddl-auto=update` reste une aide de developpement et ne remplace pas l'application de ces patchs.
