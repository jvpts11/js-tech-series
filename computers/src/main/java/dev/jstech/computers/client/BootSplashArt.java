/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.os.boot.BootSplash;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The pictures the systems come up behind, and go down behind.
 *
 * <p>The ground of each is drawn here, out of fills: a sky with its clouds, a black screen, a near-black one.
 * What sits on that ground is a texture, because a maker's lockup is lettering with weights and a face of its
 * own and drawing one out of rectangles and the game's font gets the words right and the thing itself wrong.
 * They are the one thing about starting a machine that nobody reads: a system's picture is what tells you
 * which system it is before a single word of it is on the glass.
 */
public final class BootSplashArt {

    /** The sky the oldest of the Frames editions came up on, from the horizon upward. */
    private static final int SKY_TOP = 0xFF3A78C8;
    private static final int SKY_BOTTOM = 0xFFAFD6F0;

    /** The band along the foot of that sky, which the bar runs through. */
    private static final int HORIZON = 0xFF1A4E96;

    /** The trough the newer edition's blocks run through, and the blocks themselves. */
    private static final int TROUGH = 0xFF1B1B1B;
    private static final int TROUGH_EDGE = 0xFF454545;
    private static final int BLOCK = 0xFF5A8CD8;

    /** The blue the welcome sits on, and the lighter band across it the word is written in. */
    private static final int WELCOME_TOP = 0xFF2C5FA8;
    private static final int WELCOME_BOTTOM = 0xFF17407C;
    private static final int WELCOME_BAND_TOP = 0xFF4E86D0;
    private static final int WELCOME_BAND_BOTTOM = 0xFF3A6CB4;

    /** How far into the wait that edition put its word up, in hundredths. */
    private static final int WELCOME_FROM = 70;

    /** How many blocks run through that trough, and how long one pass takes in ticks. */
    private static final int BLOCKS = 3;
    private static final int PASS_TICKS = 40;

    /** The ring of dots the newest edition turns, and how long one turn takes. */
    private static final int DOTS = 8;
    private static final int TURN_TICKS = 32;

    private BootSplashArt() {
    }

    /**
     * Draws the picture of that system over the glass at {@code (x, y)}.
     *
     * @param ticks how far into the wait the machine is, which is what moves anything that moves
     * @param total how long the whole wait is, for the one picture that fills a bar over it
     * @param going the machine is going down rather than coming up, which two of them say in words
     */
    public static void draw(final GuiGraphics g, final Font font, final BootSplash splash, final int x,
                            final int y, final int w, final int h, final int ticks, final int total,
                            final boolean going) {
        switch (splash) {
            case FRAMES_95 -> frames95(g, font, x, y, w, h, ticks, total, going);
            case FRAMES_XP -> framesXp(g, font, x, y, w, h, ticks, total, going);
            case FRAMES_11 -> frames11(g, font, x, y, w, h, ticks, going);
            default -> { }
        }
    }

    /** Whether this picture paints the whole glass itself, so nothing else should draw a ground under it. */
    public static boolean paintsItsOwnGround(final BootSplash splash) {
        return splash != BootSplash.PLAIN;
    }

    /**
     * The sky, the logo over it, and the bar along the foot.
     *
     * <p>The clouds are drawn from the machine's own position in the wait rather than at random, so the same
     * machine coming up twice looks the same twice, which a picture nobody can influence ought to.
     */
    private static void frames95(final GuiGraphics g, final Font font, final int x, final int y, final int w,
                                 final int h, final int ticks, final int total, final boolean going) {
        gradient(g, x, y, w, h, SKY_TOP, SKY_BOTTOM);
        clouds(g, x, y, w, h);

        final int logoY = y + h / 2 - SplashLogos.H / 2 - 8;
        SplashLogos.draw(g, SplashLogos.FRAMES_95, x + w / 2, logoY);

        // The band along the foot, with the bar running through it the way that one ran.
        final int band = Math.max(6, h / 16);
        g.fill(x, y + h - band, x + w, y + h, HORIZON);
        final int run = total > 0 ? Math.min(w, (int) ((long) w * ticks / total)) : 0;
        g.fill(x, y + h - band, x + run, y + h - band + 2, 0xFF8FC2F0);

        if (going) {
            g.drawCenteredString(font, "Please wait while your computer shuts down.",
                    x + w / 2, logoY + SplashLogos.H + 10, 0xFFFFFFFF);
        }
    }

