/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.client.os.CdeSplashArt;
import dev.jstech.computers.gui.CdeStyle;
import dev.jstech.computers.os.DesktopEnvironmentDef;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.PanelStyle;
import dev.jstech.computers.os.boot.BootIdentity;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.tier.HardwareEra;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

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
 *
 * <p>Their colours are the palettes {@code jsc:splash/<desktop>}, with {@code _classic} for the older looks.
 */
@PaletteHolder
public final class DesktopSplashArt {

    /** How far into the wait the desktop takes over from the system's own lines, in hundredths. */
    public static final int FROM = 68;

    /** How many pieces the older desktops reported starting, one square each. */
    private static final int STEPS = 5;

    /** What those older ones said while each piece came up, in the order they came up in. */
    private static final TextKey[] STAGES = {
            MonitorScreenTexts.STARTING_PANEL,
            MonitorScreenTexts.STARTING_DESKTOP,
            MonitorScreenTexts.STARTING_FILES,
            MonitorScreenTexts.RESTORING_SESSION,
            MonitorScreenTexts.READY,
    };

    /** The launcher's blue, the ground it sits on, and the line at the foot of the modern KDE screen. */
    private static final Palette<Plasma> PLASMA = Palettes.declare(JsComputers.MODID, "splash/kde_plasma",
            new Plasma(0xFF1D6FB8, 0xFF072747, 0xFF3DAEE9, 0xFF9EC3E0, 0xFFFCFCFC, 0x28FFFFFF));

    /** The modern GNOME ground, its one light grey, and the quieter grey under it. */
    private static final Palette<Gnome> GNOME = Palettes.declare(JsComputers.MODID, "splash/gnome",
            new Gnome(0xFF1D1D20, 0xFFDEDDDA, 0xFF8E8D8A));

    /** The greens of that desktop's ground, the pale one at its foot, its name, and its running dots. */
    private static final Palette<Mint> MINT = Palettes.declare(JsComputers.MODID, "splash/cinnamon",
            new Mint(0xFF1B5E4A, 0xFF2B8A6E, 0xFFCFE8DA, 0xFFFFFFFF, 0xFFFFFFFF, 0x59FFFFFF));

    /**
     * The two bands that told the older desktops apart across a room, the grounds and faces behind them, and the
     * box they share: its rule, its dark text, its shadow, the name and line in the band, and the squares.
     */
    private static final Palette<OldBox> KDE_CLASSIC = Palettes.declare(JsComputers.MODID, "splash/kde_classic",
            new OldBox(0xFF33679F, 0xFFD6D2CD, 0xFF6F9FD0, 0xFF33679F, 0xFF1D4C80,
                    0xFF6F6A64, 0xFF1A1A1A, 0x4C000000, 0xFFFFFFFF, 0xFFDCE8F6, 0xFFE2DED4,
                    0xFFF0B23A, 0xFF5FE07A, 0xFF7D8A9C));
    private static final Palette<OldBox> GNOME_CLASSIC = Palettes.declare(JsComputers.MODID, "splash/gnome_classic",
            new OldBox(0xFF3E3A34, 0xFFD6D2C8, 0xFF8F7D99, 0xFF6D5A78, 0xFF55455F,
                    0xFF6F6A64, 0xFF1A1A1A, 0x4C000000, 0xFFFFFFFF, 0xFFDCE8F6, 0xFFE2DED4,
                    0xFFF0B23A, 0xFF5FE07A, 0xFF7D8A9C));

    /** How much of a square's colour is left before its piece has started, as an alpha. */
    private static final int DIMMED_ALPHA = 0x52;

