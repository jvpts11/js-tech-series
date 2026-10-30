/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.crawler;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * Where the blocks of a crawler-transporter stand, as the approved crawler is built: its hull fifteen blocks long and
 * eleven wide, the Crawler Engine in its middle and Crawler Deck round it; under each of its four corners a truck four
 * blocks long and two wide, its tracks on the ground and its leveling jacks over them; and a cab two blocks square and
 * two tall at two opposite corners, outside the hull's ends. It reads the same turned half round, so it is found along
 * either way of its length.
 *
 * <p>Places are counted from the engine: along the crawler's length, up, and across it to the right.
 */
public final class CrawlerLayout {

    /** How far the hull reaches from the engine along the crawler's length, and across it. */
    public static final int HALF_LENGTH = 7;
    public static final int HALF_WIDTH = 5;
    /** How far below the engine the tracks stand, so how far its ground is. */
    public static final int TRACKS = 2;
    /* Where along the length the trucks start and end, and where across it the inner side of one is. */
    private static final int TRUCK_IN = 4;
    private static final int TRUCK_INSIDE = 4;
    /* Where the cabs start along the length, past the hull's ends. */
    private static final int CAB_FROM = 8;
    private static final int CAB_TO = 9;

    private CrawlerLayout() {
    }

    /** Every place of the crawler round its engine at {@code engine}, its length along {@code along}. */
    public static List<Spot> spots(final BlockPos engine, final Direction along) {
        final List<Spot> out = new ArrayList<>();
        final Direction across = along.getClockWise();
        for (int u = -HALF_LENGTH; u <= HALF_LENGTH; u++) {
            for (int v = -HALF_WIDTH; v <= HALF_WIDTH; v++) {
                final Kind deck = u == 0 && v == 0 ? Kind.ENGINE : Kind.DECK;
                out.add(new Spot(at(engine, along, across, u, 0, v), deck));
                if (truck(u, v)) {
                    out.add(new Spot(at(engine, along, across, u, -1, v), Kind.JACK));
                    out.add(new Spot(at(engine, along, across, u, -TRACKS, v), Kind.TRACK));
                }
            }
        }
        // The cabs at opposite corners: one behind the hull on its left, one before it on its right.
        for (final int side : new int[] {-1, 1}) {
            for (int u = CAB_FROM; u <= CAB_TO; u++) {
                for (int v = TRUCK_INSIDE; v <= HALF_WIDTH; v++) {
                    for (int y = -1; y <= 0; y++) {
                        out.add(new Spot(at(engine, along, across, u * side, y, v * side), Kind.CAB));
                    }
                }
            }
        }
        return out;
    }

    /** The places of the tracks of its four trucks, round its engine at {@code engine}, the ground under them. */
    public static List<BlockPos> tracks(final BlockPos engine, final Direction along) {
        return spots(engine, along).stream().filter(spot -> spot.kind() == Kind.TRACK).map(Spot::at).toList();
    }

    /**
     * The way along which the crawler round the engine at {@code engine} is built, its pieces all in place, along
     * either axis; none where it is not whole.
     */
    public static Optional<Direction> find(final Level level, final BlockPos engine) {
        for (final Direction along : new Direction[] {Direction.EAST, Direction.SOUTH}) {
            if (whole(level, engine, along)) {
                return Optional.of(along);
            }
        }
        return Optional.empty();
    }

    /** Whether every piece of the crawler round {@code engine} along {@code along} stands in its place. */
    public static boolean whole(final Level level, final BlockPos engine, final Direction along) {
        for (final Spot spot : spots(engine, along)) {
            if (!level.getBlockState(spot.at()).is(spot.kind().block())) {
                return false;
            }
        }
        return true;
    }

    /* Whether the place {@code u} along and {@code v} across is under a truck. */
    private static boolean truck(final int u, final int v) {
        return Math.abs(u) >= TRUCK_IN && Math.abs(v) >= TRUCK_INSIDE;
    }

    private static BlockPos at(final BlockPos engine, final Direction along, final Direction across, final int u,
                               final int y, final int v) {
        return engine.offset(along.getStepX() * u + across.getStepX() * v, y,
                along.getStepZ() * u + across.getStepZ() * v);
    }

    /** A piece of the crawler and where it stands. */
    public record Spot(BlockPos at, Kind kind) {
    }

    /** The kinds of piece a crawler is built of. */
    public enum Kind {
        TRACK, JACK, DECK, CAB, ENGINE;

        /** The block of this kind. */
        public Block block() {
            return switch (this) {
                case TRACK -> CrawlerBlocks.CRAWLER_TRACK.get();
                case JACK -> CrawlerBlocks.LEVELING_JACK.get();
                case DECK -> CrawlerBlocks.CRAWLER_DECK.get();
                case CAB -> CrawlerBlocks.CRAWLER_CAB.get();
                case ENGINE -> CrawlerBlocks.CRAWLER_ENGINE.get();
            };
        }
    }
}
