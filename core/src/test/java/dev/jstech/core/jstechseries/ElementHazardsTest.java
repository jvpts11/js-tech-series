/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.hazard.HazardClass;
import java.util.List;
import org.junit.jupiter.api.Test;

class ElementHazardsTest {

    @Test
    void of_marksEveryElementWithNoStableIsotopeRadioactive() {
        assertTrue(ElementHazards.of(Element.TECHNETIUM).contains(HazardClass.RADIOACTIVE));
        assertTrue(ElementHazards.of(Element.PROMETHIUM).contains(HazardClass.RADIOACTIVE));
        assertTrue(ElementHazards.of(Element.POLONIUM).contains(HazardClass.RADIOACTIVE));
        assertTrue(ElementHazards.of(Element.OGANESSON).contains(HazardClass.RADIOACTIVE));
        assertFalse(ElementHazards.of(Element.BISMUTH).contains(HazardClass.RADIOACTIVE));
    }

    @Test
    void of_givesTheClassesOfARealSafetyDataSheet() {
        assertEquals(List.of(HazardClass.OXIDISING, HazardClass.ACUTELY_TOXIC, HazardClass.HARMFUL,
                HazardClass.ENVIRONMENTAL), ElementHazards.of(Element.CHLORINE));
        assertEquals(List.of(HazardClass.FLAMMABLE), ElementHazards.of(Element.HYDROGEN));
        assertEquals(List.of(HazardClass.ASPHYXIANT, HazardClass.RADIOACTIVE), ElementHazards.of(Element.RADON));
        assertTrue(ElementHazards.of(Element.URANIUM).contains(HazardClass.ACUTELY_TOXIC));
    }

    @Test
    void of_givesNothingForAHarmlessElement() {
        assertTrue(ElementHazards.of(Element.IRON).isEmpty());
        assertTrue(ElementHazards.of(Element.GOLD).isEmpty());
    }
}
