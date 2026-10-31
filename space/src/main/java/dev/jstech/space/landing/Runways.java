/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.landing;

import dev.jstech.core.moving.IStructureBody;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.space.ship.ShipAutopilots;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/**
 * A runway's stretch, from its beacon on along the way it runs: long enough for a wheeled ship to touch down past the
 * beacon and roll to a stop, as wide as a small ship's wings with room either side, and free: firm land under every
 * block of it, level with the beacon's foot, nothing standing on it as high as a ship, and no other ship on it. Looked
 * at once, as a ship's autopilot takes it, or as the ship comes near and its land loads.
 */
public final class Runways {

    /** How far either side of its line a runway's stretch reaches, in blocks: thirteen wide. */
    public static final int HALF_WIDTH = 6;
    /** How high over a runway's stretch is to be free, in blocks. */
    public static final int HEADROOM = 6;
    private static final int CHUNK = 4;

    private Runways() {
    }

    /**
     * Whether the stretch of the runway of {@code beacon} is long and free for a ship, {@code ours} standing on it
     * counting for nothing; none while any of its land is not loaded, to be looked at again later.
     */
    public static Optional<Boolean> clear(final ServerLevel level, final ShipLandings.Beacon beacon,
                                          @Nullable final IStructureBody ours) {
        final Direction way = beacon.way();
        final Direction right = way.getClockWise();
        for (int along = 1; along <= ShipAutopilots.RUNWAY_LENGTH; along++) {
            for (int across = -HALF_WIDTH; across <= HALF_WIDTH; across++) {
                final BlockPos foot = beacon.at().relative(way, along).relative(right, across);
                final LevelChunk chunk = level.getChunkSource().getChunkNow(foot.getX() >> CHUNK,
                        foot.getZ() >> CHUNK);
                if (chunk == null) {
                    return Optional.empty();
                }
                final BlockPos under = foot.below();
                if (!chunk.getBlockState(under).isFaceSturdy(level, under, Direction.UP)) {
                    return Optional.of(false);
                }
                for (int up = 0; up < HEADROOM; up++) {
                    final BlockPos over = foot.above(up);
                    if (!chunk.getBlockState(over).getCollisionShape(level, over).isEmpty()) {
                        return Optional.of(false);
                    }
                }
            }
        }
        final BlockPos start = beacon.at().relative(way).relative(right, -HALF_WIDTH);
        final BlockPos end = beacon.at().relative(way, ShipAutopilots.RUNWAY_LENGTH).relative(right, HALF_WIDTH)
                .above(HEADROOM);
        for (final IStructureBody body : MovingStructures.near(level, AABB.encapsulatingFullBlocks(start, end))) {
            if (body != ours) {
                return Optional.of(false);
            }
        }
        return Optional.of(true);
    }
}
