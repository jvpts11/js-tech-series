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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Cables only the tests lay, registered with the Core as any mod's cables are: a long-distance cable, six pixels thick,
 * which never shares a block. It is laid by a vanilla item and drawn in a jacket and plug the series already has, so
 * the test mod declares no item, texture or model of its own.
 */
public final class TestCableTypes {

    public static final DeferredRegister<CableType> CABLES = DeferredRegister.create(CoreCables.KEY, JsTests.MODID);
    public static final DeferredHolder<CableType, CableType> LONG_DISTANCE = CABLES.register("long_distance",
            () -> CableType.builder(Connection.of(ResourceLocation.fromNamespaceAndPath(JsTests.MODID,
                            "long_distance")))
                    .alone().thickness(6).carries(1L, 0)
                    .jacket(ResourceLocation.fromNamespaceAndPath("jsc", "block/cable/hbw"))
                    .plug(ResourceLocation.fromNamespaceAndPath("jsc", "block/cable/plug/hbw"))
                    .build(() -> Items.STRING));

    private TestCableTypes() {
    }

    public static void register(final IEventBus modEventBus) {
        CABLES.register(modEventBus);
    }
}
