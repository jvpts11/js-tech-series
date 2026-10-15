/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries;

import dev.jstech.core.JsCore;
import dev.jstech.core.material.Forms;
import dev.jstech.core.material.HostRocks;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MaterialKind;
import dev.jstech.core.material.MaterialProperties;
import dev.jstech.core.material.Materials;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.material.OreLook;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/**
 * The series' catalogue of what the world is made of, which every mod of the series and any other shares: the metals
 * the game already has, with the things it has for them, and those it does not.
 */
@TextHolder
public final class SeriesMaterials {

    public static final TextKey IRON_NAME = TextKey.of("jscore.material.iron", "Iron");
    public static final TextKey COPPER_NAME = TextKey.of("jscore.material.copper", "Copper");
    public static final TextKey GOLD_NAME = TextKey.of("jscore.material.gold", "Gold");
    public static final TextKey TIN_NAME = TextKey.of("jscore.material.tin", "Tin");
    public static final TextKey BRONZE_NAME = TextKey.of("jscore.material.bronze", "Bronze");

    /* Iron's ores are drawn in the rust of the game's own iron ore, not in the grey of the metal. */
    public static final Material IRON = material("iron", MaterialKind.METAL, IRON_NAME).colour(0xA8A9AB)
            .with(MaterialProperties.SYMBOL, "Fe").with(MaterialProperties.ATOMIC_NUMBER, 26)
            .also(MatterState.LIQUID).ore(OreLook.PLAIN.inColour(0xAF8E77))
            .provided(Forms.INGOT, () -> Items.IRON_INGOT).provided(Forms.NUGGET, () -> Items.IRON_NUGGET)
            .provided(Forms.BLOCK, () -> Items.IRON_BLOCK).provided(Forms.RAW, () -> Items.RAW_IRON)
            .providedOre(HostRocks.STONE, () -> Blocks.IRON_ORE)
            .providedOre(HostRocks.DEEPSLATE, () -> Blocks.DEEPSLATE_IRON_ORE)
            .declare();
    public static final Material COPPER = material("copper", MaterialKind.METAL, COPPER_NAME).colour(0xD9773F)
            .with(MaterialProperties.SYMBOL, "Cu").with(MaterialProperties.ATOMIC_NUMBER, 29)
            .also(MatterState.LIQUID).ore(OreLook.of(OreLook.Pattern.COPPER, 0x3A7663, 0.25, 0.55))
            .provided(Forms.INGOT, () -> Items.COPPER_INGOT).provided(Forms.BLOCK, () -> Items.COPPER_BLOCK)
            .provided(Forms.RAW, () -> Items.RAW_COPPER)
            .providedOre(HostRocks.STONE, () -> Blocks.COPPER_ORE)
            .providedOre(HostRocks.DEEPSLATE, () -> Blocks.DEEPSLATE_COPPER_ORE)
            .declare();
    public static final Material GOLD = material("gold", MaterialKind.METAL, GOLD_NAME).colour(0xF2C33A)
            .with(MaterialProperties.SYMBOL, "Au").with(MaterialProperties.ATOMIC_NUMBER, 79)
            .also(MatterState.LIQUID).ore(OreLook.of(OreLook.Pattern.GOLD))
            .provided(Forms.INGOT, () -> Items.GOLD_INGOT).provided(Forms.NUGGET, () -> Items.GOLD_NUGGET)
            .provided(Forms.BLOCK, () -> Items.GOLD_BLOCK).provided(Forms.RAW, () -> Items.RAW_GOLD)
            .providedOre(HostRocks.STONE, () -> Blocks.GOLD_ORE)
            .providedOre(HostRocks.DEEPSLATE, () -> Blocks.DEEPSLATE_GOLD_ORE)
            .providedOre(HostRocks.NETHERRACK, () -> Blocks.NETHER_GOLD_ORE)
            .declare();
    /* Tin's ore is drawn as its mineral, cassiterite, is: dark brown grains. */
    public static final Material TIN = material("tin", MaterialKind.METAL, TIN_NAME).colour(0xB9C0C9)
            .with(MaterialProperties.SYMBOL, "Sn").with(MaterialProperties.ATOMIC_NUMBER, 50)
            .also(MatterState.LIQUID).ore(OreLook.of(OreLook.Pattern.COAL).inColour(0x5A4434))
            .declare();
    /* An alloy of copper and tin, whose proportions vary from one bronze to another: it has no one formula. */
    public static final Material BRONZE = material("bronze", MaterialKind.METAL, BRONZE_NAME).colour(0xC08436)
            .also(MatterState.LIQUID)
            .declare();

    private SeriesMaterials() {
    }

    /** Declares the catalogue, from the Core's constructor. */
    public static void declare() {
        // Loading the class declares it.
    }

    private static Material.Builder material(final String path, final MaterialKind kind, final TextKey name) {
        return Materials.material(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, path), kind, name);
    }
}
