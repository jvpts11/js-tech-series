/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries;

import dev.jstech.core.JsCore;
import dev.jstech.core.hazard.HazardClass;
import dev.jstech.core.hazard.MaterialHazards;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MaterialKind;
import dev.jstech.core.material.MaterialProperties;
import dev.jstech.core.material.Materials;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

/**
 * The solid compounds the series' electronics are made from, each with its real formula and the hazard classes of
 * the GHS its safety data sheet gives: the salts and oxides of the chemistry, the battery's lithium, the carbon and
 * silicon of the furnaces, the crystals that are grown, the crystals of light, and the plastics and resins of the
 * boards and cases. Each exists in a form only when an installed mod uses it; the recipes are each mod's own.
 */
@TextHolder
public final class SeriesCompounds {

    public static final TextKey ALUMINA_NAME = TextKey.of("jscore.material.alumina", "Alumina");
    public static final TextKey CAUSTIC_SODA_NAME = TextKey.of("jscore.material.caustic_soda", "Caustic Soda");
    public static final TextKey SALT_NAME = TextKey.of("jscore.material.salt", "Salt");
    public static final TextKey ZINC_SULFIDE_NAME = TextKey.of("jscore.material.zinc_sulfide", "Zinc Sulfide");
    public static final TextKey LITHIUM_HYDROXIDE_NAME = TextKey.of("jscore.material.lithium_hydroxide",
            "Lithium Hydroxide");
    public static final TextKey LITHIUM_COBALT_OXIDE_NAME = TextKey.of("jscore.material.lithium_cobalt_oxide",
            "Lithium Cobalt Oxide");
    public static final TextKey CALCIUM_CARBIDE_NAME = TextKey.of("jscore.material.calcium_carbide",
            "Calcium Carbide");
    public static final TextKey COKE_NAME = TextKey.of("jscore.material.coke", "Coke");
    public static final TextKey METALLURGICAL_SILICON_NAME = TextKey.of("jscore.material.metallurgical_silicon",
            "Metallurgical Silicon");
    public static final TextKey POLYSILICON_NAME = TextKey.of("jscore.material.polysilicon", "Polysilicon");
    public static final TextKey CULTURED_QUARTZ_NAME = TextKey.of("jscore.material.cultured_quartz",
            "Cultured Quartz");
    public static final TextKey GALLIUM_ARSENIDE_NAME = TextKey.of("jscore.material.gallium_arsenide",
            "Gallium Arsenide");
    public static final TextKey ALGAINP_NAME = TextKey.of("jscore.material.algainp",
            "Aluminium Gallium Indium Phosphide");
    public static final TextKey GALLIUM_NITRIDE_NAME = TextKey.of("jscore.material.gallium_nitride",
            "Gallium Nitride");
    public static final TextKey CALCIUM_FLUORIDE_NAME = TextKey.of("jscore.material.calcium_fluoride",
            "Calcium Fluoride");
    public static final TextKey WOOD_PULP_NAME = TextKey.of("jscore.material.wood_pulp", "Wood Pulp");
    public static final TextKey GLASS_NAME = TextKey.of("jscore.material.glass", "Glass");
    public static final TextKey PHENOLIC_RESIN_NAME = TextKey.of("jscore.material.phenolic_resin", "Phenolic Resin");
    public static final TextKey EPOXY_RESIN_NAME = TextKey.of("jscore.material.epoxy_resin", "Epoxy Resin");
    public static final TextKey POLYCARBONATE_NAME = TextKey.of("jscore.material.polycarbonate", "Polycarbonate");
    public static final TextKey PVC_NAME = TextKey.of("jscore.material.pvc", "PVC");

