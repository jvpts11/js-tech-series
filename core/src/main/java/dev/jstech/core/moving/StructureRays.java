/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import dev.jstech.core.multiblock.shape.FormedAim;
import dev.jstech.core.multiblock.shape.FormedPlace;
import dev.jstech.core.util.Loaded;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Rays against the blocks of moving structures: an arrow, a mob's look, a bucket, a player's aim. A ray of the world
 * is followed through each structure near it in the structure's own places, block by block as the game follows it,
 * and a block of a structure hit nearer than the world's is what the ray hits. The hit gives the block's place in the
 * yard, so whatever acts on the block acts on the structure's real block, and the place it was hit at in the world,
 * so whatever goes on from there, an arrow stuck in it, goes on in the world.
 */
public final class StructureRays {

    private StructureRays() {
    }

    /**
     * The block of a moving structure the ray of {@code context} hits nearer than {@code world}, the hit of the
     * world's blocks; null when it hits none nearer.
     */
    public static @Nullable BlockHitResult clip(final Level level, final ClipContext context,
                                                final BlockHitResult world) {
        if (!LevelMovingStructures.anyLoaded()) {
            return null;
        }
        final LevelMovingStructures all = LevelMovingStructures.of(level);
        if (all.isEmpty()) {
            return null;
        }
        final Vec3 from = context.getFrom();
        final Vec3 to = context.getTo();
        final List<IStructureBody> near = all.near(new AABB(from, to).inflate(1));
        if (near.isEmpty()) {
            return null;
        }
        double nearest = world.getType() == HitResult.Type.MISS ? Double.MAX_VALUE
                : from.distanceToSqr(world.getLocation());
        BlockHitResult best = null;
        for (final IStructureBody body : near) {
            if (!body.ready()) {
                continue;
            }
            final BlockHitResult hit = clipYard(level, context, body.frame());
            if (hit == null) {
                continue;
            }
            final StructureFrame frame = body.frame();
            final Vec3 at = hit.getLocation();
            final Vec3 inWorld = new Vec3(frame.worldX(at.x, at.y, at.z), frame.worldY(at.x, at.y, at.z),
                    frame.worldZ(at.x, at.y, at.z));
            final double distance = from.distanceToSqr(inWorld);
            if (distance < nearest) {
                nearest = distance;
                best = new BlockHitResult(inWorld, hit.getDirection(), hit.getBlockPos(), hit.isInside());
            }
        }
        return best;
    }

    /**
     * The block of a structure hit by the ray of {@code context}, wholly in the yard's places: where a player aims,
     * the place they click must be the block's own, as the server checks it.
     */
    public static @Nullable BlockHitResult clipYard(final Level level, final ClipContext context,
                                                    final StructureFrame frame) {
        final Vec3 from = context.getFrom();
        final Vec3 to = context.getTo();
        final Vec3 yardFrom = new Vec3(frame.yardX(from.x, from.y, from.z), frame.yardY(from.x, from.y, from.z),
                frame.yardZ(from.x, from.y, from.z));
        final Vec3 yardTo = new Vec3(frame.yardX(to.x, to.y, to.z), frame.yardY(to.x, to.y, to.z),
                frame.yardZ(to.x, to.y, to.z));
        final BlockHitResult hit = BlockGetter.traverseBlocks(yardFrom, yardTo, context,
                (ray, pos) -> hitIn(level, ray, yardFrom, yardTo, pos), ray -> null);
        return hit == null || hit.getType() == HitResult.Type.MISS ? null : hit;
    }

    /* Whether the ray meets the block at {@code pos} of the yard, or its fluid, and where: the nearer of the two. */
    private static @Nullable BlockHitResult hitIn(final Level level, final ClipContext ray, final Vec3 from,
                                                  final Vec3 to, final BlockPos pos) {
        if (!Loaded.chunk(level, pos.getX() >> 4, pos.getZ() >> 4)) {
            return null;
        }
        final BlockState state = level.getBlockState(pos);
        final FluidState fluid = level.getFluidState(pos);
        if (state.isAir() && fluid.isEmpty()) {
            return null;
        }
        // A block of a formed structure aboard, whose model is known here, is aimed at by the model's faces.
        final Optional<FormedPlace> formed = FormedAim.place(level, pos, state);
        if (formed.isPresent()) {
            return FormedAim.hit(level, formed.get(), from, to, pos);
        }
        final VoxelShape shape = ray.getBlockShape(state, level, pos);
        final BlockHitResult block = level.clipWithInteractionOverride(from, to, pos, shape, state);
        final VoxelShape fluidShape = ray.getFluidShape(fluid, level, pos);
        final BlockHitResult wet = fluidShape.clip(from, to, pos);
        final double blockDistance = block == null ? Double.MAX_VALUE : from.distanceToSqr(block.getLocation());
        final double wetDistance = wet == null ? Double.MAX_VALUE : from.distanceToSqr(wet.getLocation());
        return blockDistance <= wetDistance ? block : wet;
    }
}
