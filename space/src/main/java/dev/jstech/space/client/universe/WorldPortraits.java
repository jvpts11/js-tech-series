/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.universe;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.jstech.core.api.client.GpuSurface;
import dev.jstech.core.client.live.RenderTargets;
import dev.jstech.space.universe.SolarBody;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3d;

/**
 * A world's portrait: its sphere drawn as the system's space draws it, with its maps, its relief and its light, alone
 * on a clear ground, north up, seen from far enough off that nearly the whole of the half facing the eye shows. It is
 * drawn finer than it is kept and its pixels averaged, as a photograph of it shrunk, so its outline is smooth and its
 * texels are the space's own. The picture of a world on its sheet in the Mission Handbook is one.
 */
public final class WorldPortraits {

    /* How many times finer each way a portrait is drawn than it is kept. */
    private static final int FINER = 4;
    /* The world's radius as drawn and how far off its middle stands, within the nearest the game ever sees. */
    private static final double RADIUS = 4;
    private static final double AWAY = 80;
    /* How much wider the picture is than the disc: room for an air's glow past the limb. */
    private static final double ROOM = 1.02;
    private static final float NEAR = 1F;
    private static final float FAR = 200F;
    /* As in space, a texel is never drawn smaller than a pixel of the finer picture. */
    private static final float TEXEL_PIXELS = 1F;
    private static final int CHANNEL = 0xFF;
    private static final int ALPHA_SHIFT = 24;

    private WorldPortraits() {
    }

    /**
     * {@code body} drawn {@code size} pixels square, the place at {@code latitude} degrees north and {@code longitude}
     * degrees east of its map's middle facing the eye, its pole leaning that much towards the eye, lit by a sun
     * {@code phase} degrees west of the eye, so that much of its eastern half lies in its night; clear round its disc.
     * None while the shader or its maps are not on the graphics card yet. On the render thread; the caller closes the
     * picture.
     */
    public static @Nullable NativeImage draw(final SolarBody body, final int size, final double latitude,
                                             final double longitude, final double phase) {
        if (!SphereDrawing.ready() || WorldSpheres.of(body) == null) {
            return null;
        }
        final int fine = size * FINER;
        final GpuSurface surface = new GpuSurface(fine, fine);
        try {
            final RenderTarget target = surface.target();
            target.setClearColor(0, 0, 0, 0);
            RenderTargets.clear(target);
            target.bindWrite(true);
            final double up = Math.toRadians(latitude);
            final double across = Math.toRadians(longitude);
            final double sun = Math.toRadians(phase);
            // The view's half width: the disc's, as it is seen from AWAY, with the room round it.
            final double half = Math.atan(Math.tan(Math.asin(RADIUS / AWAY)) * ROOM);
            final Matrix4f projection = new Matrix4f().setPerspective((float) (2 * half), 1F, NEAR, FAR);
            // The pole leans towards the eye as far as the latitude facing it, and east is to the right of the eye, so
            // the map's middle stands that far west of the place facing it; the sun's light comes from the left.
            final double[] north = {0, Math.cos(up), Math.sin(up)};
            final double[] middle = {-Math.sin(across), -Math.sin(up) * Math.cos(across),
                Math.cos(up) * Math.cos(across)};
            SphereDrawing.begin();
            SphereDrawing.draw(body, 0, new Vector3d(0, 0, -AWAY), RADIUS, north, middle,
                    new double[] {-Math.sin(sun), 0, Math.cos(sun)}, TEXEL_PIXELS, 1F, SphereDrawing.Veil.NONE,
                    new Matrix4f(), projection);
            SphereDrawing.end();
            return shrunk(read(target, fine), size);
        } finally {
            Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
            surface.close();
        }
    }

    /* The target's picture, its top row first, its alpha kept. */
    private static NativeImage read(final RenderTarget target, final int fine) {
        final NativeImage image = new NativeImage(fine, fine, false);
        RenderSystem.bindTexture(target.getColorTextureId());
        image.downloadTexture(0, false);
        image.flipY();
        return image;
    }

    /*
     * {@code fine} shrunk to {@code size}, each pixel the mean of those it covers. The worlds are blended with their
     * colours already weighed by their alpha, so the sums are, and each colour is taken back out of its alpha.
     */
    private static NativeImage shrunk(final NativeImage fine, final int size) {
        try (fine) {
            final NativeImage out = new NativeImage(size, size, false);
            final int covered = FINER * FINER;
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    final int[] sums = new int[4];
                    for (int dy = 0; dy < FINER; dy++) {
                        for (int dx = 0; dx < FINER; dx++) {
                            // The picture keeps its pixels as ABGR.
                            final int abgr = fine.getPixelRGBA(x * FINER + dx, y * FINER + dy);
                            for (int channel = 0; channel < sums.length; channel++) {
                                sums[channel] += abgr >>> (channel * Byte.SIZE) & CHANNEL;
                            }
                        }
                    }
                    final int alpha = sums[3];
                    int pixel = 0;
                    if (alpha > 0) {
                        pixel = (alpha + covered / 2) / covered << ALPHA_SHIFT;
                        for (int channel = 0; channel < 3; channel++) {
                            pixel |= Math.min(CHANNEL, Math.round(sums[channel] * (float) CHANNEL / alpha))
                                    << (channel * Byte.SIZE);
                        }
                    }
                    out.setPixelRGBA(x, y, pixel);
                }
            }
            return out;
        }
    }
}