    /** The marks the two desktops come up behind, and the size they are made at. */
    private static final ResourceLocation PLASMA_MARK =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "textures/gui/splash/kde_plasma_mark.png");
    private static final ResourceLocation CINNAMON_MARK =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "textures/gui/splash/cinnamon_mark.png");
    private static final int MARK = 30;

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
     * <p>Only the desktops that are a separate thing from the system under them, which here are the ones of the
     * Unix families: they are chosen, installed and replaced on their own, and each one announces itself. A
     * system that comes with its desktop built in has no such moment, and asking this of one of those used to
     * answer yes, so every Frames machine finished its start behind another system's loading screen.
     *
     * @param desktopId the desktop by the last part of its id, or empty when none is installed
     */
    public static boolean has(final String desktopId) {
        final PanelStyle style = styleOf(desktopId);
        return style != null && style.unixLike();
    }

    /**
     * Draws that desktop's loading screen over the glass at {@code (x, y)}.
     *
     * @param who      the desktop by the last part of its id, the system it is coming up on, which two of these
     *                 name at the foot, and the machine's host name, which one of them names
     * @param era      the generation of the machine, which decides which of the two looks it wears
     * @param ticks    how far into the wait the machine is, which is what moves anything that moves
     * @param progress how far through the desktop's own share of the wait, in hundredths
     */
    public static void draw(final GuiGraphics g, final Font font, final BootIdentity who, final HardwareEra era,
                            final int x, final int y, final int w, final int h, final int ticks,
                            final int progress) {
        final boolean old = era != null && era.compareTo(HardwareEra.STANDARD) < 0;
        final String systemName = who.systemName();
        final PanelStyle style = styleOf(who.desktopId());
        switch (style == null ? PanelStyle.GNOME : style) {
            // CDE never changed its face, so it has one look, drawn where the rest of CDE is drawn.
            case CDE -> CdeSplashArt.draw(g, font, who.hostName(), CdeStyle.parse(who.look()), x, y, w, h);
            case KDE -> {
                if (old) {
                    oldBox(g, font, x, y, w, h, progress, KDE_CLASSIC.get(), "KDE", "K Desktop Environment");
                } else {
                    kdePlasma(g, font, x, y, w, h, progress);
                }
            }
            case CINNAMON -> cinnamon(g, font, x, y, w, h, ticks, systemName);
            /* Only GNOME is left to reach here; {@link #has} is what keeps anything else from asking. */
            default -> {
                if (old) {
                    oldBox(g, font, x, y, w, h, progress, GNOME_CLASSIC.get(), "GNOME",
                            GameText.resolve(MonitorScreenTexts.STARTING_YOUR_DESKTOP));
                } else {
                    gnome(g, font, x, y, w, h, ticks, systemName);
                }
            }
        }
    }

    /** The launcher's mark, the name under it, and a bar that really does say how far along the desktop is. */
    private static void kdePlasma(final GuiGraphics g, final Font font, final int x, final int y, final int w,
                                  final int h, final int progress) {
        final Plasma c = PLASMA.get();
        gradient(g, x, y, w, h, c.top(), c.bottom());
        final int cx = x + w / 2;
        final int side = MARK;
        final int top = y + h / 2 - 34;
        g.blit(PLASMA_MARK, cx - side / 2, top, 0.0F, 0.0F, side, side, side, side);
        big(g, font, "Plasma", cx, top + side + 6, 1.8f, c.name());

        final int barW = 88;
        final int by = top + side + 28;
        g.fill(cx - barW / 2, by, cx + barW / 2, by + 2, c.track());
        g.fill(cx - barW / 2, by, cx - barW / 2 + barW * progress / 100, by + 2, c.blue());
        small(g, font, "KDE Plasma · the KDE Guild", cx, y + h - 14, c.foot());
    }

    /** A dark ground, the turning ring, and the desktop's name over the distribution at the foot. */
    private static void gnome(final GuiGraphics g, final Font font, final int x, final int y, final int w,
                              final int h, final int ticks, final String systemName) {
        final Gnome c = GNOME.get();
        g.fill(x, y, x + w, y + h, c.ground());
        spinner(g, x + w / 2, y + h / 2 - 12, ticks, c.text());
        big(g, font, "GNOME", x + w / 2, y + h - 30, 1.4f, c.text());
        small(g, font, systemName.isEmpty() ? "" : GameText.resolve(MonitorScreenTexts.ON_SYSTEM.with(systemName)),
                x + w / 2, y + h - 14, c.foot());
    }

    /** The menu button's mark on the green it wears, its name, and three dots running under it. */
    private static void cinnamon(final GuiGraphics g, final Font font, final int x, final int y, final int w,
                                 final int h, final int ticks, final String systemName) {
        final Mint c = MINT.get();
        gradient(g, x, y, w, h, c.top(), c.bottom());
        final int cx = x + w / 2;
        final int side = MARK;
        final int top = y + h / 2 - 32;
        // The menu button's mark: the green plate, the white square in it, and the green one inside that.
        g.blit(CINNAMON_MARK, cx - side / 2, top, 0.0F, 0.0F, side, side, side, side);
        big(g, font, "Cinnamon", cx, top + side + 8, 1.7f, c.name());

        final int dy = top + side + 30;
        final int lit = ticks / PULSE_TICKS % 3;
        for (int i = 0; i < 3; i++) {
            final int dx = cx - 10 + i * 10;
            g.fill(dx, dy, dx + 4, dy + 4, i == lit ? c.dotLit() : c.dot());
        }
        small(g, font, systemName, cx, y + h - 14, c.foot());
    }

    /**
     * The box the desktops of the earlier generation came up in: a coloured band with the name in it, a row of
     * squares lighting up as each piece started, and a line saying which piece that was.
     *
     * <p>One drawing for both of them, because they really were the same box in two colours: the band and the
     * face changed and the shape did not, which is exactly what a player of that age would have seen.
     */
    private static void oldBox(final GuiGraphics g, final Font font, final int x, final int y, final int w,
                               final int h, final int progress, final OldBox c, final String name,
                               final String under) {
        g.fill(x, y, x + w, y + h, c.ground());
        final int boxW = w * 68 / 100;
        final int boxH = 100;
        final int bx = x + (w - boxW) / 2;
        final int by = y + (h - boxH) / 2;
        g.fill(bx + 3, by + 3, bx + boxW + 3, by + boxH + 3, c.shadow());
        g.fill(bx, by, bx + boxW, by + boxH, c.face());
        rule(g, bx, by, boxW, boxH, c.rule());

        final int bandH = 51;
        for (int row = 0; row < bandH; row++) {
            final float at = row / (float) (bandH - 1);
            final int shade = at < 0.55f
                    ? blend(c.bandTop(), c.bandMid(), at / 0.55f)
                    : blend(c.bandMid(), c.bandLow(), (at - 0.55f) / 0.45f);
            g.fill(bx + 1, by + 1 + row, bx + boxW - 1, by + 2 + row, shade);
        }
        g.fill(bx, by + bandH, bx + boxW, by + bandH + 1, c.rule());
        big(g, font, name, bx + boxW / 2, by + 10, 2.2f, c.nameInk());
        small(g, font, under, bx + boxW / 2, by + 36, c.underInk());

        /* The squares: one per piece, lit in order, so the row fills as the desktop assembles itself. */
        final int side = 19;
        final int gap = 8;
        final int rowW = STEPS * side + (STEPS - 1) * gap;
        final int sx = bx + (boxW - rowW) / 2;
        final int sy = by + bandH + 10;
        final int done = Math.min(STEPS, Math.max(1, STEPS * progress / 100 + 1));
        final int[] inner = {c.bandLow(), c.bandMid(), c.stepThree(), c.stepFour(), c.stepFive()};
        for (int i = 0; i < STEPS; i++) {
            final int px = sx + i * (side + gap);
            g.fill(px, sy, px + side, sy + side, c.square());
            rule(g, px, sy, side, side, c.rule());
            g.fill(px + 5, sy + 5, px + side - 5, sy + side - 5,
                    i < done ? inner[i] : dimmed(inner[i]));
        }
        small(g, font, GameText.resolve(STAGES[Math.min(STAGES.length - 1, done - 1)]), bx + boxW / 2,
                sy + side + 8, c.text());
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
        return DIMMED_ALPHA << 24 | color & 0xFFFFFF;
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

    /** The chrome the desktop under that last part of an id declares, or null when there is no such desktop. */
    @Nullable
    private static PanelStyle styleOf(final String desktopId) {
        if (desktopId == null || desktopId.isEmpty()) {
            return null;
        }
        final DesktopEnvironmentDef desktop =
                OsRegistry.getDesktop(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, desktopId));
        return desktop == null ? null : desktop.panelStyle();
    }

    /** The modern KDE screen: its ground from top to bottom, the bar's blue, the foot, the name and the track. */
    private record Plasma(int top, int bottom, int blue, int foot, int name, int track) {
    }

    /** The modern GNOME screen: its ground, its one light grey, and the quieter grey under it. */
    private record Gnome(int ground, int text, int foot) {
    }

    /** The Cinnamon screen: its ground from top to bottom, its foot, its name, and a running dot lit and not. */
    private record Mint(int top, int bottom, int foot, int name, int dotLit, int dot) {
    }

    /**
     * The box of an older desktop: the ground behind it, its face, its band from top to bottom, its rule and dark
     * text, the shadow under it, the name and line in the band, the squares' face, and the last three squares'
     * colours (the first two are the band's own).
     */
    private record OldBox(int ground, int face, int bandTop, int bandMid, int bandLow, int rule, int text, int shadow,
                          int nameInk, int underInk, int square, int stepThree, int stepFour, int stepFive) {
    }
}
