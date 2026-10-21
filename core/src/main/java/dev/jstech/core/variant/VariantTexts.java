/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.variant;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * The names of the game's own woods and metals that the game has no word for by themselves, only for their blocks
 * ("Oak Planks"), so a variant made of one is named in the player's language: "Oak Frame". The keys are the ones a
 * found material asks for ({@link FoundMaterial#name()}); another mod's woods are named by the words of their ids
 * unless a resource pack gives them a name under the same kind of key.
 */
@TextHolder
public final class VariantTexts {

    public static final TextKey OAK = TextKey.of("jscore.variant.wood.minecraft.oak", "Oak");
    public static final TextKey SPRUCE = TextKey.of("jscore.variant.wood.minecraft.spruce", "Spruce");
    public static final TextKey BIRCH = TextKey.of("jscore.variant.wood.minecraft.birch", "Birch");
    public static final TextKey JUNGLE = TextKey.of("jscore.variant.wood.minecraft.jungle", "Jungle");
    public static final TextKey ACACIA = TextKey.of("jscore.variant.wood.minecraft.acacia", "Acacia");
    public static final TextKey DARK_OAK = TextKey.of("jscore.variant.wood.minecraft.dark_oak", "Dark Oak");
    public static final TextKey MANGROVE = TextKey.of("jscore.variant.wood.minecraft.mangrove", "Mangrove");
    public static final TextKey CHERRY = TextKey.of("jscore.variant.wood.minecraft.cherry", "Cherry");
    public static final TextKey BAMBOO = TextKey.of("jscore.variant.wood.minecraft.bamboo", "Bamboo");
    public static final TextKey CRIMSON = TextKey.of("jscore.variant.wood.minecraft.crimson", "Crimson");
    public static final TextKey WARPED = TextKey.of("jscore.variant.wood.minecraft.warped", "Warped");
    public static final TextKey NETHERITE = TextKey.of("jscore.variant.metal.netherite", "Netherite");

    private VariantTexts() {
    }
}
