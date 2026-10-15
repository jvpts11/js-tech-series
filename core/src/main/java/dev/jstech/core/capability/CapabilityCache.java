/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.capability;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import org.jetbrains.annotations.Nullable;

/**
 * What a block offers on one face, kept between ticks: asking it costs a field read, and the game tells it when the
 * block there is placed, broken or changes what it offers, so it is worked out again only then. A machine pushing
 * into its neighbour every tick keeps one per face, made once:
 *
 * <pre>{@code
 * private CapabilityCache<IItemHandler> output;
 * ...
 * output = CapabilityCache.of(Capabilities.ItemHandler.BLOCK, level, pos.east(), Direction.WEST, this);
 * IItemHandler target = output.get();
 * }</pre>
 *
 * <p>It is kept for as long as its owner stands, and lets go when the owner is removed.
 *
 * @param <T> what the capability hands over
 */
public final class CapabilityCache<T> {

    private final BlockCapabilityCache<T, ?> cache;

    private CapabilityCache(final BlockCapabilityCache<T, ?> cache) {
        this.cache = cache;
    }

    /** What the block at {@code pos} offers as {@code capability} in {@code context}, kept for {@code owner}. */
    public static <T, C> CapabilityCache<T> of(final BlockCapability<T, C> capability, final ServerLevel level,
                                               final BlockPos pos, final C context, final BlockEntity owner) {
        return of(capability, level, pos, context, owner, () -> {
        });
    }

    /** The same, telling {@code whenStale} each time what the block offers there may have changed. */
    public static <T, C> CapabilityCache<T> of(final BlockCapability<T, C> capability, final ServerLevel level,
                                               final BlockPos pos, final C context, final BlockEntity owner,
                                               final Runnable whenStale) {
        return new CapabilityCache<>(BlockCapabilityCache.create(capability, level, pos.immutable(), context,
                () -> !owner.isRemoved(), whenStale));
    }

    /** What the block offers now, or null when it offers nothing there. */
    public @Nullable T get() {
        return cache.getCapability();
    }

    /** Where the block looked at stands. */
    public BlockPos pos() {
        return cache.pos();
    }
}
