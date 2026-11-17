/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.ship;

import dev.jstech.core.moving.MovingStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A starship crash its crew are hurt by, seen and heard: the game's explosion where the ship is seen meeting the land,
 * the bigger the faster, breaking nothing and hurting no one, the crash itself doing that ({@link ShipImpacts}). Kept
 * apart from the crash's figures, which are read where the game is not running.
 */
public final class ShipBursts {

    /* How big a crash's burst is, as the game's explosions are sized: least, most, and more each block a second. */
    private static final double LEAST_BURST = 2;
    private static final double MOST_BURST = 8;
    private static final double BURST_PER = 0.05;

    private ShipBursts() {
    }

    /** A crash at {@code speed} blocks a second bursts where the block {@code at} of a ship's yard is seen. */
    public static void burst(final ServerLevel level, final BlockPos at, final double speed) {
        final Vec3 seen = MovingStructures.inWorld(level, Vec3.atCenterOf(at));
        final float size = (float) Math.min(MOST_BURST, LEAST_BURST + (speed - ShipImpacts.HURTS) * BURST_PER);
        level.explode(null, null, harmless(), seen.x, seen.y, seen.z, size, false, Level.ExplosionInteraction.NONE,
                ParticleTypes.EXPLOSION_EMITTER, ParticleTypes.EXPLOSION_EMITTER, SoundEvents.GENERIC_EXPLODE);
    }

    /* A burst that only shows and sounds, hurting and pushing no one. */
    private static ExplosionDamageCalculator harmless() {
        return new ExplosionDamageCalculator() {
            @Override
            public boolean shouldDamageEntity(final Explosion explosion, final Entity entity) {
                return false;
            }

            @Override
            public float getKnockbackMultiplier(final Entity entity) {
                return 0;
            }
        };
    }
}
