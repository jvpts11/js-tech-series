/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.universe;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.server.packs.resources.ResourceManager;
import org.lwjgl.opengl.GL11;

/**
 * A map of the ground on the graphics card, its texels never blended with each other, as a block's pixels are not: a
 * map of colours with smaller copies of itself, each texel of a copy the mean of the four under it, so the ground far
 * off keeps its colour and never shimmers; or a map of heights read texel by texel, with no copies. Made on the render
 * thread from images it closes.
 */
final class GroundTexture extends AbstractTexture {

    /* The smallest copy of a map of colours, in texels across. */
    private static final int SMALLEST = 4;
    private static final int CHANNEL = 0xFF;

    private GroundTexture(final List<NativeImage> chain, final int shrunk, final int grown) {
        TextureUtil.prepareImage(getId(), chain.size() - 1, chain.getFirst().getWidth(), chain.getFirst().getHeight());
        for (int level = 0; level < chain.size(); level++) {
            final NativeImage image = chain.get(level);
            image.upload(level, 0, 0, 0, 0, image.getWidth(), image.getHeight(), false, grown == GL11.GL_LINEAR,
                    chain.size() > 1, true);
        }
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, shrunk);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, grown);
    }

    /** A map of colours from {@code map}, which it closes, with its averaged copies. */
    static GroundTexture colours(final NativeImage map) {
        return new GroundTexture(chain(map), GL11.GL_NEAREST_MIPMAP_NEAREST, GL11.GL_NEAREST);
    }

    /**
     * A map of cover from {@code map}, which it closes, with its averaged copies: its texels whole up close, but read
     * smoothly between texels and copies once they are smaller than a pixel, so a far cover reads as how much of it
     * there is.
     */
    static GroundTexture cover(final NativeImage map) {
        return new GroundTexture(chain(map), GL11.GL_LINEAR_MIPMAP_LINEAR, GL11.GL_NEAREST);
    }

    /** A map read texel by texel from {@code map}, which it closes. */
    static GroundTexture plain(final NativeImage map) {
        return new GroundTexture(List.of(map), GL11.GL_NEAREST, GL11.GL_NEAREST);
    }

    /**
     * A map of heights from {@code map}, which it closes, read smoothly between its texels, so the ground between two
     * of its points rises as the land between them does; it does not wrap round at its edges.
     */
    static GroundTexture heights(final NativeImage map) {
        return new GroundTexture(List.of(map), GL11.GL_LINEAR, GL11.GL_LINEAR);
    }

    @Override
    public void load(final ResourceManager manager) {
        // Made from a map in memory, not from a file.
    }

    /* {@code map} followed by each smaller copy made from it. */
    private static List<NativeImage> chain(final NativeImage map) {
        final List<NativeImage> chain = new ArrayList<>();
        chain.add(map);
        NativeImage level = map;
        while (level.getWidth() > SMALLEST && level.getHeight() > SMALLEST) {
            level = half(level);
            chain.add(level);
        }
        return chain;
    }

    /* Each texel the mean of the four under it, the clear ones weighed by how clear they are. */
    private static NativeImage half(final NativeImage image) {
        final int width = Math.max(1, image.getWidth() / 2);
        final int height = Math.max(1, image.getHeight() / 2);
        final NativeImage out = new NativeImage(width, height, false);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                long red = 0;
                long green = 0;
                long blue = 0;
                long alpha = 0;
                for (int dy = 0; dy < 2; dy++) {
                    for (int dx = 0; dx < 2; dx++) {
                        // Kept as ABGR.
                        final int pixel = image.getPixelRGBA(Math.min(image.getWidth() - 1, x * 2 + dx),
                                Math.min(image.getHeight() - 1, y * 2 + dy));
                        final int a = pixel >>> 24;
                        alpha += a;
                        red += (long) (pixel & CHANNEL) * a;
                        green += (long) (pixel >> 8 & CHANNEL) * a;
                        blue += (long) (pixel >> 16 & CHANNEL) * a;
                    }
                }
                final int a = (int) (alpha / 4);
                final long weight = Math.max(1, alpha);
                out.setPixelRGBA(x, y, a << 24 | (int) (blue / weight) << 16 | (int) (green / weight) << 8
                        | (int) (red / weight));
            }
        }
        return out;
    }
}
