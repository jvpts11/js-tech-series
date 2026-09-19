/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.gui.CdePalette;
import dev.jstech.computers.gui.layout.CdeFrontPanelLayout;
import dev.jstech.computers.gui.layout.CdeFrontPanelLayout.Control;
import dev.jstech.computers.gui.layout.CdeFrontPanelLayout.Rect;
import dev.jstech.computers.os.WorkspaceSet;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;

/**
 * CDE's Front Panel: a raised slab at the bottom centre of the desktop, and the whole of what CDE had where
 * the others have a taskbar.
 *
 * <p>It lists no open windows, because CDE never did: a window that is put away becomes an icon on its
 * workspace. What it holds is a clock and the date to look at, the things a player reaches for most, the four
 * workspaces in its middle with the way out beside them, and the door to everything installed. Every control
 * on it opens something; a control with nothing behind it is not drawn.
 *
 * <p>Where each thing sits is {@link CdeFrontPanelLayout}'s to say, and both the drawing and the clicks ask
 * it, so the control a player sees and the one they hit are the same rectangle.
 */
final class CdePanels {

    private static final String[] WORKSPACE_NAMES = {"One", "Two", "Three", "Four"};

    /** What each control opens, by the key its program goes by; the two that only show something open nothing. */
    private static final String FILES = "files";
    private static final String EDITOR = "editor";
    private static final String STYLE = "settings";

    private final DesktopScreen desktop;

    /** The control the pointer rests on and since when, which is what a tip waits for; null while on none. */
    @Nullable
    private Control resting;
    private long restingSince;

    /** The tip on show, or empty while none is. */
    private String shownTip = "";

    /** How long the pointer rests on a control before its name comes up. */
    private static final long TIP_AFTER_MS = 500L;
    private static final int TIP_H = 12;
    private static final int TIP_PAD = 4;

    CdePanels(final DesktopScreen desktop) {
        this.desktop = desktop;
    }

    /** The tip on show, or empty while none is, for a test to read. */
    String shownTip() {
        return this.shownTip;
    }

    /**
     * The name of the control the pointer has rested on, in a small raised plate just above the panel. The panel
     * is pictures and nothing else, so this is how a player learns what each one opens. A control whose subpanel
     * is up needs no name: its subpanel is headed with it.
     */
    void renderTip(final GuiGraphics g, final int sw, final int sh, final CdePalette p) {
        Control over = null;
        for (final Control control : Control.values()) {
            final Rect r = CdeFrontPanelLayout.control(control, sw, sh);
            if (desktop.hoverIn(r.x(), r.y(), r.w(), r.h())) {
                over = control;
                break;
            }
        }
        final long now = System.currentTimeMillis();
        if (over != this.resting) {
            this.resting = over;
            this.restingSince = now;
        }
        if (over == null || now - this.restingSince < TIP_AFTER_MS || desktop.subpanelOpen(over)) {
            this.shownTip = "";
            return;
        }
        this.shownTip = over.tip();
        final int w = desktop.textFont().width(this.shownTip) + TIP_PAD * 2;
        final Rect at = CdeFrontPanelLayout.tip(over, w, TIP_H, sw, sh);
        MotifChrome.raised(g, at.x(), at.y(), at.w(), at.h(), p.window(), p);
        g.drawString(desktop.textFont(), this.shownTip, at.x() + TIP_PAD, at.y() + 2, p.ink(), false);
    }

    /** What workspace {@code index} is called, counted from nought: the four names CDE's switch came with. */
    static String workspaceName(final int index) {
        return WORKSPACE_NAMES[WorkspaceSet.clampIndex(index)];
    }

    void render(final GuiGraphics g, final int sw, final int sh, final CdePalette p) {
        final Rect panel = CdeFrontPanelLayout.panel(sw, sh);
        MotifChrome.raised(g, panel.x(), panel.y(), panel.w(), panel.h(), p.window(), p);
        for (final Control control : Control.values()) {
            final Rect r = CdeFrontPanelLayout.control(control, sw, sh);
            final boolean open = desktop.subpanelOpen(control);
            MotifChrome.raised(g, r.x(), r.y(), r.w(), r.h(), p.window(), p);
            if (CdeLaunchers.hasSubpanel(control)) {
                arrow(g, r, open, p);
            }
            picture(g, control, r, p);
        }
        workspaces(g, sw, sh, p);
    }

