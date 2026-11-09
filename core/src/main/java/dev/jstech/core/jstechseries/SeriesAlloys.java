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
import dev.jstech.core.heat.Thermal;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MaterialKind;
import dev.jstech.core.material.MaterialProperties;
import dev.jstech.core.material.Materials;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

/**
 * The alloys the series' electronics are made from, and copper refined for them: the steels of the frames and cases,
 * the solders, permalloy for the magnetic heads and indium tin oxide, the clear conductor of a touch screen, taken as a
 * metal since it comes as ingot and foil. An alloy's proportions vary from one maker to another, so it has no one
 * formula but its name, and its heat is the common grade's. Bronze, the oldest, is with the series' metals
 * ({@link SeriesMaterials#BRONZE}).
 */
@TextHolder
public final class SeriesAlloys {

    public static final TextKey STEEL_NAME = TextKey.of("jscore.material.steel", "Steel");
    public static final TextKey STAINLESS_STEEL_NAME = TextKey.of("jscore.material.stainless_steel",
            "Stainless Steel");
    public static final TextKey SOLDER_NAME = TextKey.of("jscore.material.solder", "Solder");
    public static final TextKey LEAD_FREE_SOLDER_NAME = TextKey.of("jscore.material.lead_free_solder",
            "Lead-Free Solder");
    public static final TextKey PERMALLOY_NAME = TextKey.of("jscore.material.permalloy", "Permalloy");
    public static final TextKey INDIUM_TIN_OXIDE_NAME = TextKey.of("jscore.material.indium_tin_oxide",
            "Indium Tin Oxide");
    public static final TextKey REFINED_COPPER_NAME = TextKey.of("jscore.material.refined_copper", "Refined Copper");

    /** Iron with a little carbon; a mild steel's heat. */
    public static final Material STEEL = Materials.material(id("steel"), MaterialKind.METAL, STEEL_NAME)
            .colour(0x8A8F96).with(Thermal.DENSITY, 7_850.0).with(Thermal.SPECIFIC_HEAT, 490.0)
            .with(Thermal.CONDUCTIVITY, 50.0).with(Thermal.MELTING, 1_773.0).declare();
    /** Steel with chromium and nickel, which does not rust; the heat of the common grade, 304. */
    public static final Material STAINLESS_STEEL = Materials.material(id("stainless_steel"), MaterialKind.METAL,
            STAINLESS_STEEL_NAME).colour(0xB8BEC6).with(Thermal.DENSITY, 8_000.0)
            .with(Thermal.SPECIFIC_HEAT, 500.0).with(Thermal.CONDUCTIVITY, 16.0).with(Thermal.MELTING, 1_723.0)
            .declare();
    /** Tin and lead, 60 and 40, which starts to melt at 183 °C; its lead harms health and the water. */
    public static final Material SOLDER = Materials.material(id("solder"), MaterialKind.METAL, SOLDER_NAME)
            .colour(0xA8ADB4).with(Thermal.DENSITY, 8_500.0).with(Thermal.SPECIFIC_HEAT, 176.0)
            .with(Thermal.CONDUCTIVITY, 50.0).with(Thermal.MELTING, 456.0)
            .with(MaterialHazards.HAZARDS, List.of(HazardClass.HEALTH_HAZARD, HazardClass.ENVIRONMENTAL)).declare();
    /** Tin with 3% silver and a little copper, which took the leaded solder's place in 2006. */
    public static final Material LEAD_FREE_SOLDER = Materials.material(id("lead_free_solder"), MaterialKind.METAL,
            LEAD_FREE_SOLDER_NAME).colour(0xC8CCD2).with(Thermal.DENSITY, 7_400.0)
            .with(Thermal.SPECIFIC_HEAT, 230.0).with(Thermal.CONDUCTIVITY, 58.0).with(Thermal.MELTING, 490.0)
            .declare();
    /** Nickel and iron, 80 and 20, which a magnetic field turns at once. */
    public static final Material PERMALLOY = Materials.material(id("permalloy"), MaterialKind.METAL, PERMALLOY_NAME)
            .colour(0xA8B09A).with(Thermal.DENSITY, 8_700.0).with(Thermal.MELTING, 1_728.0).declare();
    /* Indium oxide with a tenth of tin oxide: written as its two oxides, as the trade writes it. */
    public static final Material INDIUM_TIN_OXIDE = Materials.material(id("indium_tin_oxide"), MaterialKind.METAL,
            INDIUM_TIN_OXIDE_NAME).colour(0xBFE0D4).with(MaterialProperties.FORMULA, "In₂O₃·SnO₂")
            .with(Thermal.DENSITY, 7_140.0).declare();
    /** Copper of 99.99%, refined in an electrolytic cell: what fine wires and a chip's wiring are drawn from. */
    public static final Material REFINED_COPPER = Materials.material(id("refined_copper"), MaterialKind.METAL,
            REFINED_COPPER_NAME).colour(0xE0784A).with(MaterialProperties.FORMULA, "Cu")
            .with(Thermal.DENSITY, 8_960.0).with(Thermal.SPECIFIC_HEAT, 385.0).with(Thermal.CONDUCTIVITY, 401.0)
            .with(Thermal.MELTING, 1_358.0).declare();

    private SeriesAlloys() {
    }

    /** Declares the alloys, from the Core's constructor. */
    public static void declare() {
        // Loading the class declares them.
    }

    /** The alloys, in the order they are declared. */
    public static List<Material> all() {
        return List.of(STEEL, STAINLESS_STEEL, SOLDER, LEAD_FREE_SOLDER, PERMALLOY, INDIUM_TIN_OXIDE,
                REFINED_COPPER);
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsCore.MODID, path);
    }
}
