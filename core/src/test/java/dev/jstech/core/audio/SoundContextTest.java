/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SoundContextTest {

    @Test
    void with_setsOrReplacesOneDimension() {
        final SoundContext context = SoundContext.EMPTY.with(SoundContext.ERA, "vintage").with(SoundContext.ERA,
                "legacy");
        assertEquals("legacy", context.get(SoundContext.ERA));
        assertNull(context.get(SoundContext.DEVICE));
        assertNull(SoundContext.EMPTY.get(SoundContext.ERA));
    }

    @Test
    void and_laysTheOtherOnTopWhereBothSay() {
        final SoundContext machine = new SoundContext(Map.of(SoundContext.ERA, "vintage", SoundContext.FAMILY, "dos"));
        final SoundContext played = machine.and(new SoundContext(Map.of(SoundContext.FAMILY, "unix",
                SoundContext.DEVICE, "jsc:pc_speaker")));
        assertEquals(Map.of(SoundContext.ERA, "vintage", SoundContext.FAMILY, "unix", SoundContext.DEVICE,
                "jsc:pc_speaker"), played.values());
    }

    @Test
    void constructor_refusesWhatCannotTravel() {
        final Map<String, String> many = new HashMap<>();
        for (int i = 0; i <= SoundContext.MAX_DIMENSIONS; i++) {
            many.put("d" + i, "v");
        }
        assertThrows(IllegalArgumentException.class, () -> new SoundContext(many));
        assertThrows(IllegalArgumentException.class, () -> new SoundContext(Map.of("", "v")));
        assertThrows(IllegalArgumentException.class,
                () -> new SoundContext(Map.of("era", "x".repeat(SoundContext.MAX_LENGTH + 1))));
    }
}
