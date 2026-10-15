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
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

/**
 * How well a block of the world conducts heat, in watts a metre a kelvin, as a data map of blocks
 * ({@code data/<namespace>/data_maps/block/thermal_conductivity.json}) a mod or a data pack fills: wool and rock wool
 * insulate, glass and brick hardly better than stone, metal blocks carry heat away. A hot block beside one of them
 * loses its heat through that face as the block conducts, and a block the map says nothing of conducts as stone.
 *
 * <pre>{@code
 * { "values": { "mymod:rock_wool": 0.04, "#minecraft:wool": 0.04 } }
 * }</pre>
 */
public final class BlockConductivity {

    /* No block conducts better than this, in watts a metre a kelvin: diamond's is about 2,200. */
    private static final double MOST = 10_000.0;
    /** Each block's thermal conductivity, in watts a metre a kelvin. */
    public static final DataMapType<Block, Double> CONDUCTIVITY = DataMapType.builder(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "thermal_conductivity"), Registries.BLOCK,
            Codec.doubleRange(0.0, MOST)).build();

    private BlockConductivity() {
    }

    /** Hands the mod's bus the data map, once, from the Core's constructor. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterDataMapTypesEvent.class, event -> event.register(CONDUCTIVITY));
    }

    /** How well {@code state}'s block conducts heat: the map's value, or stone's. */
    public static double of(final BlockState state) {
        final Double value = state.getBlockHolder().getData(CONDUCTIVITY);
        return value == null ? LevelHeat.GROUND : value;
    }
}
