/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import com.mojang.blaze3d.platform.NativeImage;
import dev.jstech.core.font.CoreFonts;
import dev.jstech.tests.TestFonts;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;

/**
 * A font the data generation made from a TrueType file draws in the game: the test mod's font of squares, made from
 * the X11 fixed 6 by 10 font drawn a square to each pixel, writes a line exactly as the Core's fixed 6 by 10 font,
 * made from its bitmap, writes it.
 */
public final class OutlineFontClientTests {

    private static final String LINE = "Quick brown fox, 0123456789 & !?";
    private static final int GROUND = 0xFF000000;
    private static final int INK = 0xFFFFFFFF;
    private static final int TOP = 20;
    private static final int APART = 30;
    private static final int LEFT = 10;
    private static final int TALL = 12;

    private OutlineFontClientTests() {
    }

    @ClientTest(timeoutTicks = 200)
    public static void outlineFont_drawsAsTheBitmapFontItWasMadeFrom(final ClientTestContext ctx) {
        final boolean[] same = new boolean[2];
        ctx.afterTest(() -> ctx.mc().setScreen(null));
        ctx.then(0, () -> ctx.mc().setScreen(new Lines()))
                .then(4, () -> {
                    final int width = ctx.mc().font.width(Component.literal(LINE).withStyle(style(
                            CoreFonts.FIXED_6X10.id()))) + 2;
                    try (NativeImage outline = ctx.capture(LEFT, TOP, width, TALL);
                         NativeImage bitmap = ctx.capture(LEFT, TOP + APART, width, TALL)) {
                        boolean alike = true;
                        boolean inked = false;
                        for (int y = 0; y < outline.getHeight(); y++) {
                            for (int x = 0; x < outline.getWidth(); x++) {
                                alike &= outline.getPixelRGBA(x, y) == bitmap.getPixelRGBA(x, y);
                                inked |= (outline.getPixelRGBA(x, y) & 0xFF) > 0x80;
                            }
                        }
                        same[0] = alike;
                        same[1] = inked;
                    }
                })
                .thenAssert(0, () -> same[1], "the line is written in the font made from the TrueType file")
                .thenAssert(0, () -> same[0], "and pixel for pixel as the bitmap font it was made from")
                .thenScreenshot(0, "outline-font-beside-its-bitmap");
    }

    private static Style style(final ResourceLocation font) {
        return Style.EMPTY.withFont(font);
    }

    /* The line in the font of squares, and under it in the Core's fixed font. */
    private static final class Lines extends Screen {

        Lines() {
            super(Component.empty());
        }

        @Override
        public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
            g.fill(0, 0, this.width, this.height, GROUND);
            g.drawString(this.font, Component.literal(LINE).withStyle(style(TestFonts.SQUARES.id())), LEFT, TOP,
                    INK, false);
            g.drawString(this.font, Component.literal(LINE).withStyle(style(CoreFonts.FIXED_6X10.id())), LEFT,
                    TOP + APART, INK, false);
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
