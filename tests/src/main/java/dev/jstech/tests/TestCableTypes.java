/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import dev.jstech.core.cable.CableType;
import dev.jstech.core.cable.CoreCables;
import dev.jstech.core.connect.Connection;
import dev.jstech.core.fluid.CoreFluidTags;
import dev.jstech.core.grid.GridKind;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Cables only the tests lay, registered with the Core as any mod's cables are: a long-distance cable, six pixels thick,
 * which never shares a block, an energy cable that loses a tenth of what crosses each block of it, and two pipes, a
 * plain one and one that holds pressure. Each is laid by a vanilla item and drawn in a jacket and plug the series
 * already has, so the test mod declares no item, texture or model of its own.
 */
public final class TestCableTypes {

    public static final long PLAIN_RATE = 100L;
    public static final int PLAIN_HOTTEST = 1_000;
    public static final long PRESSURE_RATE = 800L;
    public static final int PRESSURE_HOTTEST = 2_000;
    public static final DeferredRegister<CableType> CABLES = DeferredRegister.create(CoreCables.KEY, JsTests.MODID);
    public static final DeferredHolder<CableType, CableType> LONG_DISTANCE = CABLES.register("long_distance",
            () -> CableType.builder(Connection.of(ResourceLocation.fromNamespaceAndPath(JsTests.MODID,
                            "long_distance")))
                    .alone().thickness(6).carries(1L, 0)
                    .jacket(ResourceLocation.fromNamespaceAndPath("jsc", "block/cable/hbw"))
                    .plug(ResourceLocation.fromNamespaceAndPath("jsc", "block/cable/plug/hbw"))
                    .build(() -> Items.STRING));
    public static final DeferredHolder<CableType, CableType> LOSSY_ENERGY = CABLES.register("lossy_energy",
            () -> CableType.builder(Connection.of(ResourceLocation.fromNamespaceAndPath(JsTests.MODID,
                            "lossy_energy")))
                    .grid(GridKind.POWER).alone().carries(Long.MAX_VALUE, 0).loses(100)
                    .jacket(ResourceLocation.fromNamespaceAndPath("jsindustrial", "block/cable/energy"))
                    .plug(ResourceLocation.fromNamespaceAndPath("jsindustrial", "block/cable/plug/energy"))
                    .build(() -> Items.REDSTONE));
    /** The coolest pipe: a hundred millibuckets a tick, nothing above 1000 K, no gas and nothing corrosive. */
    public static final DeferredHolder<CableType, CableType> PLAIN_PIPE = CABLES.register("plain_pipe",
            () -> CableType.builder(Connection.of(ResourceLocation.fromNamespaceAndPath(JsTests.MODID,
                            "plain_pipe")))
                    .grid(GridKind.FLUID).alone().carries(PLAIN_RATE, 0).withstands(0, PLAIN_HOTTEST)
                    .jacket(ResourceLocation.fromNamespaceAndPath("jsc", "block/cable/hpc"))
                    .plug(ResourceLocation.fromNamespaceAndPath("jsc", "block/cable/plug/qsfp"))
                    .build(() -> Items.BUCKET));
    /** A pipe that holds pressure: eight hundred millibuckets a tick, up to 2000 K, gases and corrosives. */
    public static final DeferredHolder<CableType, CableType> PRESSURE_PIPE = CABLES.register("pressure_pipe",
            () -> CableType.builder(Connection.of(ResourceLocation.fromNamespaceAndPath(JsTests.MODID,
                            "pressure_pipe")))
                    .grid(GridKind.FLUID).alone().carries(PRESSURE_RATE, 0).withstands(0, PRESSURE_HOTTEST)
                    .takes(CoreFluidTags.GASES).takes(CoreFluidTags.CORROSIVE)
                    .jacket(ResourceLocation.fromNamespaceAndPath("jsc", "block/cable/crafting"))
                    .plug(ResourceLocation.fromNamespaceAndPath("jsc", "block/cable/plug/crafting"))
                    .build(() -> Items.IRON_BARS));

    private TestCableTypes() {
    }

    public static void register(final IEventBus modEventBus) {
        CABLES.register(modEventBus);
    }
}
