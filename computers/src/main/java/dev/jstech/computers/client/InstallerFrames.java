/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.os.install.InstallerChrome;
import dev.jstech.computers.os.install.InstallerFlow;
import dev.jstech.computers.os.install.InstallerPage;
import dev.jstech.computers.os.install.InstallerStyle;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The shapes the installers are drawn in: the whole screen of text, the grey window with its title in a tab, the
 * wizard with a picture down its side, the coloured ground with the steps on the left, and the pale card.
 *
 * <p>Each one paints its own frame and hands back the rectangle the page's own content goes in, along with the
 * ink to write it in and whatever buttons it drew. The pages themselves know nothing about any of this, which is
 * what lets one installer change shape halfway through without the questions changing with it.
 */
final class InstallerFrames {

    /** How tall a title bar is in the frames that have one. */
    private static final int TITLE_BAR = 14;

    /** How tall a button is, and how far one sits from the next. */
    private static final int BUTTON = 14;

    private InstallerFrames() {
    }

    /**
     * Paints the frame for that page and answers where its content goes.
     *
     * @param sx the left of the monitor's picture
     * @param sy the top of the monitor's picture
     * @param sw how wide the picture is
     * @param sh how tall it is
     */
    static Frame paint(final GuiGraphics g, final Font font, final InstallerFlow flow, final int ticksDone,
                       final int sx, final int sy, final int sw, final int sh) {
        return switch (flow.chrome()) {
            case FULL_TEXT -> fullText(g, font, flow, sx, sy, sw, sh);
            case BOXED_TEXT -> boxedText(g, font, flow, sx, sy, sw, sh);
            case WIZARD -> wizard(g, font, flow, sx, sy, sw, sh);
            case SIDE_PANEL -> sidePanel(g, font, flow, ticksDone, sx, sy, sw, sh);
            case CARD -> card(g, font, flow, sx, sy, sw, sh);
        };
    }

    /** The whole screen in text: a heading at the top, the keys along the foot. */
    private static Frame fullText(final GuiGraphics g, final Font font, final InstallerFlow flow,
                                  final int sx, final int sy, final int sw, final int sh) {
        final Ink ink = Ink.of(flow.style());
        g.fill(sx, sy, sx + sw, sy + sh, ink.back());
        g.drawString(font, flow.style().title(flow.systemName()), sx + 8, sy + 8, ink.bright(), false);
        g.drawString(font, flow.style().heading(flow.page(), flow.systemName()), sx + 8, sy + 22, ink.text(),
                false);
        final String hint = flow.style().hint(flow.page());
        if (!hint.isEmpty()) {
            g.fill(sx, sy + sh - TITLE_BAR, sx + sw, sy + sh, ink.bar());
            g.drawString(font, hint, sx + 6, sy + sh - TITLE_BAR + 3, ink.barText(), false);
        }
        return new Frame(sx + 16, sy + 40, sw - 32, sh - 58, ink.paint(), null, null, null, null);
    }

    /** A grey window over a coloured ground, its title in a tab on the top edge. */
    private static Frame boxedText(final GuiGraphics g, final Font font, final InstallerFlow flow,
                                   final int sx, final int sy, final int sw, final int sh) {
        final Ink ink = Ink.of(flow.style());
        g.fill(sx, sy, sx + sw, sy + sh, ink.back());
        final int wx = sx + 10;
        final int wy = sy + 14;
        final int ww = sw - 20;
        final int wh = sh - 34;
        g.fill(wx + 3, wy + 3, wx + ww + 3, wy + wh + 3, 0xFF000000);
        g.fill(wx, wy, wx + ww, wy + wh, ink.panel());
        final String tab = " " + flow.style().heading(flow.page(), flow.systemName()) + " ";
        final int tabX = wx + (ww - font.width(tab)) / 2;
        g.fill(tabX, wy - 5, tabX + font.width(tab), wy + 5, ink.panel());
        g.drawString(font, tab, tabX, wy - 4, ink.panelText(), false);
        final String hint = flow.style().hint(flow.page());
        if (!hint.isEmpty()) {
            g.drawString(font, hint, sx + 8, sy + sh - 11, 0xFFFFFFFF, false);
        }
        final Paint paint = ink.paint();
        return new Frame(wx + 8, wy + 12, ww - 16, wh - 20,
                new Paint(ink.panelText(), ink.panelText(), paint.dim(), ink.accent(), ink.select(),
                        ink.selectText()),
                null, null, null, null);
    }

