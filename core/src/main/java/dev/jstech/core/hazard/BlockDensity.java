/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.hazard;

import com.mojang.serialization.Codec;
import dev.jstech.core.JsCore;
import dev.jstech.core.heat.Thermal;
import dev.jstech.core.material.MaterialThing;
import dev.jstech.core.material.MaterialValues;
import dev.jstech.core.material.Materials;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

/**
 * How dense a block of the world is, in kilograms a cubic metre, for what passes through it to be weakened by: a data
 * map of blocks ({@code data/<namespace>/data_maps/block/block_density.json}) a mod or a data pack fills, lead at
 * 11,340, stone at 2,700, wood at 600. A block the map says nothing of is as dense as the material of its item, as
 * water or lava for a liquid, as rock for any other full block, and as air for the rest.
 *
 * <pre>{@code
 * { "values": { "mymod:lead_bricks": 11340, "#c:storage_blocks/lead": 11340 } }
 * }</pre>
 */
public final class BlockDensity {

    /** The air's, in kilograms a cubic metre. */
    public static final double AIR = 1.2;
    /** Each block's density, in kilograms a cubic metre. */
    public static final DataMapType<Block, Double> DENSITY = DataMapType.builder(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "block_density"), Registries.BLOCK,
            Codec.doubleRange(0.0, 50_000.0)).build();
    /* Water's and lava's, and that of a full block the map and its material say nothing of. */
    private static final double WATER = 1_000.0;
    private static final double LAVA = 3_100.0;
    private static final double ROCK = 2_000.0;
    /* The density of each block's material, found once; none when its item has no material with one. */
    private static final Map<Block, Optional<Double>> OF_MATERIAL = new ConcurrentHashMap<>();

    private BlockDensity() {
    }

    /** Hands the mod's bus the data map, once, from the Core's constructor. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterDataMapTypesEvent.class, event -> event.register(DENSITY));
    }

    /** How dense the block {@code state} at {@code pos} is, in kilograms a cubic metre. */
    public static double of(final BlockGetter level, final BlockPos pos, final BlockState state) {
        if (state.isAir()) {
            return AIR;
        }
        final Double mapped = state.getBlockHolder().getData(DENSITY);
        if (mapped != null) {
            return mapped;
        }
        final FluidState fluid = state.getFluidState();
        if (!fluid.isEmpty()) {
            return fluid.is(FluidTags.LAVA) ? LAVA : WATER;
        }
        final Optional<Double> material = OF_MATERIAL.computeIfAbsent(state.getBlock(), BlockDensity::ofMaterial);
        if (material.isPresent()) {
            return material.get();
        }
        return state.isCollisionShapeFullBlock(level, pos) ? ROCK : AIR;
    }

    private static Optional<Double> ofMaterial(final Block block) {
        return Materials.of(block.asItem()).map(MaterialThing::material)
                .flatMap(material -> MaterialValues.value(material, Thermal.DENSITY, false));
    }
}
