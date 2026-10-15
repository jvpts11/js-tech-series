/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries;

import dev.jstech.core.JsCore;
import dev.jstech.core.heat.Thermal;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MaterialKind;
import dev.jstech.core.material.Materials;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import net.minecraft.resources.ResourceLocation;

/**
 * The series' catalogue of what the world is made of, which every mod of the series and any other shares: the 118
 * known elements ({@link Elements}), their isotopes ({@link Isotopes}), the elements beyond the known table
 * ({@link Superheavies}), the exotic forms of matter ({@link ExoticMatter}), antimatter ({@link Antimatter}) and the
 * alloys. The metals the mods name
 * most are here by name.
 */
@TextHolder
public final class SeriesMaterials {

    public static final TextKey BRONZE_NAME = TextKey.of("jscore.material.bronze", "Bronze");

    public static final Material IRON = Elements.of(Element.IRON);
    public static final Material COPPER = Elements.of(Element.COPPER);
    public static final Material GOLD = Elements.of(Element.GOLD);
    public static final Material TIN = Elements.of(Element.TIN);
    /*
     * An alloy of copper and tin, whose proportions vary from one bronze to another: it has no one formula. Its heat
     * is a tin bronze's, about a tenth of tin.
     */
    public static final Material BRONZE = Materials.material(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "bronze"), MaterialKind.METAL, BRONZE_NAME)
            .colour(0xC08436).also(MatterState.LIQUID).with(Thermal.DENSITY, 8_800.0)
            .with(Thermal.SPECIFIC_HEAT, 380.0).with(Thermal.CONDUCTIVITY, 50.0).with(Thermal.EMISSIVITY, 0.2)
            .with(Thermal.MELTING, 1_223.0).with(Thermal.BOILING, 2_573.0).declare();

    private SeriesMaterials() {
    }

    /** Declares the whole catalogue, from the Core's constructor. */
    public static void declare() {
        Elements.declare();
        Isotopes.declare();
        Superheavies.declare();
        ExoticMatter.declare();
        Antimatter.declare();
        SharedCatalogue.declare();
        NaturalRadioactivity.declare();
        ChemicalLinks.declare();
    }
}