    /** A grey dialog with a picture down one side and the buttons along the bottom. */
    private static Frame wizard(final GuiGraphics g, final Font font, final InstallerFlow flow,
                                final int sx, final int sy, final int sw, final int sh) {
        g.fillGradient(sx, sy, sx + sw, sy + sh, 0xFF000080, 0xFF1084D0);
        g.drawString(font, flow.style().title(flow.systemName()), sx + 14, sy + 8, 0xFFFFFFFF, true);

        final int dx = sx + 22;
        final int dy = sy + 26;
        final int dw = sw - 44;
        final int dh = sh - 38;
        bevel(g, dx, dy, dw, dh, 0xFFC0C0C0, true);
        g.fillGradient(dx + 2, dy + 2, dx + dw - 2, dy + 2 + TITLE_BAR, 0xFF000080, 0xFF1084D0);
        g.drawString(font, flow.style().title(flow.systemName()) + " Wizard", dx + 6, dy + 6, 0xFFFFFFFF, false);

        final int railW = 46;
        final int railTop = dy + TITLE_BAR + 8;
        final int railBottom = dy + dh - 30;
        bevel(g, dx + 6, railTop, railW, railBottom - railTop, 0xFF008080, false);
        emblem(g, dx + 6 + (railW - 20) / 2, railTop + (railBottom - railTop - 20) / 2, 20, flow.style());

        // The groove above the buttons, which is two lines and not one: that is what makes it look pressed in.
        g.fill(dx + 6, dy + dh - 24, dx + dw - 6, dy + dh - 23, 0xFF808080);
        g.fill(dx + 6, dy + dh - 23, dx + dw - 6, dy + dh - 22, 0xFFFFFFFF);

        final int by = dy + dh - 18;
        final int[] cancel = button(g, font, dx + dw - 6 - 52, by, 52, "Cancel", false);
        final boolean canGo = flow.canContinue() || flow.page() == InstallerPage.DONE;
        final int[] next = button(g, font, dx + dw - 6 - 106, by, 52,
                flow.page() == InstallerPage.DONE ? "Restart" : "Next >", canGo);
        final int[] back = button(g, font, dx + dw - 6 - 160, by, 52, "< Back", false);

        final int cx = dx + 6 + railW + 8;
        final Paint paint = new Paint(0xFF000000, 0xFF000000, 0xFF404040, 0xFF0000A8, 0xFF000080, 0xFFFFFFFF);
        return new Frame(cx, railTop, dx + dw - 8 - cx, railBottom - railTop, paint, next, back, cancel, null);
    }

