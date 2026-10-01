/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.connect;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelProperty;

/**
 * Connected textures, the part that knows the world: for each face of a block, which of the eight blocks around it
 * in the plane of that face it joins, so the face's texture can run on into theirs. A model that draws connected
 * textures reads the masks from its model data under {@link #MASKS}; how each mask is drawn is the model's.
 *
 * <p>A face's plane has an up and a right as somebody looking at the face from outside sees them: on a side face, up
 * is up and right is to the right; on the top and the bottom, north is up. The eight neighbours are counted clockwise
 * from up: up, up-right, right, down-right, down, down-left, left, up-left, one bit each, from the lowest.
 */
public final class ConnectedFaces {

    /** Where a model finds the masks of the block it draws. */
    public static final ModelProperty<FaceMasks> MASKS = new ModelProperty<>();

    public static final int UP = 1;
    public static final int UP_RIGHT = 1 << 1;
    public static final int RIGHT = 1 << 2;
    public static final int DOWN_RIGHT = 1 << 3;
    public static final int DOWN = 1 << 4;
    public static final int DOWN_LEFT = 1 << 5;
    public static final int LEFT = 1 << 6;
    public static final int UP_LEFT = 1 << 7;

    private ConnectedFaces() {
    }

    /** The masks of every face of the block in {@code state} at {@code pos}. */
    public static FaceMasks of(final BlockGetter level, final BlockPos pos, final BlockState state,
                               final IJoinRule joins) {
        long bits = 0L;
        for (final Direction face : Direction.values()) {
            bits |= (long) mask(level, pos, state, face, joins) << (face.get3DDataValue() * Byte.SIZE);
        }
        return new FaceMasks(bits);
    }

    /** Which of the eight blocks around {@code pos} in the plane of {@code face} the block joins. */
    public static int mask(final BlockGetter level, final BlockPos pos, final BlockState state, final Direction face,
                           final IJoinRule joins) {
        final Direction up = up(face);
        final Direction right = right(face);
        int mask = 0;
        mask |= joined(level, pos.relative(up), state, joins) ? UP : 0;
        mask |= joined(level, pos.relative(up).relative(right), state, joins) ? UP_RIGHT : 0;
        mask |= joined(level, pos.relative(right), state, joins) ? RIGHT : 0;
        mask |= joined(level, pos.relative(up.getOpposite()).relative(right), state, joins) ? DOWN_RIGHT : 0;
        mask |= joined(level, pos.relative(up.getOpposite()), state, joins) ? DOWN : 0;
        mask |= joined(level, pos.relative(up.getOpposite()).relative(right.getOpposite()), state, joins)
                ? DOWN_LEFT : 0;
        mask |= joined(level, pos.relative(right.getOpposite()), state, joins) ? LEFT : 0;
        mask |= joined(level, pos.relative(up).relative(right.getOpposite()), state, joins) ? UP_LEFT : 0;
        return mask;
    }

    /** What is up on {@code face}, seen from outside. */
    public static Direction up(final Direction face) {
        return face.getAxis() == Direction.Axis.Y ? Direction.NORTH : Direction.UP;
    }

    /** What is right on {@code face}, seen from outside. */
    public static Direction right(final Direction face) {
        return switch (face) {
            case UP -> Direction.EAST;
            case DOWN -> Direction.WEST;
            default -> face.getCounterClockWise();
        };
    }

    private static boolean joined(final BlockGetter level, final BlockPos at, final BlockState state,
                                  final IJoinRule joins) {
        return joins.joins(state, level.getBlockState(at));
    }

    /**
     * The masks of a block's six faces, a byte each, in the order of the faces' data values.
     *
     * @param bits the six masks
     */
    public record FaceMasks(long bits) {

        /** The mask of {@code face}. */
        public int of(final Direction face) {
            return (int) (this.bits >>> (face.get3DDataValue() * Byte.SIZE)) & 0xFF;
        }
    }
}
