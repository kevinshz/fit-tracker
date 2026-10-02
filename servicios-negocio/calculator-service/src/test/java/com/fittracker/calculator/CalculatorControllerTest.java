package com.fittracker.calculator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.closeTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("CalculatorController - Tests de integracion web")
class CalculatorControllerTest {

    @Autowired private MockMvc mockMvc;

    @Test
    @DisplayName("POST /one-rep-max calcula Epley 100kg x 5 = 116.67")
    void oneRepMax() throws Exception {
        mockMvc.perform(post("/api/v1/calculator/one-rep-max")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"weight\":100,\"reps\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.oneRepMax").value(closeTo(116.67, 0.01)))
                .andExpect(jsonPath("$.weight").value(100))
                .andExpect(jsonPath("$.reps").value(5));
    }

    @Test
    @DisplayName("POST /one-rep-max con reps invalidas retorna 400")
    void oneRepMax_invalidReps_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/calculator/one-rep-max")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"weight\":100,\"reps\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /one-rep-max sin peso retorna 400")
    void oneRepMax_missingWeight_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/calculator/one-rep-max")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reps\":5}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /load STRENGTH aplica 85% del 1RM")
    void load_strength() throws Exception {
        mockMvc.perform(post("/api/v1/calculator/load")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exerciseSet\":{\"weight\":100,\"reps\":5},\"goal\":\"STRENGTH\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.oneRepMax").value(closeTo(116.67, 0.01)))
                .andExpect(jsonPath("$.percentageApplied").value(closeTo(0.85, 0.001)))
                .andExpect(jsonPath("$.rawLoad").value(closeTo(99.17, 0.01)))
                .andExpect(jsonPath("$.goal").value("STRENGTH"));
    }

    @Test
    @DisplayName("POST /load HYPERTROPHY aplica 70% del 1RM")
    void load_hypertrophy() throws Exception {
        mockMvc.perform(post("/api/v1/calculator/load")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exerciseSet\":{\"weight\":100,\"reps\":5},\"goal\":\"HYPERTROPHY\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.percentageApplied").value(closeTo(0.70, 0.001)))
                .andExpect(jsonPath("$.rawLoad").value(closeTo(81.67, 0.01)));
    }

    @Test
    @DisplayName("POST /round-load redondea al incremento BARBELL (2.5)")
    void roundLoad_barbell() throws Exception {
        mockMvc.perform(post("/api/v1/calculator/round-load")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetWeight\":101.3,\"equipment\":\"BARBELL\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roundedWeight").value(closeTo(102.50, 0.01)))
                .andExpect(jsonPath("$.incrementKg").value(closeTo(2.5, 0.01)))
                .andExpect(jsonPath("$.equipment").value("BARBELL"));
    }

    @Test
    @DisplayName("POST /round-load con equipo invalido retorna 400")
    void roundLoad_invalidEquipment_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/calculator/round-load")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetWeight\":100,\"equipment\":\"INVALID\"}"))
                .andExpect(status().isBadRequest());
    }
}
