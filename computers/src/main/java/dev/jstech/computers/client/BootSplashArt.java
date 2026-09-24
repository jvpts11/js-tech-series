/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.os.boot.BootSplash;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * The pictures the systems come up behind, and go down behind.
 *
 * <p>The grounds are pictures where they are pictures (a sky with its clouds, the blue bands of the later edition)
 * and plain fills where they are plain (a black screen, a near-black one). What sits on that ground is a texture
 * too, because a maker's lockup is lettering with weights and a face of its own, and drawing one out of rectangles
 * and the game's font gets the words right and the thing itself wrong.
 * They are the one thing about starting a machine that nobody reads: a system's picture is what tells you
 * which system it is before a single word of it is on the glass.
 *
 * <p>Their colours are the palettes {@code jsc:boot/<edition>}.
 */
@PaletteHolder
public final class BootSplashArt {

    /** The sky the oldest of the Frames editions came up on, clouds and all, and the size it is made at. */
    private static final ResourceLocation FRAMES_95_SKY =
            ResourceLocation.fromNamespaceAndPath("jsc", "textures/gui/splash/ground/frames_95.png");

    /** The blue bands the later edition drew every screen after its start on. */
    private static final ResourceLocation FRAMES_XP_BANDS =
            ResourceLocation.fromNamespaceAndPath("jsc", "textures/gui/splash/ground/frames_xp_bands.png");

    private static final int GROUND_W = 384;
    private static final int GROUND_H = 256;

    /**
     * The oldest edition: the band along the foot of its sky, the colours its bar ran through (darkest at the ends
     * and brightest in the middle), its last screen in black with the one sentence in the amber those machines
     * wrote it in, and the line it says on its way down.
     */
    private static final Palette<Classic> CLASSIC = Palettes.declare(JsComputers.MODID, "boot/frames_95",
            new Classic(0xFF000080, 0xFF000080, 0xFF1084D0, 0xFF7FCBFF, 0xFF000000, 0xFFE8A33A, 0xFFFFFFFF));

    /**
     * The next edition: its black start screen, the trough its blocks run through and the blocks, the rule and the
     * line on its way down, its small print and maker's name, and the word it welcomes you with.
     */
    private static final Palette<Luna> LUNA = Palettes.declare(JsComputers.MODID, "boot/frames_xp",
            new Luna(0xFF000000, 0xFF1B1B1B, 0xFF454545, 0xFF5A8CD8, 0xA0FFFFFF, 0xFFFFFFFF, 0xFF9AA4B2,
                    0xFFFFFFFF, 0xFFFFFFFF));

    /** The newest edition: the ground its firmware posts against, its greeting and the lines under it. */
    private static final Palette<Modern> MODERN = Palettes.declare(JsComputers.MODID, "boot/frames_11",
            new Modern(0xFF10121C, 0xFFFFFFFF, 0xFFA8B2C6, 0xFFE6ECF6));

    /** How wide one pass of that bar is, against the glass, and how long one pass takes in ticks. */
    private static final int RUN_SPAN = 4;
    private static final int RUN_TICKS = 32;

