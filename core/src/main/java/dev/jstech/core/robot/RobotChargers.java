/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.robot;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Where the charging stations of each level are, as they tell it working: a robot told to recharge goes to the nearest.
 * A station not heard of for two seconds, broken or unloaded, is no longer there.
 */
public final class RobotChargers {

    /* Each level's stations, by place, with the tick each last told it is there. */
    private static final Map<Level, Long2LongMap> HEARD = new WeakHashMap<>();
    /* A station unheard of for this long, in ticks, is gone. */
    private static final long FORGOTTEN_AFTER = 40;

    private RobotChargers() {
    }

    /** The station at {@code pos} is there and working, told each time it charges. */
    public static void heard(final Level level, final BlockPos pos) {
        HEARD.computeIfAbsent(level, any -> new Long2LongOpenHashMap()).put(pos.asLong(), level.getGameTime());
    }

    /** The station nearest {@code from} within {@code radius} blocks, or none. */
    public static Optional<BlockPos> nearest(final Level level, final BlockPos from, final int radius) {
        final Long2LongMap stations = HEARD.get(level);
        if (stations == null || stations.isEmpty()) {
            return Optional.empty();
        }
        final long now = level.getGameTime();
        stations.long2LongEntrySet().removeIf(entry -> now - entry.getLongValue() > FORGOTTEN_AFTER);
        BlockPos best = null;
        double bestDistance = (double) radius * radius;
        for (final long each : stations.keySet()) {
            final BlockPos pos = BlockPos.of(each);
            final double distance = pos.distSqr(from);
            if (distance <= bestDistance) {
                best = pos;
                bestDistance = distance;
            }
        }
        return Optional.ofNullable(best);
    }
}
