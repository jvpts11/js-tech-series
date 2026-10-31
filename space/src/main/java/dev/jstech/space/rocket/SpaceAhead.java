/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.rocket;

import java.util.Comparator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;

/**
 * The space a rocket or a ship flies into, faster than the land round a player loads: it keeps the chunks it stands in
 * and those ahead of it loaded and going a while, and waits at one not going yet rather than fly into it, since a
 * structure that moves into a chunk whose entities do not tick stops there. Like any moving structure it freezes with
 * its space when no player is in it and no chunk loader holds it, and goes on when someone comes.
 */
public final class SpaceAhead {

    /*
     * The ticket the space on a rocket's way is held by: going by itself a little after it was last taken, and making
     * its chunk tick as the land round a player does.
     */
    private static final TicketType<ChunkPos> PASSING = TicketType.create("jsspace_passing",
            Comparator.comparingLong(ChunkPos::toLong), 200);
    private static final int REACH = 2;
    /* A chunk further: the chunks round the one held go too, as wide as a small ship either side of its way. */
    private static final int WIDE_REACH = 3;

    private SpaceAhead() {
    }

    /**
     * Keeps the chunk of the place {@code x}, {@code y}, {@code z} of {@code level} and the chunks round it loaded and
     * going a while: a ship's way, where every block and seat of it goes, not only its middle.
     */
    public static void holdAround(final ServerLevel level, final double x, final double y, final double z) {
        final ChunkPos at = new ChunkPos(BlockPos.containing(x, y, z));
        level.getChunkSource().addRegionTicket(PASSING, at, WIDE_REACH, at, true);
    }

    /** Keeps the chunk {@code chunk} of {@code level}, packed, loaded and going a while. */
    public static void hold(final ServerLevel level, final long chunk) {
        final ChunkPos at = new ChunkPos(chunk);
        level.getChunkSource().addRegionTicket(PASSING, at, REACH, at, true);
    }

    /** Keeps the chunk of the place {@code x}, {@code y}, {@code z} of {@code level} loaded and going a while. */
    public static void hold(final ServerLevel level, final double x, final double y, final double z) {
        hold(level, ChunkPos.asLong(BlockPos.containing(x, y, z)));
    }

    /** Whether a structure may move to {@code x}, {@code y}, {@code z} of {@code level}: its entities tick there. */
    public static boolean ready(final ServerLevel level, final double x, final double y, final double z) {
        return level.isPositionEntityTicking(BlockPos.containing(x, y, z));
    }
}