    /** Black, the logo above the middle, the trough beneath it, and the small print at the feet. */
    private static void framesXp(final GuiGraphics g, final Font font, final int x, final int y, final int w,
                                 final int h, final int ticks, final int total, final boolean going) {
        /*
         * That edition came up in two beats, and the second is the one people remember by name: the logo over
         * its trough, and then a blue ground with one word on it while the desktop was made ready. A machine
         * going down skips it, because it was only ever on the way up.
         */
        if (!going && total > 0 && ticks >= total * WELCOME_FROM / 100) {
            welcome(g, font, x, y, w, h);
            return;
        }
        g.fill(x, y, x + w, y + h, 0xFF000000);

        final int logoY = y + h / 2 - SplashLogos.H - 4;
        SplashLogos.draw(g, SplashLogos.FRAMES_XP, x + w / 2, logoY);

        /*
         * The trough with the blocks running left to right and starting over, which is the one thing everybody
         * who ever saw this screen remembers about it. The blocks run whether or not the wait is nearly done,
         * because that one never told you how far along it was.
         */
        final int troughW = Math.max(60, w / 4);
        final int tx = x + (w - troughW) / 2;
        final int ty = y + h / 2 + 24;
        g.fill(tx - 1, ty - 1, tx + troughW + 1, ty + 7, TROUGH_EDGE);
        g.fill(tx, ty, tx + troughW, ty + 6, TROUGH);
        final int blockW2 = 6;
        final int span = troughW + BLOCKS * (blockW2 + 2);
        for (int i = 0; i < BLOCKS; i++) {
            final int at = (ticks * span / PASS_TICKS + i * (blockW2 + 2)) % span - BLOCKS * (blockW2 + 2);
            final int left = tx + at;
            if (left + blockW2 > tx && left < tx + troughW) {
                g.fill(Math.max(tx, left), ty + 1, Math.min(tx + troughW, left + blockW2), ty + 5, BLOCK);
            }
        }

        g.drawString(font, "(C) 2001 Midsoft Corp.", x + 8, y + h - 12, 0xFF9AA4B2, false);
        g.drawString(font, "Midsoft", x + w - font.width("Midsoft") - 8, y + h - 12, 0xFFFFFFFF, false);

        if (going) {
            g.drawCenteredString(font, "Frames is shutting down...", x + w / 2, y + h / 2 + 40, 0xFFFFFFFF);
        }
    }

    /** The blue ground with one word on it, which is how that edition ended every start. */
    private static void welcome(final GuiGraphics g, final Font font, final int x, final int y, final int w,
                                final int h) {
        gradient(g, x, y, w, h, WELCOME_TOP, WELCOME_BOTTOM);
        // The band across the middle the word sits on, lighter than the ground above and below it.
        final int band = h / 3;
        gradient(g, x, y + (h - band) / 2, w, band, WELCOME_BAND_TOP, WELCOME_BAND_BOTTOM);
        g.fill(x, y + (h - band) / 2, x + w, y + (h - band) / 2 + 1, 0xFFDCE8FA);
        big(g, font, "welcome", x + w / 2, y + h / 2 - 8);
    }

    /**
     * One word at three times the font's size, which is the only way to write large with the game's own
     * letters. It is a word rather than a lockup, so the font is the right thing to draw it with.
     */
    private static void big(final GuiGraphics g, final Font font, final String text, final int cx, final int top) {
        final float scale = 3.0f;
        g.pose().pushPose();
        g.pose().translate(cx - font.width(text) * scale / 2.0f, top, 0);
        g.pose().scale(scale, scale, 1.0f);
        g.drawString(font, text, 0, 0, 0xFFFFFFFF, false);
        g.pose().popPose();
    }

