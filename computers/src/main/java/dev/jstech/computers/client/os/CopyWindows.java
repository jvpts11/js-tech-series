/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.os.PanelStyle;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.text.GameText;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.Nullable;

/**
 * Shows this desktop's machine copying: a copy window once a run of copies outlasts a moment, in the shape its system
 * gave it, gone when the run ends; or, where the system showed a copy without a window, Plasma's notification over the
 * panel with a ring filling at the tray, its pause button and its cross live, and GNOME's operations pie at the top
 * bar with its popover under it. CDE shows nothing but its busy pointer and its panel's light.
 */
final class CopyWindows {

    private final DesktopState desktop;
    /** The copy window up now, or null. */
    @Nullable
    private CopyProgressApp window;
    /* Where Plasma's notification, its pause button and its cross were drawn last, desktop-local, for a click. */
    private int[] cardAt = NOWHERE;
    private int[] pauseAt = NOWHERE;
    private int[] cancelAt = NOWHERE;

    /** The key the copy window opens under, which no program has. */
    static final String KEY = "jsc:copy_progress";
    private static final int CARD_W = 168;
    private static final int CARD_H = 46;
    private static final int POPOVER_W = 176;
    private static final int POPOVER_H = 38;
    private static final int RING = 9;
    private static final int[] NOWHERE = {0, 0, 0, 0};

    CopyWindows(final DesktopState desktop) {
        this.desktop = desktop;
    }

    /** Opens the copy window when a run has outlasted a moment, and takes it away when the run has ended. */
    void sync() {
        final CopyProgressApp.Style style = CopyProgressApp.styleOf(desktop.panelStyle(), desktop.periodPanel());
        if (window != null && desktop.wm().all().stream().noneMatch(w -> w.app() == window)) {
            window = null;
        }
        if (style == null || !desktop.surface().moves()) {
            return;
        }
        final CopyRun run = run();
        if (window == null && run != null && run.showsWindow()) {
            window = new CopyProgressApp(desktop.hostPos(), style);
            desktop.wm().open(KEY, window);
        } else if (window != null && run == null && !window.keptOpen()) {
            desktop.wm().closeOf(window);
            window = null;
        }
    }

    /** The copy window up now, or null; what a test reads to see the copy shown. */
    @Nullable
    CopyProgressApp window() {
        return window;
    }

    /**
     * Draws what a system that showed a copy without a window showed: Plasma's notification and ring, GNOME's pie and
     * popover. Nothing for a run that has not yet outlasted a moment.
     */
    void renderOverlay(final GuiGraphics g, final int tbY, final int sw) {
        cardAt = NOWHERE;
        pauseAt = NOWHERE;
        cancelAt = NOWHERE;
        final CopyRun run = run();
        if (run == null || !run.showsWindow() || !desktop.surface().moves()) {
            return;
        }
        if (desktop.panelStyle() == PanelStyle.KDE && !desktop.periodPanel()) {
            plasma(g, run, tbY, sw);
        } else if (desktop.panelStyle() == PanelStyle.GNOME && !desktop.periodPanel()) {
            gnome(g, run, sw);
        }
    }

    /**
     * A click at the desktop-local point ({@code x}, {@code y}) on Plasma's notification: the pause button pauses the
     * run or takes it up again, the cross calls it off, and the rest of the notification takes the click so nothing
     * under it reacts. Returns whether the notification took it.
     */
    boolean click(final double x, final double y) {
        if (!inside(cardAt, x, y)) {
            return false;
        }
        if (inside(pauseAt, x, y)) {
            final CopyRun run = run();
            CopyProgressApp.pause(desktop.hostPos(), run == null || !run.paused());
        } else if (inside(cancelAt, x, y)) {
            CopyProgressApp.cancel(desktop.hostPos());
        }
        return true;
    }

    @Nullable
    private CopyRun run() {
        return CopyRun.of(DesktopCopies.run(desktop.hostPos()), DesktopCopies.now());
    }

