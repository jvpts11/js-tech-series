/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import com.mojang.blaze3d.platform.NativeImage;
import dev.jstech.core.client.gui.component.Panel;
import dev.jstech.core.client.gui.component.UiComponent;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.client.gui.style.Styles;
import dev.jstech.core.client.motion.MotionClock;
import dev.jstech.core.motion.IEasing;
import dev.jstech.core.motion.MotionSpec;
import dev.jstech.core.motion.MotionStyles;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * A component animated with one call: one that appears fades in as one picture, between the ground and its colour on
 * the way, and one that slides comes up from below its place, the ground still showing there at first; each stands
 * where it rests once its motion is over.
 */
public final class ComponentMotionClientTests {

    private static final int GROUND = 0xFF2040C0;
    private static final int SOLID = 0xFFF0D020;
    /* Long enough to be caught on the way, short enough for a test. */
    private static final int MILLIS = 3000;
    private static final int ON_THE_WAY = 20;
    private static final int OVER = 80;
    private static final int SIDE = 60;
    /* How far a channel may stand from what is worked out, in its 255ths. */
    private static final int CLOSE = 8;
    private static final int CHANNEL = 0xFF;

    private ComponentMotionClientTests() {
    }

    @ClientTest(timeoutTicks = 300)
    public static void appear_fadesAComponentInAsOnePicture(final ClientTestContext ctx) {
        final int[] seen = new int[2];
        ctx.afterTest(() -> {
            MotionClock.setReduced(true);
            ctx.mc().setScreen(null);
        });
        ctx.then(0, () -> {
                    MotionClock.setReduced(false);
                    ctx.mc().setScreen(new Moving(MotionSpec.of(MotionStyles.APPEAR, MILLIS, IEasing.LINEAR, "")));
                })
                .then(ON_THE_WAY, () -> seen[0] = middle(ctx))
                .thenAssert(0, () -> between(seen[0]), "on its way in, the swatch is between the ground and its colour")
                .thenScreenshot(0, "component-appearing")
                .then(OVER, () -> seen[1] = middle(ctx))
                .thenAssert(0, () -> near(seen[1], SOLID), "once in, it is its own colour");
    }

    @ClientTest(timeoutTicks = 300)
    public static void slide_bringsAComponentUpFromBelowItsPlace(final ClientTestContext ctx) {
        final int[] seen = new int[2];
        ctx.afterTest(() -> {
            MotionClock.setReduced(true);
            ctx.mc().setScreen(null);
        });
        ctx.then(0, () -> {
                    MotionClock.setReduced(false);
                    ctx.mc().setScreen(new Moving(MotionSpec.of(MotionStyles.SLIDE, MILLIS, IEasing.LINEAR, "")
                            .with("distance", 1.0)));
                })
                .then(2, () -> seen[0] = top(ctx))
                .thenAssert(0, () -> near(seen[0], GROUND), "at first the ground shows at the top of its place")
                .then(OVER, () -> seen[1] = top(ctx))
                .thenAssert(0, () -> near(seen[1], SOLID), "and once in, the swatch fills it");
    }

    /* The pixel at the middle of the swatch's place. */
    private static int middle(final ClientTestContext ctx) {
        return pixelAt(ctx, 0);
    }

    /* The pixel near the top of the swatch's place. */
    private static int top(final ClientTestContext ctx) {
        return pixelAt(ctx, -SIDE / 2 + 4);
    }

    private static int pixelAt(final ClientTestContext ctx, final int down) {
        final int x = ctx.mc().getWindow().getGuiScaledWidth() / 2;
        final int y = ctx.mc().getWindow().getGuiScaledHeight() / 2 + down;
        try (NativeImage picture = ctx.capture(x - 1, y - 1, 2, 2)) {
            return picture.getPixelRGBA(1, 1);
        }
    }

    /* Whether a picture's pixel, red in its lowest byte, stands strictly between the ground and the colour. */
    private static boolean between(final int pixel) {
        for (int i = 0; i < 3; i++) {
            final int seen = pixel >> (8 * i) & CHANNEL;
            final int from = GROUND >> (16 - 8 * i) & CHANNEL;
            final int to = SOLID >> (16 - 8 * i) & CHANNEL;
            if (from != to && (seen <= Math.min(from, to) + CLOSE || seen >= Math.max(from, to) - CLOSE)) {
                return false;
            }
        }
        return true;
    }

    private static boolean near(final int pixel, final int argb) {
        for (int i = 0; i < 3; i++) {
            if (Math.abs((pixel >> (8 * i) & CHANNEL) - (argb >> (16 - 8 * i) & CHANNEL)) > CLOSE) {
                return false;
            }
        }
        return true;
    }

    /* A ground of one colour and, in its middle, a swatch of another, set moving as the screen opens. */
    private static final class Moving extends Screen {

        private final Panel panel = new Panel();
        private final Swatch swatch = this.panel.add(new Swatch());

        Moving(final MotionSpec spec) {
            super(Component.empty());
            this.swatch.animate(spec);
        }

        @Override
        public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
            g.fill(0, 0, this.width, this.height, GROUND);
            this.swatch.setBounds(this.width / 2 - SIDE / 2, this.height / 2 - SIDE / 2, SIDE, SIDE);
            this.panel.render(g, new UiContext(Styles.skin(Styles.game()), this.font, mouseX, mouseY, partialTick));
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

    /* A square of one colour. */
    private static final class Swatch extends UiComponent {

        @Override
        public void render(final GuiGraphics g, final UiContext ctx) {
            g.fill(this.x(), this.y(), this.right(), this.bottom(), SOLID);
        }
    }
}