    /** Aluminium's oxide, what the Bayer process makes of bauxite and the electrolytic cell of aluminium. */
    public static final Material ALUMINA = compound("alumina", MaterialKind.NONMETAL_SOLID, ALUMINA_NAME, 0xECE8DC,
            "Al2O3");
    /** Sodium hydroxide, which eats skin and dissolves alumina out of its ore. */
    public static final Material CAUSTIC_SODA = compound("caustic_soda", MaterialKind.NONMETAL_SOLID,
            CAUSTIC_SODA_NAME, 0xF4F4F2, "NaOH", HazardClass.CORROSIVE);
    public static final Material SALT = compound("salt", MaterialKind.NONMETAL_SOLID, SALT_NAME, 0xF2F0EA, "NaCl");
    /** The phosphor that glows when struck: a cathode-ray tube's screen. */
    public static final Material ZINC_SULFIDE = compound("zinc_sulfide", MaterialKind.NONMETAL_SOLID,
            ZINC_SULFIDE_NAME, 0xD8ECB8, "ZnS");
    /** Corrosive, and harmful swallowed. */
    public static final Material LITHIUM_HYDROXIDE = compound("lithium_hydroxide", MaterialKind.NONMETAL_SOLID,
            LITHIUM_HYDROXIDE_NAME, 0xF2F0EA, "LiOH", HazardClass.CORROSIVE, HazardClass.HARMFUL);
    /** A lithium-ion cell's cathode: it sensitises the skin, and its cobalt causes cancer. */
    public static final Material LITHIUM_COBALT_OXIDE = compound("lithium_cobalt_oxide",
            MaterialKind.NONMETAL_SOLID, LITHIUM_COBALT_OXIDE_NAME, 0x2E3038, "LiCoO2", HazardClass.HARMFUL,
            HazardClass.HEALTH_HAZARD);
    /** Water sets acetylene free from it, which burns; it burns the eyes besides. */
    public static final Material CALCIUM_CARBIDE = compound("calcium_carbide", MaterialKind.NONMETAL_SOLID,
            CALCIUM_CARBIDE_NAME, 0x6A6A72, "CaC2", HazardClass.FLAMMABLE, HazardClass.CORROSIVE,
            HazardClass.HARMFUL);
    /** Coal baked without air: nearly pure carbon, the reducing agent of the furnaces. */
    public static final Material COKE = compound("coke", MaterialKind.NONMETAL_SOLID, COKE_NAME, 0x3C3C42, "C");
    /** Silicon of about 98%, from sand and coke in an arc furnace: too impure for a chip. */
    public static final Material METALLURGICAL_SILICON = compound("metallurgical_silicon", MaterialKind.CRYSTAL,
            METALLURGICAL_SILICON_NAME, 0x7A808A, "Si");
    /** Silicon of nine nines, grown as rods in a Siemens reactor: what a chip's crystal is pulled from. */
    public static final Material POLYSILICON = compound("polysilicon", MaterialKind.CRYSTAL, POLYSILICON_NAME,
            0x8A929C, "Si");
    /** Quartz grown in an autoclave, flawless, for the oscillators. */
    public static final Material CULTURED_QUARTZ = compound("cultured_quartz", MaterialKind.CRYSTAL,
            CULTURED_QUARTZ_NAME, 0xE4EEF2, "SiO2");
    /** The crystal of the infrared lasers; its arsenic causes cancer and harms the organs. */
    public static final Material GALLIUM_ARSENIDE = compound("gallium_arsenide", MaterialKind.CRYSTAL,
            GALLIUM_ARSENIDE_NAME, 0x4A4E5A, "GaAs", HazardClass.HEALTH_HAZARD);
    /*
     * The crystal of the red lasers and LEDs. Its proportions of aluminium and gallium are set by the colour it is
     * grown for: it has no one formula.
     */
    public static final Material ALGAINP = Materials.material(id("algainp"), MaterialKind.CRYSTAL, ALGAINP_NAME)
            .colour(0x8A5A4A).declare();
    /** The crystal of the blue and violet lasers and the white LEDs. */
    public static final Material GALLIUM_NITRIDE = compound("gallium_nitride", MaterialKind.CRYSTAL,
            GALLIUM_NITRIDE_NAME, 0xC8BC6A, "GaN");
    /** Fluorite grown clear, for the lenses that ultraviolet light passes through. */
    public static final Material CALCIUM_FLUORIDE = compound("calcium_fluoride", MaterialKind.CRYSTAL,
            CALCIUM_FLUORIDE_NAME, 0xE4DCF4, "CaF2");
    /* Wood's fibres, its cellulose with what is left of the rest: a mixture, with no one formula. */
    public static final Material WOOD_PULP = Materials.material(id("wood_pulp"), MaterialKind.NONMETAL_SOLID,
            WOOD_PULP_NAME).colour(0xE8DCC0).declare();
    /*
     * The game's glass ground and polished into the lenses of microscopes and the first lithography: soda-lime glass,
     * a mixture with no one formula. It is not a crystal, but it takes the forms a crystal is cut into.
     */
    public static final Material GLASS = Materials.material(id("glass"), MaterialKind.CRYSTAL, GLASS_NAME)
            .colour(0xD8ECF2).declare();
    /** Bakelite, the first plastic: phenol and formaldehyde, the brown board of the first circuits. */
    public static final Material PHENOLIC_RESIN = compound("phenolic_resin", MaterialKind.POLYMER,
            PHENOLIC_RESIN_NAME, 0x7A4A22, "(C₆H₆O·CH₂O)ₙ");
    /*
     * The glue and the board of glass fibre: a family of polymers of bisphenol, with no one formula. Uncured, it
     * sensitises the skin.
     */
    public static final Material EPOXY_RESIN = Materials.material(id("epoxy_resin"), MaterialKind.POLYMER,
            EPOXY_RESIN_NAME).colour(0xD8C48A).with(MaterialHazards.HAZARDS, List.of(HazardClass.HARMFUL))
            .declare();
    /** The clear plastic of the optical discs. */
    public static final Material POLYCARBONATE = compound("polycarbonate", MaterialKind.POLYMER, POLYCARBONATE_NAME,
            0xD4E4EE, "(C₁₆H₁₄O₃)ₙ");
    /** Polyvinyl chloride: the insulation of the cables and the case of a lead-acid battery. */
    public static final Material PVC = compound("pvc", MaterialKind.POLYMER, PVC_NAME, 0xE4E4E2, "(C₂H₃Cl)ₙ");