    /** A coloured ground with the steps listed down one side and the question in a dialog over it. */
    private static Frame sidePanel(final GuiGraphics g, final Font font, final InstallerFlow flow,
                                   final int ticksDone, final int sx, final int sy, final int sw, final int sh) {
        g.fill(sx, sy, sx + sw, sy + sh, 0xFF5A7EDC);
        g.fill(sx, sy, sx + sw, sy + 22, 0xFF00309C);
        g.fill(sx, sy + 22, sx + sw, sy + 23, 0xFF9DB9EB);
        emblem(g, sx + 8, sy + 5, 12, flow.style());
        g.drawString(font, "Midsoft", sx + 24, sy + 4, 0xFFBFD0F0, false);
        g.drawString(font, flow.systemName(), sx + 24, sy + 13, 0xFFFFFFFF, false);
        g.fill(sx, sy + sh - 14, sx + sw, sy + sh, 0xFF00309C);
        g.fill(sx, sy + sh - 15, sx + sw, sy + sh - 14, 0xFFF3A660);

        final int panelW = 116;
        g.fillGradient(sx, sy + 23, sx + panelW, sy + sh - 15, 0xFF2A56C6, 0xFF1B3FA8);
        int ty = sy + 30;
        final int running = flow.stepAt(ticksDone);
        for (int i = 0; i < flow.steps().size(); i++) {
            final boolean done = i < running;
            final boolean now = i == running;
            g.fill(sx + 8, ty + 2, sx + 13, ty + 7, done ? 0xFF5FE07A : now ? 0xFFF3A660 : 0xFF7D96D8);
            g.drawString(font, clip(font, flow.steps().get(i).label(), panelW - 26), sx + 17, ty,
                    done || now ? 0xFFFFFFFF : 0xFFB7C8F2, false);
            ty += 11;
        }
        ty += 6;
        g.drawString(font, "Setup will complete in", sx + 8, ty, 0xFFDCE6FA, false);
        g.drawString(font, "approximately:", sx + 8, ty + 9, 0xFFDCE6FA, false);
        final int left = Math.max(0, (flow.ticksTotal() - ticksDone) / 20);
        g.drawString(font, left + " seconds", sx + 8, ty + 20, 0xFFFFFFFF, false);
        g.fill(sx + 8, ty + 34, sx + panelW - 8, ty + 40, 0xFF16307F);
        g.fill(sx + 8, ty + 34, sx + 8 + (panelW - 16) * flow.permille(ticksDone) / 1000, ty + 40, 0xFF7BC34A);

        final int cx = sx + panelW + 8;
        final int cw = sw - panelW - 16;
        if (!flow.stage().asks()) {
            // Nothing to ask: the maker's name over the middle of the ground, the way it waits in life.
            emblem(g, cx + cw / 2 - 30, sy + 70, 20, flow.style());
            g.drawString(font, "Midsoft " + flow.systemName(), cx + cw / 2 - 10, sy + 76, 0xFFFFFFFF, true);
            final Paint plain = new Paint(0xFFFFFFFF, 0xFFFFFFFF, 0xFFDCE6FA, 0xFFF3A660, 0xFF00309C, 0xFFFFFFFF);
            return new Frame(cx, sy + 104, cw, sh - 122, plain, null, null, null, null);
        }
        final int dy = sy + 40;
        final int dh = sh - 80;
        g.fill(cx, dy, cx + cw, dy + dh, 0xFFECE9D8);
        outline(g, cx, dy, cw, dh, 0xFF0831D9);
        g.fillGradient(cx + 1, dy + 1, cx + cw - 1, dy + 1 + TITLE_BAR, 0xFF3F8CF3, 0xFF0846C0);
        g.drawString(font, flow.style().title(flow.systemName()), cx + 5, dy + 5, 0xFFFFFFFF, false);
        final int by = dy + dh - 18;
        final boolean canGo = flow.canContinue();
        final int[] next = button(g, font, cx + cw - 6 - 52, by, 52, "Next >", canGo);
        final int[] back = button(g, font, cx + cw - 6 - 106, by, 52, "< Back", false);
        final Paint paint = new Paint(0xFF000000, 0xFF000000, 0xFF505050, 0xFF0846C0, 0xFF0846C0, 0xFFFFFFFF);
        return new Frame(cx + 8, dy + TITLE_BAR + 8, cw - 16, dh - TITLE_BAR - 32, paint, next, back, null, null);
    }

