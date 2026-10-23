/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.chemistry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class RareEarthCascadeTest {

    @Test
    void stages_areFourEightAndSixteen() {
        assertEquals(4, RareEarthCascade.LIGHT.stages());
        assertEquals(8, RareEarthCascade.MIDDLE.stages());
        assertEquals(16, RareEarthCascade.HEAVY.stages());
    }

    @Test
    void separates_upToTheGroupItsStagesReach() {
        assertEquals(Optional.of(RareEarthCascade.MIDDLE), RareEarthCascade.separates(10));
        assertEquals(Optional.of(RareEarthCascade.HEAVY), RareEarthCascade.separates(16));
        assertTrue(RareEarthCascade.separates(3).isEmpty());
    }
}