    /**
     * A click on the desktop, which this answers when it lands on the panel: a control opens what it stands
     * for, the way out asks before it goes, and the slab itself swallows the rest so nothing under it reacts.
     */
    boolean click(final double mx, final double my, final int sw, final int sh) {
        if (!CdeFrontPanelLayout.panel(sw, sh).holds(mx, my)) {
            return false;
        }
        if (CdeFrontPanelLayout.exit(sw, sh).holds(mx, my)) {
            desktop.askToPowerOff();
            return true;
        }
        for (int i = 0; i < CdeFrontPanelLayout.WORKSPACES; i++) {
            if (CdeFrontPanelLayout.workspace(i, sw, sh).holds(mx, my)) {
                desktop.switchWorkspace(i);
                return true;
            }
        }
        for (final Control control : Control.values()) {
            final Rect r = CdeFrontPanelLayout.control(control, sw, sh);
            if (r.holds(mx, my)) {
                // The arrow at the head of a control raises what is behind it; the control itself opens its program.
                if (CdeLaunchers.hasSubpanel(control) && my < r.y() + CdeFrontPanelLayout.ARROW_H + 2) {
                    desktop.toggleSubpanel(control);
                } else {
                    press(control);
                }
                return true;
            }
        }
        return true;
    }

    private void press(final Control control) {
        switch (control) {
            case FILES -> open(FILES);
            case EDITOR -> open(EDITOR);
            case STYLE -> open(STYLE);
            case APPLICATIONS -> desktop.openApplicationManager(null);
            default -> { }
        }
    }

    private void open(final String programPath) {
        for (final DesktopScreen.Launcher launcher : desktop.launcherList()) {
            if (launcher.programId() != null && launcher.programId().getPath().equals(programPath)) {
                desktop.launch(launcher);
                return;
            }
        }
    }

    /** The four workspaces, the one that is up pushed in and lit, and the way out standing beside them. */
    private void workspaces(final GuiGraphics g, final int sw, final int sh, final CdePalette p) {
        final Rect well = CdeFrontPanelLayout.switchWell(sw, sh);
        MotifChrome.raised(g, well.x(), well.y(), well.w(), well.h(), p.window(), p);
        for (int i = 0; i < CdeFrontPanelLayout.WORKSPACES; i++) {
            final Rect r = CdeFrontPanelLayout.workspace(i, sw, sh);
            final boolean up = i == desktop.workspace();
            if (up) {
                MotifChrome.sunken(g, r.x(), r.y(), r.w(), r.h(), p.active(), p);
            } else {
                MotifChrome.raised(g, r.x(), r.y(), r.w(), r.h(), p.window(), p);
            }
            final String name = WORKSPACE_NAMES[i];
            g.drawString(desktop.textFont(), name, r.x() + (r.w() - desktop.textFont().width(name)) / 2,
                    r.y() + (r.h() - 7) / 2, up ? p.activeInk() : p.ink(), false);
        }
        final Rect exit = CdeFrontPanelLayout.exit(sw, sh);
        // Pushed in for as long as the question it raised is still up.
        if (desktop.powerDialogOpen()) {
            MotifChrome.sunken(g, exit.x(), exit.y(), exit.w(), exit.h(), p.inset(), p);
        } else {
            MotifChrome.raised(g, exit.x(), exit.y(), exit.w(), exit.h(), p.window(), p);
        }
        final String word = "EXIT";
        g.drawString(desktop.textFont(), word, exit.x() + (exit.w() - desktop.textFont().width(word)) / 2,
                exit.y() + (exit.h() - 7) / 2, p.ink(), false);
    }

    /** The small raised button at the head of a control that has more behind it, its mark turned when open. */
    private static void arrow(final GuiGraphics g, final Rect r, final boolean open, final CdePalette p) {
        final int x = r.x() + 3;
        final int y = r.y() + 2;
        final int w = r.w() - 6;
        MotifChrome.raised(g, x, y, w, CdeFrontPanelLayout.ARROW_H - 2, p.window(), p);
        final int cx = x + w / 2;
        for (int row = 0; row < 3; row++) {
            final int half = open ? 3 - row : row + 1;
            g.fill(cx - half, y + 2 + row, cx + half, y + 3 + row, p.ink());
        }
    }

