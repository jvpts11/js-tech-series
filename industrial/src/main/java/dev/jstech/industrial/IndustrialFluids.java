/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial;

import dev.jstech.core.content.FluidEntry;
import dev.jstech.core.heat.HeatForms;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import net.minecraft.resources.ResourceLocation;

/**
 * The liquids of J's Industrial: hot water, the water a cooling loop gives back warmed, which a cooling plant or a
 * tower cools again. It is water's hot form, so whatever passes heat between liquids turns one into the other.
 */
@PaletteHolder
public final class IndustrialFluids {

    /* How hot the water of a cooling loop comes back, in kelvin: 60 degrees Celsius, as a server room's loop runs. */
    private static final int HOT_WATER_KELVIN = 333;
    private static final ResourceLocation WATER_STILL = ResourceLocation.withDefaultNamespace("block/water_still");
    private static final ResourceLocation WATER_FLOW = ResourceLocation.withDefaultNamespace("block/water_flow");
    /** The colours the liquids' textures are tinted with. */
    public static final Palette<Colours> PALETTE = Palettes.declare(JsIndustrial.MODID, "fluids",
            new Colours(0xFF5C7BD8));

    public static final FluidEntry HOT_WATER = IndustrialModule.CONTENT.fluid("hot_water").named("Hot Water")
            .look(WATER_STILL, WATER_FLOW, PALETTE.get().hotWater()).temperature(HOT_WATER_KELVIN)
            .tab(IndustrialModule.RESEARCH).register();

    private IndustrialFluids() {
    }

    /** Declares the liquids, and water and hot water as one liquid's two forms, from the mod's constructor. */
    public static void declare() {
        HeatForms.declare(ResourceLocation.withDefaultNamespace("water"), HOT_WATER.id());
    }

    /**
     * The liquids' tints.
     *
     * @param hotWater the game's water a little warmer in tone, so a pipe's window tells it from cold water
     */
    public record Colours(int hotWater) {
    }
}