    private SeriesCompounds() {
    }

    /** Declares the compounds, from the Core's constructor. */
    public static void declare() {
        // Loading the class declares them.
    }

    /** The compounds, in the order they are declared. */
    public static List<Material> all() {
        return List.of(ALUMINA, CAUSTIC_SODA, SALT, ZINC_SULFIDE, LITHIUM_HYDROXIDE, LITHIUM_COBALT_OXIDE,
                CALCIUM_CARBIDE, COKE, METALLURGICAL_SILICON, POLYSILICON, CULTURED_QUARTZ, GALLIUM_ARSENIDE, ALGAINP,
                GALLIUM_NITRIDE, CALCIUM_FLUORIDE, WOOD_PULP, PHENOLIC_RESIN, EPOXY_RESIN, POLYCARBONATE, PVC);
    }

    /*
     * A compound of {@code kind} called {@code path}, with its formula, written as chemists write it (a polymer's
     * repeating unit in brackets, its counts already small), and its hazard classes, none for a harmless one.
     */
    private static Material compound(final String path, final MaterialKind kind, final TextKey name, final int colour,
                                     final String formula, final HazardClass... hazards) {
        final Material.Builder builder = Materials.material(id(path), kind, name).colour(colour)
                .with(MaterialProperties.FORMULA, formula);
        if (hazards.length > 0) {
            builder.with(MaterialHazards.HAZARDS, List.of(hazards));
        }
        return builder.declare();
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsCore.MODID, path);
    }
}
