/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries;

import dev.jstech.core.JsCore;
import dev.jstech.core.worldgen.vein.Deposits;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;

/**
 * The veins of the series' shared metals, which stand in for the game's scattered iron, copper and gold while ores lie
 * in veins: iron through the overworld's rock from deep to the hills, copper higher and wider, gold deep and rare,
 * gold in the Nether's netherrack, and tin, laid only once a mod uses tin's ore. Each lays its ore in every rock some
 * installed mod uses it in, the game's own in stone and deepslate.
 */
public final class SeriesDeposits {

    public static final ResourceLocation IRON = Deposits.deposit(id("iron")).mineral(SeriesMaterials.IRON, 1)
            .heights(-48, 96).frequency(0.16).size(20, 6).density(0.5).biomes(BiomeTags.IS_OVERWORLD).declare();
    public static final ResourceLocation COPPER = Deposits.deposit(id("copper")).mineral(SeriesMaterials.COPPER, 1)
            .heights(-16, 112).frequency(0.12).size(22, 7).density(0.5).biomes(BiomeTags.IS_OVERWORLD).declare();
    public static final ResourceLocation GOLD = Deposits.deposit(id("gold")).mineral(SeriesMaterials.GOLD, 1)
            .heights(-64, 32).frequency(0.05).size(16, 5).density(0.4).biomes(BiomeTags.IS_OVERWORLD).declare();
    public static final ResourceLocation NETHER_GOLD = Deposits.deposit(id("nether_gold"))
            .mineral(SeriesMaterials.GOLD, 1).heights(10, 117).frequency(0.2).size(18, 6).density(0.4)
            .biomes(BiomeTags.IS_NETHER).declare();
    public static final ResourceLocation TIN = Deposits.deposit(id("tin")).mineral(SeriesMaterials.TIN, 1)
            .heights(-16, 80).frequency(0.08).size(18, 6).density(0.45).biomes(BiomeTags.IS_OVERWORLD).declare();

    private SeriesDeposits() {
    }

    /** Declares the veins, from the Core's constructor. */
    public static void declare() {
        // Loading the class declares them.
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsCore.MODID, path);
    }
}
