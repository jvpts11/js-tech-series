/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.universe;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3d;

/**
 * The sky of the way up, drawn over the game's own: a dome round the eye whose colour goes by height over the bent
 * horizon, the horizon's own colour along it and below it, the colour overhead from a little way up, as the approved
 * climb's sky does. Everything here is on the render thread.
 */
final class ClimbSky {

    /* How many bands of height and slices round the dome is made of, and how far from the eye it is drawn. */
    private static final int BANDS = 36;
    private static final int SLICES = 32;
    private static final float RADIUS = 50F;
    /* How far over the horizon, in degrees, the sky is wholly the colour overhead, and how it gets there. */
    private static final double OVERHEAD = 40;
    private static final double EASE = 0.6;

    @Nullable
    private static VertexBuffer dome;

    private ClimbSky() {
    }

    /**
     * Draws the dome, its up {@code up}, the horizon {@code dip} degrees under the level, the sky over it going from
     * {@code horizon} to {@code overhead}, {@code alpha} of it over the game's own sky.
     */
    static void draw(final double[] up, final double dip, final float[] horizon, final float[] overhead,
                     final float alpha, final Matrix4f view, final Matrix4f projection) {
        final ShaderInstance flat = GameRenderer.getPositionColorShader();
        if (alpha <= 0 || flat == null) {
            return;
        }
        final Vector3d upward = new Vector3d(up[0], up[1], up[2]).normalize();
        final Vector3d helper = Math.abs(upward.z) > 0.9 ? new Vector3d(1, 0, 0) : new Vector3d(0, 0, 1);
        final Vector3d east = new Vector3d(upward).cross(helper).normalize();
        final Vector3d south = new Vector3d(east).cross(upward).normalize();
        final BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_COLOR);
        for (int band = 0; band < BANDS; band++) {
            final double low = -90 + 180.0 * band / BANDS;
            final double high = -90 + 180.0 * (band + 1) / BANDS;
            for (int slice = 0; slice < SLICES; slice++) {
                final double from = 2 * Math.PI * slice / SLICES;
                final double to = 2 * Math.PI * (slice + 1) / SLICES;
                corner(builder, upward, east, south, low, from, dip, horizon, overhead, alpha);
                corner(builder, upward, east, south, low, to, dip, horizon, overhead, alpha);
                corner(builder, upward, east, south, high, to, dip, horizon, overhead, alpha);
                corner(builder, upward, east, south, high, from, dip, horizon, overhead, alpha);
            }
        }
        RenderSystem.defaultBlendFunc();
        if (dome == null) {
            dome = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
        }
        dome.bind();
        dome.upload(builder.buildOrThrow());
        dome.drawWithShader(view, projection, flat);
        VertexBuffer.unbind();
    }

    private static void corner(final BufferBuilder builder, final Vector3d up, final Vector3d east,
                               final Vector3d south, final double height, final double round, final double dip,
                               final float[] horizon, final float[] overhead, final float alpha) {
        final double lift = Math.toRadians(height);
        final double flat = Math.cos(lift) * RADIUS;
        final double x = east.x * Math.cos(round) * flat + south.x * Math.sin(round) * flat + up.x * Math.sin(lift)
                * RADIUS;
        final double y = east.y * Math.cos(round) * flat + south.y * Math.sin(round) * flat + up.y * Math.sin(lift)
                * RADIUS;
        final double z = east.z * Math.cos(round) * flat + south.z * Math.sin(round) * flat + up.z * Math.sin(lift)
                * RADIUS;
        final double t = Math.pow(Math.max(0, Math.min(1, (height + dip) / OVERHEAD)), EASE);
        builder.addVertex((float) x, (float) y, (float) z).setColor(
                (float) (horizon[0] + (overhead[0] - horizon[0]) * t),
                (float) (horizon[1] + (overhead[1] - horizon[1]) * t),
                (float) (horizon[2] + (overhead[2] - horizon[2]) * t), alpha);
    }
}
