/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import dev.jstech.core.dimension.DimensionRulesData;
import dev.jstech.core.region.Box;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * The pull of a moving structure that pulls what stands in it towards its own floor, however it is turned
 * ({@link MovingStructure#deckGravity(boolean)}): a ship's artificial gravity, keeping its floor down for its crew
 * through a dive, a roll or a loop, as a ship in a game of space does.
 *
 * <p>Its field is the box of its blocks and {@link #REACH} round it, so one standing on its roof is in it too. In
 * the field the floor is down, at the pull of the dimension it is in, a sixth on the Moon; where that dimension pulls
 * nothing, as space does, at the usual pull of the ground. Out of it the world's own rules hold.
 */
@ApiStatus.Experimental
public final class DeckGravity {

    /** How far round a structure's blocks its pull reaches, in blocks. */
    public static final double REACH = 1.0;
    /* Under this share of the usual pull a dimension counts as weightless, and a deck pulls at the usual pull. */
    private static final double WEIGHTLESS = 0.01;

    private DeckGravity() {
    }

    /**
     * How hard a structure's deck pulls in {@code level}, as a share of the usual pull at the ground: the dimension's
     * own pull, or the usual one where it pulls nothing.
     */
    public static double strength(final Level level) {
        final double pull = DimensionRulesData.of(level).gravity();
        return pull < WEIGHTLESS ? 1.0 : pull;
    }

    /** The structure whose deck pulls {@code entity}, the one whose field holds where it stands; null for none. */
    public static @Nullable IStructureBody fieldOf(final Entity entity) {
        return fieldAt(entity.level(), entity.position());
    }

    /**
     * The structure whose deck pulls at {@code at} of {@code level}: of those with a deck's pull whose field holds the
     * place, the one it lies deepest in; null for none.
     */
    public static @Nullable IStructureBody fieldAt(final Level level, final Vec3 at) {
        IStructureBody found = null;
        double nearest = Double.MAX_VALUE;
        for (final IStructureBody body : LevelMovingStructures.of(level).near(new AABB(at, at).inflate(REACH))) {
            if (!body.deckGravity() || !body.ready()) {
                continue;
            }
            final double out = outside(body, at);
            if (out <= REACH && out < nearest) {
                found = body;
                nearest = out;
            }
        }
        return found;
    }

    /** The way {@code body}'s deck pulls, in the world: its floor's down, however it is turned, of length one. */
    public static Vec3 down(final IStructureBody body) {
        final StructureFrame frame = body.frame();
        return new Vec3(frame.turnX(0, -1, 0), frame.turnY(0, -1, 0), frame.turnZ(0, -1, 0));
    }

    /* How far {@code at} lies out of the box of {@code body}'s blocks, in blocks of its yard; 0 inside it. */
    private static double outside(final IStructureBody body, final Vec3 at) {
        final StructureFrame frame = body.frame();
        final Box box = body.bounds();
        final double x = frame.yardX(at.x, at.y, at.z);
        final double y = frame.yardY(at.x, at.y, at.z);
        final double z = frame.yardZ(at.x, at.y, at.z);
        return Math.max(Math.max(beyond(x, box.minX(), box.maxX() + 1), beyond(y, box.minY(), box.maxY() + 1)),
                beyond(z, box.minZ(), box.maxZ() + 1));
    }

    /* How far {@code value} lies beyond {@code from} to {@code to}; 0 between them. */
    private static double beyond(final double value, final double from, final double to) {
        return Math.max(0, Math.max(from - value, value - to));
    }
}
