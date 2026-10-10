/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.computers.os.media.EjectButton;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;

/**
 * What the tests that shoot a device close up share: where the player stands and looks, and when in the clip
 * that moves a medium the shot is taken. Two copies of this would drift, and a shot taken a tick too early or
 * late shows the wrong half of the movement.
 */
public final class DeviceCloseUp {

    /** The floppy half out of its slot, before the hand takes it. */
    public static final int FLOPPY_OUT = 9;
    /** The tray fully out, or fully in, after its button was pressed. */
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

    /**
     * Stands the player two blocks in front of a device that faces them and turns them to look straight at the
     * middle of its eject button, then waits until the crosshair is on the device, so a shot shows the button's
     * outline and {@link #clickCrosshair} presses it as the player's own click would.
     */
    public static ClientTestContext aimAtEjectButton(final ClientTestContext ctx, final BlockPos device,
                                                     final EjectButton button) {
        final BlockPos at = device.south(2);
        final float[] turned = new float[2];
        return ctx.thenServer(0, level -> {
                    final BlockPos stand = ctx.abs(at);
                    final Vec3 eye = Vec3.atBottomCenterOf(stand).add(0, ctx.serverPlayer().getEyeHeight(), 0);
                    final Vec3 look = buttonCentre(ctx.abs(device), button).subtract(eye);
                    turned[0] = (float) (Mth.atan2(look.z, look.x) * Mth.RAD_TO_DEG) - 90F;
                    turned[1] = (float) -(Mth.atan2(look.y, look.horizontalDistance()) * Mth.RAD_TO_DEG);
                    ctx.serverPlayer().teleportTo(level, stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5,
                            turned[0], turned[1]);
                })
                .thenWaitUntil(() -> ctx.player() != null
                                && Math.abs(Mth.wrapDegrees(ctx.player().getYRot() - turned[0])) < 0.5F
                                && Math.abs(ctx.player().getXRot() - turned[1]) < 0.5F
                                && ctx.mc().hitResult instanceof BlockHitResult hit
                                && hit.getBlockPos().equals(ctx.abs(device)),
                        200, "the client's player to look at the eject button of the device at " + device);
    }

    /** Right-clicks whatever block the crosshair is on with the held item, the path the player's own click takes. */
    public static ClientTestContext clickCrosshair(final ClientTestContext ctx, final int delayTicks) {
        return ctx.then(delayTicks, () -> {
            if (ctx.mc().hitResult instanceof BlockHitResult hit) {
                Objects.requireNonNull(ctx.mc().gameMode, "no game mode")
                        .useItemOn(ctx.player(), InteractionHand.MAIN_HAND, hit);
            }
        });
    }

    /*
     * The middle of the button's face in the world, on a device that faces south: across its front runs east, and the
     * button's face stands its depth behind the front face of the block.
     */
    private static Vec3 buttonCentre(final BlockPos device, final EjectButton button) {
        final double across = (button.left() + button.right()) / 2.0 / EjectButton.FRONT;
        final double down = (button.top() + button.bottom()) / 2.0 / EjectButton.FRONT;
        return new Vec3(device.getX() + across, device.getY() + 1.0 - down,
                device.getZ() + 1.0 - button.depth() / (double) EjectButton.FRONT);
    }
}
