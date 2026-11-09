/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.chain;

import dev.jstech.computers.JsComputers;
import dev.jstech.core.jstechseries.Element;
import dev.jstech.core.jstechseries.Elements;
import dev.jstech.core.jstechseries.SeriesMinerals;
import dev.jstech.core.material.HostRock;
import dev.jstech.core.material.HostRocks;
import dev.jstech.core.material.Material;
import dev.jstech.core.worldgen.vein.Deposits;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;

/**
 * The veins of the chain's ores (computers/docs/design/survival.md), on J's Core's veins, all in the Overworld: the
 * metals of the Vintage near the surface, those of the Legacy deeper, those of the Standard and the Advanced only in
 * the deepslate and rarer, so each era takes the player further across the world and further down. Tin's vein is the
 * Core's own. A vein lays the ore of its element in each rock the chain uses it in.
 */
public final class ChainDeposits {

    /* A vein of the first era lies, on average, 12 chunks from the next of its kind; the zones are 3 chunks a side. */
    private static final double VINTAGE = frequency(12);
    private static final double LEGACY = frequency(16);
    private static final double DEEPSLATE = frequency(24);

    /** The Vintage's veins, near the surface. */
    public static final List<ResourceLocation> NEAR_THE_SURFACE = List.of(
            shallow(Element.LEAD), shallow(Element.ZINC), shallow(Element.NICKEL), shallow(Element.ALUMINIUM),
            shallow(Element.TANTALUM), shallow(Element.CHROMIUM), shallow(Element.MANGANESE),
            shallow(Element.MERCURY), vein("fluorspar", SeriesMinerals.FLUORSPAR, 0, 80, VINTAGE,
                    ChainMaterials.FLUORSPAR_ROCKS.toArray(HostRock[]::new)));
    /** The Legacy's veins, deeper. */
    public static final List<ResourceLocation> DEEP = List.of(deep(Element.COBALT), deep(Element.SILVER),
            deep(Element.LITHIUM));
    /** The Standard's and the Advanced's veins, only in the deepslate. */
    public static final List<ResourceLocation> IN_THE_DEEPSLATE = List.of(deepslate(Element.ZIRCONIUM),
            deepslate(Element.TUNGSTEN), deepslate(Element.MOLYBDENUM));

    private ChainDeposits() {
    }

    /** Declares the veins, from the mod's constructor. */
    public static void declare() {
        // Loading the class declares them.
    }

    private static ResourceLocation shallow(final Element element) {
        return vein(element.path(), Elements.of(element), 0, 80, VINTAGE, HostRocks.STONE, HostRocks.DEEPSLATE);
    }

    private static ResourceLocation deep(final Element element) {
        return vein(element.path(), Elements.of(element), -16, 40, LEGACY, HostRocks.STONE, HostRocks.DEEPSLATE);
    }

    private static ResourceLocation deepslate(final Element element) {
        return vein(element.path(), Elements.of(element), -64, 0, DEEPSLATE, HostRocks.DEEPSLATE);
    }

    /*
     * A vein of {@code material} alone, its middle from {@code low} to {@code high}, the size of the Core's kit,
     * through the rocks the chain lays its ore in.
     */
    private static ResourceLocation vein(final String path, final Material material, final int low, final int high,
                                         final double frequency, final HostRock... rocks) {
        return Deposits.deposit(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, path))
                .mineral(material, 1).heights(low, high).frequency(frequency).size(18, 6).density(0.45)
                .rocks(rocks).biomes(BiomeTags.IS_OVERWORLD).declare();
    }

    /* How often a zone holds a vein of a kind whose veins lie {@code spacing} chunks apart on average. */
    private static double frequency(final int spacing) {
        final double zones = (double) spacing / 3;
        return 1 / (zones * zones);
    }
}
