/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import dev.jstech.core.content.ContentTab;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.variant.VariantFamily;
import dev.jstech.core.variant.VariantKind;
import dev.jstech.core.variant.Variants;

/**
 * Kinds of variant of every family, to prove the Core makes them by themselves for every material found: a frame of
 * every wood, drawn as a plain cube and in a tag of its own; a tile of every stone; a casing of every metal, drawn with
 * a column's model.
 */
@TextHolder
public final class TestVariants {

    public static final TextKey FRAME_NAME = TextKey.of("block.jstests.variant.frame", "%s Test Frame");
    public static final TextKey TILE_NAME = TextKey.of("block.jstests.variant.tile", "%s Test Tile");
    public static final TextKey CASING_NAME = TextKey.of("block.jstests.variant.casing", "%s Test Casing");

    private static final ContentTab.Section VARIANTS = TestItems.TAB.section();

    /** A frame of every wood. */
    public static final VariantKind FRAMES = Variants.kind(JsTests.MODID, "test_frame", VariantFamily.WOOD,
            FRAME_NAME).tagged("jstests:test_frames").tab(VARIANTS).declare();
    /** A tile of every stone. */
    public static final VariantKind TILES = Variants.kind(JsTests.MODID, "test_tile", VariantFamily.STONE,
            TILE_NAME).tab(VARIANTS).declare();
    /** A casing of every metal, its sides and its ends drawn with the metal. */
    public static final VariantKind CASINGS = Variants.kind(JsTests.MODID, "test_casing", VariantFamily.METAL,
            CASING_NAME).model("minecraft:block/cube_column", "side", "end").tab(VARIANTS).declare();

    private TestVariants() {
    }

    /** Declares the kinds, before the game registers its things. */
    public static void declare() {
        // Loading the class declares them.
    }
}
