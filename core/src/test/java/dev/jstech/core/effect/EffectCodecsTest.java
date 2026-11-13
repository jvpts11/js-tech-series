/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonPrimitive;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

class EffectCodecsTest {

    @Test
    void rgba_readsAColourOfSixOrEightHexDigits() {
        assertEquals(0xFFFF8000, read("#FF8000"));
        assertEquals(0x80FF8000, read("FF800080"));
    }

    @Test
    void rgba_refusesASignWhereADigitShouldBe() {
        for (final String written : new String[] {"#-FFFFF", "+FFFFF", "#+FFFFFFF", "-FFFFFFF"}) {
            assertTrue(EffectCodecs.RGBA.parse(JsonOps.INSTANCE, new JsonPrimitive(written)).error().isPresent(),
                    written + " is no colour");
        }
    }

    private static int read(final String written) {
        return EffectCodecs.RGBA.parse(JsonOps.INSTANCE, new JsonPrimitive(written)).getOrThrow();
    }
}
