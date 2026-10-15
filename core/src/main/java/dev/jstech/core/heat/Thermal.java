/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.heat;

import com.mojang.serialization.Codec;
import dev.jstech.core.JsCore;
import dev.jstech.core.format.UnitFormatter;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MaterialKind;
import dev.jstech.core.material.MaterialProperties;
import dev.jstech.core.material.MaterialProperty;
import dev.jstech.core.material.MaterialValues;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import net.minecraft.resources.ResourceLocation;

/**
 * What a material does with heat, in real units: how dense it is, how much heat a kilogram of it holds for each
 * kelvin, how well it conducts, how well its surface radiates, and where it melts and boils. Each is a property of the
 * material catalogue, which a mod gives its materials and a data pack may change; a material that gives none is taken
 * as its kind is on the whole (a metal, a gas, a mineral).
 *
 * @param density      how many kilograms a cubic metre of it weighs
 * @param specificHeat how many joules a kilogram of it holds for each kelvin
 * @param conductivity how many watts a metre of it conducts for each kelvin across it, through a square metre
 * @param emissivity   how well its surface radiates, from 0 (a mirror) to 1 (a black body)
 * @param melting      where it melts, in kelvin
 * @param boiling      where it boils, in kelvin
 */
@TextHolder
public record Thermal(double density, double specificHeat, double conductivity, double emissivity, double melting,
                      double boiling) {

    public static final TextKey DENSITY_NAME = TextKey.of("jscore.material_property.density", "Density");
    public static final TextKey SPECIFIC_HEAT_NAME = TextKey.of("jscore.material_property.specific_heat",
            "Specific heat");
    public static final TextKey CONDUCTIVITY_NAME = TextKey.of("jscore.material_property.conductivity",
            "Thermal conductivity");
    public static final TextKey EMISSIVITY_NAME = TextKey.of("jscore.material_property.emissivity", "Emissivity");
    public static final TextKey MELTING_NAME = TextKey.of("jscore.material_property.melting", "Melts at");
    public static final TextKey BOILING_NAME = TextKey.of("jscore.material_property.boiling", "Boils at");
    public static final TextKey DENSITY_VALUE = TextKey.of("jscore.material_property.density_value", "%s kg/m³");
    public static final TextKey SPECIFIC_HEAT_VALUE = TextKey.of("jscore.material_property.specific_heat_value",
            "%s J/(kg·K)");
    public static final TextKey CONDUCTIVITY_VALUE = TextKey.of("jscore.material_property.conductivity_value",
            "%s W/(m·K)");
    public static final TextKey KELVIN_VALUE = TextKey.of("jscore.material_property.kelvin_value", "%s K");

    /** How many kilograms a cubic metre of it weighs. */
    public static final MaterialProperty<Double> DENSITY = MaterialProperties.property(id("density"), Codec.DOUBLE,
            DENSITY_NAME).shown(MaterialProperty.Shown.SHIFT).format(value -> DENSITY_VALUE.with(number(value)))
            .declare();
    /** How many joules a kilogram of it holds for each kelvin. */
    public static final MaterialProperty<Double> SPECIFIC_HEAT = MaterialProperties.property(id("specific_heat"),
            Codec.DOUBLE, SPECIFIC_HEAT_NAME).shown(MaterialProperty.Shown.SHIFT)
            .format(value -> SPECIFIC_HEAT_VALUE.with(number(value))).declare();
    /** How many watts a metre of it conducts for each kelvin, through a square metre. */
    public static final MaterialProperty<Double> CONDUCTIVITY = MaterialProperties.property(id("conductivity"),
            Codec.DOUBLE, CONDUCTIVITY_NAME).shown(MaterialProperty.Shown.SHIFT)
            .format(value -> CONDUCTIVITY_VALUE.with(number(value))).declare();
    /** How well its surface radiates, from 0 to 1. */
    public static final MaterialProperty<Double> EMISSIVITY = MaterialProperties.property(id("emissivity"),
            Codec.doubleRange(0.0, 1.0), EMISSIVITY_NAME).shown(MaterialProperty.Shown.NEVER).declare();
    /** Where it melts, in kelvin. */
    public static final MaterialProperty<Double> MELTING = MaterialProperties.property(id("melting"), Codec.DOUBLE,
            MELTING_NAME).shown(MaterialProperty.Shown.SHIFT).format(value -> KELVIN_VALUE.with(number(value)))
            .declare();
    /** Where it boils, in kelvin. */
    public static final MaterialProperty<Double> BOILING = MaterialProperties.property(id("boiling"), Codec.DOUBLE,
            BOILING_NAME).shown(MaterialProperty.Shown.SHIFT).format(value -> KELVIN_VALUE.with(number(value)))
            .declare();

    public Thermal {
        if (!(density > 0.0) || !(specificHeat > 0.0) || !(conductivity >= 0.0) || !(emissivity >= 0.0)
                || emissivity > 1.0 || !(melting >= 0.0) || !(boiling >= 0.0)) {
            throw new IllegalArgumentException("a material's density and specific heat are above 0, its conductivity "
                    + "and its melting and boiling points no less than 0 and its emissivity from 0 to 1");
        }
    }

    /** Declares the properties, from the Core's constructor. */
    public static void declare() {
        // Loading the class declares them.
    }

    /**
     * What {@code material} does with heat: its own values where it gives them, on the side {@code playerSide} names,
     * and its kind's otherwise.
     */
    public static Thermal of(final Material material, final boolean playerSide) {
        final Thermal kind = ofKind(material.kind());
        return new Thermal(MaterialValues.value(material, DENSITY, playerSide).orElse(kind.density()),
                MaterialValues.value(material, SPECIFIC_HEAT, playerSide).orElse(kind.specificHeat()),
                MaterialValues.value(material, CONDUCTIVITY, playerSide).orElse(kind.conductivity()),
                MaterialValues.value(material, EMISSIVITY, playerSide).orElse(kind.emissivity()),
                MaterialValues.value(material, MELTING, playerSide).orElse(kind.melting()),
                MaterialValues.value(material, BOILING, playerSide).orElse(kind.boiling()));
    }

    /**
     * What a material of {@code kind} that says nothing of heat is taken to do: a metal as iron is, much as a crystal
     * as silicon, a mineral or a solid non-metal as stone, a liquid as water, a gas as air.
     */
    public static Thermal ofKind(final MaterialKind kind) {
        return switch (kind) {
            case METAL -> new Thermal(7_874.0, 449.0, 80.0, 0.3, 1_811.0, 3_134.0);
            case CRYSTAL -> new Thermal(2_329.0, 705.0, 149.0, 0.6, 1_687.0, 3_538.0);
            case GEM -> new Thermal(3_500.0, 750.0, 30.0, 0.8, 2_300.0, 3_500.0);
            case NONMETAL_SOLID, MINERAL -> new Thermal(2_700.0, 840.0, 2.5, 0.9, 1_500.0, 3_000.0);
            case LIQUID -> new Thermal(1_000.0, 4_186.0, 0.6, 0.95, 273.15, 373.15);
            case GAS -> new Thermal(1.2, 1_005.0, 0.026, 0.0, 63.0, 77.0);
            case EXOTIC -> new Thermal(10_000.0, 500.0, 50.0, 0.5, 5_000.0, 10_000.0);
        };
    }

    /** The heat capacity of {@code cubicMetres} of it, in joules a kelvin. */
    public double capacity(final double cubicMetres) {
        return HeatMath.capacity(this.density * Math.max(0.0, cubicMetres), this.specificHeat);
    }

    private static Text number(final double value) {
        return Text.literal(UnitFormatter.forCurrentLocale().decimal(value));
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsCore.MODID, path);
    }
}
