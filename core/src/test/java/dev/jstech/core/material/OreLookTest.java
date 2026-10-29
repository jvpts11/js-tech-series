/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.material;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class OreLookTest {

    @Test
    void pattern_ofTheStoneIsTakenOffTheStoneOrTheDeepslate() {
        assertEquals("block/iron_ore", OreLook.Pattern.IRON.texture(false));
        assertEquals("block/stone", OreLook.Pattern.IRON.rock(false));
        assertEquals("block/deepslate_iron_ore", OreLook.Pattern.IRON.texture(true));
        assertEquals("block/deepslate", OreLook.Pattern.IRON.rock(true));
    }

    @Test
    void pattern_ofQuartzVeinsIsTakenOffTheNetherrackWhateverTheRock() {
        for (final boolean deepslate : new boolean[] {false, true}) {
            assertEquals("block/nether_quartz_ore", OreLook.Pattern.QUARTZ.texture(deepslate));
            assertEquals("block/netherrack", OreLook.Pattern.QUARTZ.rock(deepslate));
        }
    }
}
