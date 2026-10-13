/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.monitor;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.block.MonitorBlock;
import dev.jstech.computers.block.MonitorKind;
import dev.jstech.computers.client.ButtonOutline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;

/**
 * The outline a monitor's power button gets while the player looks at it: the {@link ButtonOutline button alone},
 * rather than the whole monitor. Looked at anywhere else, the monitor is outlined whole, and a click opens its screen.
 */
@EventBusSubscriber(modid = JsComputers.MODID, value = Dist.CLIENT)
public final class MonitorButtonOutline {

    /** How far the button stands out of the bezel, as its model draws it: half a pixel. */
    private static final double STANDS_OUT = 0.5 / 16.0;

    private MonitorButtonOutline() {
    }

    @SubscribeEvent
    public static void onBlockHighlight(final RenderHighlightEvent.Block event) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        final BlockHitResult hit = event.getTarget();
        final BlockPos pos = hit.getBlockPos();
        final BlockState state = minecraft.level.getBlockState(pos);
        if (!(state.getBlock() instanceof MonitorBlock monitor) || !state.getValue(MonitorBlock.BUTTON)) {
            return;
        }
        final double[] at = MonitorBlock.frontPoint(state, pos, hit);
        final MonitorKind kind = monitor.kind();
        if (at == null || !kind.onButton(at[0], at[1])) {
            return;
        }
        final Vec3 camera = event.getCamera().getPosition();
        ButtonOutline.draw(event.getPoseStack(), event.getMultiBufferSource().getBuffer(RenderType.lines()),
                button(kind, state.getValue(HorizontalDirectionalBlock.FACING)).move(pos.getX() - camera.x,
                        pos.getY() - camera.y, pos.getZ() - camera.z));
        event.setCanceled(true);
    }

    /*
     * The button's box in the block, from its edges on the front in sixty-fourths from the top left as seen: the
     * same reading of the front a click is measured by, so the outline stands exactly where a click presses.
     */
    private static AABB button(final MonitorKind kind, final Direction facing) {
        final double left = kind.button(0) / (double) MonitorKind.FRONT;
        final double right = kind.button(2) / (double) MonitorKind.FRONT;
        final double bottom = 1.0 - kind.button(3) / (double) MonitorKind.FRONT;
        final double top = 1.0 - kind.button(1) / (double) MonitorKind.FRONT;
        // Across the front runs toward the monitor's right, as the player facing it sees it.
        final double[] across = switch (facing.getClockWise()) {
            case EAST, SOUTH -> new double[] {left, right};
            default -> new double[] {1.0 - right, 1.0 - left};
        };
        final boolean alongX = facing.getClockWise().getAxis() == Direction.Axis.X;
        final Direction front = facing.getOpposite();
        final double face = front.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1.0 : 0.0;
        final double out = front.getAxisDirection() == Direction.AxisDirection.POSITIVE ? STANDS_OUT : -STANDS_OUT;
        final double depthFrom = Math.min(face, face + out);
        final double depthTo = Math.max(face, face + out);
        return alongX ? new AABB(across[0], bottom, depthFrom, across[1], top, depthTo)
                : new AABB(depthFrom, bottom, across[0], depthTo, top, across[1]);
    }
}