    /** Near black, the maker's mark in the middle, and the ring of dots turning below it. */
    private static void frames11(final GuiGraphics g, final Font font, final int x, final int y, final int w,
                                 final int h, final int ticks, final boolean going) {
        g.fill(x, y, x + w, y + h, 0xFF0A0C10);

        final int logoY = y + h / 2 - SplashLogos.H / 2 - 10;
        SplashLogos.draw(g, SplashLogos.JSC, x + w / 2, logoY);

        /*
         * The ring: every dot is drawn, and the one at the head of the turn is bright while the rest fade back
         * behind it, which is how that spinner reads without anything being drawn between the frames.
         */
        final int cx = x + w / 2;
        final int cy = logoY + SplashLogos.H + 20;
        final int radius = 9;
        final int head = ticks * DOTS / TURN_TICKS % DOTS;
        for (int i = 0; i < DOTS; i++) {
            final double angle = Math.PI * 2 * i / DOTS - Math.PI / 2;
            final int dx = cx + (int) Math.round(Math.cos(angle) * radius);
            final int dy = cy + (int) Math.round(Math.sin(angle) * radius);
            final int behind = (head - i + DOTS) % DOTS;
            final int shade = Math.max(0x30, 0xFF - behind * 0x28);
            g.fill(dx - 1, dy - 1, dx + 1, dy + 1, 0xFF000000 | shade << 16 | shade << 8 | shade);
        }

        if (going) {
            g.drawCenteredString(font, "Restarting", x + w / 2, cy + 20, 0xFFE6ECF6);
        }
    }

    /** A vertical wash from one colour to the other, a row at a time, since the glass is small enough for it. */
    private static void gradient(final GuiGraphics g, final int x, final int y, final int w, final int h,
                                 final int top, final int bottom) {
        for (int row = 0; row < h; row++) {
            g.fill(x, y + row, x + w, y + row + 1, blend(top, bottom, (float) row / Math.max(1, h - 1)));
        }
    }

    /**
     * The clouds, at places that follow from the size of the glass.
     *
     * <p>Flat ellipses of white over the sky, each a little lighter at its top, which is as much cloud as a
     * screen this size can carry without turning into a smudge.
     */
    private static void clouds(final GuiGraphics g, final int x, final int y, final int w, final int h) {
        cloud(g, x + w / 6, y + h / 5, w / 5, h / 14);
        cloud(g, x + w * 2 / 3, y + h / 8, w / 4, h / 12);
        cloud(g, x + w / 3, y + h * 2 / 3, w / 4, h / 13);
        cloud(g, x + w * 5 / 6, y + h / 2, w / 6, h / 16);
    }

    private static void cloud(final GuiGraphics g, final int cx, final int cy, final int rw, final int rh) {
        final int wide = Math.max(6, rw);
        final int tall = Math.max(2, rh);
        for (int row = -tall; row <= tall; row++) {
            final double t = (double) row / tall;
            final int half = (int) (wide * Math.sqrt(Math.max(0, 1 - t * t)));
            final int shade = row < 0 ? 0x55FFFFFF : 0x33FFFFFF;
            g.fill(cx - half, cy + row, cx + half, cy + row + 1, shade);
        }
    }

    private static int blend(final int from, final int to, final float t) {
        final int a = channel(from, 24, to, t);
        final int r = channel(from, 16, to, t);
        final int gr = channel(from, 8, to, t);
        final int b = channel(from, 0, to, t);
        return a << 24 | r << 16 | gr << 8 | b;
    }

    private static int channel(final int from, final int shift, final int to, final float t) {
        final int a = from >> shift & 0xFF;
        final int b = to >> shift & 0xFF;
        return (int) (a + (b - a) * t);
    }
}
