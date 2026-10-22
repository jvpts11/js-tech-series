/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import com.mojang.blaze3d.platform.NativeImage;
import dev.jstech.core.client.motion.FadeLayer;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * A whole window faded as one picture: drawn at half strength over a green ground, the white middle of a window whose
 * frame is red shows half white over the green, and none of the red beneath it, as a window fades on a real desktop;
 * drawn at half strength straight onto the screen, the red would show through.
 */
public final class FadeLayerClientTests {

    private static final int GROUND = 0xFF20C040;
    private static final int FRAME = 0xFFE02020;
    private static final int MIDDLE = 0xFFFFFFFF;
    private static final float HALF = 0.5F;
    /* How far a channel may stand from what is worked out, in its 255ths. */
    private static final int CLOSE = 6;
    private static final int CHANNEL = 0xFF;
    private static final int GREEN_SHIFT = 8;
    private static final int BLUE_SHIFT = 16;

    private FadeLayerClientTests() {
    }

    @ClientTest(timeoutTicks = 200)
    public static void window_fadesAsOnePicture(final ClientTestContext ctx) {
        final int[] seen = new int[1];
        ctx.then(0, () -> ctx.mc().setScreen(new Faded()))
                .then(4, () -> {
                    final int width = ctx.mc().getWindow().getGuiScaledWidth();
                    final int height = ctx.mc().getWindow().getGuiScaledHeight();
                    try (NativeImage middle = ctx.capture(width / 2 - 2, height / 2 - 2, 4, 4)) {
                        seen[0] = middle.getPixelRGBA(2, 2);
                    }
                })
                .thenAssert(0, () -> near(seen[0], half(MIDDLE, GROUND)), "the window's middle is half white over "
                        + "the green, with none of its red frame beneath showing")
                .thenScreenshot(0, "a-window-faded-as-one-picture")
                .then(0, () -> ctx.mc().setScreen(null))
                .thenAwaitNoScreen(20);
    }

    /* What a colour laid at half strength over another comes to, red, green and blue. */
    private static int[] half(final int over, final int under) {
        final int[] out = new int[3];
        for (int i = 0; i < 3; i++) {
            final int shift = 16 - i * 8;
            out[i] = Math.round(((over >> shift & CHANNEL) + (under >> shift & CHANNEL)) * HALF);
        }
        return out;
    }

    /* Whether a picture's pixel, red, green, blue and alpha from the lowest byte up, is near {@code rgb}. */
    private static boolean near(final int pixel, final int[] rgb) {
        return Math.abs((pixel & CHANNEL) - rgb[0]) <= CLOSE && Math.abs((pixel >> GREEN_SHIFT & CHANNEL) - rgb[1])
                <= CLOSE && Math.abs((pixel >> BLUE_SHIFT & CHANNEL) - rgb[2]) <= CLOSE;
    }

    /* A green screen with a window on it, red framed and white within, faded to half as one picture. */
    private static final class Faded extends Screen {

        Faded() {
            super(Component.empty());
        }

        @Override
        public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
            g.fill(0, 0, this.width, this.height, GROUND);
            final int x = this.width / 2;
            final int y = this.height / 2;
            FadeLayer.draw(g, HALF, () -> {
                g.fill(x - 40, y - 30, x + 40, y + 30, FRAME);
                g.fill(x - 30, y - 20, x + 30, y + 20, MIDDLE);
            });
        }

        @Override
        public void renderBackground(final GuiGraphics g, final int mouseX, final int mouseY,
                                     final float partialTick) {
            // The ground is the test's own, drawn in render.
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }
}
