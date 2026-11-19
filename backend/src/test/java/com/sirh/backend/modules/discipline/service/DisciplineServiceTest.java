package com.sirh.backend.modules.discipline.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sirh.backend.modules.discipline.dto.DecisionRequest;
import com.sirh.backend.modules.discipline.dto.DeclareIncidentRequest;
import com.sirh.backend.modules.discipline.dto.DemandeExplicationRequest;
import com.sirh.backend.modules.notifications.service.NotificationCenterService;
import com.sirh.backend.modules.temps.service.TimeManagementService;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DisciplineServiceTest {
    private static final ZoneId HR_ZONE = ZoneId.of("Indian/Antananarivo");
    private static final Integer RH_ID = 1;
    private static final Integer PROCEDURE_ID = 10;
    private static final Integer EMPLOYE_ID = 7;
    private static final Integer EMPLOYE_USER_ID = 12;

    @Mock private JdbcTemplate jdbc;
    @Mock private NotificationCenterService notifications;
    @Mock private TimeManagementService timeManagement;
    @Mock private DisciplineStorageService storage;

    private DisciplineService service;

    private String procedureStatut;
    private String respondStatut;
    private boolean demandeExists;
    private boolean hasReponse;
    private LocalDate dateLimite;
    private String mode;
    private List<java.sql.Date> punchedDates;
    private List<Map<String, Object>> leaveRows;

    @BeforeEach
    void setUp() {
        service = new DisciplineService(jdbc, notifications, timeManagement, storage);
        procedureStatut = "EXPLICATION_RECUE";
        respondStatut = "EXPLICATION_DEMANDEE";
        demandeExists = true;
        hasReponse = true;
        dateLimite = today().plusDays(1);
        mode = "CALENDAIRE";
        punchedDates = List.of();
        leaveRows = List.of();
        stubJdbc();
    }

    @Test
    void miseAPiedCalendaireTroisJoursCalculeLaRetenue() throws Exception {
        procedureStatut = "EXPLICATION_RECUE";
        mode = "CALENDAIRE";
        LocalDate debut = today().plusDays(2);
        LocalDate fin = debut.plusDays(2);

        Map<String, Object> result = service.decide(RH_ID, PROCEDURE_ID,
            new DecisionRequest("MISE_A_PIED", "Retards répétés", debut, fin));

        assertEquals(1, result.get("retenues"));

        ArgumentCaptor<Object[]> sanctionArgs = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc).queryForObject(contains("INSERT INTO sanctions"), eq(Integer.class), sanctionArgs.capture());
        assertEquals(3, sanctionArgs.getValue()[5]);

        ArgumentCaptor<Object[]> retenueArgs = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc).queryForObject(contains("INSERT INTO retenues_sanction"), eq(Integer.class), retenueArgs.capture());
        Object[] args = retenueArgs.getValue();
        assertEquals(debut, args[1]);
        assertEquals(fin, args[2]);
        assertEquals(3, args[3]);
        assertEquals(new BigDecimal("20000.00"), args[4]);
        assertEquals(new BigDecimal("60000.00"), args[5]);

        verify(jdbc).queryForObject(contains("INSERT INTO variables_paie"), eq(Integer.class), any(Object[].class));
    }

    @Test
    void miseAPiedOuvreeCompteLesJoursOuvrables() throws Exception {
        procedureStatut = "EXPLICATION_RECUE";
        mode = "OUVRE";
        LocalDate lundi = today().plusWeeks(2).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        while (lundi.plusDays(4).getMonth() != lundi.getMonth()) lundi = lundi.plusWeeks(1);
        LocalDate dimanche = lundi.plusDays(4);

        Map<String, Object> result = service.decide(RH_ID, PROCEDURE_ID,
            new DecisionRequest("MISE_A_PIED", "Retards répétés", lundi, dimanche));

        assertEquals(1, result.get("retenues"));
        ArgumentCaptor<Object[]> sanctionArgs = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc).queryForObject(contains("INSERT INTO sanctions"), eq(Integer.class), sanctionArgs.capture());
        assertEquals(5, sanctionArgs.getValue()[5]);

        ArgumentCaptor<Object[]> retenueArgs = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc).queryForObject(contains("INSERT INTO retenues_sanction"), eq(Integer.class), retenueArgs.capture());
        assertEquals(5, retenueArgs.getValue()[3]);
        assertEquals(new BigDecimal("100000.00"), retenueArgs.getValue()[5]);
    }

    @Test
    void managerNePeutDeclarerUnIncidentHorsDeSonEquipe() {
        when(timeManagement.managerDepartmentId(2)).thenReturn(3);
        when(jdbc.query(contains("SELECT p.departement_id FROM employes e JOIN postes p"),
            any(ResultSetExtractor.class), any(Object[].class))).thenReturn(4);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> service.declareIncident(2, new DeclareIncidentRequest(EMPLOYE_ID, "LEGERE",
                "Retard", "Retard répété", today()), true));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertTrue(exception.getReason().contains("votre équipe"));
    }

    @Test
    void employeNeVoitQueSesProcedures() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("procedureId", PROCEDURE_ID);
        row.put("incidentId", 50);
        row.put("statutCode", "OUVERTE");
        when(jdbc.queryForList(contains("WHERE i.employe_id = ?"), any(Object[].class)))
            .thenReturn(List.of(row));

        List<Map<String, Object>> rows = service.employeeProcedures(EMPLOYE_USER_ID);

        assertEquals(1, rows.size());
        ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc).queryForList(contains("WHERE i.employe_id = ?"), args.capture());
        assertEquals(EMPLOYE_USER_ID, args.getValue()[0]);
        assertEquals(EMPLOYE_ID, args.getValue()[1]);
        ArgumentCaptor<Object[]> journalArgs = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc).update(contains("INSERT INTO journal_acces_discipline"), journalArgs.capture());
        assertEquals("LECTURE", journalArgs.getValue()[1]);
        assertEquals("DISCIPLINE_EMPLOYE", journalArgs.getValue()[2]);
        assertEquals(EMPLOYE_ID, journalArgs.getValue()[3]);
    }

    @Test
    void consultationDuDetailJournaliseLaLecture() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", PROCEDURE_ID);
        row.put("incidentId", 50);
        row.put("employeId", EMPLOYE_ID);
        when(jdbc.queryForList(contains("WHERE pr.id = ?"), any(Object[].class)))
            .thenReturn(List.of(row));

        service.procedureDetail(RH_ID, PROCEDURE_ID);

        ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc).update(contains("INSERT INTO journal_acces_discipline"), args.capture());
        assertEquals("LECTURE", args.getValue()[1]);
        assertEquals("PROCEDURE", args.getValue()[2]);
        assertEquals(PROCEDURE_ID, args.getValue()[3]);
    }

    @Test
    void employeNeTelechargeQueLesPiecesLieesAUneReponse() {
        Map<String, Object> piece = new LinkedHashMap<>();
        piece.put("id", 5);
        piece.put("incident_id", 50);
        piece.put("cle_stockage", "discipline/50/00000000-0000-0000-0000-000000000000.pdf");
        piece.put("nom_fichier", "justificatif.pdf");
        piece.put("type_mime", "application/pdf");
        piece.put("reponse_id", null);
        piece.put("ajoutee_par_id", EMPLOYE_USER_ID);
        piece.put("employe_id", EMPLOYE_ID);
        when(jdbc.queryForList(contains("FROM pieces_disciplinaires pc JOIN incidents i"), any(Object[].class)))
            .thenReturn(List.of(piece));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> service.downloadPiece(EMPLOYE_USER_ID, 5, false));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void telechargementRhJournaliseLaConsultation() {
        Map<String, Object> piece = new LinkedHashMap<>();
        piece.put("id", 5);
        piece.put("incident_id", 50);
        piece.put("cle_stockage", "discipline/50/00000000-0000-0000-0000-000000000000.pdf");
        piece.put("nom_fichier", "justificatif.pdf");
        piece.put("type_mime", "application/pdf");
        piece.put("reponse_id", 31);
        piece.put("ajoutee_par_id", EMPLOYE_USER_ID);
        piece.put("employe_id", EMPLOYE_ID);
        when(jdbc.queryForList(contains("FROM pieces_disciplinaires pc JOIN incidents i"), any(Object[].class)))
            .thenReturn(List.of(piece));
        when(storage.read(piece.get("cle_stockage").toString(), 50)).thenReturn(new byte[]{1, 2, 3});

        DisciplineService.Document document = service.downloadPiece(RH_ID, 5, true);

        assertEquals("justificatif.pdf", document.fileName());
        ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc).update(contains("INSERT INTO journal_acces_discipline"), args.capture());
        assertEquals("TELECHARGEMENT", args.getValue()[1]);
        assertEquals("PIECE", args.getValue()[2]);
    }

    @Test
    void enchainementDesStatutsOuvreDemandePuisReponse() {
        procedureStatut = "OUVERTE";

        Map<String, Object> demande = service.requestExplanation(RH_ID, PROCEDURE_ID,
            new DemandeExplicationRequest("Expliquez votre retard", today().plusDays(5)));
        assertEquals(31, demande.get("demandeId"));
        verify(notifications).notifyUser(eq(EMPLOYE_USER_ID), eq("DISCIPLINE_EXPLICATION_DEMANDEE"),
            anyString(), contains("Répondez avant"), eq("/espace/employe/discipline"));

        Map<String, Object> reponse = service.respondToExplanation(EMPLOYE_USER_ID, 31,
            "J'étais malade", List.of());
        assertEquals(41, reponse.get("reponseId"));

        ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc, times(2)).update(contains("UPDATE procedures_disciplinaires SET statut_id"), args.capture());
        assertEquals(statutId("EXPLICATION_DEMANDEE"), args.getAllValues().get(0)[0]);
        assertEquals(statutId("EXPLICATION_RECUE"), args.getAllValues().get(1)[0]);
    }

    @Test
    void decisionSansReponseEtDelaiNonExpireEstRefusee() {
        procedureStatut = "EXPLICATION_DEMANDEE";
        hasReponse = false;
        dateLimite = today().plusDays(3);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> service.decide(RH_ID, PROCEDURE_ID,
                new DecisionRequest("AVERTISSEMENT", "Retard", null, null)));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertTrue(exception.getReason().contains("délai"));
    }

    @Test
    void miseAPiedSurCongeApprouveEstRefusee() {
        procedureStatut = "EXPLICATION_RECUE";
        LocalDate debut = today().plusDays(2);
        LocalDate fin = debut.plusDays(1);
        leaveRows = List.of(Map.<String, Object>of(
            "date_debut", java.sql.Date.valueOf(debut),
            "date_fin", java.sql.Date.valueOf(fin)));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> service.decide(RH_ID, PROCEDURE_ID,
                new DecisionRequest("MISE_A_PIED", "Retard", debut, fin)));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertTrue(exception.getReason().contains("congé approuvé"));
    }

    @Test
    void miseAPiedRetroactiveEstRefusee() {
        procedureStatut = "EXPLICATION_RECUE";
        LocalDate debut = today().minusDays(1);
        LocalDate fin = today().plusDays(1);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> service.decide(RH_ID, PROCEDURE_ID,
                new DecisionRequest("MISE_A_PIED", "Retard", debut, fin)));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertTrue(exception.getReason().contains("rétroactive"));
    }

    @Test
    void miseAPiedSurJourDejaPointeEstRefusee() {
        procedureStatut = "EXPLICATION_RECUE";
        LocalDate debut = today().plusDays(2);
        LocalDate fin = debut;
        punchedDates = List.of(java.sql.Date.valueOf(debut));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> service.decide(RH_ID, PROCEDURE_ID,
                new DecisionRequest("MISE_A_PIED", "Retard", debut, fin)));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertTrue(exception.getReason().contains("pointée"));
    }

    @Test
    void avertissementClotureImmediatementLaProcedure() throws Exception {
        procedureStatut = "EXPLICATION_RECUE";

        service.decide(RH_ID, PROCEDURE_ID,
            new DecisionRequest("AVERTISSEMENT", "Retard", null, null));

        ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc).update(contains("cloturee_at = CURRENT_TIMESTAMP"), args.capture());
        assertEquals(statutId("CLOTUREE"), args.getValue()[0]);
    }

    @Test
    void closeCompletedProceduresClotureEtJournaliseEnSysteme() {
        procedureStatut = "DECIDEE";
        when(jdbc.queryForList(contains("sp.code = 'DECIDEE'"), any(Object[].class)))
            .thenReturn(List.of(Map.<String, Object>of("id", PROCEDURE_ID, "employeId", EMPLOYE_ID)));

        int closed = service.closeCompletedProcedures();

        assertEquals(1, closed);
        ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc).update(contains("UPDATE procedures_disciplinaires"), args.capture());
        assertEquals(statutId("CLOTUREE"), args.getValue()[0]);
        verify(jdbc).update(contains("'SYSTEME'"), any(Object[].class));
    }

    @Test
    void listProceduresJournaliseLaLecture() {
        service.listProcedures(RH_ID, null);

        ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc).update(contains("INSERT INTO journal_acces_discipline"), args.capture());
        assertEquals("LECTURE", args.getValue()[1]);
    }

    @Test
    void listProceduresExposeDemandeIdPourAfficherLaReponse() {
        service.listProcedures(RH_ID, null);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc).queryForList(sql.capture(), any(Object[].class));
        assertTrue(sql.getValue().contains("de.id AS \"demandeId\""));
    }

    private void stubJdbc() {
        when(jdbc.query(anyString(), any(ResultSetExtractor.class), any(Object[].class)))
            .thenAnswer(invocation -> {
                ResultSetExtractor<?> extractor = invocation.getArgument(1);
                String sql = invocation.getArgument(0);
                Object[] args = varargs(invocation, 2);
                return extractor.extractData(resultSet(rowsFor(sql, args)));
            });
        when(jdbc.queryForObject(anyString(), any(Class.class), any(Object[].class)))
            .thenAnswer(invocation -> {
                String sql = invocation.getArgument(0);
                Object[] args = varargs(invocation, 2);
                return scalarFor(sql, args);
            });
        when(jdbc.queryForList(anyString(), any(Class.class), any(Object[].class)))
            .thenAnswer(invocation -> {
                String sql = invocation.getArgument(0);
                if (sql.contains("sessions_pointage")) return punchedDates;
                return List.of();
            });
        when(jdbc.queryForList(anyString(), any(Object[].class)))
            .thenAnswer(invocation -> {
                String sql = invocation.getArgument(0);
                if (sql.contains("demandes_conge")) return leaveRows;
                return List.of();
            });
        when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);
    }

    private List<Map<Object, Object>> rowsFor(String sql, Object[] args) {
        String normalized = sql.replaceAll("\\s+", " ");
        if (normalized.contains("FROM procedures_disciplinaires pr")
                && normalized.contains("statut_code")
                && normalized.contains("FOR UPDATE OF pr")) {
            Map<Object, Object> row = new LinkedHashMap<>();
            row.put("id", PROCEDURE_ID);
            row.put("statut_code", procedureStatut);
            row.put("employe_id", EMPLOYE_ID);
            row.put("titre", "Retards répétés");
            return List.of(row);
        }
        if (normalized.contains("FROM demandes_explication d JOIN procedures_disciplinaires pr")) {
            Map<Object, Object> row = new LinkedHashMap<>();
            row.put("id", 31);
            row.put("procedure_id", PROCEDURE_ID);
            row.put("statut_code", respondStatut);
            row.put("incident_id", 50);
            row.put("employe_id", EMPLOYE_ID);
            return List.of(row);
        }
        if (normalized.contains("FROM demandes_explication d WHERE d.procedure_id = ?")) {
            if (!demandeExists) return List.of();
            Map<Object, Object> row = new LinkedHashMap<>();
            row.put("id", 31);
            row.put("date_limite", java.sql.Date.valueOf(dateLimite));
            row.put("has_reponse", hasReponse);
            return List.of(row);
        }
        if (normalized.contains("FROM types_sanction WHERE code = ?")) {
            String code = String.valueOf(args[0]);
            boolean miseAPied = "MISE_A_PIED".equals(code);
            Map<Object, Object> row = new LinkedHashMap<>();
            row.put("id", 3);
            row.put("code", code);
            row.put("libelle", miseAPied ? "Mise à pied" : "Avertissement");
            row.put("retenue_salaire", miseAPied);
            row.put("necessite_periode", miseAPied);
            return List.of(row);
        }
        if (normalized.contains("FROM parametres_discipline WHERE code = ?")) {
            String code = String.valueOf(args[0]);
            String value = switch (code) {
                case "MODE_COMPTAGE_RETENUE" -> mode;
                case "DIVISEUR_TAUX_JOURNALIER" -> "30";
                case "DELAI_REPONSE_JOURS" -> "5";
                default -> null;
            };
            if (value == null) return List.of();
            Map<Object, Object> row = new LinkedHashMap<>();
            row.put(1, value);
            return List.of(row);
        }
        if (normalized.contains("SELECT id FROM statuts_procedure_disciplinaire")) {
            Map<Object, Object> row = new LinkedHashMap<>();
            row.put(1, statutId(String.valueOf(args[0])));
            return List.of(row);
        }
        if (normalized.contains("SELECT id FROM gravites_incident")) {
            Map<Object, Object> row = new LinkedHashMap<>();
            row.put(1, 2);
            return List.of(row);
        }
        if (normalized.contains("SELECT id FROM types_sanction")) {
            Map<Object, Object> row = new LinkedHashMap<>();
            row.put(1, 3);
            return List.of(row);
        }
        if (normalized.contains("SELECT c.salaire_base_mensuel")) {
            Map<Object, Object> row = new LinkedHashMap<>();
            row.put(1, new BigDecimal("600000"));
            return List.of(row);
        }
        if (normalized.contains("FROM periodes_paie p") && normalized.contains("sp.code = 'BROUILLON'")) {
            Map<Object, Object> row = new LinkedHashMap<>();
            row.put(1, 900);
            return List.of(row);
        }
        if (normalized.contains("FROM employes e") && normalized.contains("WHERE e.user_id = ?")) {
            Map<Object, Object> row = new LinkedHashMap<>();
            row.put(1, EMPLOYE_ID);
            return List.of(row);
        }
        if (normalized.contains("SELECT user_id FROM employes WHERE id = ?")) {
            Map<Object, Object> row = new LinkedHashMap<>();
            row.put(1, EMPLOYE_USER_ID);
            return List.of(row);
        }
        return List.of();
    }

    private Object scalarFor(String sql, Object[] args) {
        String normalized = sql.replaceAll("\\s+", " ");
        if (normalized.contains("FROM users u") && normalized.contains("user_roles")) return true;
        if (normalized.contains("INSERT INTO sanctions")) return 55;
        if (normalized.contains("INSERT INTO demandes_explication")) return 31;
        if (normalized.contains("INSERT INTO reponses_explication")) return 41;
        if (normalized.contains("INSERT INTO retenues_sanction")) return 77;
        if (normalized.contains("INSERT INTO variables_paie")) return 88;
        if (normalized.contains("FROM jours_feries")) return false;
        if (normalized.contains("FROM jours_semaine")) {
            return ((Number) args[0]).intValue() <= 5;
        }
        if (normalized.contains("SELECT EXISTS(SELECT 1 FROM periodes_paie")) return true;
        return null;
    }

    private static int statutId(String code) {
        return switch (code.toUpperCase()) {
            case "OUVERTE" -> 1;
            case "EXPLICATION_DEMANDEE" -> 2;
            case "EXPLICATION_RECUE" -> 3;
            case "DECIDEE" -> 4;
            case "CLOTUREE" -> 5;
            case "SANS_SUITE" -> 6;
            default -> 0;
        };
    }

    private static Object[] varargs(InvocationOnMock invocation, int start) {
        Object[] all = invocation.getArguments();
        return Arrays.copyOfRange(all, start, all.length);
    }

    private static ResultSet resultSet(List<Map<Object, Object>> rows) throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        AtomicInteger cursor = new AtomicInteger(-1);
        when(rs.next()).thenAnswer(invocation -> cursor.incrementAndGet() < rows.size());
        org.mockito.stubbing.Answer<Object> value = invocation -> {
            Map<Object, Object> row = rows.get(cursor.get());
            return row.get(invocation.getArgument(0));
        };
        when(rs.getString(anyString())).thenAnswer(invocation -> (String) value.answer(invocation));
        when(rs.getString(anyInt())).thenAnswer(invocation -> (String) value.answer(invocation));
        when(rs.getInt(anyString())).thenAnswer(invocation -> ((Number) value.answer(invocation)).intValue());
        when(rs.getInt(anyInt())).thenAnswer(invocation -> ((Number) value.answer(invocation)).intValue());
        when(rs.getBoolean(anyString())).thenAnswer(invocation -> (Boolean) value.answer(invocation));
        when(rs.getBoolean(anyInt())).thenAnswer(invocation -> (Boolean) value.answer(invocation));
        when(rs.getBigDecimal(anyString())).thenAnswer(invocation -> (BigDecimal) value.answer(invocation));
        when(rs.getBigDecimal(anyInt())).thenAnswer(invocation -> (BigDecimal) value.answer(invocation));
        when(rs.getDate(anyString())).thenAnswer(invocation -> (java.sql.Date) value.answer(invocation));
        when(rs.getDate(anyInt())).thenAnswer(invocation -> (java.sql.Date) value.answer(invocation));
        return rs;
    }

    private static LocalDate today() {
        return LocalDate.now(HR_ZONE);
    }
}