    /** A pale card, the question at the top and the buttons at the bottom right. */
    private static Frame card(final GuiGraphics g, final Font font, final InstallerFlow flow,
                              final int sx, final int sy, final int sw, final int sh) {
        g.fillGradient(sx, sy, sx + sw, sy + sh, 0xFF1E3E74, 0xFF0B1530);
        final int cx = sx + 14;
        final int cy = sy + 10;
        final int cw = sw - 28;
        final int ch = sh - 20;
        g.fill(cx, cy, cx + cw, cy + ch, 0xFFFAFAFE);
        outline(g, cx, cy, cw, ch, 0xFFC0C4D2);

        emblem(g, cx + 8, cy + 6, 8, flow.style());
        g.drawString(font, flow.style().title(flow.systemName()), cx + 20, cy + 6, 0xFF6B7488, false);
        g.fill(cx + 1, cy + 19, cx + cw - 1, cy + 20, 0xFFE3E5EE);
        g.drawString(font, flow.style().heading(flow.page(), flow.systemName()), cx + 10, cy + 27, 0xFF202434,
                false);

        final int by = cy + ch - 20;
        final boolean canGo = flow.canContinue() || flow.page() == InstallerPage.DONE;
        final int[] next = primary(g, font, cx + cw - 10 - 56, by, 56,
                flow.page() == InstallerPage.DONE ? "Restart" : "Next", canGo);
        final int[] back = pale(g, font, cx + cw - 10 - 118, by, 56, "Back");
        final int[] erase = flow.page() == InstallerPage.DISK
                ? pale(g, font, cx + 10, by, 66, "Erase disk") : null;
        final Paint paint = new Paint(0xFF202434, 0xFF202434, 0xFF6B7488, 0xFF3A6AE0, 0xFFE7EEFC, 0xFF202434);
        return new Frame(cx + 10, cy + 42, cw - 20, ch - 68, paint, next, back, null, erase);
    }

    /** The four panes of a window, in the colours of the edition being installed. */
    static void emblem(final GuiGraphics g, final int x, final int y, final int size, final InstallerStyle style) {
        final int gap = Math.max(1, size / 10);
        final int half = (size - gap) / 2;
        final int[] panes = switch (style) {
            case FRAMES_95 -> new int[]{0xFF000080, 0xFF1F8A8A, 0xFF5FC3C3, 0xFF3A4FA8};
            case FRAMES_XP -> new int[]{0xFF6F9FE0, 0xFF8FB8F0, 0xFF4E8B26, 0xFF9BD164};
            default -> new int[]{0xFF3A6AE0, 0xFF5C86EA, 0xFF2B55C4, 0xFF4471DD};
        };
        g.fill(x, y, x + half, y + half, panes[0]);
        g.fill(x + half + gap, y, x + size, y + half, panes[1]);
        g.fill(x, y + half + gap, x + half, y + size, panes[2]);
        g.fill(x + half + gap, y + half + gap, x + size, y + size, panes[3]);
    }

    /** A raised or sunken bevel in the manner of the grey machines: light one way, dark the other. */
    private static void bevel(final GuiGraphics g, final int x, final int y, final int w, final int h,
                              final int fill, final boolean raised) {
        g.fill(x, y, x + w, y + h, fill);
        final int light = raised ? 0xFFFFFFFF : 0xFF808080;
        final int dark = raised ? 0xFF000000 : 0xFFFFFFFF;
        g.fill(x, y, x + w, y + 1, light);
        g.fill(x, y, x + 1, y + h, light);
        g.fill(x, y + h - 1, x + w, y + h, dark);
        g.fill(x + w - 1, y, x + w, y + h, dark);
    }

    private static void outline(final GuiGraphics g, final int x, final int y, final int w, final int h,
                                final int colour) {
        g.fill(x, y, x + w, y + 1, colour);
        g.fill(x, y + h - 1, x + w, y + h, colour);
        g.fill(x, y, x + 1, y + h, colour);
        g.fill(x + w - 1, y, x + w, y + h, colour);
    }

    /** A bevelled grey button; the one that carries the page forward wears the outline the default one wore. */
    private static int[] button(final GuiGraphics g, final Font font, final int x, final int y, final int w,
                                final String label, final boolean strong) {
        bevel(g, x, y, w, BUTTON, 0xFFC0C0C0, true);
        if (strong) {
            outline(g, x - 1, y - 1, w + 2, BUTTON + 2, 0xFF000000);
        }
        g.drawString(font, label, x + (w - font.width(label)) / 2, y + 3, 0xFF000000, false);
        return new int[]{x, y, w, BUTTON};
    }

    private static int[] primary(final GuiGraphics g, final Font font, final int x, final int y, final int w,
                                 final String label, final boolean on) {
        g.fill(x, y, x + w, y + BUTTON, on ? 0xFF3A6AE0 : 0xFFCDD1DD);
        g.drawString(font, label, x + (w - font.width(label)) / 2, y + 3, 0xFFFFFFFF, false);
        return new int[]{x, y, w, BUTTON};
    }

