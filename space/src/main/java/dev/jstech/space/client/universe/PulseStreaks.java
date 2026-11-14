/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.universe;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.space.JsSpace;
import java.util.Random;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3d;

/**
 * The streaks of a pulse jump, as the approved HUD draws them: a field of faint points round the ship, each drawn as a
 * streak going out from where the ship jumps towards, longer the further from it, as stars rushing past would be.
 * Drawn over the sky and the worlds, added to what is behind, only while the ship jumps.
 */
@PaletteHolder
public final class PulseStreaks {

    /** The streaks' colour, bright at their head and gone at their tail. */
    public static final Palette<Colours> PALETTE = Palettes.declare(JsSpace.MODID, "pulse_streaks",
            new Colours(0xB0E8F0FF));
    /* How many streaks there are, how long each is across the sky at most, and the seed they are laid out by. */
    private static final int COUNT = 1200;
    private static final double LONGEST = 0.35;
    private static final long SEED = 11;
    private static final int CHANNEL = 0xFF;
    private static final float CHANNEL_MOST = 255F;
    private static final double[][] POINTS = points();
    @Nullable
    private static VertexBuffer lines;

    private PulseStreaks() {
    }

    /**
     * Draws the streaks for a ship jumping the level way {@code way}, x and z, through the drawing {@code view} and
     * {@code projection} round the eye.
     */
    static void draw(final double[] way, final Matrix4f view, final Matrix4f projection) {
        final ShaderInstance coloured = GameRenderer.getPositionColorShader();
        final double length = Math.hypot(way[0], way[1]);
        if (coloured == null || length <= 0) {
            return;
        }
        final double vx = way[0] / length;
        final double vz = way[1] / length;
        final int colour = PALETTE.get().streak();
        final float red = (colour >> 16 & CHANNEL) / CHANNEL_MOST;
        final float green = (colour >> 8 & CHANNEL) / CHANNEL_MOST;
        final float blue = (colour & CHANNEL) / CHANNEL_MOST;
        final float alpha = (colour >>> 24 & CHANNEL) / CHANNEL_MOST;
        final BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.DEBUG_LINES,
                DefaultVertexFormat.POSITION_COLOR);
        final double far = SphereDrawing.DRAWN_WITHIN;
        for (final double[] point : POINTS) {
            // Away from the way it goes, square to it: the further from it, the longer the streak.
            final double along = point[0] * vx + point[2] * vz;
            final Vector3d out = new Vector3d(point[0] - vx * along, point[1], point[2] - vz * along);
            final Vector3d head = new Vector3d(point[0], point[1], point[2]);
            final Vector3d tail = new Vector3d(head).add(out.mul(LONGEST)).normalize();
            builder.addVertex((float) (head.x * far), (float) (head.y * far), (float) (head.z * far))
                    .setColor(red, green, blue, alpha);
            builder.addVertex((float) (tail.x * far), (float) (tail.y * far), (float) (tail.z * far))
                    .setColor(red, green, blue, 0F);
        }
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        if (lines == null) {
            lines = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
        }
        SphereDrawing.draw(lines, builder.buildOrThrow(), view, projection, coloured);
    }

    /* The streaks' heads, spread evenly over the sky by the seed. */
    private static double[][] points() {
        final Random random = new Random(SEED);
        final double[][] points = new double[COUNT][];
        for (int i = 0; i < COUNT; i++) {
            final double y = random.nextDouble() * 2 - 1;
            final double turn = random.nextDouble() * 2 * Math.PI;
            final double across = Math.sqrt(1 - y * y);
            points[i] = new double[] {across * Math.cos(turn), y, across * Math.sin(turn)};
        }
        return points;
    }

    /**
     * The colours of the streaks.
     *
     * @param streak a streak's head, its alpha how bright it is
     */
    public record Colours(int streak) {
    }
}
