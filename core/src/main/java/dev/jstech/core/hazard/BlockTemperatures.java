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
import dev.jstech.core.util.Loaded;
import java.util.OptionalDouble;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

/**
 * How hot or how cold the blocks of the world are where a body stands near them, in kelvin: a data map of blocks
 * ({@code data/<namespace>/data_maps/block/block_temperature.json}) a mod or a data pack fills, fire at 1,100, a
 * campfire at 900, ice at 268; a liquid as hot as its fluid says, lava at 1,300, though only where it is far from
 * room temperature; a block that is lit or not by its state only while it is lit. A body near them takes or loses
 * their heat by radiation, as much as each fills of what it sees: a block a metre away a few hundredths of it, and
 * nothing through a wall.
 */
public final class BlockTemperatures {

    /** How hot each block is, in kelvin. */
    public static final DataMapType<Block, Double> TEMPERATURE = DataMapType.builder(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "block_temperature"), Registries.BLOCK,
            Codec.doubleRange(0.0, 10_000.0)).synced(Codec.doubleRange(0.0, 10_000.0), false).build();
    /* How far round a body its blocks are looked at, in blocks, and the least difference from the air worth it. */
    private static final int REACH = 3;
    private static final double LEAST_DIFFERENCE = 20.0;
    /* A liquid counts only when its fluid is at least this far from room temperature, which the game's water is. */
    private static final double ROOM = 300.0;
    private static final double LIQUID_DIFFERENCE = 25.0;
    /* The most of what a body sees one block fills, and all the blocks round it together. */
    private static final double MOST_EACH = 0.03;
    private static final double MOST_IN_ALL = 0.5;
    private static final double NEAREST = 0.75;

    private BlockTemperatures() {
    }

    /** Hands the mod's bus the data map, once, from the Core's constructor. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterDataMapTypesEvent.class, event -> event.register(TEMPERATURE));
    }

    /** How hot {@code state} is, in kelvin; none for a block at the temperature of the place around it. */
    public static OptionalDouble of(final BlockState state) {
        if (state.isAir() || state.hasProperty(BlockStateProperties.LIT) && !state.getValue(BlockStateProperties.LIT)) {
            return OptionalDouble.empty();
        }
        final Double mapped = state.getBlockHolder().getData(TEMPERATURE);
        if (mapped != null) {
            return OptionalDouble.of(mapped);
        }
        final FluidState fluid = state.getFluidState();
        if (!fluid.isEmpty()) {
            final double kelvin = fluid.getFluidType().getTemperature();
            if (Math.abs(kelvin - ROOM) >= LIQUID_DIFFERENCE) {
                return OptionalDouble.of(kelvin);
            }
        }
        return OptionalDouble.empty();
    }

    /**
     * The temperature what is around {@code body} gives and takes radiation as, in kelvin, where the air is
     * {@code airKelvin}: the air's own, warmer by each hot block it sees within a few blocks, cooler by each cold one,
     * each by the share of what it sees that block fills.
     */
    public static double radiantKelvin(final Level level, final Entity body, final double airKelvin) {
        final Vec3 centre = body.getBoundingBox().getCenter();
        final BlockPos middle = BlockPos.containing(centre);
        final double air4 = Math.pow(airKelvin, 4);
        double seen = 0;
        double sum = 0;
        final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int dx = -REACH; dx <= REACH; dx++) {
            for (int dy = -REACH; dy <= REACH; dy++) {
                for (int dz = -REACH; dz <= REACH; dz++) {
                    at.set(middle.getX() + dx, middle.getY() + dy, middle.getZ() + dz);
                    if (!Loaded.at(level, at)) {
                        continue;
                    }
                    final OptionalDouble kelvin = of(level.getBlockState(at));
                    if (kelvin.isEmpty() || Math.abs(kelvin.getAsDouble() - airKelvin) < LEAST_DIFFERENCE) {
                        continue;
                    }
                    final Vec3 to = Vec3.atCenterOf(at);
                    if (!seen(level, body, centre, to, at)) {
                        continue;
                    }
                    final double distance = Math.max(NEAREST, centre.distanceTo(to));
                    final double share = Math.min(MOST_EACH, 1 / (4 * Math.PI * distance * distance));
                    seen += share;
                    sum += share * (Math.pow(kelvin.getAsDouble(), 4) - air4);
                }
            }
        }
        if (seen > MOST_IN_ALL) {
            sum *= MOST_IN_ALL / seen;
        }
        return Math.pow(Math.max(0, air4 + sum), 0.25);
    }

    /* Whether nothing stands between {@code from} and the block at {@code pos}: what is hit first is the block. */
    private static boolean seen(final Level level, final Entity body, final Vec3 from, final Vec3 to,
                                final BlockPos pos) {
        final BlockHitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, body));
        return hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(pos);
    }
}
