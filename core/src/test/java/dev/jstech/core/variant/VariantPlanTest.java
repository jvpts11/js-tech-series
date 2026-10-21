/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.variant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class VariantPlanTest {

    private static final VariantPlan.Kind FRAME = new VariantPlan.Kind("othermod:frame", VariantFamily.WOOD);
    private static final VariantPlan.Kind CASING = new VariantPlan.Kind("othermod:casing", VariantFamily.METAL);
    private static final VariantPlan.Found OAK = new VariantPlan.Found(VariantFamily.WOOD, "minecraft:oak",
            "minecraft:oak_planks");
    private static final VariantPlan.Found MAPLE = new VariantPlan.Found(VariantFamily.WOOD, "woodmod:maple",
            "woodmod:maple_planks");
    private static final VariantPlan.Found TIN = new VariantPlan.Found(VariantFamily.METAL, "c:tin",
            "metalmod:tin_block");

    @Test
    void of_makesEveryKindForEveryMaterialOfItsFamily() {
        final List<VariantPlan.Planned> planned = VariantPlan.of(List.of(FRAME, CASING), List.of(OAK, MAPLE, TIN),
                VariantPlan.Skipped.NONE);

        assertEquals(List.of("othermod:oak_frame", "othermod:woodmod_maple_frame", "othermod:tin_casing"),
                planned.stream().map(VariantPlan.Planned::id).toList());
        assertEquals("metalmod:tin_block", planned.get(2).source());
    }

    @Test
    void of_leavesOutTheModsMaterialsAndKindsTheSettingsSay() {
        final List<VariantPlan.Found> found = List.of(OAK, MAPLE, TIN);

        assertEquals(List.of("othermod:oak_frame", "othermod:tin_casing"), ids(VariantPlan.of(List.of(FRAME,
                CASING), found, new VariantPlan.Skipped(true, Set.of("woodmod"), Set.of(), Set.of()))));
        assertEquals(List.of("othermod:woodmod_maple_frame"), ids(VariantPlan.of(List.of(FRAME, CASING), found,
                new VariantPlan.Skipped(true, Set.of("metalmod"), Set.of("minecraft:oak"), Set.of()))));
        assertEquals(List.of("othermod:tin_casing"), ids(VariantPlan.of(List.of(FRAME, CASING), found,
                new VariantPlan.Skipped(true, Set.of(), Set.of(), Set.of("othermod:frame")))));
        assertTrue(VariantPlan.of(List.of(FRAME, CASING), found, new VariantPlan.Skipped(false, Set.of(), Set.of(),
                Set.of())).isEmpty(), "and nothing when variants are off");
    }

    @Test
    void of_makesEachIdOnce() {
        final VariantPlan.Found again = new VariantPlan.Found(VariantFamily.METAL, "c:tin", "othermetals:tin_block");

        assertEquals(List.of("othermod:tin_casing"), ids(VariantPlan.of(List.of(CASING), List.of(TIN, again),
                VariantPlan.Skipped.NONE)));
    }

    @Test
    void woodOf_readsTheWoodFromThePlanksNameEitherWay() {
        assertEquals("dark_oak", VariantPlan.woodOf("dark_oak_planks"));
        assertEquals("maple", VariantPlan.woodOf("planks_maple"));
        assertEquals("planks", VariantPlan.woodOf("planks"));
        assertEquals("boards", VariantPlan.woodOf("boards"));
    }

    @Test
    void englishOf_readsAPathAsAName() {
        assertEquals("Dark Oak", VariantPlan.englishOf("dark_oak"));
        assertEquals("Stone Types Limestone", VariantPlan.englishOf("stone_types/limestone"));
    }

    private static List<String> ids(final List<VariantPlan.Planned> planned) {
        return planned.stream().map(VariantPlan.Planned::id).toList();
    }
}
