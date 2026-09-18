/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.core.tier.HardwareEra;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The picture a desktop puts up between its system's last line and the desktop itself.
 *
 * <p>These come after the init lines and before anything a player can click, and they are the one moment a
 * distribution stops sounding like every other one: two machines running the same kernel and the same service
 * manager show completely different things here, because the desktop is the part somebody chose.
 *
 * <p>Two looks for each, because a desktop of the earlier generation did not look like the one that carries its
 * name now: the older ones put up a bordered box with a coloured band and a row of squares lighting up as each
 * piece started, and the modern ones fill the glass and say almost nothing.
 */
public final class DesktopSplashArt {

    /** How far into the wait the desktop takes over from the system's own lines, in hundredths. */
    public static final int FROM = 68;

    /** How many pieces the older desktops reported starting, one square each. */
    private static final int STEPS = 5;

    /** What those older ones said while each piece came up, in the order they came up in. */
    private static final String[] STAGES = {
            "Starting the panel...",
            "Starting the desktop...",
            "Starting Files...",
            "Restoring the session...",
            "Ready",
    };

    /** The launcher's blue, the ground it sits on, and the line at the foot of the modern KDE screen. */
    private static final int KDE_TOP = 0xFF1D6FB8;
    private static final int KDE_BOTTOM = 0xFF072747;
    private static final int KDE_BLUE = 0xFF3DAEE9;
    private static final int KDE_FOOT = 0xFF9EC3E0;

    /** The modern GNOME ground, its one light grey, and the quieter grey under it. */
    private static final int GNOME_GROUND = 0xFF1D1D20;
    private static final int GNOME_TEXT = 0xFFDEDDDA;
    private static final int GNOME_FOOT = 0xFF8E8D8A;

    /** The greens of the menu button that desktop is known by, and its ground. */
    private static final int MINT_TOP = 0xFF1B5E4A;
    private static final int MINT_BOTTOM = 0xFF2B8A6E;
    private static final int MINT_GREEN = 0xFF69B03B;
    private static final int MINT_FOOT = 0xFFCFE8DA;

    /** The older box: its face, its rule, and the dark text on it. */
    private static final int BOX_FACE = 0xFFD6D2CD;
    private static final int BOX_RULE = 0xFF6F6A64;
    private static final int BOX_TEXT = 0xFF1A1A1A;

    /** The two bands that told those two desktops apart across a room, and the grounds behind them. */
    private static final int KDE_OLD_GROUND = 0xFF33679F;
    private static final int KDE_BAND_TOP = 0xFF6F9FD0;
    private static final int KDE_BAND_MID = 0xFF33679F;
    private static final int KDE_BAND_LOW = 0xFF1D4C80;
    private static final int GNOME_OLD_GROUND = 0xFF3E3A34;
    private static final int GNOME_OLD_FACE = 0xFFD6D2C8;
    private static final int GNOME_BAND_TOP = 0xFF8F7D99;
    private static final int GNOME_BAND_MID = 0xFF6D5A78;
    private static final int GNOME_BAND_LOW = 0xFF55455F;

    /** How long one turn of the modern spinner takes, and how many dots it is made of. */
    private static final int DOTS = 8;
    private static final int TURN_TICKS = 32;

    /** How long one pass of the older desktops' three running dots takes. */
    private static final int PULSE_TICKS = 12;

    private DesktopSplashArt() {
    }

    /**
     * Whether this desktop has a loading screen of its own.
     *
     * <p>Only the desktops that are a separate thing from the system under them, which here is the Linux
     * family: they are chosen, installed and replaced on their own, and each one announces itself. A system
     * that comes with its desktop built in has no such moment, and asking this of one of those used to answer
     * yes, so every Frames machine finished its start behind another system's loading screen.
     *
     * @param desktopId the desktop by the last part of its id, or empty when none is installed
     */
    public static boolean has(final String desktopId) {
        return switch (desktopId) {
            case "kde_plasma", "gnome", "cinnamon" -> true;
            default -> false;
        };
    }