    /** What stands on a control, drawn out of a few rectangles each, the way the panel's own pictures were. */
    private void picture(final GuiGraphics g, final Control control, final Rect r, final CdePalette p) {
        final int cx = r.x() + r.w() / 2;
        final int top = r.y() + CdeFrontPanelLayout.ARROW_H + 3;
        switch (control) {
            case CLOCK -> clockFace(g, cx, top + 12);
            case DATE -> calendarPage(g, cx - 10, top, p);
            case FILES -> folder(g, cx - 11, top + 6);
            case EDITOR -> page(g, cx - 8, top);
            case STYLE -> paintPots(g, cx - 10, top + 2);
            case APPLICATIONS -> tiles(g, cx - 9, top + 3);
        }
    }

    /** A clock with hands that tell the world's own time, which is the one thing on the panel that moves. */
    private void clockFace(final GuiGraphics g, final int cx, final int cy) {
        disc(g, cx, cy, 11, 0xFF1A1A1A);
        disc(g, cx, cy, 10, 0xFFF4F1E8);
        final int minute = desktop.minuteOfDay();
        hand(g, cx, cy, (minute % 720) / 720.0, 6);
        hand(g, cx, cy, (minute % 60) / 60.0, 9);
    }

    /** A filled circle a row at a time, which at this size is two dozen rectangles. */
    private static void disc(final GuiGraphics g, final int cx, final int cy, final int radius, final int color) {
        for (int dy = -radius; dy <= radius; dy++) {
            final int half = (int) Math.round(Math.sqrt((double) radius * radius - (double) dy * dy));
            g.fill(cx - half, cy + dy, cx + half, cy + dy + 1, color);
        }
    }

    /** One hand, from the middle of the face, {@code turn} of the way round from twelve. */
    private static void hand(final GuiGraphics g, final int cx, final int cy, final double turn, final int length) {
        final double angle = turn * Math.PI * 2.0;
        for (int step = 0; step <= length; step++) {
            final int x = cx + (int) Math.round(Math.sin(angle) * step);
            final int y = cy - (int) Math.round(Math.cos(angle) * step);
            g.fill(x, y, x + 1, y + 1, 0xFF1A1A1A);
        }
    }

    /** A calendar page with the day of the world on it under a red band. */
    private void calendarPage(final GuiGraphics g, final int x, final int y, final CdePalette p) {
        g.fill(x - 1, y - 1, x + 21, y + 25, p.shade());
        g.fill(x, y, x + 20, y + 24, 0xFFF4F1E8);
        g.fill(x, y, x + 20, y + 6, 0xFFB8412F);
        final String day = Integer.toString(desktop.dayOfWorld());
        g.drawString(desktop.textFont(), day, x + (20 - desktop.textFont().width(day)) / 2 + 1, y + 11,
                0xFF1A1A1A, false);
    }

    private static void folder(final GuiGraphics g, final int x, final int y) {
        g.fill(x, y - 3, x + 9, y, 0xFFD5B35A);
        g.fillGradient(x, y, x + 22, y + 15, 0xFFE2C36B, 0xFFC9A447);
        g.fill(x, y + 14, x + 22, y + 15, 0xFF8C6F2A);
    }

    private static void page(final GuiGraphics g, final int x, final int y) {
        g.fill(x, y, x + 16, y + 22, 0xFF6E7282);
        g.fill(x + 1, y + 1, x + 15, y + 21, 0xFFFFFFFF);
        for (int line = 0; line < 5; line++) {
            g.fill(x + 3, y + 4 + line * 3, x + 13, y + 5 + line * 3, 0xFF9AA3B8);
        }
    }

    /** Three pots of paint on a pale palette: what a desktop's colours were chosen with. */
    private static void paintPots(final GuiGraphics g, final int x, final int y) {
        g.fill(x, y + 2, x + 20, y + 18, 0xFFE9DDB8);
        g.fill(x + 3, y + 5, x + 8, y + 10, 0xFFD8332C);
        g.fill(x + 11, y + 4, x + 16, y + 9, 0xFF3B6FD8);
        g.fill(x + 7, y + 11, x + 12, y + 16, 0xFF3FA34D);
    }

    /** Four tiles in four colours, which is every program at once. */
    private static void tiles(final GuiGraphics g, final int x, final int y) {
        g.fill(x, y, x + 8, y + 8, 0xFF3B6FD8);
        g.fill(x + 10, y, x + 18, y + 8, 0xFFD8332C);
        g.fill(x, y + 10, x + 8, y + 18, 0xFF3FA34D);
        g.fill(x + 10, y + 10, x + 18, y + 18, 0xFFE2C36B);
    }
}