    private static int[] pale(final GuiGraphics g, final Font font, final int x, final int y, final int w,
                              final String label) {
        g.fill(x, y, x + w, y + BUTTON, 0xFFFFFFFF);
        outline(g, x, y, w, BUTTON, 0xFFCDD1DD);
        g.drawString(font, label, x + (w - font.width(label)) / 2, y + 3, 0xFF202434, false);
        return new int[]{x, y, w, BUTTON};
    }

    /** A label cut to the room it has, so a long step name never runs out of its panel. */
    static String clip(final Font font, final String text, final int room) {
        if (font.width(text) <= room) {
            return text;
        }
        String cut = text;
        while (cut.length() > 1 && font.width(cut + "...") > room) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut + "...";
    }

    /**
     * Where a page's content goes and what it is written in, with whatever buttons the frame drew.
     *
     * <p>A button that a frame does not draw is null, and the page reaches the same thing with a key.
     */
    record Frame(int x, int y, int w, int h, Paint paint, int[] next, int[] back, int[] cancel, int[] erase) {
    }

    /** The ink a page is written in inside its frame. */
    record Paint(int text, int bright, int dim, int accent, int select, int selectText) {
    }

    /**
     * The ground and ink of the installers drawn as plain text.
     *
     * <p>The frames with a shape of their own carry their colours with that shape, since a wizard is grey on
     * every machine that ever ran one.
     */
    private record Ink(int back, int text, int bright, int dim, int bar, int barText, int select, int selectText,
                       int accent, int panel, int panelText) {

        static Ink of(final InstallerStyle style) {
            return switch (style) {
                case MC_DOS -> new Ink(0xFF000000, 0xFF41D862, 0xFFA4FFBC, 0xFF1F8A3F, 0xFF41D862, 0xFF000000,
                        0xFF41D862, 0xFF000000, 0xFFA4FFBC, 0xFF000000, 0xFF41D862);
                case FRAMES_95, FRAMES_XP -> new Ink(0xFF0000A8, 0xFFC0C0C0, 0xFFFFFFFF, 0xFF7B7BB8,
                        0xFFC0C0C0, 0xFF000000, 0xFFC0C0C0, 0xFF0000A8, 0xFFFFFF55, 0xFFC0C0C0, 0xFF000000);
                case FRAMES_11 -> new Ink(0xFF0B1530, 0xFFE8EAF2, 0xFFFFFFFF, 0xFF9AA2B2, 0xFF3A6AE0,
                        0xFFFFFFFF, 0xFF3A6AE0, 0xFFFFFFFF, 0xFF7FA6FF, 0xFF0B1530, 0xFFE8EAF2);
                case UBUNTU -> new Ink(0xFF111111, 0xFFE6E6E6, 0xFFFFFFFF, 0xFF8A8A8A, 0xFFE95420, 0xFFFFFFFF,
                        0xFFE95420, 0xFFFFFFFF, 0xFFE95420, 0xFF111111, 0xFFE6E6E6);
                case DEBIAN -> new Ink(0xFF0000A8, 0xFF000000, 0xFF000000, 0xFF5A5A5A, 0xFF0000A8, 0xFFFFFFFF,
                        0xFFA80000, 0xFFFFFFFF, 0xFFA80000, 0xFFC0C0C0, 0xFF000000);
                case FEDORA -> new Ink(0xFF000000, 0xFFE6E6E6, 0xFFFFFFFF, 0xFF8A8A8A, 0xFF1A1A1A, 0xFFE6E6E6,
                        0xFF294172, 0xFFFFFFFF, 0xFF5FE07A, 0xFF000000, 0xFFE6E6E6);
                case PLAIN -> new Ink(0xFF10151B, 0xFFCDD6E2, 0xFF39D6C4, 0xFF7D8A9C, 0xFF19212B, 0xFFCDD6E2,
                        0xFF19212B, 0xFF39D6C4, 0xFF39D6C4, 0xFF10151B, 0xFFCDD6E2);
            };
        }

        Paint paint() {
            return new Paint(this.text, this.bright, this.dim, this.accent, this.select, this.selectText);
        }
    }
}
