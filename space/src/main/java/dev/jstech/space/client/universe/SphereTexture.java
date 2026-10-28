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
import net.minecraft.util.FastColor;
import org.lwjgl.opengl.GL11;

/**
 * A world's map on the graphics card, with smaller copies of itself the card picks from by how big the world looks,
 * each made of every other texel of the one before, and read whole, never blended with the next: so a world's texels
 * never shrink below a pixel or shimmer however far it is, as a block's do. The copies are made on any thread
 * ({@link #chain}); only putting them on the card waits for the render thread.
 */
final class SphereTexture extends AbstractTexture {

    /* The smallest copy, in texels round. */
    private static final int SMALLEST = 16;

    /** A texture holding {@code chain} ({@link #chain}), which it closes. On the render thread. */
    SphereTexture(final NativeImage[] chain) {
        TextureUtil.prepareImage(getId(), chain.length - 1, chain[0].getWidth(), chain[0].getHeight());
        put(chain);
    }

    /** A map of {@code width} by {@code width / 2} texels from colours as ARGB, row by row, with its copies. */
    static NativeImage[] chain(final int[] argb, final int width) {
        final int height = width / 2;
        final NativeImage image = new NativeImage(width, height, false);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // The image keeps its pixels as ABGR.
                image.setPixelRGBA(x, y, FastColor.ABGR32.fromArgb32(argb[y * width + x]));
            }
        }
        return chain(image);
    }

    /** {@code map}, which must hold RGBA, followed by each smaller copy made from it, down to the smallest. */
    static NativeImage[] chain(final NativeImage map) {
        final List<NativeImage> chain = new ArrayList<>();
        chain.add(map);
        NativeImage level = map;
        while (level.getWidth() > SMALLEST) {
            level = half(level);
            chain.add(level);
        }
        return chain.toArray(NativeImage[]::new);
    }

    /** Closes every image of {@code chain}. */
    static void close(final NativeImage[] chain) {
        for (final NativeImage image : chain) {
            image.close();
        }
    }

    /** Puts {@code chain}, as big as the texture's own, in place of what it holds, and closes it. */
    void replace(final NativeImage[] chain) {
        bind();
        put(chain);
    }

    @Override
    public void load(final ResourceManager manager) {
        // Made from a map in memory, not from a file.
    }

    private void put(final NativeImage[] chain) {
        for (int level = 0; level < chain.length; level++) {
            final NativeImage image = chain[level];
            image.upload(level, 0, 0, 0, 0, image.getWidth(), image.getHeight(), false, false, true, true);
        }
        // The nearest texel of the nearest copy: never two copies blended.
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST_MIPMAP_NEAREST);
    }

    /* Every other texel of {@code image}, the one nearest the middle of each square of four. */
    private static NativeImage half(final NativeImage image) {
        final int width = Math.max(1, image.getWidth() / 2);
        final int height = Math.max(1, image.getHeight() / 2);
        final NativeImage out = new NativeImage(width, height, false);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                out.setPixelRGBA(x, y, image.getPixelRGBA(Math.min(image.getWidth() - 1, x * 2 + 1),
                        Math.min(image.getHeight() - 1, y * 2 + 1)));
            }
        }
        return out;
    }
}
