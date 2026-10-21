/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.variant;

/**
 * The kinds of other mods' materials variants are made for, each found through the game's common tags: the woods by
 * their planks ({@code minecraft:planks}), the stones by the tags of the world's stones ({@code c:stones} and the
 * game's own), the metals by their blocks ({@code c:storage_blocks/tin}) where an ingot of the same name exists
 * ({@code c:ingots/tin}). Each says the tool its variants are broken with.
 *
 * <p>Pure: no game types.
 */
public enum VariantFamily {

    /** The woods, found by their planks; broken with an axe. */
    WOOD("wood", "minecraft:mineable/axe"),
    /** The stones, found by the tags of the world's stones; broken with a pickaxe. */
    STONE("stone", "minecraft:mineable/pickaxe"),
    /** The metals, found by their blocks and ingots; broken with a pickaxe. */
    METAL("metal", "minecraft:mineable/pickaxe");

    private final String serializedName;
    private final String toolTag;

    VariantFamily(final String serializedName, final String toolTag) {
        this.serializedName = serializedName;
        this.toolTag = toolTag;
    }

    /** Its name in files and settings. */
    public String serializedName() {
        return this.serializedName;
    }

    /** The game's tag its variants are in for the tool that breaks them fastest, as {@code minecraft:mineable/axe}. */
    public String toolTag() {
        return this.toolTag;
    }
}
