/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock.shape;

import dev.jstech.core.multiblock.FormedPartBlock;
import dev.jstech.core.multiblock.FormedPartBlockEntity;
import dev.jstech.core.multiblock.damage.FormedDamage;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A line of sight meeting the blocks of formed structures where their models are known, on a player's game: against
 * the faces each block is drawn with, not the sixteenths its shape is made of, so a player aims at a curved wall where
 * it is and through the gap beside it. The game's own aim finds the block first, by its shape; the line is then
 * followed again from its start, each merged block met tested against its faces, every other block as the game tests
 * it. Elsewhere, and on a server, the shapes alone are aimed at.
 */
public final class FormedAim {

    private FormedAim() {
    }

    /**
     * Where the line of {@code context} meets the world, now that the game found {@code found}: a different hit when
     * {@code found} is a merged block whose faces are known, else none, keeping {@code found}.
     */
    public static @Nullable BlockHitResult clip(final Level level, final ClipContext context,
                                                final BlockHitResult found) {
        if (!level.isClientSide() || found.getType() != HitResult.Type.BLOCK
                || place(level, found.getBlockPos(), level.getBlockState(found.getBlockPos())).isEmpty()) {
            return null;
        }
        final Vec3 from = context.getFrom();
        final Vec3 to = context.getTo();
        return BlockGetter.traverseBlocks(from, to, context, (ray, pos) -> {
            final BlockState state = level.getBlockState(pos);
            final List<FormedPlace> places = places(level, pos, state);
            if (!places.isEmpty()) {
                return hit(level, places, from, to, pos);
            }
            final FluidState fluid = level.getFluidState(pos);
            final VoxelShape shape = ray.getBlockShape(state, level, pos);
            final BlockHitResult block = level.clipWithInteractionOverride(from, to, pos, shape, state);
            final BlockHitResult wet = ray.getFluidShape(fluid, level, pos).clip(from, to, pos);
            final double blockDistance = block == null ? Double.MAX_VALUE : from.distanceToSqr(block.getLocation());
            final double wetDistance = wet == null ? Double.MAX_VALUE : from.distanceToSqr(wet.getLocation());
            return blockDistance <= wetDistance ? block : wet;
        }, ray -> {
            final Vec3 back = from.subtract(to);
            return BlockHitResult.miss(to, Direction.getNearest(back.x, back.y, back.z), BlockPos.containing(to));
        });
    }

    /** Which block of its model the block at {@code pos} is, when it is a merged block whose faces are known here. */
    public static Optional<FormedPlace> place(final Level level, final BlockPos pos, final BlockState state) {
        final List<FormedPlace> places = places(level, pos, state);
        return places.isEmpty() ? Optional.empty() : Optional.of(places.getFirst());
    }

    /**
     * Every block of a formed model the block at {@code pos} is drawn with, its own and those of models reaching into
     * it, when it is a merged block whose faces are known here; none otherwise.
     */
    public static List<FormedPlace> places(final Level level, final BlockPos pos, final BlockState state) {
        if (!level.isClientSide() || !(state.getBlock() instanceof FormedPartBlock)
                || !(level.getBlockEntity(pos) instanceof FormedPartBlockEntity part)) {
            return List.of();
        }
        return part.places().stream().filter(place -> FormedShapes.triangles(level, place).isPresent()).toList();
    }

    /**
     * Where the line from {@code from} to {@code to} first meets the faces of any of the blocks {@code places} of the
     * merged block at {@code pos}; none where it meets none.
     */
    public static @Nullable BlockHitResult hit(final Level level, final List<FormedPlace> places, final Vec3 from,
                                               final Vec3 to, final BlockPos pos) {
        BlockHitResult best = null;
        for (final FormedPlace place : places) {
            final BlockHitResult one = hit(level, place, from, to, pos);
            if (one != null && (best == null
                    || from.distanceToSqr(one.getLocation()) < from.distanceToSqr(best.getLocation()))) {
                best = one;
            }
        }
        return best;
    }

    /**
     * Where the line from {@code from} to {@code to} meets the faces of the merged block {@code place} at
     * {@code pos}; none where it meets none.
     */
    public static @Nullable BlockHitResult hit(final Level level, final FormedPlace place, final Vec3 from,
                                               final Vec3 to, final BlockPos pos) {
        // A block a strike reached is aimed at by its faces as the strike left them.
        final Optional<float[]> struck = FormedDamage.triangles(level, pos, place);
        final Optional<float[]> faces = struck.isPresent() ? struck : FormedShapes.triangles(level, place);
        if (faces.isEmpty()) {
            return null;
        }
        final double[] start = new double[3];
        final double[] end = new double[3];
        CellSpace.toModel(new double[] {from.x - pos.getX(), from.y - pos.getY(), from.z - pos.getZ()}, place.turn(),
                start);
        CellSpace.toModel(new double[] {to.x - pos.getX(), to.y - pos.getY(), to.z - pos.getZ()}, place.turn(), end);
        final double[] facing = new double[3];
        final double along = MeshPick.along(faces.get(), start, end, facing);
        if (along < 0) {
            return null;
        }
        final double[] turned = new double[3];
        CellSpace.toWorld(new double[] {0.5 + facing[0], 0.5 + facing[1], 0.5 + facing[2]}, place.turn(), turned);
        final Vec3 at = from.add(to.subtract(from).scale(along));
        return new BlockHitResult(at, Direction.getNearest(turned[0] - 0.5, turned[1] - 0.5, turned[2] - 0.5), pos,
                false);
    }
}
