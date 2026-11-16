/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.ship;

import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.StructureFrame;
import dev.jstech.core.region.Box;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * What a ship that meets the land too fast suffers. Up to {@link #SAFE} blocks a second it touches down as a ship
 * should. Faster, the blocks it meets the land with are holed, one more for every {@link #PER_HOLE} blocks a second
 * past that: those foremost the way it went, its feet, wheels and underside coming down, its top going up into what is
 * over it, its nose flying into a hill; each a hole its part holds round, as a block broken aboard leaves, and the
 * blow is heard as the game's own explosion. Faster than {@link #HURTS} blocks a second its crew are hurt too, more
 * the faster, and the crash bursts where the ship is seen, an explosion that breaks and hurts nothing of itself. The
 * figures are estimates, to be weighed in play.
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
    /* How big a crash's burst is, as the game's explosions are sized: least, most, and more each block a second. */
    private static final double LEAST_BURST = 2;
    private static final double MOST_BURST = 8;
    private static final double BURST_PER = 0.05;
    /* A burst that only shows and sounds: it hurts no one and pushes no one. */
    private static final ExplosionDamageCalculator HARMLESS = new ExplosionDamageCalculator() {
        @Override
        public boolean shouldDamageEntity(final Explosion explosion, final Entity entity) {
            return false;
        }

        @Override
        public float getKnockbackMultiplier(final Entity entity) {
            return 0;
        }
    };

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
     * {@code ship} came down onto the land at {@code speed} blocks a second: once its tick is done, so its blocks do
     * not change under its own step, the lowest of its blocks are holed and its crew hurt as the speed says. Nothing at
     * a touch down.
     */
    public static void met(final ServerLevel level, final MovingStructure ship, final double speed) {
        met(level, ship, speed, 0, -1, 0);
    }

    /**
     * {@code ship} going the way {@code (wayX, wayY, wayZ)} in the world met the land at {@code speed} blocks a second:
     * as {@link #met(ServerLevel, MovingStructure, double)}, the blocks holed those foremost that way.
     */
    public static void met(final ServerLevel level, final MovingStructure ship, final double speed, final double wayX,
                           final double wayY, final double wayZ) {
        final int holes = holes(speed);
        final float hurt = hurt(speed);
        if (holes == 0 && hurt == 0) {
            return;
        }
        level.getServer().execute(() -> {
            if (ship.entity().isRemoved() || ship.level() != level) {
                return;
            }
            final List<BlockPos> struck = foremost(level, ship, Math.max(1, holes), wayX, wayY, wayZ);
            if (hurt > 0 && !struck.isEmpty()) {
                burst(level, struck.getFirst(), speed);
            } else if (holes > 0 && !struck.isEmpty()) {
                ShipSounds.impact(level, struck.getFirst());
            }
            for (final BlockPos block : struck.subList(0, Math.min(holes, struck.size()))) {
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

    /*
     * A crash its crew are hurt by bursts where the ship is seen meeting the land, at its block {@code at} of the yard,
     * the bigger the faster: the game's explosion, seen and heard, breaking nothing and hurting no one, the crash
     * itself doing that.
     */
    private static void burst(final ServerLevel level, final BlockPos at, final double speed) {
        final Vec3 seen = MovingStructures.inWorld(level, Vec3.atCenterOf(at));
        final float size = (float) Math.min(MOST_BURST, LEAST_BURST + (speed - HURTS) * BURST_PER);
        level.explode(null, null, HARMLESS, seen.x, seen.y, seen.z, size, false, Level.ExplosionInteraction.NONE,
                ParticleTypes.EXPLOSION_EMITTER, ParticleTypes.EXPLOSION_EMITTER, SoundEvents.GENERIC_EXPLODE);
    }

    /*
     * The {@code count} blocks of {@code ship}'s yard foremost in the world, as it stands, the way {@code (wayX, wayY,
     * wayZ)} it went: what meets the land first.
     */
    private static List<BlockPos> foremost(final ServerLevel level, final MovingStructure ship, final int count,
                                           final double wayX, final double wayY, final double wayZ) {
        final StructureFrame frame = ship.frame();
        final Box box = ship.bounds();
        final List<BlockPos> blocks = new ArrayList<>();
        for (final BlockPos at : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(),
                box.maxZ())) {
            if (!level.getBlockState(at).isAir()) {
                blocks.add(at.immutable());
            }
        }
        blocks.sort(Comparator.comparingDouble(at -> -along(frame, at, wayX, wayY, wayZ)));
        return blocks.subList(0, Math.min(count, blocks.size()));
    }

    /* How far along the way {@code (wayX, wayY, wayZ)} the middle of the yard's block {@code at} is in the world. */
    private static double along(final StructureFrame frame, final BlockPos at, final double wayX, final double wayY,
                                final double wayZ) {
        final double x = at.getX() + HALF;
        final double y = at.getY() + HALF;
        final double z = at.getZ() + HALF;
        return frame.worldX(x, y, z) * wayX + frame.worldY(x, y, z) * wayY + frame.worldZ(x, y, z) * wayZ;
    }
}
