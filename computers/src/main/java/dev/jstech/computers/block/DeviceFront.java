/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block;

import dev.jstech.computers.hardware.FrontArea;
import dev.jstech.computers.os.media.EjectButton;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The front of a device, the face it turns to the player who placed it: where on it a look lands, measured the way the
 * device's model is drawn, so a part of the front, such as the eject button, is pressed on its own.
 */
public final class DeviceFront {

    /** How far past a block's own space a point still counts as on it, for the rounding of a look's landing point. */
    private static final double HAIR = 1.0E-4;

    private DeviceFront() {
    }

    /** Whether the look from {@code eye} that hit the device at {@code pos} lands on its eject button. */
    public static boolean presses(final EjectButton button, final BlockState state, final BlockPos pos,
                                  final BlockHitResult hit, final Vec3 eye) {
        final double[] at = point(state.getValue(HorizontalDirectionalBlock.FACING), pos, hit, eye, button.depth());
        return at != null && button.pressedAt(at[0], at[1]);
    }

    /**
     * Where the look from {@code eye} through {@code hit} meets the front of a device facing {@code front}, on the plane
     * {@code depth} sixty-fourths behind its face, in sixty-fourths from the front's top left as it is seen; null when
     * the look landed on another face. The look is followed on to that plane, so a part set back in the front is found
     * where the player sees it even from above or from the side.
     */
    @Nullable
    public static double[] point(final Direction front, final BlockPos pos, final BlockHitResult hit, final Vec3 eye,
                                 final int depth) {
        // A look that landed out of the block landed on a part standing out of it, which no point of the face is.
        if (hit.getDirection() != front || outside(pos, hit.getLocation())) {
            return null;
        }
        final Vec3 onFace = hit.getLocation();
        final Vec3 look = onFace.subtract(eye);
        // How fast the look goes into the block, against the way the front faces.
        final double inward = -(look.x * front.getStepX() + look.z * front.getStepZ());
        final Vec3 at = inward <= 0 ? onFace : onFace.add(look.scale(depth / (double) EjectButton.FRONT / inward));
        final double lx = at.x - pos.getX();
        final double lz = at.z - pos.getZ();
        // Across runs toward the device's right as the player facing its front sees it.
        final double across = switch (front.getCounterClockWise()) {
            case EAST -> lx;
            case WEST -> 1.0 - lx;
            case SOUTH -> lz;
            default -> 1.0 - lz;
        };
        return new double[] {across * EjectButton.FRONT, (1.0 - (at.y - pos.getY())) * EjectButton.FRONT};
    }

    /**
     * Whether a point lies out of the block at {@code pos}, by more than a hair: where a look lands on a part standing
     * out of the block rather than on the block.
     */
    public static boolean outside(final BlockPos pos, final Vec3 location) {
        final double x = location.x - pos.getX();
        final double y = location.y - pos.getY();
        final double z = location.z - pos.getZ();
        return x < -HAIR || y < -HAIR || z < -HAIR || x > 1.0 + HAIR || y > 1.0 + HAIR || z > 1.0 + HAIR;
    }

    /**
     * The one of {@code parts}, boxes in the block at {@code pos}, that a look landing at {@code location} out of the
     * block landed on: its index, or -1 when the look landed on the block itself or on none of them.
     */
    public static int partAt(final List<AABB> parts, final BlockPos pos, final Vec3 location) {
        if (!outside(pos, location)) {
            return -1;
        }
        final Vec3 local = location.subtract(pos.getX(), pos.getY(), pos.getZ());
        for (int i = 0; i < parts.size(); i++) {
            if (parts.get(i).inflate(HAIR).contains(local)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * The box the eject button of a device facing {@code front} fills in its block, from its own face a sixty-fourth
     * deep, measured as {@link #point} measures the front, so an outline drawn round it stands where a click presses.
     */
    public static AABB box(final EjectButton button, final Direction front) {
        // The face of the button, and a sixty-fourth behind it.
        return box(new FrontArea(button.left(), button.top(), button.right(), button.bottom()), button.depth(),
                button.depth() + 1, front);
    }

    /**
     * The box a part of the front of a device facing {@code front} fills in its block, from {@code near} to
     * {@code far} sixty-fourths behind the block's face (less than nothing for a part standing out of it), measured
     * as {@link #point} measures the front, so an outline drawn round it stands where a click works it.
     */
    public static AABB box(final FrontArea part, final int near, final int far, final Direction front) {
        final double unit = EjectButton.FRONT;
        final double left = part.left() / unit;
        final double right = part.right() / unit;
        final double top = 1.0 - part.top() / unit;
        final double bottom = 1.0 - part.bottom() / unit;
        final Direction toRight = front.getCounterClockWise();
        final boolean rightIsPositive = toRight.getAxisDirection() == Direction.AxisDirection.POSITIVE;
        final double acrossFrom = rightIsPositive ? left : 1.0 - right;
        final double acrossTo = rightIsPositive ? right : 1.0 - left;
        // Counted in from the face of the block, toward its back.
        final boolean frontIsPositive = front.getAxisDirection() == Direction.AxisDirection.POSITIVE;
        final double faceAt = near / unit;
        final double backAt = far / unit;
        final double depthFrom = frontIsPositive ? 1.0 - backAt : faceAt;
        final double depthTo = frontIsPositive ? 1.0 - faceAt : backAt;
        return toRight.getAxis() == Direction.Axis.X
                ? new AABB(acrossFrom, bottom, depthFrom, acrossTo, top, depthTo)
                : new AABB(depthFrom, bottom, acrossFrom, depthTo, top, acrossTo);
    }
}
