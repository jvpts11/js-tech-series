/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.monitor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.block.MonitorBlock;
import dev.jstech.computers.block.MonitorKind;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
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
 * The outline a monitor's power button gets while the player looks at it: the button alone, the way the game outlines
 * a button on a wall, rather than the whole monitor, so it reads as a part of its own that a click presses. Looked at
 * anywhere else, the monitor is outlined whole, and a click opens its screen.
 *
 * <p>The outline is drawn in the series' accent rather than the game's black, which vanishes on the black bezel of a
 * modern monitor; its colour is {@code jsc:monitor/button_outline}.
 */
@PaletteHolder
@EventBusSubscriber(modid = JsComputers.MODID, value = Dist.CLIENT)
public final class MonitorButtonOutline {

    /** How far the button stands out of the bezel, as its model draws it: half a pixel. */
    private static final double STANDS_OUT = 0.5 / 16.0;
    private static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "monitor/button_outline",
            new Colours(0xE639D6C4));

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
        draw(event.getPoseStack(), event.getMultiBufferSource().getBuffer(RenderType.lines()),
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

    /* The twelve edges of a box, as the game draws a block's outline. */
    private static void draw(final PoseStack poseStack, final VertexConsumer lines, final AABB box) {
        final PoseStack.Pose pose = poseStack.last();
        final int colour = PALETTE.get().line();
        final double[] xs = {box.minX, box.maxX};
        final double[] ys = {box.minY, box.maxY};
        final double[] zs = {box.minZ, box.maxZ};
        for (final double y : ys) {
            for (final double z : zs) {
                edge(pose, lines, colour, xs[0], y, z, xs[1], y, z);
            }
        }
        for (final double x : xs) {
            for (final double z : zs) {
                edge(pose, lines, colour, x, ys[0], z, x, ys[1], z);
            }
        }
        for (final double x : xs) {
            for (final double y : ys) {
                edge(pose, lines, colour, x, y, zs[0], x, y, zs[1]);
            }
        }
    }

    private static void edge(final PoseStack.Pose pose, final VertexConsumer lines, final int colour, final double x1,
                             final double y1, final double z1, final double x2, final double y2, final double z2) {
        final float nx = (float) (x2 - x1);
        final float ny = (float) (y2 - y1);
        final float nz = (float) (z2 - z1);
        final float length = Mth.sqrt(nx * nx + ny * ny + nz * nz);
        lines.addVertex(pose, (float) x1, (float) y1, (float) z1).setColor(colour)
                .setNormal(pose, nx / length, ny / length, nz / length);
        lines.addVertex(pose, (float) x2, (float) y2, (float) z2).setColor(colour)
                .setNormal(pose, nx / length, ny / length, nz / length);
    }

    /** The outline's colour, with its alpha. */
    private record Colours(int line) {
    }
}
