package com.gestion.exception;

import com.gestion.dto.CreateEmployeDTO;
import jakarta.validation.Valid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTests {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void jsonInvalide_retourne400EtDetailleLesChampsEnErreur() throws Exception {
        String json = """
                {
                  "nomEmp": "  ",
                  "dateEmbauche": "2050-01-01T10:00:00",
                  "posteId": null,
                  "departementId": 2,
                  "salaireEmp": -100,
                  "provenanceId": 3
                }
                """;

        mockMvc.perform(post("/test/employes").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Erreur de validation"))
                .andExpect(jsonPath("$.fieldErrors.nomEmp").exists())
                .andExpect(jsonPath("$.fieldErrors.posteId").exists())
                .andExpect(jsonPath("$.fieldErrors.salaireEmp").exists())
                .andExpect(jsonPath("$.fieldErrors.dateEmbauche").exists());
    }

    @Test
    void datesIncoherentes_remontentUneErreurSurLeChampCoherence() throws Exception {
        String json = """
                {
                  "nomEmp": "Dupont",
                  "dateEmbauche": "2020-01-01T10:00:00",
                  "dateDepart": "2019-01-01T10:00:00",
                  "posteId": 1,
                  "departementId": 2,
                  "salaireEmp": 3200,
                  "provenanceId": 3
                }
                """;

        mockMvc.perform(post("/test/employes").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.dateDepartCoherente").exists());
    }

    @Test
    void jsonValide_estAccepté() throws Exception {
        String json = """
                {
                  "nomEmp": "Dupont",
                  "dateEmbauche": "2020-01-01T10:00:00",
                  "posteId": 1,
                  "departementId": 2,
                  "salaireEmp": 3200,
                  "provenanceId": 3
                }
                """;

        mockMvc.perform(post("/test/employes").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomEmp").value("Dupont"));
    }

    @RestController
    static class TestController {

        @PostMapping("/test/employes")
        CreateEmployeDTO create(@Valid @RequestBody CreateEmployeDTO dto) {
            return dto;
        }
    }
}
