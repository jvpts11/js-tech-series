/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.computers.api.client.ComponentRenderers;
import dev.jstech.computers.api.client.DesktopApps;
import dev.jstech.computers.api.client.DesktopProgram;
import dev.jstech.computers.api.client.IComponentActions;
import dev.jstech.computers.api.client.IComponentRenderer;
import dev.jstech.core.api.client.ISkin;
import dev.jstech.core.api.client.ISurface;
import dev.jstech.core.api.client.PixelSurface;
import dev.jstech.core.api.client.SurfaceRenderer;
import dev.jstech.tests.testkit.TestComponents;
import dev.jstech.tests.testkit.TestDesktopPrograms;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * What the test mod draws on a player's game, registered the way another mod registers it from its client setup: the
 * dial component's renderer, and the window of the program that draws its own picture.
 */
public final class TestClientParts {

    /** How many frames the picture program's renderer has been asked for, for the tests to read. */
    public static final AtomicInteger FRAMES = new AtomicInteger();
    /** The most frames a second the picture program asks for. */
    public static final int FRAME_CAP = 10;
    /** How far a click on the dial turns it, in degrees. */
    public static final int TURN_BY = 15;

    private TestClientParts() {
    }

    /** Registers both, on the client. */
    public static void register() {
        ComponentRenderers.register(TestComponents.DIAL, new DialRenderer());
        DesktopApps.register(TestDesktopPrograms.PIXELS, true, (host, monitor, desktop) -> new PixelProgram());
    }

    /** A program that draws nothing, for a window made outside any player's hand. */
    public static final class Probe extends DesktopProgram {

        @Override
        public String title() {
            return "Probe";
        }
    }

    /** A dial: a box with a needle at the angle it holds, turned by a click. */
    private static final class DialRenderer implements IComponentRenderer {

        @Override
        public int width() {
            return 40;
        }

        @Override
        public void draw(final GuiGraphics graphics, final Font font, final ISkin skin, final int x, final int y,
                         final int width, final int height, final Object data, final int mouseX,
                         final int mouseY) {
            skin.field(graphics, x, y, width, height, false);
            final int angle = data instanceof Integer turned ? turned : 0;
            final int cx = x + width / 2;
            final int cy = y + height / 2;
            final int reach = Math.min(width, height) / 2 - 3;
            for (int step = 0; step <= reach; step++) {
                final int px = cx + (int) Math.round(Math.sin(Math.toRadians(angle)) * step);
                final int py = cy - (int) Math.round(Math.cos(Math.toRadians(angle)) * step);
                graphics.fill(px, py, px + 1, py + 1, skin.accent());
            }
            graphics.drawString(font, String.valueOf(angle), x + 2, y + height - 9, skin.text(), false);
        }

        @Override
        public boolean mouseClicked(final double mouseX, final double mouseY, final int button, final Object data,
                                    final IComponentActions actions) {
            actions.send(TestComponents.TURN, TURN_BY);
            return true;
        }
    }

    /** A program whose window is a surface of pixels, a band of colour moving across it a frame at a time. */
    private static final class PixelProgram extends DesktopProgram {

        private final SurfaceRenderer renderer = new SurfaceRenderer(64, 40, false) {
            @Override
            public int frameCap() {
                return FRAME_CAP;
            }

            @Override
            public void frame(final ISurface surface, final double seconds) {
                final int frame = FRAMES.incrementAndGet();
                final PixelSurface pixels = (PixelSurface) surface;
                for (int y = 0; y < pixels.height(); y++) {
                    for (int x = 0; x < pixels.width(); x++) {
                        final int shade = (x + frame) * 4 & 0xFF;
                        pixels.set(x, y, 0xFF << 24 | shade << 16 | y * 6 << 8 | 0x80);
                    }
                }
            }
        };

        @Override
        public String title() {
            return "Pixels";
        }

        @Override
        public SurfaceRenderer renderer() {
            return this.renderer;
        }
    }
}
