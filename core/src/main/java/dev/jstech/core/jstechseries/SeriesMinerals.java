/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries;

import dev.jstech.core.JsCore;
import dev.jstech.core.material.HostRocks;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MineralFamilies;
import dev.jstech.core.material.MineralFamily;
import dev.jstech.core.material.OreLook;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

/**
 * The real minerals of the series' catalogue, one for each family a first industry meets: the oxides, carbonates and
 * sulfides of iron and copper, gold's natural alloy with silver, and tin's oxide. Each is a material of its own
 * ({@code jscore:chalcopyrite}), with its formula, its family, the rocks its veins cross and the byproducts it gives,
 * and has an ore only once a mod uses it. Which minerals exist is J's Industrial's to say; the rest of its list joins
 * this one as the mod grows.
 */
@TextHolder
public final class SeriesMinerals {

    public static final TextKey HEMATITE_NAME = TextKey.of("jscore.material.hematite", "Hematite");
    public static final TextKey MAGNETITE_NAME = TextKey.of("jscore.material.magnetite", "Magnetite");
    public static final TextKey SIDERITE_NAME = TextKey.of("jscore.material.siderite", "Siderite");
    public static final TextKey PYRITE_NAME = TextKey.of("jscore.material.pyrite", "Pyrite");
    public static final TextKey CHALCOPYRITE_NAME = TextKey.of("jscore.material.chalcopyrite", "Chalcopyrite");
    public static final TextKey CUPRITE_NAME = TextKey.of("jscore.material.cuprite", "Cuprite");
    public static final TextKey MALACHITE_NAME = TextKey.of("jscore.material.malachite", "Malachite");
    public static final TextKey ELECTRUM_NAME = TextKey.of("jscore.material.electrum", "Electrum");
    public static final TextKey CASSITERITE_NAME = TextKey.of("jscore.material.cassiterite", "Cassiterite");

    /** Iron's red oxide, the ore of the world's steel. */
    public static final Material HEMATITE = mineral("hematite", HEMATITE_NAME, MineralFamilies.OXIDE, Element.IRON)
            .formula("Fe2O3").colour(0x8B3A2E).ore(OreLook.of(OreLook.Pattern.IRON).inColour(0x9C4A3A))
            .rocks(HostRocks.STONE, HostRocks.SANDSTONE).declare();
    /** Iron's black, magnetic oxide, which carries titanium and vanadium. */
    public static final Material MAGNETITE = mineral("magnetite", MAGNETITE_NAME, MineralFamilies.OXIDE,
            Element.IRON).formula("Fe3O4").trace(Element.TITANIUM, 0.03).trace(Element.VANADIUM, 0.005)
            .colour(0x2B2B2E).ore(OreLook.of(OreLook.Pattern.COAL).inColour(0x26262A))
            .rocks(HostRocks.DEEPSLATE, HostRocks.DIORITE, HostRocks.ANDESITE).declare();
    /** Iron's carbonate, with manganese in it. */
    public static final Material SIDERITE = mineral("siderite", SIDERITE_NAME, MineralFamilies.CARBONATE,
            Element.IRON).formula("FeCO3").trace(Element.MANGANESE, 0.02).colour(0xA0794A)
            .ore(OreLook.of(OreLook.Pattern.IRON).inColour(0xA8804E)).rocks(HostRocks.STONE, HostRocks.CALCITE)
            .declare();
    /** Fool's gold, iron's sulfide, found everywhere: mostly sulfur, with traces of gold and arsenic. */
    public static final Material PYRITE = mineral("pyrite", PYRITE_NAME, MineralFamilies.SULFIDE, Element.IRON)
            .formula("FeS2").byproduct(Element.SULFUR).trace(Element.GOLD, 0.000_001).trace(Element.ARSENIC, 0.001)
            .colour(0xC8B560).ore(OreLook.of(OreLook.Pattern.GOLD).inColour(0xCDBA62))
            .rocks(HostRocks.STONE, HostRocks.DEEPSLATE, HostRocks.GRANITE, HostRocks.DIORITE, HostRocks.ANDESITE,
                    HostRocks.TUFF, HostRocks.CALCITE, HostRocks.SANDSTONE).declare();
    /** Copper's main ore, a sulfide of copper and iron, with gold, silver, selenium and tellurium in it. */
    public static final Material CHALCOPYRITE = mineral("chalcopyrite", CHALCOPYRITE_NAME, MineralFamilies.SULFIDE,
            Element.COPPER).formula("CuFeS2").byproduct(Element.SULFUR).trace(Element.GOLD, 0.000_01)
            .trace(Element.SILVER, 0.000_1).trace(Element.SELENIUM, 0.000_05).trace(Element.TELLURIUM, 0.000_01)
            .colour(0xB89A3E).ore(OreLook.of(OreLook.Pattern.COPPER).inColour(0xC2A23C))
            .rocks(HostRocks.DEEPSLATE, HostRocks.GRANITE).declare();
    /** Copper's red oxide, near the surface. */
    public static final Material CUPRITE = mineral("cuprite", CUPRITE_NAME, MineralFamilies.OXIDE, Element.COPPER)
            .formula("Cu2O").colour(0x7A1F1F).ore(OreLook.of(OreLook.Pattern.REDSTONE).inColour(0x8A2424))
            .rocks(HostRocks.STONE, HostRocks.SANDSTONE).declare();
    /** Copper's green carbonate, near the surface. */
    public static final Material MALACHITE = mineral("malachite", MALACHITE_NAME, MineralFamilies.CARBONATE,
            Element.COPPER).formula("Cu2CO3(OH)2").colour(0x2E8B57)
            .ore(OreLook.of(OreLook.Pattern.EMERALD).inColour(0x2F9A5E))
            .rocks(HostRocks.STONE, HostRocks.CALCITE, HostRocks.SANDSTONE).declare();
    /** Gold's natural alloy with silver, which needs no breaking down. */
    public static final Material ELECTRUM = mineral("electrum", ELECTRUM_NAME, MineralFamilies.NATIVE, Element.GOLD)
            .part(Element.GOLD, 0.75).part(Element.SILVER, 0.25).byproduct(Element.SILVER).colour(0xD9C27A)
            .ore(OreLook.of(OreLook.Pattern.GOLD).inColour(0xDCC77E)).rocks(HostRocks.STONE).declare();
    /** Tin's oxide, the only tin ore that matters. */
    public static final Material CASSITERITE = mineral("cassiterite", CASSITERITE_NAME, MineralFamilies.OXIDE,
            Element.TIN).formula("SnO2").colour(0x4A3426).ore(OreLook.of(OreLook.Pattern.COAL).inColour(0x3E2C20))
            .rocks(HostRocks.GRANITE).declare();

    private SeriesMinerals() {
    }

    /** Declares the minerals, from the Core's constructor. */
    public static void declare() {
        // Loading the class declares them.
    }

    /** Every mineral of the catalogue, in the order declared. */
    public static List<Material> all() {
        return List.of(HEMATITE, MAGNETITE, SIDERITE, PYRITE, CHALCOPYRITE, CUPRITE, MALACHITE, ELECTRUM,
                CASSITERITE);
    }

    private static MineralBuilder mineral(final String path, final TextKey name, final MineralFamily family,
                                          final Element main) {
        return MineralBuilder.of(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, path), name, family, main);
    }
}
