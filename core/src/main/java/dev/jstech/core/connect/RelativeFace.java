/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.connect;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * A face of a block named from the block's own point of view, whichever way it was placed: its front (the way it
 * faces), its back, its left and right as the block itself has them (facing out of its front), its top and its
 * bottom.
 *
 * <p>A block's facing is read from the game's own facing properties: the horizontal one most blocks have, or the full
 * one a block that can face up or down has. A block facing up or down keeps north as its top, and its left and right
 * follow from its top and its front.
 */
public enum RelativeFace {

    FRONT,
    BACK,
    LEFT,
    RIGHT,
    TOP,
    BOTTOM;

    /** The way {@code state} faces, when it has a facing at all. */
    public static Optional<Direction> facingOf(final BlockState state) {
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return Optional.of(state.getValue(BlockStateProperties.HORIZONTAL_FACING));
        }
        if (state.hasProperty(BlockStateProperties.FACING)) {
            return Optional.of(state.getValue(BlockStateProperties.FACING));
        }
        return Optional.empty();
    }

    /** Which face of a block facing {@code facing} the world's {@code face} is. */
    public static RelativeFace of(final Direction facing, final Direction face) {
        for (final RelativeFace relative : values()) {
            if (relative.toWorld(facing) == face) {
                return relative;
            }
        }
        throw new IllegalStateException("no face of a block facing " + facing + " is " + face);
    }

    /** Which face of the world this face of a block facing {@code facing} is. */
    public Direction toWorld(final Direction facing) {
        final Direction top = facing.getAxis() == Direction.Axis.Y ? Direction.NORTH : Direction.UP;
        return switch (this) {
            case FRONT -> facing;
            case BACK -> facing.getOpposite();
            case TOP -> top;
            case BOTTOM -> top.getOpposite();
            case LEFT -> left(facing, top);
            case RIGHT -> left(facing, top).getOpposite();
        };
    }

    /*
     * The left of a block facing {@code facing} with {@code top} as its top: the cross product of its top and its
     * front, which for a block facing north with up on top is west.
     */
    private static Direction left(final Direction facing, final Direction top) {
        final Vec3i front = facing.getNormal();
        final Vec3i up = top.getNormal();
        return Objects.requireNonNull(Direction.fromDelta(
                up.getY() * front.getZ() - up.getZ() * front.getY(),
                up.getZ() * front.getX() - up.getX() * front.getZ(),
                up.getX() * front.getY() - up.getY() * front.getX()), "a face across from two others");
    }
}