    /**
     * Draws that desktop's loading screen over the glass at {@code (x, y)}.
     *
     * @param desktopId  the desktop by the last part of its id
     * @param systemName the distribution it is coming up on, which two of these name at the foot
     * @param era        the generation of the machine, which decides which of the two looks it wears
     * @param ticks      how far into the wait the machine is, which is what moves anything that moves
     * @param progress   how far through the desktop's own share of the wait, in hundredths
     */
    public static void draw(final GuiGraphics g, final Font font, final String desktopId,
                            final String systemName, final HardwareEra era, final int x, final int y,
                            final int w, final int h, final int ticks, final int progress) {
        final boolean old = era != null && era.compareTo(HardwareEra.STANDARD) < 0;
        switch (desktopId) {
            case "kde_plasma" -> {
                if (old) {
                    oldBox(g, font, x, y, w, h, progress, KDE_OLD_GROUND, BOX_FACE, "KDE",
                            "K Desktop Environment", KDE_BAND_TOP, KDE_BAND_MID, KDE_BAND_LOW);
                } else {
                    kdePlasma(g, font, x, y, w, h, progress);
                }
            }
            case "cinnamon" -> cinnamon(g, font, x, y, w, h, ticks, systemName);
            /* Only the three reach here; {@link #has} is what keeps anything else from asking. */
            default -> {
                if (old) {
                    oldBox(g, font, x, y, w, h, progress, GNOME_OLD_GROUND, GNOME_OLD_FACE, "GNOME",
                            "starting your desktop", GNOME_BAND_TOP, GNOME_BAND_MID, GNOME_BAND_LOW);
                } else {
                    gnome(g, font, x, y, w, h, ticks, systemName);
                }
            }
        }
    }

    /** The launcher's mark, the name under it, and a bar that really does say how far along the desktop is. */
    private static void kdePlasma(final GuiGraphics g, final Font font, final int x, final int y, final int w,
                                  final int h, final int progress) {
        gradient(g, x, y, w, h, KDE_TOP, KDE_BOTTOM);
        final int cx = x + w / 2;
        final int side = 30;
        final int top = y + h / 2 - 34;
        g.fill(cx - side / 2, top, cx + side / 2, top + side, 0x243DAEE9);
        rule(g, cx - side / 2, top, side, side, KDE_BLUE);
        big(g, font, "K", cx, top + 8, 2.0f, 0xFFFCFCFC);
        big(g, font, "Plasma", cx, top + side + 6, 1.8f, 0xFFFCFCFC);

        final int barW = 88;
        final int by = top + side + 28;
        g.fill(cx - barW / 2, by, cx + barW / 2, by + 2, 0x28FFFFFF);
        g.fill(cx - barW / 2, by, cx - barW / 2 + barW * progress / 100, by + 2, KDE_BLUE);
        small(g, font, "KDE Plasma · the KDE Guild", cx, y + h - 14, KDE_FOOT);
    }

    /** A dark ground, the turning ring, and the desktop's name over the distribution at the foot. */
    private static void gnome(final GuiGraphics g, final Font font, final int x, final int y, final int w,
                              final int h, final int ticks, final String systemName) {
        g.fill(x, y, x + w, y + h, GNOME_GROUND);
        spinner(g, x + w / 2, y + h / 2 - 12, ticks, GNOME_TEXT);
        big(g, font, "GNOME", x + w / 2, y + h - 30, 1.4f, GNOME_TEXT);
        small(g, font, systemName.isEmpty() ? "" : "on " + systemName, x + w / 2, y + h - 14, GNOME_FOOT);
    }

    /** The menu button's mark on the green it wears, its name, and three dots running under it. */
    private static void cinnamon(final GuiGraphics g, final Font font, final int x, final int y, final int w,
                                 final int h, final int ticks, final String systemName) {
        gradient(g, x, y, w, h, MINT_TOP, MINT_BOTTOM);
        final int cx = x + w / 2;
        final int side = 30;
        final int top = y + h / 2 - 32;
        /* The button's mark: the green plate, the white square in it, and the green one inside that. */
        g.fill(cx - side / 2, top, cx + side / 2, top + side, MINT_GREEN);
        g.fill(cx - 9, top + 6, cx + 9, top + 24, 0xFFFFFFFF);
        g.fill(cx - 4, top + 11, cx + 4, top + 19, MINT_GREEN);
        big(g, font, "Cinnamon", cx, top + side + 8, 1.7f, 0xFFFFFFFF);

        final int dy = top + side + 30;
        final int lit = ticks / PULSE_TICKS % 3;
        for (int i = 0; i < 3; i++) {
            final int dx = cx - 10 + i * 10;
            g.fill(dx, dy, dx + 4, dy + 4, i == lit ? 0xFFFFFFFF : 0x59FFFFFF);
        }
        small(g, font, systemName, cx, y + h - 14, MINT_FOOT);
    }

