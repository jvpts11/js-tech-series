/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries;

import dev.jstech.core.JsCore;
import dev.jstech.core.hazard.GasLimits;
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
import org.jetbrains.annotations.Nullable;

/**
 * The liquids and gases the series' electronics are made with, each with its real formula, or none for a mixture, and
 * the hazard classes of the GHS its safety data sheet gives: the coke oven's tar, the chemistry's solvents and acids,
 * the etchant of the boards, the photoresist of the masks, the water a chip is rinsed in, a screen's liquid crystal and
 * a battery's electrolyte; and the two gases the silicon and the lamps need, hydrogen chloride and acetylene, each in
 * its cylinder's colour by the European code (EN 1089-3). Each exists only when an installed mod uses it.
 */
@TextHolder
public final class SeriesChemicals {

    public static final TextKey COAL_TAR_NAME = TextKey.of("jscore.material.coal_tar", "Coal Tar");
    public static final TextKey PHENOL_NAME = TextKey.of("jscore.material.phenol", "Phenol");
    public static final TextKey METHANOL_NAME = TextKey.of("jscore.material.methanol", "Methanol");
    public static final TextKey HYDROFLUORIC_ACID_NAME = TextKey.of("jscore.material.hydrofluoric_acid",
            "Hydrofluoric Acid");
    public static final TextKey SULFURIC_ACID_NAME = TextKey.of("jscore.material.sulfuric_acid", "Sulfuric Acid");
    public static final TextKey FERRIC_CHLORIDE_NAME = TextKey.of("jscore.material.ferric_chloride",
            "Ferric Chloride");
    public static final TextKey PHOTORESIST_NAME = TextKey.of("jscore.material.photoresist", "Photoresist");
    public static final TextKey ULTRAPURE_WATER_NAME = TextKey.of("jscore.material.ultrapure_water",
            "Ultrapure Water");
    public static final TextKey LIQUID_CRYSTAL_NAME = TextKey.of("jscore.material.liquid_crystal", "Liquid Crystal");
    public static final TextKey BATTERY_ELECTROLYTE_NAME = TextKey.of("jscore.material.battery_electrolyte",
            "Battery Electrolyte");
    public static final TextKey HYDROGEN_CHLORIDE_NAME = TextKey.of("jscore.material.hydrogen_chloride",
            "Hydrogen Chloride");
    public static final TextKey ACETYLENE_NAME = TextKey.of("jscore.material.acetylene", "Acetylene");

