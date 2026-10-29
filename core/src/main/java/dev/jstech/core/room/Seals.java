/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.room;

import dev.jstech.core.JsCore;
import dev.jstech.core.dimension.DimensionRulesData;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

/**
 * How each block seals a room, as a data map of blocks ({@code data/<namespace>/data_maps/block/seals.json}) any mod or
 * data pack fills ({@link Seal}). A block the map says nothing of is a wall when it is a whole solid block, which does
 * not leak and stands as hard as it is to blow up (stone a great deal, dirt little); anything else, air, water, a slab
 * or a fence, lets air through and is part of the room it stands in. The game's glass leaks a little and stands
 * little, its doors, trapdoors and fence gates leak a little more while shut and let everything through while open. A
 * dimension's rules may say some blocks seal otherwise there ({@link DimensionSeal}).
 */
public final class Seals {

    /** Each block's seal. */
    public static final DataMapType<Block, Seal> SEALS = DataMapType.builder(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "seals"), Registries.BLOCK, Seal.CODEC).build();
    /* A block stands this many kilopascals for each point of how hard it is to blow up: glass 120, stone 2,400. */
    private static final double KILOPASCALS_PER_RESISTANCE = 400.0;
    private static final double WEAKEST = 20.0;

    private Seals() {
    }

    /** Hands the mod's bus the data map, once, from the Core's constructor. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterDataMapTypesEvent.class, event -> event.register(SEALS));
    }

    /**
     * How {@code state}, at {@code pos} of {@code level}, seals: as its dimension's rules say there, else the map's
     * seal, or what its shape makes it.
     */
    public static Seal of(final BlockGetter level, final BlockPos pos, final BlockState state) {
        return of(level instanceof Level placed ? DimensionRulesData.of(placed).seals() : List.of(), level, pos,
                state);
    }

    /**
     * How {@code state} seals where {@code here} are the seals its dimension's rules say, read once by whoever asks of
     * many blocks.
     */
    static Seal of(final List<DimensionSeal> here, final BlockGetter level, final BlockPos pos,
                   final BlockState state) {
        for (final DimensionSeal local : here) {
            if (local.covers(state)) {
                return local.seal();
            }
        }
        final Seal declared = state.getBlockHolder().getData(SEALS);
        if (declared != null) {
            return declared;
        }
        if (state.isAir() || state.getBlock() instanceof LiquidBlock) {
            return Seal.PASSES;
        }
        return state.isCollisionShapeFullBlock(level, pos) ? Seal.SOLID : Seal.PASSES;
    }

    /** The share of a face of {@code state} gas crosses: its leak, the whole face for no wall or an open door. */
    public static double leak(final Seal seal, final BlockState state) {
        if (!seal.wall() || seal.door() && open(state)) {
            return 1.0;
        }
        return seal.leak();
    }

    /** Whether {@code state} is open, as a door, a trapdoor or a gate is. */
    public static boolean open(final BlockState state) {
        return state.hasProperty(BlockStateProperties.OPEN) && state.getValue(BlockStateProperties.OPEN);
    }

    /** The difference in pressure {@code state} stands before it bursts, in kilopascals. */
    public static double rating(final Seal seal, final BlockState state) {
        return seal.rating().orElseGet(() -> Math.clamp(state.getBlock().getExplosionResistance()
                * KILOPASCALS_PER_RESISTANCE, WEAKEST, Seal.STRONGEST));
    }
}
