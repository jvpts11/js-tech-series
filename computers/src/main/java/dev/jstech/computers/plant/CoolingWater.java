/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.plant;

import dev.jstech.computers.JsComputers;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MaterialKind;
import dev.jstech.core.material.MaterialLiquids;
import dev.jstech.core.material.MaterialProperties;
import dev.jstech.core.material.Materials;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;

/**
 * The two waters of the datacenter's cooling loops (computers/docs/design/temperature.md): Cold Water going out to
 * what it cools and Hot Water coming back, in both loops, the racks' and the tower's. The heat rides the water at
 * {@link #KILOWATT_A_MILLIBUCKET}: a machine shedding 8 kW into the loop turns 8 mB of cold water into hot every
 * second. Each is the game's water drawn in its own colour, blue and warm orange, as the loops' pipe tags are.
 */
@TextHolder
public final class CoolingWater {

    public static final TextKey COLD_WATER_NAME = TextKey.of("jsc.material.cold_water", "Cold Water");
    public static final TextKey HOT_WATER_NAME = TextKey.of("jsc.material.hot_water", "Hot Water");

    /** How many kilowatts of heat a millibucket of water carries away each second, cold in and hot out. */
    public static final int KILOWATT_A_MILLIBUCKET = 1;
    /* How warm each water is, in kelvin: chilled water leaves the plant at about 7 C and comes back at about 35 C. */
    private static final int COLD_KELVIN = 280;
    private static final int HOT_KELVIN = 308;
    private static final String WATER = "H2O";

    /** The water going out to what it cools. */
    public static final Material COLD_WATER = water("cold_water", COLD_WATER_NAME, 0x4082DE);
    /** The water coming back from what it cooled. */
    public static final Material HOT_WATER = water("hot_water", HOT_WATER_NAME, 0xE28460);
    public static final Supplier<Fluid> COLD = MaterialLiquids.use(JsComputers.MODID, COLD_WATER, COLD_KELVIN);
    public static final Supplier<Fluid> HOT = MaterialLiquids.use(JsComputers.MODID, HOT_WATER, HOT_KELVIN);

    private CoolingWater() {
    }

    /** Declares the waters, from the mod's constructor. */
    public static void declare() {
        // Loading the class declares them.
    }

    private static Material water(final String path, final TextKey name, final int colour) {
        return Materials.material(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, path), MaterialKind.LIQUID,
                name).colour(colour).with(MaterialProperties.FORMULA, WATER).declare();
    }
}
