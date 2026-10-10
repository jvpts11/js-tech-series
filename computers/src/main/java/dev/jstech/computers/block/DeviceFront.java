/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block;

import dev.jstech.computers.os.media.EjectButton;
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
        if (hit.getDirection() != front) {
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
     * The box the eject button of a device facing {@code front} fills in its block, from its own face a sixty-fourth
     * deep, measured as {@link #point} measures the front, so an outline drawn round it stands where a click presses.
     */
    public static AABB box(final EjectButton button, final Direction front) {
        final double unit = EjectButton.FRONT;
        final double left = button.left() / unit;
        final double right = button.right() / unit;
        final double top = 1.0 - button.top() / unit;
        final double bottom = 1.0 - button.bottom() / unit;
        final Direction toRight = front.getCounterClockWise();
        final boolean rightIsPositive = toRight.getAxisDirection() == Direction.AxisDirection.POSITIVE;
        final double acrossFrom = rightIsPositive ? left : 1.0 - right;
        final double acrossTo = rightIsPositive ? right : 1.0 - left;
        // The face of the button, and a sixty-fourth behind it, counted in from the face of the block.
        final boolean frontIsPositive = front.getAxisDirection() == Direction.AxisDirection.POSITIVE;
        final double faceAt = button.depth() / unit;
        final double backAt = (button.depth() + 1) / unit;
        final double depthFrom = frontIsPositive ? 1.0 - backAt : faceAt;
        final double depthTo = frontIsPositive ? 1.0 - faceAt : backAt;
        return toRight.getAxis() == Direction.Axis.X
                ? new AABB(acrossFrom, bottom, depthFrom, acrossTo, top, depthTo)
                : new AABB(depthFrom, bottom, acrossFrom, depthTo, top, acrossTo);
    }
}
