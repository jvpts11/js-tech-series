/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ElementWeightsTest {

    @Test
    void of_holdsTheWeightOfEveryElementOfTheKnownTable() {
        assertEquals(Element.values().length, ElementWeights.count());
        assertEquals(1.008, ElementWeights.of(Element.HYDROGEN.number()));
        assertEquals(55.845, ElementWeights.of(Element.IRON.number()));
        assertEquals(238.03, ElementWeights.of(Element.URANIUM.number()));
        assertEquals(294.0, ElementWeights.of(Element.OGANESSON.number()));
    }

    @Test
    void of_growsAlongTheTableButForItsFamousExceptions() {
        int inversions = 0;
        for (int number = 2; number <= ElementWeights.count(); number++) {
            if (ElementWeights.of(number) < ElementWeights.of(number - 1)) {
                inversions++;
            }
        }
        // Argon and potassium, cobalt and nickel, tellurium and iodine, thorium and protactinium, and a few of the
        // superheavies by their longest-lived isotopes.
        assertTrue(inversions >= 4 && inversions <= 8, "inversions: " + inversions);
    }

    @Test
    void of_refusesWhatIsNoElement() {
        assertThrows(IllegalArgumentException.class, () -> ElementWeights.of(0));
        assertThrows(IllegalArgumentException.class, () -> ElementWeights.of(119));
    }
}
