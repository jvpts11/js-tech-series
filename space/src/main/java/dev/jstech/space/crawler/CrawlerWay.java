/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.crawler;

import dev.jstech.core.moving.StructureBalance;
import dev.jstech.core.multiblock.FormedPartBlockEntity;
import dev.jstech.core.text.Text;
import dev.jstech.space.flight.FlightComputerBlockEntity;
import dev.jstech.space.hangar.HangarBlocks;
import dev.jstech.space.hangar.HangarPieceBlock;
import dev.jstech.space.launcher.LauncherBlocks;
import dev.jstech.space.launcher.LauncherDeckBlockEntity;
import dev.jstech.space.launcher.LauncherLayout;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The way a crawler-transporter goes and what it takes along it: its crawlerway, a lane of gravel under each side's
 * trucks, followed along its length to the end farther from it; the mobile launcher standing on it, if one stands
 * centred over it with the launcher's length along its own, with the tower and the rocket on it; and whether the way
 * is clear for all of it, the bay doors it passes through opened for it.
 */
public final class CrawlerWay {

    /** The longest crawlerway followed, in blocks. */
    public static final int LONGEST = 1024;
    /* How far round the crawler a launcher over it is looked for, and how far out from its deck its blocks are. */
    private static final int LOOK_ROUND = 16;
    private static final int REACH = 64;
    /* The most blocks of bay door opened together. */
    private static final int LARGEST_DOOR = 4096;

    private CrawlerWay() {
    }

    /**
     * Which way along {@code along} the crawler round the engine at {@code engine} goes, and how many blocks: to the
     * end of its crawlerway farther from it, gravel under every track all the way; none where it has none.
     */
    public static Optional<Route> route(final ServerLevel level, final BlockPos engine, final Direction along) {
        final List<BlockPos> tracks = CrawlerLayout.tracks(engine, along);
        Route best = null;
        for (final Direction way : new Direction[] {along, along.getOpposite()}) {
            int steps = 0;
            while (steps < LONGEST && laneUnder(level, tracks, way, steps + 1)) {
                steps++;
            }
            if (steps > 0 && (best == null || steps > best.steps())) {
                best = new Route(way, steps);
            }
        }
        return Optional.ofNullable(best);
    }

