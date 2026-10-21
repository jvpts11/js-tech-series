/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.developer;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import org.jetbrains.annotations.Nullable;

/**
 * Two pictures of a screen compared as a client test compares them: a pixel may be a little off in each colour, and a
 * few may be off by more, so the small differences between one graphics card and another pass.
 */
public final class Pictures {

    /** How far a pixel's colour may be off in each channel, and the share of the pixels that may be off by more. */
    public static final int CHANNEL = 12;
    public static final double SHARE = 0.005;
    /** Where a pixel is off, in the picture of the differences: opaque red, as the image holds it. */
    public static final int OFF = 0xFF0000FF;

    private Pictures() {
    }

    /**
     * How many pixels of {@code drawn} are off from {@code reference}, the two the same size; each off pixel marked
     * in {@code differences} when given, the others copied from {@code drawn}.
     */
    public static int differing(final NativeImage drawn, final NativeImage reference,
                                @Nullable final NativeImage differences) {
        int off = 0;
        for (int y = 0; y < drawn.getHeight(); y++) {
            for (int x = 0; x < drawn.getWidth(); x++) {
                final int a = drawn.getPixelRGBA(x, y);
                final int b = reference.getPixelRGBA(x, y);
                final boolean differs = Math.abs((a & 0xFF) - (b & 0xFF)) > CHANNEL
                        || Math.abs((a >> 8 & 0xFF) - (b >> 8 & 0xFF)) > CHANNEL
                        || Math.abs((a >> 16 & 0xFF) - (b >> 16 & 0xFF)) > CHANNEL;
                if (differences != null) {
                    differences.setPixelRGBA(x, y, differs ? OFF : a);
                }
                off += differs ? 1 : 0;
            }
        }
        return off;
    }

    /** The most pixels of a picture so large that may be off by more and still pass. */
    public static int mostOff(final NativeImage picture) {
        return (int) Math.floor(picture.getWidth() * picture.getHeight() * SHARE);
    }

    /** The frame's rectangle {@code x}, {@code y}, {@code width} by {@code height} in the GUI's pixels, a copy. */
    public static NativeImage capture(final Minecraft game, final int x, final int y, final int width,
                                      final int height) {
        final double scale = game.getWindow().getGuiScale();
        try (NativeImage frame = Screenshot.takeScreenshot(game.getMainRenderTarget())) {
            final int left = (int) Math.round(x * scale);
            final int top = (int) Math.round(y * scale);
            final int w = Math.max(1, Math.min((int) Math.round(width * scale), frame.getWidth() - left));
            final int h = Math.max(1, Math.min((int) Math.round(height * scale), frame.getHeight() - top));
            final NativeImage out = new NativeImage(w, h, true);
            for (int py = 0; py < h; py++) {
                for (int px = 0; px < w; px++) {
                    out.setPixelRGBA(px, py, frame.getPixelRGBA(left + px, top + py));
                }
            }
            return out;
        }
    }
}
