/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import dev.jstech.core.content.ContentTab;
import dev.jstech.core.content.FluidEntry;
import dev.jstech.core.heat.HeatForms;
import net.minecraft.resources.ResourceLocation;

/**
 * The fluids the tests pour and pipe, declared through the Core as any mod's fluids are: a corrosive acid, a liquid
 * with a block and a bucket, a hot gas, held only in tanks, and an oil that carries heat in its two forms, cool and
 * hot. All wear the game's water, tinted, so the test mod draws nothing.
 */
public final class TestFluids {

    public static final int ACID_TEMPERATURE = 310;
    public static final int GAS_TEMPERATURE = 420;
    public static final int COOL_OIL_TEMPERATURE = 300;
    public static final int HOT_OIL_TEMPERATURE = 600;
    /** How many kilograms a cubic metre of the oil weighs. */
    public static final int OIL_DENSITY = 900;

    private static final ResourceLocation STILL = ResourceLocation.withDefaultNamespace("block/water_still");
    private static final ResourceLocation FLOW = ResourceLocation.withDefaultNamespace("block/water_flow");
    private static final ContentTab.Section FLUIDS = TestItems.TAB.section();

    /** A corrosive liquid. */
    public static final FluidEntry ACID = TestSounds.CONTENT.fluid("test_acid").named("Test Acid")
            .look(STILL, FLOW, 0xFF7FE040).temperature(ACID_TEMPERATURE).corrosive().tab(FLUIDS).register();

    /** A hot gas. */
    public static final FluidEntry GAS = TestSounds.CONTENT.fluid("test_gas").named("Test Gas")
            .look(STILL, FLOW, 0xFFD0D0E0).temperature(GAS_TEMPERATURE).density(10).gas().register();

    /** An oil that carries heat round a loop, in its two forms: cool, and hot. */
    public static final FluidEntry COOL_OIL = TestSounds.CONTENT.fluid("test_cool_oil").named("Test Cool Oil")
            .look(STILL, FLOW, 0xFF8A6A2A).temperature(COOL_OIL_TEMPERATURE).density(OIL_DENSITY).tab(FLUIDS)
            .register();
    public static final FluidEntry HOT_OIL = TestSounds.CONTENT.fluid("test_hot_oil").named("Test Hot Oil")
            .look(STILL, FLOW, 0xFFC0501A).temperature(HOT_OIL_TEMPERATURE).density(OIL_DENSITY).tab(FLUIDS)
            .register();

    static {
        HeatForms.declare(COOL_OIL.id(), HOT_OIL.id());
    }

    private TestFluids() {
    }

    /** Declares the fluids, before the test mod's content is registered. */
    public static void declare() {
        // Loading the class declares them.
    }
}
