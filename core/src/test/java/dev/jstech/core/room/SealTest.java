/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.room;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

class SealTest {

    @Test
    void codec_readsAWallUnlessToldItIsNone() {
        final Seal cracked = Seal.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"leak\": 0.5}"))
                .getOrThrow();
        assertTrue(cracked.wall(), "a seal written without a word of walls is a wall: " + cracked);
        assertEquals(0.5, cracked.leak());
        final Seal furniture = Seal.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"wall\": false}"))
                .getOrThrow();
        assertFalse(furniture.wall(), "one told it is no wall lets air through: " + furniture);
    }

    @Test
    void codec_writesAndReadsBackWallsAndWhatPasses() {
        for (final Seal seal : new Seal[] {Seal.PASSES, Seal.SOLID, Seal.wall(0.02, 300), Seal.door(0.1, 50)}) {
            final JsonElement written = Seal.CODEC.encodeStart(JsonOps.INSTANCE, seal).getOrThrow();
            assertEquals(seal, Seal.CODEC.parse(JsonOps.INSTANCE, written).getOrThrow(), "read back as written: "
                    + written);
        }
    }
}
