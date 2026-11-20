package com.sirh.backend.modules.discipline.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.Objects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DisciplineSchemaConstraintsTest {
    private static final String URL = env("SIRH_TEST_DB_URL", "jdbc:postgresql://localhost:5432/sirh");
    private static final String USER = env("SIRH_TEST_DB_USER", "postgres");
    private static final String PASSWORD = env("SIRH_TEST_DB_PASSWORD", "1234");

    private Connection conn;
    private Integer userId;
    private Integer employeId;
    private Integer graviteId;
    private Integer statutId;
    private Integer miseAPiedTypeId;
    private Integer avertissementTypeId;

    @BeforeEach
    void setUp() throws SQLException {
        try {
            conn = DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            Assumptions.assumeTrue(false, "PostgreSQL indisponible : " + e.getMessage());
        }
        conn.setAutoCommit(false);
        Assumptions.assumeTrue(tableExists("sanctions"), "Schéma bloc 11 absent de la base de test.");
        Assumptions.assumeTrue(tableExists("users") && tableExists("employes"), "Tables RH absentes de la base de test.");
        userId = scalarInt("SELECT id FROM users ORDER BY id LIMIT 1");
        employeId = scalarInt("SELECT id FROM employes ORDER BY id LIMIT 1");
        graviteId = scalarInt("SELECT id FROM gravites_incident ORDER BY id LIMIT 1");
        statutId = scalarInt("SELECT id FROM statuts_procedure_disciplinaire ORDER BY id LIMIT 1");
        miseAPiedTypeId = scalarInt("SELECT id FROM types_sanction WHERE code = 'MISE_A_PIED'");
        avertissementTypeId = scalarInt(
            "SELECT id FROM types_sanction WHERE NOT retenue_salaire AND NOT necessite_periode ORDER BY id LIMIT 1");
        Assumptions.assumeTrue(userId != null && employeId != null && graviteId != null && statutId != null
            && miseAPiedTypeId != null && avertissementTypeId != null,
            "Données de référence disciplinaires absentes de la base de test.");
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (conn == null) {
            return;
        }
        try {
            conn.rollback();
        } finally {
            conn.close();
        }
    }

    @Test
    void refuseUnePeriodeIncoherente() throws SQLException {
        int procedureId = createProcedure(createIncident());
        String sql = sanctionSql();
        assertRejected("23514", sql, procedureId, miseAPiedTypeId, "Motif", date("2026-01-05"), date("2026-01-09"), 0, userId);
        assertRejected("23514", sql, procedureId, miseAPiedTypeId, "Motif", date("2026-01-09"), date("2026-01-05"), 5, userId);
        assertRejected("23514", sql, procedureId, miseAPiedTypeId, "Motif", date("2026-01-05"), date("2026-01-09"), 6, userId);
    }

    @Test
    void exigeUnePeriodePourUneMiseAPied() throws SQLException {
        int procedureId = createProcedure(createIncident());
        assertRejected("23514", sanctionSql(), procedureId, miseAPiedTypeId, "Motif", null, null, null, userId);
    }

    @Test
    void refuseUnePeriodePourUnAvertissement() throws SQLException {
        int procedureId = createProcedure(createIncident());
        assertRejected("23514", sanctionSql(), procedureId, avertissementTypeId, "Motif",
            date("2026-01-05"), date("2026-01-05"), 1, userId);
    }

    @Test
    void refuseUneRetenueSurUneSanctionSansRetenue() throws SQLException {
        int sanctionId = createSanction(createProcedure(createIncident()), avertissementTypeId, null, null, null);
        assertRejected("23514", retenueSql(), sanctionId, date("2026-01-05"), date("2026-01-05"), 1, 100.00, 100.00);
    }

    @Test
    void refuseUneRetenueHorsPeriode() throws SQLException {
        int sanctionId = createSanction(createProcedure(createIncident()), miseAPiedTypeId,
            date("2026-02-02"), date("2026-02-06"), 5);
        assertRejected("23514", retenueSql(), sanctionId, date("2026-02-01"), date("2026-02-02"), 2, 100.00, 200.00);
    }

    @Test
    void refuseDeuxRetenuesQuiSeChevauchent() throws SQLException {
        int sanctionId = createSanction(createProcedure(createIncident()), miseAPiedTypeId,
            date("2026-03-02"), date("2026-03-06"), 5);
        insert(retenueSql(), sanctionId, date("2026-03-02"), date("2026-03-03"), 2, 100.00, 200.00);
        assertRejected("23P01", retenueSql(), sanctionId, date("2026-03-03"), date("2026-03-04"), 2, 100.00, 200.00);
    }

    @Test
    void refuseUnMontantDeRetenueIncoherent() throws SQLException {
        int sanctionId = createSanction(createProcedure(createIncident()), miseAPiedTypeId,
            date("2026-04-06"), date("2026-04-08"), 3);
        assertRejected("23514", retenueSql(), sanctionId, date("2026-04-06"), date("2026-04-08"), 3, 100.00, 100.00);
    }

    @Test
    void journalDisciplinaireImmuable() throws SQLException {
        int journalId = insert("""
            INSERT INTO journal_acces_discipline(utilisateur_id,action,objet_type,objet_id,employe_id,detail)
            VALUES (?,?,?,?,?,?)
            """, userId, "LECTURE", "TEST", null, employeId, "Consultation test");
        assertRejected("23001", "UPDATE journal_acces_discipline SET detail = ? WHERE id = ?", "Modifié", journalId);
        assertRejected("23001", "DELETE FROM journal_acces_discipline WHERE id = ?", journalId);
    }

    @Test
    void journalOrigineSystemeSansUtilisateurAutoriseeMaisPasUtilisateur() throws SQLException {
        int journalId = insert("""
            INSERT INTO journal_acces_discipline(utilisateur_id,origine,action,objet_type,detail)
            VALUES (NULL,'SYSTEME','MODIFICATION','TEST','Tâche planifiée')
            """);
        assertNotNull(journalId);
        assertRejected("23514", """
            INSERT INTO journal_acces_discipline(utilisateur_id,origine,action,objet_type,detail)
            VALUES (NULL,'UTILISATEUR','LECTURE','TEST','Sans utilisateur')
            """);
    }

    @Test
    void refuseUnePieceLieeALaReponseDUnAutreIncident() throws SQLException {
        int incidentA = createIncident();
        int procedureA = createProcedure(incidentA);
        int reponseA = createReponse(createDemande(procedureA));
        int incidentB = createIncident();
        int procedureB = createProcedure(incidentB);
        int reponseB = createReponse(createDemande(procedureB));

        insert(pieceSql(), incidentA, reponseA, "discipline/a.pdf", "a.pdf", "application/pdf", 10, userId);
        assertRejected("23514", pieceSql(), incidentA, reponseB, "discipline/b.pdf", "b.pdf", "application/pdf", 10, userId);
    }

    @Test
    void refuseUnePieceVideOuSansTypeMime() throws SQLException {
        int incidentId = createIncident();
        assertRejected("23514", pieceSql(), incidentId, null, "discipline/a.pdf", "a.pdf", "application/pdf", 0, userId);
        assertRejected("23514", pieceSql(), incidentId, null, "discipline/a.pdf", "a.pdf", "   ", 10, userId);
    }

    private String sanctionSql() {
        return """
            INSERT INTO sanctions(procedure_id,type_id,motif,date_debut,date_fin,nombre_jours,decide_par_id)
            VALUES (?,?,?,?,?,?,?)
            """;
    }

    private String retenueSql() {
        return """
            INSERT INTO retenues_sanction(sanction_id,date_debut,date_fin,nombre_jours,taux_journalier,montant)
            VALUES (?,?,?,?,?,?)
            """;
    }

    private String pieceSql() {
        return """
            INSERT INTO pieces_disciplinaires(incident_id,reponse_id,cle_stockage,nom_fichier,type_mime,taille_octets,ajoutee_par_id)
            VALUES (?,?,?,?,?,?,?)
            """;
    }

    private int createIncident() throws SQLException {
        return insert("""
            INSERT INTO incidents(employe_id,declarant_id,gravite_id,titre,description,date_faits)
            VALUES (?,?,?,?,?,?)
            """, employeId, userId, graviteId, "Incident test", "Description test", date("2026-01-01"));
    }

    private int createProcedure(int incidentId) throws SQLException {
        return insert("""
            INSERT INTO procedures_disciplinaires(incident_id,statut_id,ouverte_par_id)
            VALUES (?,?,?)
            """, incidentId, statutId, userId);
    }

    private int createSanction(int procedureId, int typeId, LocalDate debut, LocalDate fin, Integer jours) throws SQLException {
        return insert(sanctionSql(), procedureId, typeId, "Motif test", debut, fin, jours, userId);
    }

    private int createDemande(int procedureId) throws SQLException {
        return insert("""
            INSERT INTO demandes_explication(procedure_id,message,date_limite,demandee_par_id)
            VALUES (?,?,?,?)
            """, procedureId, "Merci de vous expliquer.", date("2026-01-10"), userId);
    }

    private int createReponse(int demandeId) throws SQLException {
        return insert("""
            INSERT INTO reponses_explication(demande_id,texte,repondue_par_id)
            VALUES (?,?,?)
            """, demandeId, "Réponse de test.", userId);
    }

    private int insert(String sql, Object... args) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, args);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    return 0;
                }
                return keys.getInt(1);
            }
        }
    }

    private void assertRejected(String expectedState, String sql, Object... args) throws SQLException {
        Savepoint savepoint = conn.setSavepoint();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, args);
            ps.executeUpdate();
            fail("La requête aurait dû être refusée : " + sql);
        } catch (SQLException e) {
            conn.rollback(savepoint);
            assertEquals(expectedState, e.getSQLState(), e.getMessage());
        }
    }

    private void bind(PreparedStatement ps, Object... args) throws SQLException {
        for (int i = 0; i < args.length; i++) {
            Object value = args[i];
            if (value instanceof LocalDate localDate) {
                ps.setDate(i + 1, java.sql.Date.valueOf(localDate));
            } else {
                ps.setObject(i + 1, value);
            }
        }
    }

    private boolean tableExists(String table) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT to_regclass(?)")) {
            ps.setString(1, "public." + table);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getString(1) != null;
            }
        }
    }

    private Integer scalarInt(String sql) throws SQLException {
        try (Statement statement = conn.createStatement(); ResultSet rs = statement.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : null;
        }
    }

    private static LocalDate date(String value) {
        return LocalDate.parse(value);
    }

    private static String env(String name, String fallback) {
        return Objects.requireNonNullElse(System.getenv(name), fallback);
    }
}
