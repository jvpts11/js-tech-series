/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.cable;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * The Core's rules for a wire that something it is not made for reaches: it holds and carries none of it, or it breaks
 * once it has been reached for a while in a row, and then its cable's mod says what follows (the pipe's scrap, what
 * leaks out). {@link #spill} lets a liquid out where the broken pipe was.
 */
public final class BreachRules {

    /** The wire holds, and what it is not made for stays where it is: a cable's rule unless it declares one. */
    public static final IBreachRule HOLDS = (level, breach, gameTime) -> { };

    /* How many wires a rule may be counting before it forgets those no longer reached. */
    private static final int PRUNE_ABOVE = 64;

    private BreachRules() {
    }

    /**
     * The wire breaks once it has been reached for {@code ticks} ticks in a row, a tick it is not reached starting the
     * count again: it is taken out of its block, and {@code then} says what follows.
     */
    public static IBreachRule breaksAfter(final int ticks, final IBroken then) {
        if (ticks < 1) {
            throw new IllegalArgumentException("a wire breaks after a tick at least, not " + ticks);
        }
        Objects.requireNonNull(then, "then");
        final Map<Exposed, long[]> since = new ConcurrentHashMap<>();
        return (level, breach, gameTime) -> {
            final Exposed key = new Exposed(level.dimension(), breach.at().asLong(), breach.type().id());
            final long[] run = since.computeIfAbsent(key, k -> new long[] {gameTime, gameTime});
            if (run[1] < gameTime - 1) {
                run[0] = gameTime;
            }
            run[1] = gameTime;
            if (gameTime - run[0] + 1 >= ticks) {
                since.remove(key);
                if (level.getBlockEntity(breach.at()) instanceof CableBlockEntity cable && cable.take(breach.type())) {
                    then.broken(level, breach);
                }
            }
            if (since.size() > PRUNE_ABOVE) {
                prune(since, gameTime);
            }
        };
    }

    /**
     * Lets {@code what}, a liquid of the game, out where a broken pipe was, as a source of it, when the place is open to
     * it; a gas or a fluid with no block of its own is left to its mod.
     */
    public static void spill(final ServerLevel level, final BlockPos at, final String what) {
        final ResourceLocation id = ResourceLocation.tryParse(what);
        final Fluid fluid = id == null ? Fluids.EMPTY : BuiltInRegistries.FLUID.get(id);
        if (fluid == Fluids.EMPTY) {
            return;
        }
        final BlockState liquid = fluid.defaultFluidState().createLegacyBlock();
        final BlockState there = level.getBlockState(at);
        if (!liquid.isAir() && (there.isAir() || there.canBeReplaced())) {
            level.setBlockAndUpdate(at, liquid);
        }
    }

    /* Forgets the wires not reached in the last tick. */
    private static void prune(final Map<Exposed, long[]> since, final long gameTime) {
        final Iterator<Map.Entry<Exposed, long[]>> each = since.entrySet().iterator();
        while (each.hasNext()) {
            if (each.next().getValue()[1] < gameTime - 1) {
                each.remove();
            }
        }
    }

    /** What follows a wire breaking: its cable's mod says. */
    @FunctionalInterface
    public interface IBroken {

        /** The wire {@code breach} reached broke and was taken out of its block. */
        void broken(ServerLevel level, LineBreach breach);
    }

    /* One wire being reached: where, and of which cable. */
    private record Exposed(ResourceKey<Level> dimension, long pos, ResourceLocation type) {
    }
}
