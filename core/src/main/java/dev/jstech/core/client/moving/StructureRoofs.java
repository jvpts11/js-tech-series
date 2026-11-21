/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.moving;

import dev.jstech.core.moving.IStructureBody;
import dev.jstech.core.moving.LevelMovingStructures;
import dev.jstech.core.moving.StructureRays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.ApiStatus;

/**
 * Where rain and snow stop falling in a column, on the player's game: on the world's blocks, as the game has it, or on
 * the highest block of a moving structure over them, so none falls inside a ship's hull, on its deck under its roof,
 * or under it, and its splashes land on its roof. The world's heights know nothing of a structure's blocks, which are
 * kept in its yard.
 */
@ApiStatus.Internal
public final class StructureRoofs {

    private StructureRoofs() {
    }

    /**
     * The height rain and snow stop at in the column ({@code x}, {@code z}) of {@code level}, the world's blocks
     * stopping them at {@code world}: the top of the highest block of a moving structure over that, if any.
     */
    public static int rainStopsAt(final Level level, final int x, final int z, final int world) {
        if (!LevelMovingStructures.anyLoaded()) {
            return world;
        }
        final LevelMovingStructures all = LevelMovingStructures.of(level);
        if (all.isEmpty()) {
            return world;
        }
        double top = world;
        for (final IStructureBody body : all.near(new AABB(x, world, z, x + 1, level.getMaxBuildHeight(), z + 1))) {
            top = Math.max(top, body.worldBox().maxY + 1);
        }
        if (top <= world) {
            return world;
        }
        final Vec3 from = new Vec3(x + 0.5, top, z + 0.5);
        final Vec3 to = new Vec3(x + 0.5, world, z + 0.5);
        final BlockHitResult hit = StructureRays.clip(level, new ClipContext(from, to, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, CollisionContext.empty()), BlockHitResult.miss(to, Direction.UP,
                BlockPos.containing(to)));
        return hit == null ? world : Math.max(world, Mth.ceil(hit.getLocation().y));
    }

    /** {@code pos}, the world's top of its column, raised to where rain stops on a moving structure over it. */
    public static BlockPos rainStopsAt(final Level level, final BlockPos pos) {
        final int stops = rainStopsAt(level, pos.getX(), pos.getZ(), pos.getY());
        return stops == pos.getY() ? pos : new BlockPos(pos.getX(), stops, pos.getZ());
    }
}
