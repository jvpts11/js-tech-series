/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.acoustics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MaterialSoundsTest {

    private MaterialSounds table;

    @BeforeEach
    void setUp() {
        table = new MaterialSounds();
    }

    @Test
    void of_aBlocksOwnEntryWinsOverItsTagsFieldByField() {
        table.put("#minecraft:wool", new MaterialSounds.Material(Map.of(MaterialSounds.Kind.STEP, "pack:soft_step"),
                OptionalDouble.of(0.7), OptionalDouble.of(0.3)));
        table.put("minecraft:white_wool", new MaterialSounds.Material(
                Map.of(MaterialSounds.Kind.STEP, "pack:white_step"), OptionalDouble.empty(), OptionalDouble.empty()));
        final MaterialSounds.Material white = table.of("minecraft:white_wool", List.of("#minecraft:wool"));
        assertEquals(Optional.of("pack:white_step"), white.sound(MaterialSounds.Kind.STEP));
        assertEquals(0.7, white.absorption().getAsDouble(), 1e-9, "what the block leaves unsaid comes from its tag");
        final MaterialSounds.Material red = table.of("minecraft:red_wool", List.of("#minecraft:wool"));
        assertEquals(Optional.of("pack:soft_step"), red.sound(MaterialSounds.Kind.STEP));
    }

    @Test
    void put_aLaterPackReplacesWhatItSaysAndKeepsTheRest() {
        table.put("minecraft:chest", new MaterialSounds.Material(Map.of(MaterialSounds.Kind.HIT, "pack:creak"),
                OptionalDouble.of(0.15), OptionalDouble.empty()));
        table.put("minecraft:chest", new MaterialSounds.Material(Map.of(MaterialSounds.Kind.BREAK, "pack:splinter"),
                OptionalDouble.of(0.2), OptionalDouble.empty()));
        final MaterialSounds.Material chest = table.of("minecraft:chest", List.of());
        assertEquals(Optional.of("pack:creak"), chest.sound(MaterialSounds.Kind.HIT));
        assertEquals(Optional.of("pack:splinter"), chest.sound(MaterialSounds.Kind.BREAK));
        assertEquals(0.2, chest.absorption().getAsDouble(), 1e-9);
    }

    @Test
    void of_aBlockNoPackNamesSaysNothing() {
        assertEquals(MaterialSounds.Material.NOTHING, table.of("minecraft:stone", List.of("#minecraft:base_stone")));
        assertTrue(table.isEmpty());
    }

    @Test
    void material_refusesAShareOutOfRange() {
        assertThrows(IllegalArgumentException.class, () -> new MaterialSounds.Material(Map.of(),
                OptionalDouble.of(1.5), OptionalDouble.empty()));
    }
}
