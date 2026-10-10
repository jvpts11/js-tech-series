/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.jstech.computers.JsComputers;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

/**
 * The outline a button on a block's front gets while the player looks at it, a monitor's power button or a drive's
 * eject button: the button alone, the way the game outlines a button on a wall, rather than the whole block, so it reads
 * as a part of its own that a click presses.
 *
 * <p>It is drawn in the series' accent rather than the game's black, which vanishes on a dark bezel; its colour is
 * {@code jsc:button_outline}.
 */
@PaletteHolder
public final class ButtonOutline {

    private static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "button_outline",
            new Colours(0xE639D6C4));

    private ButtonOutline() {
    }

    /** Draws the twelve edges of {@code box}, already moved to where the camera sees it, as the game draws a block's. */
    public static void draw(final PoseStack poseStack, final VertexConsumer lines, final AABB box) {
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