    /** How much of a going-down that edition spent on that last screen, in hundredths. */
    private static final int SAFE_FROM = 72;

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
     * @param ticks    how far into the wait the machine is, which is what moves anything that moves
     * @param total    how long the whole wait is, for the pictures that measure themselves against it
     * @param endsDark what follows this is a dark monitor rather than another screen, which is what tells a
     *                 machine being switched off from one starting over
     * @param title    what the system has to say over its picture, which is almost always nothing
     * @param subtitle the line under it, which on the way down is the sentence that system said while it closed
     */
    public static void draw(final GuiGraphics g, final Font font, final BootSplash splash, final int x,
                            final int y, final int w, final int h, final int ticks, final int total,
                            final boolean endsDark, final String title, final String subtitle) {
        switch (splash) {
            case FRAMES_95 -> frames95(g, font, x, y, w, h, ticks, total, endsDark, subtitle);
            case FRAMES_XP -> framesXp(g, font, x, y, w, h, ticks, total, subtitle);
            case FRAMES_11 -> frames11(g, font, x, y, w, h, ticks, title, subtitle);
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
                                 final int h, final int ticks, final int total, final boolean endsDark,
                                 final String message) {
        final Classic c = CLASSIC.get();
        final boolean going = !message.isEmpty();
        /*
         * A machine of that age finished by switching its own power off, and the last thing on the glass was
         * one sentence telling the person in front of it that they could reach for the switch. It is the end
         * of the same wait rather than a screen of its own, so it takes the last part of it; a machine coming
         * straight back up never reached it.
         */
        if (going && endsDark && total > 0 && ticks >= total * SAFE_FROM / 100) {
            g.fill(x, y, x + w, y + h, c.safeGround());
            g.drawCenteredString(font, GameText.resolve(MonitorScreenTexts.SAFE_TO_TURN_OFF),
                    x + w / 2, y + h / 2 - 4, c.safeText());
            return;
        }
        ground(g, FRAMES_95_SKY, x, y, w, h);

        final int logoY = y + h / 2 - SplashLogos.H / 2 - 8;
        SplashLogos.draw(g, SplashLogos.FRAMES_95, x + w / 2, logoY);

        /*
         * The bar along the foot never told anybody how far along the load was: it was one band of colour
         * running left to right and starting over, for as long as the machine took. A bar that fills was the
         * one thing that screen deliberately did not have.
         */
        if (going) {
            g.drawCenteredString(font, message, x + w / 2, y + h * 71 / 100, c.message());
            return;
        }
        final int band = Math.max(6, h / 16);
        g.fill(x, y + h - band, x + w, y + h, c.horizon());
        runningBar(g, c, x, y + h - band + 1, w, band - 2, ticks);
    }

    /**
     * One band of colour running along the foot and starting over, which is what that bar always was.
     *
     * <p>Drawn as columns: every column takes its colour from where it falls inside the pass, and the whole
     * pass slides along by the tick, so the light runs through the dark and comes round again.
     */
    private static void runningBar(final GuiGraphics g, final Classic c, final int x, final int y, final int w,
                                   final int h, final int ticks) {
        final int span = Math.max(8, w / RUN_SPAN);
        final int offset = ticks * span / RUN_TICKS % span;
        for (int column = 0; column < w; column++) {
            final float at = ((column + span - offset) % span) / (float) span;
            /* Dark at both ends of a pass and brightest in the middle, which is how that band was made. */
            final int shade = at < 0.5f
                    ? blend(c.runDark(), c.runBright(), at * 2.0f)
                    : blend(c.runBright(), c.runDark(), (at - 0.5f) * 2.0f);
            final int toned = at < 0.25f || at > 0.75f ? blend(shade, c.runMid(), 0.35f) : shade;
            g.fill(x + column, y, x + column + 1, y + h, toned);
        }
    }

    /** Black, the logo above the middle, the trough beneath it, and the small print at the feet. */
    private static void framesXp(final GuiGraphics g, final Font font, final int x, final int y, final int w,
                                 final int h, final int ticks, final int total, final String message) {
        /*
         * A machine of that edition went down on the same blue bands it welcomed you on, with the logo moved
         * to one side and the sentence beside it. It never went down on the black start screen, which is the
         * one screen of the three that only ever belonged to coming up.
         */
        final Luna c = LUNA.get();
        if (!message.isEmpty()) {
            bands(g, x, y, w, h);
            final int logoY = y + h / 2 - SplashLogos.H / 2;
            SplashLogos.draw(g, SplashLogos.FRAMES_XP, x + w / 4 + 6, logoY);
            g.fill(x + w * 53 / 100, y + h * 30 / 100, x + w * 53 / 100 + 1, y + h * 70 / 100, c.rule());
            g.drawString(font, message, x + w * 57 / 100, y + h / 2 - 4, c.message(), false);
            return;
        }
        /*
         * That edition came up in two beats, and the second is the one people remember by name: the logo over
         * its trough, and then a blue ground with one word on it while the desktop was made ready.
         */
        if (total > 0 && ticks >= total * WELCOME_FROM / 100) {
            welcome(g, font, x, y, w, h);
            return;
        }
        g.fill(x, y, x + w, y + h, c.ground());

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
        g.fill(tx - 1, ty - 1, tx + troughW + 1, ty + 7, c.troughEdge());
        g.fill(tx, ty, tx + troughW, ty + 6, c.trough());
        final int blockW2 = 6;
        final int span = troughW + BLOCKS * (blockW2 + 2);
        for (int i = 0; i < BLOCKS; i++) {
            final int at = (ticks * span / PASS_TICKS + i * (blockW2 + 2)) % span - BLOCKS * (blockW2 + 2);
            final int left = tx + at;
            if (left + blockW2 > tx && left < tx + troughW) {
                g.fill(Math.max(tx, left), ty + 1, Math.min(tx + troughW, left + blockW2), ty + 5, c.block());
            }
        }

        g.drawString(font, "(C) 2001 Midsoft Corp.", x + 8, y + h - 12, c.smallPrint(), false);
        g.drawString(font, "Midsoft", x + w - font.width("Midsoft") - 8, y + h - 12, c.maker(), false);
    }

    /** The blue ground with one word on it, which is how that edition ended every start. */
    private static void welcome(final GuiGraphics g, final Font font, final int x, final int y, final int w,
                                final int h) {
        bands(g, x, y, w, h);
        big(g, font, GameText.resolve(MonitorScreenTexts.WELCOME), x + w * 45 / 100, y + h / 2 - 12,
                LUNA.get().welcome());
    }

    /**
     * The three bands that edition drew every screen after its start on: one blue at the top, a lighter one
     * across the middle with a soft light over its left, the first again at the foot.
     */
    private static void bands(final GuiGraphics g, final int x, final int y, final int w, final int h) {
        ground(g, FRAMES_XP_BANDS, x, y, w, h);
    }

    /** A ground picture laid over the whole glass, made at the glass's own size and drawn to whatever it is. */
    private static void ground(final GuiGraphics g, final ResourceLocation picture, final int x, final int y,
                               final int w, final int h) {
        g.blit(picture, x, y, w, h, 0.0F, 0.0F, GROUND_W, GROUND_H, GROUND_W, GROUND_H);
    }

    /**
     * One word at three times the font's size, which is the only way to write large with the game's own
     * letters. It is a word rather than a lockup, so the font is the right thing to draw it with.
     */
    private static void big(final GuiGraphics g, final Font font, final String text, final int cx, final int top,
                            final int colour) {
        final float scale = 3.0f;
        g.pose().pushPose();
        g.pose().translate(cx - font.width(text) * scale / 2.0f, top, 0);
        g.pose().scale(scale, scale, 1.0f);
        g.drawString(font, text, 0, 0, colour, false);
        g.pose().popPose();
    }

    /**
     * The same ground the firmware posted on, the maker's mark still on it, and the ring of dots turning below.
     *
     * <p>The ground is the firmware's own on purpose. The mark stays where the self-test left it and the
     * spinner starts under it, so the moment the firmware hands the machine over passes without a flash, which
     * is the whole trick those machines play.
     */
    private static void frames11(final GuiGraphics g, final Font font, final int x, final int y, final int w,
                                 final int h, final int ticks, final String title, final String subtitle) {
        final Modern c = MODERN.get();
        g.fill(x, y, x + w, y + h, c.ground());

        final int logoY = y + h / 2 - SplashLogos.H / 2 - 10;
        /*
         * The one start that says something greets the machine by name INSTEAD of showing the maker's mark,
         * which is the whole of what makes a first start feel like one. Every other start shows the mark.
         */
        if (title.isEmpty()) {
            SplashLogos.draw(g, SplashLogos.JSC, x + w / 2, logoY);
        } else {
            big(g, font, title, x + w / 2, logoY + 6, c.greeting());
            g.drawCenteredString(font, subtitle, x + w / 2, logoY + SplashLogos.H + 2, c.subtitle());
        }

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
            g.fill(dx - 1, dy - 1, dx + 1, dy + 1, 0xFF << 24 | shade << 16 | shade << 8 | shade);
        }

        /* The one word this edition puts on its way down, under the mark it came up behind. */
        if (title.isEmpty() && !subtitle.isEmpty()) {
            g.drawCenteredString(font, subtitle, x + w / 2, cy + 20, c.goodbye());
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

    /** The oldest edition's colours, as the palette above names them. */
    private record Classic(int horizon, int runDark, int runMid, int runBright, int safeGround, int safeText,
                           int message) {
    }

    /** The next edition's colours, as the palette above names them. */
    private record Luna(int ground, int trough, int troughEdge, int block, int rule, int message, int smallPrint,
                        int maker, int welcome) {
    }

    /** The newest edition's colours, as the palette above names them. */
    private record Modern(int ground, int greeting, int subtitle, int goodbye) {
    }
}