    /* Plasma: a notification over the panel's right end, and a ring filling at the tray while the copy runs. */
    private void plasma(final GuiGraphics g, final CopyRun run, final int tbY, final int sw) {
        final OsSkin skin = desktop.prefs().skin();
        final Font font = desktop.textFont();
        final int x = sw - CARD_W - 6;
        final int y = tbY - CARD_H - 6;
        skin.windowFrame(g, x, y, CARD_W, CARD_H);
        cardAt = new int[] {x, y, CARD_W, CARD_H};
        g.fill(x + 6, y + 5, x + 14, y + 13, skin.accent());
        Draw.text(g, font, GameText.resolve(run.paused() ? CopyTexts.PAUSED : CopyTexts.KIO_COPYING), x + 18, y + 5,
                skin.text());
        Draw.text(g, font, "x", x + CARD_W - 10, y + 5, skin.text());
        cancelAt = new int[] {x + CARD_W - 12, y + 4, 9, 9};
        CopyProgressApp.pauseMark(g, x + CARD_W - 25, y + 4, run.paused(), skin.text());
        pauseAt = new int[] {x + CARD_W - 25, y + 4, 9, 9};
        final String what = GameText.resolve(CopyTexts.FILE_TO.with(run.current().name(), run.current().to()));
        Draw.text(g, font, font.plainSubstrByWidth(what, CARD_W - 12), x + 6, y + 16, skin.text());
        g.fill(x + 6, y + 27, x + CARD_W - 6, y + 29, skin.listHover());
        g.fill(x + 6, y + 27, x + 6 + (int) Math.floor((CARD_W - 12) * run.fraction()), y + 29, skin.progressFill());
        Draw.text(g, font, CopyProgressApp.percent(run) + "%", x + 6, y + 33, skin.dim());
        final String speed = GameText.resolve(CopyTexts.MIB_PER_SECOND.with(CopyProgressApp.mb(run.rate())));
        Draw.textCentered(g, font, speed, x + CARD_W / 2, y + 33, skin.dim());
        final String files = GameText.resolve(CopyTexts.FILES_OF.with(run.items() - run.itemsLeft() + 1,
                run.items()));
        Draw.text(g, font, files, x + CARD_W - 6 - font.width(files), y + 33, skin.dim());
        ring(g, desktop.tray().left(sw) - RING - 4, tbY + (DesktopScreen.TASKBAR_H - RING) / 2, run.fraction(),
                skin.progressFill(), skin.dim());
    }

    /* GNOME: the operations pie at the top bar's right, filling as the copy runs, and its popover under it. */
    private void gnome(final GuiGraphics g, final CopyRun run, final int sw) {
        final OsSkin skin = desktop.prefs().skin();
        final Font font = desktop.textFont();
        final int pieX = desktop.tray().speakerX(sw, true) - RING - 10;
        final int pieY = (DesktopScreen.TASKBAR_H - RING) / 2;
        pie(g, pieX, pieY, run.fraction(), skin.progressFill(), skin.dim());
        final int x = Math.max(4, pieX + RING / 2 - POPOVER_W + 20);
        final int y = DesktopScreen.TASKBAR_H + 3;
        skin.windowFrame(g, x, y, POPOVER_W, POPOVER_H);
        final String what = GameText.resolve(run.deleting()
                ? CopyTexts.DELETING_QUOTED.with(run.current().name())
                : CopyTexts.COPYING_QUOTED.with(run.current().name(), run.current().to()));
        Draw.text(g, font, font.plainSubstrByWidth(what, POPOVER_W - 12), x + 6, y + 5, skin.text());
        g.fill(x + 6, y + 17, x + POPOVER_W - 6, y + 19, skin.listHover());
        g.fill(x + 6, y + 17, x + 6 + (int) Math.floor((POPOVER_W - 12) * run.fraction()), y + 19,
                skin.progressFill());
        final String line = GameText.resolve(CopyTexts.PROGRESS_LINE.with(CopyProgressApp.mb(run.mbDone()),
                CopyProgressApp.mb(run.mbTotal()), Math.max(1, run.secondsLeft()), CopyProgressApp.mb(run.rate())));
        Draw.text(g, font, font.plainSubstrByWidth(line, POPOVER_W - 12), x + 6, y + 24, skin.dim());
    }

    /* A ring of dots lit round from the top as far as the copy has gone. */
    private static void ring(final GuiGraphics g, final int x, final int y, final double fraction, final int lit,
                             final int unlit) {
        final double c = (RING - 1) / 2.0;
        for (int i = 0; i < RING; i++) {
            for (int j = 0; j < RING; j++) {
                final double dx = i - c;
                final double dy = j - c;
                final double r = Math.sqrt(dx * dx + dy * dy);
                if (r < c - 1.6 || r > c + 0.4) {
                    continue;
                }
                g.fill(x + i, y + j, x + i + 1, y + j + 1, turned(dx, dy) <= fraction ? lit : unlit);
            }
        }
    }

    /* A disc filled round from the top as far as the copy has gone, like a pie. */
    private static void pie(final GuiGraphics g, final int x, final int y, final double fraction, final int lit,
                            final int unlit) {
        final double c = (RING - 1) / 2.0;
        for (int i = 0; i < RING; i++) {
            for (int j = 0; j < RING; j++) {
                final double dx = i - c;
                final double dy = j - c;
                if (Math.sqrt(dx * dx + dy * dy) > c + 0.4) {
                    continue;
                }
                g.fill(x + i, y + j, x + i + 1, y + j + 1, turned(dx, dy) <= fraction ? lit : unlit);
            }
        }
    }

    private static boolean inside(final int[] r, final double mx, final double my) {
        return r[2] > 0 && mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3];
    }

    /* How far round from the top, clockwise, a point stands, from 0 to 1. */
    private static double turned(final double dx, final double dy) {
        final double angle = Math.atan2(dx, -dy);
        return (angle < 0 ? angle + 2 * Math.PI : angle) / (2 * Math.PI);
    }
}
