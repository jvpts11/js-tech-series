/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.ship;

import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.StructureFrame;
import dev.jstech.core.region.Box;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * What a ship that meets the land too fast suffers. Up to {@link #SAFE} blocks a second it touches down as a ship
 * should. Faster, the blocks it meets the land with are holed, one more for every {@link #PER_HOLE} blocks a second
 * past that: its feet, wheels and underside first, each a hole its part holds round, as a block broken aboard leaves.
 * Faster than {@link #HURTS} blocks a second its crew are hurt too, more the faster. The figures are estimates, to be
 * weighed in play.
 */
public final class ShipImpacts {

    /** The fastest a ship touches down unharmed, in blocks a second. */
    public static final double SAFE = 5;
    /** How many blocks a second past {@link #SAFE} hole one more of its blocks. */
    public static final double PER_HOLE = 5;
    /** The fastest a ship meets the land with its crew unhurt, in blocks a second. */
    public static final double HURTS = 30;
    /** How much each crew member aboard is hurt for each block a second past {@link #HURTS}, in health points. */
    public static final float CREW_PER = 0.5F;
    /* How far round the ship's box one aboard may be, in blocks, to be hurt with it. */
    private static final double ABOARD = 1;
    private static final double HALF = 0.5;

    private ShipImpacts() {
    }

    /** How many of its blocks a ship meeting the land at {@code speed} blocks a second is holed at. */
    public static int holes(final double speed) {
        return speed <= SAFE ? 0 : (int) Math.ceil((speed - SAFE) / PER_HOLE);
    }

    /** How much each of the crew of a ship meeting the land at {@code speed} blocks a second is hurt, in health. */
    public static float hurt(final double speed) {
        return speed <= HURTS ? 0 : (float) ((speed - HURTS) * CREW_PER);
    }

    /**
     * {@code ship} met the land at {@code speed} blocks a second: once its tick is done, so its blocks do not change
     * under its own step, the lowest of its blocks are holed and its crew hurt as the speed says. Nothing at a touch
     * down.
     */
    public static void met(final ServerLevel level, final MovingStructure ship, final double speed) {
        final int holes = holes(speed);
        final float hurt = hurt(speed);
        if (holes == 0 && hurt == 0) {
            return;
        }
        level.getServer().execute(() -> {
            if (ship.entity().isRemoved() || ship.level() != level) {
                return;
            }
            for (final BlockPos block : lowest(level, ship, holes)) {
                level.destroyBlock(block, false);
            }
            if (hurt > 0) {
                for (final ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class,
                        ship.worldBox().inflate(ABOARD))) {
                    player.hurt(level.damageSources().fall(), hurt);
                }
            }
        });
    }

    /* The {@code count} blocks of {@code ship}'s yard lowest in the world as it stands: what meets the land first. */
    private static List<BlockPos> lowest(final ServerLevel level, final MovingStructure ship, final int count) {
        final StructureFrame frame = ship.frame();
        final Box box = ship.bounds();
        final List<BlockPos> blocks = new ArrayList<>();
        for (final BlockPos at : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(),
                box.maxZ())) {
            if (!level.getBlockState(at).isAir()) {
                blocks.add(at.immutable());
            }
        }
        blocks.sort(Comparator.comparingDouble(at -> frame.worldY(at.getX() + HALF, at.getY() + HALF,
                at.getZ() + HALF)));
        return blocks.subList(0, Math.min(count, blocks.size()));
    }
}
