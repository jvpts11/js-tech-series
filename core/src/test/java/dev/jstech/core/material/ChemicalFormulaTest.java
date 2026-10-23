/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.material;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.function.ToDoubleFunction;
import org.junit.jupiter.api.Test;

class ChemicalFormulaTest {

    private static final double CLOSE = 1e-3;
    private static final Map<String, Double> WEIGHTS = Map.of("H", 1.008, "C", 12.011, "O", 15.999, "S", 32.06,
            "Fe", 55.845, "Cu", 63.546, "N", 14.007);
    private static final ToDoubleFunction<String> WEIGHT = symbol -> WEIGHTS.getOrDefault(symbol, 0.0);

    @Test
    void parse_countsTheAtomsOfEachElement() {
        assertEquals(Map.of("Cu", 1, "Fe", 1, "S", 2), ChemicalFormula.parse("CuFeS2").atoms());
        assertEquals(Map.of("Fe", 2, "O", 3), ChemicalFormula.parse("Fe2O3").atoms());
    }

    @Test
    void parse_multipliesBracketedGroups() {
        assertEquals(Map.of("Cu", 2, "C", 1, "O", 5, "H", 2), ChemicalFormula.parse("Cu2CO3(OH)2").atoms());
        assertEquals(Map.of("Fe", 1, "C", 6, "N", 6), ChemicalFormula.parse("Fe[(CN)6]").atoms());
    }

    @Test
    void parse_addsAHydratesWaterAfterItsDot() {
        assertEquals(Map.of("Cu", 1, "S", 1, "O", 9, "H", 10), ChemicalFormula.parse("CuSO4·5H2O").atoms());
        assertEquals(ChemicalFormula.parse("CuSO4·5H2O"), ChemicalFormula.parse("CuSO4.5H2O"));
    }

    @Test
    void parse_readsSubscriptsAsPlainCounts() {
        assertEquals(ChemicalFormula.parse("CuFeS2"), ChemicalFormula.parse("CuFeS₂"));
    }

    @Test
    void parse_refusesWhatIsNoFormula() {
        assertThrows(IllegalArgumentException.class, () -> ChemicalFormula.parse(""));
        assertThrows(IllegalArgumentException.class, () -> ChemicalFormula.parse("Cu2(OH"));
        assertThrows(IllegalArgumentException.class, () -> ChemicalFormula.parse("cuFe"));
        assertThrows(IllegalArgumentException.class, () -> ChemicalFormula.parse("Fe1-xS"));
        assertTrue(ChemicalFormula.read("Fe0").isEmpty());
    }

    @Test
    void massFraction_isEachElementsShareOfTheMass() {
        final ChemicalFormula chalcopyrite = ChemicalFormula.parse("CuFeS2");
        assertEquals(0.3463, chalcopyrite.massFraction("Cu", WEIGHT), CLOSE);
        assertEquals(0.3043, chalcopyrite.massFraction("Fe", WEIGHT), CLOSE);
        assertEquals(0.3494, chalcopyrite.massFraction("S", WEIGHT), CLOSE);
        assertEquals(0.0, chalcopyrite.massFraction("O", WEIGHT), CLOSE);
    }

    @Test
    void massFractions_addUpToTheWhole() {
        final double total = ChemicalFormula.parse("Cu2CO3(OH)2").massFractions(WEIGHT).values().stream()
                .mapToDouble(Double::doubleValue).sum();
        assertEquals(1.0, total, 1e-9);
    }

    @Test
    void molarMass_addsTheAtomsWeights() {
        assertEquals(159.687, ChemicalFormula.parse("Fe2O3").molarMass(WEIGHT), CLOSE);
        assertThrows(IllegalArgumentException.class, () -> ChemicalFormula.parse("Xe").molarMass(WEIGHT));
    }

    @Test
    void subscripted_writesTheCountsSmallButAHydratesWaterLarge() {
        assertEquals("Cu₂CO₃(OH)₂", ChemicalFormula.parse("Cu2CO3(OH)2").subscripted());
        assertEquals("CuSO₄·5H₂O", ChemicalFormula.parse("CuSO4.5H2O").subscripted());
    }
}