    /**
     * The box the desktops of the earlier generation came up in: a coloured band with the name in it, a row of
     * squares lighting up as each piece started, and a line saying which piece that was.
     *
     * <p>One drawing for both of them, because they really were the same box in two colours: the band and the
     * face changed and the shape did not, which is exactly what a player of that age would have seen.
     */
    private static void oldBox(final GuiGraphics g, final Font font, final int x, final int y, final int w,
                               final int h, final int progress, final int ground, final int face,
                               final String name, final String under, final int bandTop, final int bandMid,
                               final int bandLow) {
        g.fill(x, y, x + w, y + h, ground);
        final int boxW = w * 68 / 100;
        final int boxH = 100;
        final int bx = x + (w - boxW) / 2;
        final int by = y + (h - boxH) / 2;
        g.fill(bx + 3, by + 3, bx + boxW + 3, by + boxH + 3, 0x4C000000);
        g.fill(bx, by, bx + boxW, by + boxH, face);
        rule(g, bx, by, boxW, boxH, BOX_RULE);

        final int bandH = 51;
        for (int row = 0; row < bandH; row++) {
            final float at = row / (float) (bandH - 1);
            final int shade = at < 0.55f
                    ? blend(bandTop, bandMid, at / 0.55f)
                    : blend(bandMid, bandLow, (at - 0.55f) / 0.45f);
            g.fill(bx + 1, by + 1 + row, bx + boxW - 1, by + 2 + row, shade);
        }
        g.fill(bx, by + bandH, bx + boxW, by + bandH + 1, BOX_RULE);
        big(g, font, name, bx + boxW / 2, by + 10, 2.2f, 0xFFFFFFFF);
        small(g, font, under, bx + boxW / 2, by + 36, 0xFFDCE8F6);

        /* The squares: one per piece, lit in order, so the row fills as the desktop assembles itself. */
        final int side = 19;
        final int gap = 8;
        final int rowW = STEPS * side + (STEPS - 1) * gap;
        final int sx = bx + (boxW - rowW) / 2;
        final int sy = by + bandH + 10;
        final int done = Math.min(STEPS, Math.max(1, STEPS * progress / 100 + 1));
        final int[] inner = {bandLow, bandMid, 0xFFF0B23A, 0xFF5FE07A, 0xFF7D8A9C};
        for (int i = 0; i < STEPS; i++) {
            final int px = sx + i * (side + gap);
            g.fill(px, sy, px + side, sy + side, 0xFFE2DED4);
            rule(g, px, sy, side, side, BOX_RULE);
            g.fill(px + 5, sy + 5, px + side - 5, sy + side - 5,
                    i < done ? inner[i] : dimmed(inner[i]));
        }
        small(g, font, STAGES[Math.min(STAGES.length - 1, done - 1)], bx + boxW / 2,
                sy + side + 8, BOX_TEXT);
    }

    /** The ring of dots the modern desktops turn, brightest at the head of the turn. */
    private static void spinner(final GuiGraphics g, final int cx, final int cy, final int ticks,
                                final int color) {
        final int radius = 9;
        final int head = ticks * DOTS / TURN_TICKS % DOTS;
        for (int i = 0; i < DOTS; i++) {
            final double angle = Math.PI * 2 * i / DOTS - Math.PI / 2;
            final int dx = cx + (int) Math.round(Math.cos(angle) * radius);
            final int dy = cy + (int) Math.round(Math.sin(angle) * radius);
            final int behind = (head - i + DOTS) % DOTS;
            final int alpha = Math.max(0x30, 0xFF - behind * 0x28);
            g.fill(dx - 1, dy - 1, dx + 1, dy + 1, alpha << 24 | color & 0xFFFFFF);
        }
    }

    /** One line at a size of its own, centred, which is the only way to write large in the game's letters. */
    private static void big(final GuiGraphics g, final Font font, final String text, final int cx,
                            final int top, final float scale, final int color) {
        if (text.isEmpty()) {
            return;
        }
        g.pose().pushPose();
        g.pose().translate(cx - font.width(text) * scale / 2.0f, top, 0.0F);
        g.pose().scale(scale, scale, 1.0F);
        g.drawString(font, text, 0, 0, color, false);
        g.pose().popPose();
    }

    /** The small print these screens put at their feet, at the size every wall of text here is drawn at. */
    private static void small(final GuiGraphics g, final Font font, final String text, final int cx,
                              final int top, final int color) {
        big(g, font, text, cx, top, 0.75f, color);
    }

    /** A one-pixel frame, which is the whole of the furniture on every one of these. */
    private static void rule(final GuiGraphics g, final int x, final int y, final int w, final int h,
                             final int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }

    /** The same colour with the life taken out of it, for a piece that has not started yet. */
    private static int dimmed(final int color) {
        return 0x52000000 | color & 0xFFFFFF;
    }

    /** A vertical wash from one colour to the other, a row at a time. */
    private static void gradient(final GuiGraphics g, final int x, final int y, final int w, final int h,
                                 final int top, final int bottom) {
        for (int row = 0; row < h; row++) {
            g.fill(x, y + row, x + w, y + row + 1, blend(top, bottom, (float) row / Math.max(1, h - 1)));
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
