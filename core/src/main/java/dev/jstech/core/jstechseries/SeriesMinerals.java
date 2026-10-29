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
 * and has an ore only once a mod uses it. Which minerals exist is the mods' to say: J's Industrial's first veins
 * ({@link #all()}), the rest of its list joining as the mod grows, and the minerals J's Space finds on the Moon and
 * Mars ({@link #ofTheMoonAndMars()}), each declared once here so every mod shares it.
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
    public static final TextKey ILMENITE_NAME = TextKey.of("jscore.material.ilmenite", "Ilmenite");
    public static final TextKey ARMALCOLITE_NAME = TextKey.of("jscore.material.armalcolite", "Armalcolite");
    public static final TextKey TRANQUILLITYITE_NAME = TextKey.of("jscore.material.tranquillityite",
            "Tranquillityite");
    public static final TextKey MERRILLITE_NAME = TextKey.of("jscore.material.merrillite", "Merrillite");
    public static final TextKey NATIVE_IRON_NAME = TextKey.of("jscore.material.native_iron", "Native Iron");
    public static final TextKey OLIVINE_NAME = TextKey.of("jscore.material.olivine", "Olivine");
    public static final TextKey JAROSITE_NAME = TextKey.of("jscore.material.jarosite", "Jarosite");
    public static final TextKey GYPSUM_NAME = TextKey.of("jscore.material.gypsum", "Gypsum");

    /**
     * Iron's oxide, the ore of the world's steel: red as a dust, its crystals a steel grey with a metal's glint, round
     * as the spherules Opportunity found on Mars.
     */
    public static final Material HEMATITE = mineral("hematite", HEMATITE_NAME, MineralFamilies.OXIDE, Element.IRON)
            .formula("Fe2O3").colour(0x8B3A2E)
            .ore(OreLook.of(OreLook.Pattern.REDSTONE).inColour(0x4A3A3C).withSheen(0x8A7A80))
            .rocks(HostRocks.STONE, HostRocks.SANDSTONE).declare();
    /** Iron's black, magnetic oxide, which carries titanium and vanadium, with a metal's glint. */
    public static final Material MAGNETITE = mineral("magnetite", MAGNETITE_NAME, MineralFamilies.OXIDE,
            Element.IRON).formula("Fe3O4").trace(Element.TITANIUM, 0.03).trace(Element.VANADIUM, 0.005)
            .colour(0x2B2B2E).ore(OreLook.of(OreLook.Pattern.IRON).inColour(0x16161A).withSheen(0x6A6E78))
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
    /** Iron and titanium's black oxide, glinting like a metal: the Moon's titanium, and its oxygen by reduction. */
    public static final Material ILMENITE = mineral("ilmenite", ILMENITE_NAME, MineralFamilies.OXIDE,
            Element.TITANIUM).formula("FeTiO3").byproduct(Element.IRON).colour(0x1E1C1F)
            .ore(OreLook.of(OreLook.Pattern.IRON).inColour(0x1E1E22).withSheen(0x8A8E98))
            .rocks(HostRocks.DIORITE, HostRocks.ANDESITE).declare();
    /**
     * A titanium oxide of magnesium and iron, found by Apollo 11 and named after Armstrong, Aldrin and Collins; its
     * formula the lunar one, magnesium and iron half and half.
     */
    public static final Material ARMALCOLITE = mineral("armalcolite", ARMALCOLITE_NAME, MineralFamilies.OXIDE,
            Element.TITANIUM).formula("MgFeTi4O10").byproduct(Element.MAGNESIUM).colour(0x3A3338)
            .ore(OreLook.of(OreLook.Pattern.DIAMOND).inColour(0x3E3630).withSheen(0x9A8E82)).rocks(HostRocks.BASALT)
            .declare();
    /**
     * A silicate of iron, zirconium and yttrium named after the Sea of Tranquillity, found on the Earth in 2011; its
     * formula the zirconium one, with the yttrium that stands in for some of it.
     */
    public static final Material TRANQUILLITYITE = mineral("tranquillityite", TRANQUILLITYITE_NAME,
            MineralFamilies.SILICATE, Element.ZIRCONIUM).formula("Fe8Zr2Ti3Si3O24").byproduct(Element.TITANIUM)
            .trace(Element.YTTRIUM, 0.02).colour(0x7A3E2A).ore(OreLook.of(OreLook.Pattern.EMERALD)
                    .inColour(0x7A3A1E).withSheen(0xC26A3A)).rocks(HostRocks.BASALT).declare();
    /**
     * The phosphate that carries the rare earths of the Moon's KREEP, with thorium and uranium in it; its formula the
     * merrillite of the rare earths, cerium standing for them.
     */
    public static final Material MERRILLITE = mineral("merrillite", MERRILLITE_NAME, MineralFamilies.PHOSPHATE,
            Element.CERIUM).formula("Ca8CeMg(PO4)7").byproduct(Element.PHOSPHORUS).trace(Element.LANTHANUM, 0.01)
            .trace(Element.NEODYMIUM, 0.01).trace(Element.THORIUM, 0.000_5).trace(Element.URANIUM, 0.000_1)
            .colour(0xD8D2C0).ore(OreLook.of(OreLook.Pattern.COAL).inColour(0xB89A6A).withSheen(0xE8D6A8))
            .rocks(HostRocks.BASALT).declare();
    /** Iron as metal, unrusted where there is no air, with nickel in it, as on the Moon and in meteorites. */
    public static final Material NATIVE_IRON = mineral("native_iron", NATIVE_IRON_NAME, MineralFamilies.NATIVE,
            Element.IRON).part(Element.IRON, 0.94).part(Element.NICKEL, 0.06).byproduct(Element.NICKEL)
            .colour(0x9A9A9E).ore(OreLook.of(OreLook.Pattern.GOLD).inColour(0x8A8C90).withSheen(0xD8DADE))
            .rocks(HostRocks.BASALT).declare();
    /**
     * The green silicate of magnesium and iron of the mantle, in bulk at Mars' Nili Fossae; its formula forsterite's,
     * the magnesium end of the series, with the iron that stands in for some of it.
     */
    public static final Material OLIVINE = mineral("olivine", OLIVINE_NAME, MineralFamilies.SILICATE,
            Element.MAGNESIUM).formula("Mg2SiO4").byproduct(Element.SILICON).trace(Element.IRON, 0.08).colour(0x6B8E23)
            .ore(OreLook.of(OreLook.Pattern.EMERALD).inColour(0x7A8A2A).withSheen(0xB8C85A)).rocks(HostRocks.BASALT)
            .declare();
    /** The yellow sulfate of iron and potassium that forms only in acid water, Mars' proof of an old wet past. */
    public static final Material JAROSITE = mineral("jarosite", JAROSITE_NAME, MineralFamilies.SULFATE,
            Element.IRON).formula("KFe3(SO4)2(OH)6").byproduct(Element.SULFUR).byproduct(Element.POTASSIUM)
            .colour(0xC9A227).ore(OreLook.of(OreLook.Pattern.GOLD).inColour(0xB8922A).withSheen(0xE8C85A))
            .rocks(HostRocks.SANDSTONE).declare();
    /**
     * Calcium's hydrated sulfate, left where water evaporated: white veins filling the cracks of its rock, on Mars as
     * on the Earth, drawn as the veins of the Nether's quartz.
     */
    public static final Material GYPSUM = mineral("gypsum", GYPSUM_NAME, MineralFamilies.SULFATE, Element.CALCIUM)
            .formula("CaSO4·2H2O").byproduct(Element.SULFUR).colour(0xEDE6DA)
            .ore(OreLook.of(OreLook.Pattern.QUARTZ).inColour(0xE6E0D4).withSheen(0xFFFFFF))
            .rocks(HostRocks.SANDSTONE, HostRocks.CALCITE).declare();

    private SeriesMinerals() {
    }

    /** Declares the minerals, from the Core's constructor. */
    public static void declare() {
        // Loading the class declares them.
    }

    /** Every mineral of a first industry's veins, in the order declared: the list J's Industrial mines. */
    public static List<Material> all() {
        return List.of(HEMATITE, MAGNETITE, SIDERITE, PYRITE, CHALCOPYRITE, CUPRITE, MALACHITE, ELECTRUM,
                CASSITERITE);
    }

    /**
     * The minerals of the Moon and Mars, which J's Space mines there, each in the rocks of those worlds, and which
     * any mod may mine on the Earth too: the Moon's ilmenite, armalcolite, tranquillityite, merrillite and native
     * iron, and Mars' olivine, jarosite and gypsum, beside its hematite and magnetite.
     */
    public static List<Material> ofTheMoonAndMars() {
        return List.of(ILMENITE, ARMALCOLITE, TRANQUILLITYITE, MERRILLITE, NATIVE_IRON, OLIVINE, JAROSITE, GYPSUM);
    }

    private static MineralBuilder mineral(final String path, final TextKey name, final MineralFamily family,
                                          final Element main) {
        return MineralBuilder.of(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, path), name, family, main);
    }
}
