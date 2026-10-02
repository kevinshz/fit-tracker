package com.fittracker.calculator.service;

import com.fittracker.calculator.persistence.entity.Equipment;
import com.fittracker.calculator.persistence.entity.TrainingGoal;
import com.fittracker.calculator.presentation.dto.OneRepMaxResponse;
import com.fittracker.calculator.service.implementation.CalculatorServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CalculatorServiceImpl - Tests Unitarios")
class CalculatorServiceImplTest {

    private final CalculatorServiceImpl service = new CalculatorServiceImpl();

    @Nested
    @DisplayName("calculateOneRepMax (Formula de Epley)")
    class CalculateOneRepMax {

        @Test
        @DisplayName("100kg x 5 reps = 116.67kg")
        void oneRepMax_100kg_5reps() {
            var result = service.calculateOneRepMax(new BigDecimal("100"), 5);
            assertThat(result.getOneRepMax()).isEqualByComparingTo("116.67");
            assertThat(result.getWeight()).isEqualByComparingTo("100");
            assertThat(result.getReps()).isEqualTo(5);
        }

        @Test
        @DisplayName("100kg x 1 rep = 103.33kg")
        void oneRepMax_100kg_1rep() {
            var result = service.calculateOneRepMax(new BigDecimal("100"), 1);
            assertThat(result.getOneRepMax()).isEqualByComparingTo("103.33");
        }

        @Test
        @DisplayName("100kg x 10 reps = 133.33kg")
        void oneRepMax_100kg_10reps() {
            var result = service.calculateOneRepMax(new BigDecimal("100"), 10);
            assertThat(result.getOneRepMax()).isEqualByComparingTo("133.33");
        }

        @Test
        @DisplayName("200kg x 3 reps = 220.00kg")
        void oneRepMax_200kg_3reps() {
            var result = service.calculateOneRepMax(new BigDecimal("200"), 3);
            assertThat(result.getOneRepMax()).isEqualByComparingTo("220.00");
        }

        @Test
        @DisplayName("50kg x 8 reps = 63.33kg")
        void oneRepMax_50kg_8reps() {
            var result = service.calculateOneRepMax(new BigDecimal("50"), 8);
            assertThat(result.getOneRepMax()).isEqualByComparingTo("63.33");
        }

        @ParameterizedTest
        @CsvSource({
                "100, 1,  103.33",
                "100, 5,  116.67",
                "100, 10, 133.33",
                "80,  5,  93.33",
                "60,  8,  76.00"
        })
        @DisplayName("Verificacion de formula Epley con varios datos")
        void oneRepMax_parameterized(int weight, int reps, String expected) {
            var result = service.calculateOneRepMax(BigDecimal.valueOf(weight), reps);
            assertThat(result.getOneRepMax()).isEqualByComparingTo(expected);
        }
    }

    @Nested
    @DisplayName("calculateLoad (Porcentaje por objetivo)")
    class CalculateLoad {

        @Test
        @DisplayName("STRENGTH: 1RM x 85%")
        void calculateLoad_strength() {
            var result = service.calculateLoad(new BigDecimal("100"), 5, TrainingGoal.STRENGTH);
            assertThat(result.getOneRepMax()).isEqualByComparingTo("116.67");
            assertThat(result.getPercentageApplied()).isEqualByComparingTo("0.85");
            assertThat(result.getRawLoad()).isEqualByComparingTo("99.17");
        }

        @Test
        @DisplayName("HYPERTROPHY: 1RM x 70%")
        void calculateLoad_hypertrophy() {
            var result = service.calculateLoad(new BigDecimal("100"), 5, TrainingGoal.HYPERTROPHY);
            assertThat(result.getOneRepMax()).isEqualByComparingTo("116.67");
            assertThat(result.getPercentageApplied()).isEqualByComparingTo("0.70");
            assertThat(result.getRawLoad()).isEqualByComparingTo("81.67");
        }

        @Test
        @DisplayName("Objetivo se refleja en la respuesta")
        void calculateLoad_returnsGoal() {
            var result = service.calculateLoad(BigDecimal.TEN, 5, TrainingGoal.STRENGTH);
            assertThat(result.getGoal()).isEqualTo(TrainingGoal.STRENGTH);
        }
    }

    @Nested
    @DisplayName("roundLoad (Redondeo por equipo)")
    class RoundLoad {

        @ParameterizedTest
        @CsvSource({
                "100.0, BARBELL, 100.00",
                "101.0, BARBELL, 100.00",
                "101.3, BARBELL, 102.50",
                "102.6, BARBELL, 102.50",
                "103.0, BARBELL, 102.50",
                "100.0, DUMBBELL, 100.00",
                "101.0, DUMBBELL, 102.00",
                "101.1, DUMBBELL, 102.00",
                "102.0, DUMBBELL, 102.00",
                "103.0, DUMBBELL, 104.00",
                "100.0, MACHINE, 100.00",
                "102.0, MACHINE, 100.00",
                "103.0, MACHINE, 105.00"
        })
        @DisplayName("Redondeo a incremento del equipo")
        void roundLoad_parameterized(double weight, Equipment equipment, String expected) {
            var result = service.roundLoad(BigDecimal.valueOf(weight), equipment);
            assertThat(result.getRoundedWeight()).isEqualByComparingTo(expected);
        }

        @Test
        @DisplayName("Respuesta contiene incremento y equipo correctos")
        void roundLoad_containsMetadata() {
            var result = service.roundLoad(new BigDecimal("101"), Equipment.BARBELL);
            assertThat(result.getEquipment()).isEqualTo(Equipment.BARBELL);
            assertThat(result.getIncrementKg()).isEqualByComparingTo("2.5");
            assertThat(result.getRawWeight()).isEqualByComparingTo("101");
        }

        @Test
        @DisplayName("Peso exacto en incremento se mantiene")
        void roundLoad_exactIncrement() {
            var result = service.roundLoad(new BigDecimal("100"), Equipment.MACHINE);
            assertThat(result.getRoundedWeight()).isEqualByComparingTo("100.00");
        }
    }
}
