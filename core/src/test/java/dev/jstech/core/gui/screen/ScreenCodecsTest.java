/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui.screen;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonPrimitive;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

class ScreenCodecsTest {

    @Test
    void track_writtenAsANumberNoColumnCanBeIsAnErrorInItsFile() {
        for (final String word : new String[] {"-4", "5000"}) {
            assertTrue(ScreenCodecs.TRACK.parse(JsonOps.INSTANCE, new JsonPrimitive(word)).isError(), word);
        }
        assertTrue(ScreenCodecs.TRACK.parse(JsonOps.INSTANCE, new JsonPrimitive("18")).isSuccess());
        assertTrue(ScreenCodecs.TRACK.parse(JsonOps.INSTANCE, new JsonPrimitive("2fr")).isSuccess());
    }
}
