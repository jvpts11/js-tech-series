/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * What the tests that shoot a device close up share: where the player stands and looks, and when in the clip
 * that moves a medium the shot is taken. Two copies of this would drift, and a shot taken a tick too early or
 * late shows the wrong half of the movement.
 */
public final class DeviceCloseUp {

    /** The floppy half out of its slot, before the hand takes it. */
    public static final int FLOPPY_OUT = 9;
    /** The tray fully out with its disc on it, before the hand takes the disc. */
    public static final int TRAY_OUT = 21;
    /** Past every eject clip, when a medium taken out is no longer drawn. */
    public static final int AFTER_EJECT = 45;
    /** How far below level the player looks at a device close up, in degrees. */
    public static final float CLOSE_UP_PITCH = 28F;

    private DeviceCloseUp() {
    }

    /** The block turned to face the player, who stands south of the devices. */
    public static BlockState facingPlayer(final Block block) {
        return block.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH);
    }

    /** Stands the player two blocks in front of that device, looking down at its front. */
    public static ClientTestContext closeUp(final ClientTestContext ctx, final BlockPos device) {
        return standAt(ctx, device.south(2), Direction.NORTH);
    }

    /** Stands the player there looking that way and down, and waits for the client to have got there. */
    public static ClientTestContext standAt(final ClientTestContext ctx, final BlockPos at, final Direction facing) {
        return ctx.thenServer(0, level -> {
                    final BlockPos abs = ctx.abs(at);
                    ctx.serverPlayer().teleportTo(level, abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5,
                            facing.toYRot(), CLOSE_UP_PITCH);
                })
                .thenWaitUntil(() -> ctx.player() != null
                                && ctx.player().position().distanceTo(Vec3.atBottomCenterOf(ctx.abs(at))) < 0.1,
                        200, "the client's player to stand at " + at);
    }
}