    /**
     * The mobile launcher standing centred over the crawler round the engine at {@code engine}, its length along
     * {@code along}, on the crawler's deck: its blocks, the tower's and the rocket's on it; none where none stands
     * there. Refused, saying why, where it is more than the crawler can carry.
     */
    public static Optional<Load> launcherOver(final ServerLevel level, final BlockPos engine, final Direction along)
            throws Refusal {
        final int foot = engine.getY() + 1 + LauncherLayout.DECK;
        final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int dx = -LOOK_ROUND; dx <= LOOK_ROUND; dx++) {
            for (int dz = -LOOK_ROUND; dz <= LOOK_ROUND; dz++) {
                at.set(engine.getX() + dx, foot, engine.getZ() + dz);
                if (level.getBlockEntity(at) instanceof LauncherDeckBlockEntity arm && arm.deck().isPresent()
                        && centred(arm.getBlockPos(), arm.deck().get(), engine, along)) {
                    return Optional.of(load(level, arm.getBlockPos(), engine));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * The first place in the way of {@code moving} going {@code steps} blocks {@code way}, none where it is clear;
     * the bay doors in its way are put in {@code doors}, to be opened for it.
     */
    public static Optional<BlockPos> blocked(final ServerLevel level, final Collection<BlockPos> moving,
                                             final Direction way, final int steps, final Set<BlockPos> doors) {
        // Along each line the move runs on, everything from the first of its blocks to past its last must be free.
        final Map<Long, int[]> lines = new HashMap<>();
        for (final BlockPos block : moving) {
            final int along = along(block, way);
            lines.merge(line(block, way), new int[] {along, along}, (a, b) -> new int[] {Math.min(a[0], b[0]),
                    Math.max(a[1], b[1])});
        }
        final Set<BlockPos> own = new HashSet<>(moving);
        for (final BlockPos block : moving) {
            final int[] span = lines.remove(line(block, way));
            if (span == null) {
                continue;
            }
            for (int k = span[0]; k <= span[1] + steps; k++) {
                final BlockPos at = block.relative(way, k - along(block, way));
                if (own.contains(at)) {
                    continue;
                }
                final BlockState there = level.getBlockState(at);
                if (there.is(HangarBlocks.BAY_DOOR.get())) {
                    doors.add(at);
                } else if (!there.isAir() && !there.canBeReplaced()) {
                    return Optional.of(at);
                }
            }
        }
        return Optional.empty();
    }

    /** The whole of each bay door a part of which is at {@code parts}: every door block joined to them. */
    public static Set<BlockPos> wholeDoors(final ServerLevel level, final Set<BlockPos> parts) {
        final Set<BlockPos> doors = new HashSet<>(parts);
        final ArrayDeque<BlockPos> open = new ArrayDeque<>(parts);
        while (!open.isEmpty() && doors.size() < LARGEST_DOOR) {
            final BlockPos at = open.poll();
            for (final Direction side : Direction.values()) {
                final BlockPos next = at.relative(side);
                if (!doors.contains(next) && level.getBlockState(next).is(HangarBlocks.BAY_DOOR.get())) {
                    doors.add(next);
                    open.add(next);
                }
            }
        }
        return doors;
    }

    /* Whether every track of the crawler, {@code step} blocks {@code way}, has gravel under it. */
    private static boolean laneUnder(final ServerLevel level, final List<BlockPos> tracks, final Direction way,
                                     final int step) {
        for (final BlockPos track : tracks) {
            final BlockPos ground = track.relative(way, step).below();
            if (level.getChunkSource().getChunkNow(ground.getX() >> 4, ground.getZ() >> 4) == null
                    || !level.getBlockState(ground).is(Blocks.GRAVEL)) {
                return false;
            }
        }
        return true;
    }

    /*
     * Whether the deck {@code deck}, as its arm at {@code arm} measured it, stands centred over the engine at
     * {@code engine}: its length along the crawler's, its middle over the engine to half a block.
     */
    private static boolean centred(final BlockPos arm, final LauncherDeckBlockEntity.Deck deck, final BlockPos engine,
                                   final Direction along) {
        if (deck.side().getAxis() != along.getAxis()) {
            return false;
        }
        final BlockPos axis = arm.offset(deck.axis());
        final double middle = (deck.front() - deck.back()) / 2.0;
        final double x = axis.getX() + deck.side().getStepX() * middle;
        final double z = axis.getZ() + deck.side().getStepZ() * middle;
        return Math.abs(x - engine.getX()) <= 0.5 && Math.abs(z - engine.getZ()) <= 0.5;
    }

    /*
     * The launcher whose deck the arm at {@code arm} draws: every block joined to it from its deck's underside up,
     * but the pedestals it stands on, the crawler under it and the hangar round it.
     */
    private static Load load(final ServerLevel level, final BlockPos arm, final BlockPos engine) throws Refusal {
        final int bottom = engine.getY() + 1;
        final Set<BlockPos> blocks = new HashSet<>();
        final ArrayDeque<BlockPos> open = new ArrayDeque<>();
        open.add(arm.immutable());
        blocks.add(arm.immutable());
        BlockPos terminal = null;
        BlockPos computer = null;
        while (!open.isEmpty()) {
            final BlockPos at = open.poll();
            if (level.getBlockEntity(at) instanceof FormedPartBlockEntity part && part.terminal() != null) {
                terminal = part.terminal();
            }
            if (level.getBlockEntity(at) instanceof FlightComputerBlockEntity) {
                computer = at;
            }
            for (final Direction side : Direction.values()) {
                final BlockPos next = at.relative(side);
                if (next.getY() < bottom || Math.abs(next.getX() - arm.getX()) > REACH
                        || Math.abs(next.getZ() - arm.getZ()) > REACH || blocks.contains(next)
                        || !carried(level.getBlockState(next))) {
                    continue;
                }
                if (blocks.size() >= StructureBalance.mostBlocks()) {
                    throw new Refusal(CrawlerTexts.TOO_BIG.text());
                }
                blocks.add(next);
                open.add(next);
            }
        }
        return new Load(List.copyOf(blocks), Optional.ofNullable(terminal), Optional.ofNullable(computer));
    }

    /* Whether a block standing so is carried with a launcher: anything but air, its pedestals, crawlers and hangars. */
    private static boolean carried(final BlockState state) {
        return !state.isAir() && !state.is(LauncherBlocks.PAD_PEDESTAL.get())
                && !(state.getBlock() instanceof CrawlerPieceBlock) && !(state.getBlock() instanceof HangarPieceBlock)
                && !state.is(HangarBlocks.TERMINAL.get()) && !state.is(HangarBlocks.ASSEMBLY_CONSOLE.get())
                && !state.is(HangarBlocks.ITEM_PORT.get());
    }

    /* Where {@code at} is along {@code way}. */
    private static int along(final BlockPos at, final Direction way) {
        return at.getX() * way.getStepX() + at.getY() * way.getStepY() + at.getZ() * way.getStepZ();
    }

    /* The line along {@code way} that {@code at} is on, as the place it crosses the plane through the origin. */
    private static long line(final BlockPos at, final Direction way) {
        return at.relative(way, -along(at, way)).asLong();
    }

    /**
     * Where a crawler goes: which way, and how many blocks.
     *
     * @param way   the way it goes
     * @param steps how many blocks
     */
    public record Route(Direction way, int steps) {
    }

    /**
     * A launcher a crawler carries.
     *
     * @param blocks   its blocks, its tower's and its rocket's
     * @param terminal the hangar terminal that built its rocket and still keeps its plan, if one does
     * @param computer the rocket's flight computer, if it has one
     */
    public record Load(List<BlockPos> blocks, Optional<BlockPos> terminal, Optional<BlockPos> computer) {
    }

    /** Why a crawler cannot go. */
    public static final class Refusal extends Exception {

        private static final long serialVersionUID = 1L;
        private final transient Text why;

        public Refusal(final Text why) {
            super(why.toString());
            this.why = why;
        }

        /** What the player is told. */
        public Text why() {
            return this.why;
        }
    }
}
