/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.os.OsMotions;
import dev.jstech.computers.program.Programs;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.Texts;
import dev.jstech.core.motion.Motion;
import dev.jstech.core.motion.MotionKinds;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

/**
 * Frames 10's Action Center: a dark pane down the right edge of the screen, the notices the machine raised since the
 * desktop came up listed under its heading with Clear all beside it, each one opening its program when clicked, and
 * the quick actions along its foot: All settings, the network, Quiet hours (which keeps notices from popping up while
 * it is on) and the sound settings. It slides in from the edge and out again.
 *
 * <p>Its colours are {@code jsc:panel/action_center}.
 */
@PaletteHolder
final class ActionCenter {

    private final DesktopState desktop;
    private boolean open;
    private Motion motion = Motion.FINISHED;

    static final int W = 150;
    private static final int PAD = 6;
    private static final int CARD_H = 28;
    private static final int QUICK_H = 26;
    private static final int QUICK_COLUMNS = 4;
    private static final List<TextKey> QUICK = List.of(DesktopTexts.ALL_SETTINGS, DesktopTexts.QUICK_NETWORK,
            DesktopTexts.QUIET_HOURS, DesktopTexts.QUICK_SOUND);
    private static final ResourceLocation NETWORK_MANAGER =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "network_manager");

    private static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "panel/action_center",
            new Colours(0xF21F1F1F, 0xFF1F1F1F, 0xFF2B2B2B, 0xFF3A3A3A, 0xFFFFFFFF, 0xFFA6A6A6, 0xFF76B9ED,
                    0xFF3A3A3A, 0xFF4A4A4A));

    ActionCenter(final DesktopState desktop) {
        this.desktop = desktop;
    }

    boolean isOpen() {
        return open;
    }

    /** Opens the pane, or closes it; opening it marks what it lists as seen. */
    void toggle() {
        open = !open;
        if (open) {
            desktop.notices().markRead();
            motion = desktop.motion().start(MotionKinds.MENU_SHOW);
        }
    }

    void close() {
        open = false;
    }

    void render(final GuiGraphics g, final int tbY, final int sw, final int lmx, final int lmy) {
        if (!open) {
            return;
        }
        final Colours c = PALETTE.get();
        final double now = DesktopMotion.now();
        final int slide = motion.done(now) ? 0 : (int) Math.round((1.0 - motion.progress(now)) * W);
        final int x = sw - W + slide;
        final boolean solid = desktop.prefs().effects().isOff(OsMotions.TRANSPARENCY);
        g.fill(x, 0, x + W, tbY, solid ? c.groundSolid() : c.ground());
        g.fill(x, 0, x + 1, tbY, c.card());

        Draw.text(g, desktop.textFont(), GameText.resolve(DesktopTexts.NOTIFICATIONS), x + PAD, PAD, c.ink());
        final String clear = GameText.resolve(DesktopTexts.CLEAR_ALL);
        Draw.text(g, desktop.textFont(), clear, x + W - PAD - desktop.textFont().width(clear), PAD, c.link());
        final List<DesktopNotices.Notice> notices = desktop.notices().history();
        int y = PAD + 14;
        if (notices.isEmpty()) {
            Draw.text(g, desktop.textFont(), GameText.resolve(DesktopTexts.NO_NOTIFICATIONS), x + PAD, y + 4,
                    c.dim());
        }
        for (final DesktopNotices.Notice notice : notices) {
            if (y + CARD_H > quickTop(tbY) - 4) {
                break;
            }
            final boolean hot = lmx >= x + PAD && lmx < x + W - PAD && lmy >= y && lmy < y + CARD_H;
            g.fill(x + PAD, y, x + W - PAD, y + CARD_H, hot ? c.cardHot() : c.card());
            Draw.text(g, desktop.textFont(), desktop.textFont().plainSubstrByWidth(notice.title(), W - 2 * PAD - 34),
                    x + PAD + 4, y + 4, c.ink());
            Draw.text(g, desktop.textFont(), notice.time(), x + W - PAD - 4 - desktop.textFont().width(notice.time()),
                    y + 4, c.dim());
            Draw.text(g, desktop.textFont(), desktop.textFont().plainSubstrByWidth(notice.body(), W - 2 * PAD - 8),
                    x + PAD + 4, y + 15, c.dim());
            y += CARD_H + 3;
        }

        final int qy = quickTop(tbY);
        final int qw = (W - 2 * PAD - (QUICK_COLUMNS - 1) * 2) / QUICK_COLUMNS;
        for (int i = 0; i < QUICK.size(); i++) {
            final int qx = x + PAD + i * (qw + 2);
            final boolean lit = i == 2 && desktop.notices().quiet() || i == 1 && desktop.onNetwork();
            final boolean hot = lmx >= qx && lmx < qx + qw && lmy >= qy && lmy < qy + QUICK_H;
            g.fill(qx, qy, qx + qw, qy + QUICK_H, lit ? desktop.prefs().skin().accent() : hot ? c.quickHot()
                    : c.quick());
            // A quick action's name runs onto a second line, as on the real tiles, rather than losing its end.
            final List<FormattedCharSequence> lines = desktop.textFont().split(
                    Component.literal(GameText.resolve(QUICK.get(i))), Texts.smallFits(qw - 3));
            final int shown = Math.min(2, lines.size());
            for (int line = 0; line < shown; line++) {
                g.pose().pushPose();
                g.pose().translate(qx + 2, qy + QUICK_H - 8 * (shown - line), 0);
                g.pose().scale(Texts.SMALL, Texts.SMALL, 1.0F);
                Draw.text(g, desktop.textFont(), lines.get(line), 0, 0, c.ink());
                g.pose().popPose();
            }
        }
    }

    /**
     * A click while the pane is up: Clear all, a notice (which opens its program and leaves the list), or a quick
     * action. Anywhere off the pane closes it and goes on to whatever is under it.
     */
    boolean click(final double mx, final double my, final int tbY, final int sw) {
        if (!open) {
            return false;
        }
        final int x = sw - W;
        if (mx < x || my >= tbY) {
            // Its own button on the taskbar closes it as it opened it; anything else closes it here.
            if (!desktop.tray().onActionCenter(mx, my, sw, tbY)) {
                close();
            }
            return false;
        }
        if (my < PAD + 12 && mx >= x + W / 2) {
            desktop.notices().clearAll();
            return true;
        }
        int y = PAD + 14;
        for (final DesktopNotices.Notice notice : desktop.notices().history()) {
            if (y + CARD_H > quickTop(tbY) - 4) {
                break;
            }
            if (my >= y && my < y + CARD_H) {
                desktop.notices().forget(notice);
                if (!notice.opens().isEmpty()) {
                    final IDesktopApp app = desktop.opener().factoryFor(notice.opens());
                    if (app != null) {
                        desktop.wm().open(notice.opens(), app);
                    }
                    close();
                }
                return true;
            }
            y += CARD_H + 3;
        }
        final int qy = quickTop(tbY);
        if (my >= qy && my < qy + QUICK_H) {
            final int qw = (W - 2 * PAD - (QUICK_COLUMNS - 1) * 2) / QUICK_COLUMNS;
            final int i = (int) ((mx - x - PAD) / (qw + 2));
            quick(i);
        }
        return true;
    }

    /** The desktop-local centre of quick action {@code index}, where a test clicks it. */
    int[] quickPoint(final int index, final int tbY, final int sw) {
        final int qw = (W - 2 * PAD - (QUICK_COLUMNS - 1) * 2) / QUICK_COLUMNS;
        return new int[] {sw - W + PAD + index * (qw + 2) + qw / 2, quickTop(tbY) + QUICK_H / 2};
    }

    private void quick(final int index) {
        switch (index) {
            case 0 -> {
                close();
                desktop.opener().openSettingsPage(0);
            }
            case 1 -> {
                close();
                if (!runProgram(NETWORK_MANAGER)) {
                    runProgram(Programs.SETTINGS);
                }
            }
            case 2 -> desktop.notices().toggleQuiet();
            case 3 -> {
                close();
                desktop.opener().openSettingsPage(SettingsApp.PAGE_SOUND);
            }
            default -> { }
        }
    }

    private boolean runProgram(final ResourceLocation program) {
        for (final Launcher l : desktop.launcherList()) {
            if (program.equals(l.programId())) {
                desktop.opener().run(l);
                return true;
            }
        }
        return false;
    }

    private static int quickTop(final int tbY) {
        return tbY - PAD - QUICK_H;
    }

    /**
     * The pane's colours: its ground with transparency and without, a notice's card and the card under the cursor,
     * its ink and dim ink, the light blue of Clear all, and a quick action at rest and under the cursor.
     */
    private record Colours(int ground, int groundSolid, int card, int cardHot, int ink, int dim, int link, int quick,
                           int quickHot) {
    }
}
