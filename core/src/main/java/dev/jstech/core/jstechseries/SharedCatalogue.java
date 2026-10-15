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
import dev.jstech.core.material.CatalogueItems;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MaterialKind;
import dev.jstech.core.material.MaterialLiquids;
import dev.jstech.core.material.MaterialProperties;
import dev.jstech.core.material.Materials;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

/**
 * What the series' mods share that is no element: the common fuels, liquids every mod may burn or store (the gases,
 * hydrogen, oxygen, nitrogen, helium, xenon and the rest, are their elements), and the generic components, items with
 * no game of their own. Each exists only when an installed mod declares it uses it ({@code MaterialLiquids},
 * {@link CatalogueItems}); each mod that makes one gives its own recipe, J's Industrial the whole industrial chain, a
 * mod used without it a simpler path ({@link SimplePath}).
 */
@TextHolder
public final class SharedCatalogue {

    public static final TextKey WATER_NAME = TextKey.of("jscore.material.water", "Water");
    public static final TextKey GASOLINE_NAME = TextKey.of("jscore.material.gasoline", "Gasoline");
    public static final TextKey KEROSENE_NAME = TextKey.of("jscore.material.kerosene", "Kerosene");
    public static final TextKey DIESEL_NAME = TextKey.of("jscore.material.diesel", "Diesel");

    public static final TextKey SERVOMOTOR_NAME = TextKey.of("item.jscore.servomotor", "Servomotor");
    public static final TextKey ELECTRIC_MOTOR_NAME = TextKey.of("item.jscore.electric_motor", "Electric Motor");
    public static final TextKey LED_NAME = TextKey.of("item.jscore.led", "LED");
    public static final TextKey SENSOR_NAME = TextKey.of("item.jscore.sensor", "Sensor");
    public static final TextKey OSCILLATOR_NAME = TextKey.of("item.jscore.oscillator", "Oscillator");
    public static final TextKey ANTENNA_NAME = TextKey.of("item.jscore.antenna", "Antenna");
    public static final TextKey MAGNETIC_HEAD_NAME = TextKey.of("item.jscore.magnetic_head", "Magnetic Head");

    /*
     * Water, whose liquid is the game's own: steam is its gas and ice its solid, and a boiler boils it by its real
     * enthalpy. Its values are liquid water's at 20 °C.
     */
    public static final Material WATER = Materials.material(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "water"), MaterialKind.LIQUID, WATER_NAME)
            .colour(0x3F76E4).also(MatterState.GAS, MatterState.SOLID).with(MaterialProperties.FORMULA, "H2O")
            .with(Thermal.DENSITY, 998.0).with(Thermal.SPECIFIC_HEAT, 4_184.0).with(Thermal.CONDUCTIVITY, 0.6)
            .with(Thermal.EMISSIVITY, 0.96).with(Thermal.MELTING, 273.15).with(Thermal.BOILING, 373.15)
            .with(Thermal.LATENT_HEAT, 2_257_000.0).with(Thermal.MOLAR_MASS, 0.018_015)
            .with(Thermal.LIQUID_DENSITY, 998.0).declare();

    /* Mixtures of hydrocarbons, whose make-up varies: they have no one formula. */
    public static final Material GASOLINE = fuel("gasoline", GASOLINE_NAME, 0xE9D59A);
    public static final Material KEROSENE = fuel("kerosene", KEROSENE_NAME, 0xD9E2E8);
    public static final Material DIESEL = fuel("diesel", DIESEL_NAME, 0xC9A85A);

    public static final CatalogueItems.Entry SERVOMOTOR = component("servomotor", SERVOMOTOR_NAME);
    public static final CatalogueItems.Entry ELECTRIC_MOTOR = component("electric_motor", ELECTRIC_MOTOR_NAME);
    public static final CatalogueItems.Entry LED = component("led", LED_NAME);
    public static final CatalogueItems.Entry SENSOR = component("sensor", SENSOR_NAME);
    public static final CatalogueItems.Entry OSCILLATOR = component("oscillator", OSCILLATOR_NAME);
    public static final CatalogueItems.Entry ANTENNA = component("antenna", ANTENNA_NAME);
    public static final CatalogueItems.Entry MAGNETIC_HEAD = component("magnetic_head", MAGNETIC_HEAD_NAME);

    private SharedCatalogue() {
    }

    /** Declares the catalogue, from the Core's constructor: the game's water is the series' water. */
    public static void declare() {
        MaterialLiquids.link(WATER, ResourceLocation.withDefaultNamespace("water"));
    }

    /** The common fuels. */
    public static List<Material> fuels() {
        return List.of(GASOLINE, KEROSENE, DIESEL);
    }

    /** The generic components. */
    public static List<CatalogueItems.Entry> components() {
        return List.of(SERVOMOTOR, ELECTRIC_MOTOR, LED, SENSOR, OSCILLATOR, ANTENNA, MAGNETIC_HEAD);
    }

    private static Material fuel(final String path, final TextKey name, final int colour) {
        return Materials.material(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, path), MaterialKind.LIQUID,
                name).colour(colour).also(MatterState.GAS).declare();
    }

    /* A component's picture is in the Core's own textures, item/component/<its id>. */
    private static CatalogueItems.Entry component(final String path, final TextKey name) {
        return CatalogueItems.declare(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, path), name,
                ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "item/component/" + path));
    }
}