    /** What coal leaves as it bakes into coke: a mixture of aromatics that causes cancer and poisons the water. */
    public static final Material COAL_TAR = liquid("coal_tar", COAL_TAR_NAME, 0x1C1814, null,
            HazardClass.HEALTH_HAZARD, HazardClass.ENVIRONMENTAL);
    /** Taken liquid, as the trade ships it: toxic through the skin, corrosive, and harmful to the organs. */
    public static final Material PHENOL = liquid("phenol", PHENOL_NAME, 0xE8D8CC, "C6H5OH",
            HazardClass.ACUTELY_TOXIC, HazardClass.CORROSIVE, HazardClass.HEALTH_HAZARD);
    /** Wood alcohol: it burns, and a mouthful blinds. */
    public static final Material METHANOL = liquid("methanol", METHANOL_NAME, 0xD4E4EE, "CH3OH",
            HazardClass.FLAMMABLE, HazardClass.ACUTELY_TOXIC, HazardClass.HEALTH_HAZARD);
    /** Hydrogen fluoride in water, the acid that eats glass: it etches silicon's oxide and poisons through the skin. */
    public static final Material HYDROFLUORIC_ACID = liquid("hydrofluoric_acid", HYDROFLUORIC_ACID_NAME, 0xD8ECE4,
            "HF", HazardClass.ACUTELY_TOXIC, HazardClass.CORROSIVE);
    public static final Material SULFURIC_ACID = liquid("sulfuric_acid", SULFURIC_ACID_NAME, 0xE8D878, "H2SO4",
            HazardClass.CORROSIVE);
    /** Iron's chloride in water: the etchant that eats a board's copper away. */
    public static final Material FERRIC_CHLORIDE = liquid("ferric_chloride", FERRIC_CHLORIDE_NAME, 0x7A4A14,
            "FeCl3", HazardClass.CORROSIVE, HazardClass.HARMFUL);
    /** A resin in its solvent that light hardens, so a mask's pattern can be etched: a mixture, which burns. */
    public static final Material PHOTORESIST = liquid("photoresist", PHOTORESIST_NAME, 0xD0702C, null,
            HazardClass.FLAMMABLE);
    /** Water with nothing else in it, which a chip is rinsed in between its steps. */
    public static final Material ULTRAPURE_WATER = liquid("ultrapure_water", ULTRAPURE_WATER_NAME, 0xB4D4FF, "H2O");
    /** The mixture of rod-like molecules a screen's cells are filled with, which an electric field turns. */
    public static final Material LIQUID_CRYSTAL = liquid("liquid_crystal", LIQUID_CRYSTAL_NAME, 0xC4A4E4, null);
    /*
     * A lithium salt in its carbonate solvents, a mixture: it burns, corrodes as it gives off hydrogen fluoride in
     * moist air, is harmful swallowed and harms the organs.
     */
    public static final Material BATTERY_ELECTROLYTE = liquid("battery_electrolyte", BATTERY_ELECTROLYTE_NAME,
            0xC8B4E4, null, HazardClass.FLAMMABLE, HazardClass.CORROSIVE, HazardClass.HARMFUL,
            HazardClass.HEALTH_HAZARD);
    /**
     * The gas that turns silicon to trichlorosilane in the Siemens process: corrosive and toxic, dangerous to life at
     * 50 parts in a million.
     */
    public static final Material HYDROGEN_CHLORIDE = Materials.material(id("hydrogen_chloride"), MaterialKind.GAS,
            HYDROGEN_CHLORIDE_NAME).colour(0xC8902A).with(MaterialProperties.FORMULA, "HCl")
            .with(MaterialHazards.HAZARDS, List.of(HazardClass.CORROSIVE, HazardClass.ACUTELY_TOXIC))
            .with(GasLimits.IDLH, 50.0).declare();
    /**
     * The gas a carbide lamp burns, made from calcium carbide and water: it burns in any mixture with air from 2.5% of
     * it up, and even pure, since it can break down on its own.
     */
    public static final Material ACETYLENE = Materials.material(id("acetylene"), MaterialKind.GAS, ACETYLENE_NAME)
            .colour(0x7A2430).with(MaterialProperties.FORMULA, "C2H2")
            .with(MaterialHazards.HAZARDS, List.of(HazardClass.FLAMMABLE, HazardClass.ASPHYXIANT))
            .with(GasLimits.LOWER_EXPLOSIVE, 0.025).with(GasLimits.UPPER_EXPLOSIVE, 1.0).declare();

    private SeriesChemicals() {
    }

    /** Declares the liquids and gases, from the Core's constructor. */
    public static void declare() {
        // Loading the class declares them.
    }

    /** The liquids and gases, in the order they are declared. */
    public static List<Material> all() {
        return List.of(COAL_TAR, PHENOL, METHANOL, HYDROFLUORIC_ACID, SULFURIC_ACID, FERRIC_CHLORIDE, PHOTORESIST,
                ULTRAPURE_WATER, LIQUID_CRYSTAL, BATTERY_ELECTROLYTE, HYDROGEN_CHLORIDE, ACETYLENE);
    }

    /* A liquid called {@code path}, with its formula, none for a mixture, and its hazard classes, if it has any. */
    private static Material liquid(final String path, final TextKey name, final int colour,
                                   @Nullable final String formula, final HazardClass... hazards) {
        final Material.Builder builder = Materials.material(id(path), MaterialKind.LIQUID, name).colour(colour);
        if (formula != null) {
            builder.with(MaterialProperties.FORMULA, formula);
        }
        if (hazards.length > 0) {
            builder.with(MaterialHazards.HAZARDS, List.of(hazards));
        }
        return builder.declare();
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsCore.MODID, path);
    }
}
