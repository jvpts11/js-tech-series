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
import net.minecraft.resources.ResourceLocation;

/**
 * The fluids the tests pour and pipe, declared through the Core as any mod's fluids are: a corrosive acid, a liquid
 * with a block and a bucket, and a hot gas, held only in tanks. Both wear the game's water, tinted, so the test mod
 * draws nothing.
 */
public final class TestFluids {

    public static final int ACID_TEMPERATURE = 310;
    public static final int GAS_TEMPERATURE = 420;

    private static final ResourceLocation STILL = ResourceLocation.withDefaultNamespace("block/water_still");
    private static final ResourceLocation FLOW = ResourceLocation.withDefaultNamespace("block/water_flow");
    private static final ContentTab.Section FLUIDS = TestItems.TAB.section();

    /** A corrosive liquid. */
    public static final FluidEntry ACID = TestSounds.CONTENT.fluid("test_acid").named("Test Acid")
            .look(STILL, FLOW, 0xFF7FE040).temperature(ACID_TEMPERATURE).corrosive().tab(FLUIDS).register();

    /** A hot gas. */
    public static final FluidEntry GAS = TestSounds.CONTENT.fluid("test_gas").named("Test Gas")
            .look(STILL, FLOW, 0xFFD0D0E0).temperature(GAS_TEMPERATURE).density(10).gas().register();

    private TestFluids() {
    }

    /** Declares the fluids, before the test mod's content is registered. */
    public static void declare() {
        // Loading the class declares them.
    }
}
