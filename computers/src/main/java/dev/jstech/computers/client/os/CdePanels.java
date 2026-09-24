/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.gui.CdePalette;
import dev.jstech.computers.gui.layout.CdeFrontPanelLayout;
import dev.jstech.computers.gui.layout.CdeFrontPanelLayout.Control;
import dev.jstech.computers.gui.layout.CdeFrontPanelLayout.Rect;
import dev.jstech.computers.os.WorkspaceSet;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

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
@PaletteHolder
final class CdePanels {

    private static final TextKey[] WORKSPACE_NAMES = {CdePanelsTexts.WORKSPACE_ONE, CdePanelsTexts.WORKSPACE_TWO,
        CdePanelsTexts.WORKSPACE_THREE, CdePanelsTexts.WORKSPACE_FOUR};
    /** The clock hands and the calendar day's ink, {@code jsc:desktop/front_panel}. */
    private static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "desktop/front_panel",
            new Colours(0xFF1A1A1A));

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
    /** How big a control's picture is made and drawn. */
    private static final int PICTURE = 24;

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
        this.shownTip = GameText.resolve(over.tip());
        final int w = desktop.textFont().width(this.shownTip) + TIP_PAD * 2;
        final Rect at = CdeFrontPanelLayout.tip(over, w, TIP_H, sw, sh);
        MotifChrome.raised(g, at.x(), at.y(), at.w(), at.h(), p.window(), p);
        g.drawString(desktop.textFont(), this.shownTip, at.x() + TIP_PAD, at.y() + 2, p.ink(), false);
    }

    /** What workspace {@code index} is called, counted from nought: the four names CDE's switch came with. */
    static String workspaceName(final int index) {
        return GameText.resolve(WORKSPACE_NAMES[WorkspaceSet.clampIndex(index)]);
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
            picture(g, control, r);
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
            case TRASH -> desktop.openTrash();
            case HELP -> open("help_viewer");
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
            final String name = GameText.resolve(WORKSPACE_NAMES[i]);
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
        final String word = GameText.resolve(CdePanelsTexts.EXIT);
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

    /**
     * What stands on a control: the panel's own picture of it, at {@code textures/gui/cde/panel/<control>.png}.
     * The two that change as the world does are finished here, the clock with its hands and the page with its day.
     */
    private void picture(final GuiGraphics g, final Control control, final Rect r) {
        final int cx = r.x() + r.w() / 2;
        final int top = r.y() + CdeFrontPanelLayout.ARROW_H + 3;
        if (control == Control.TRASH) {
            // The can wears CDE's own picture of it, full or empty, as it did on the real panel.
            ProgramIcons.draw(g, cx - ProgramIcons.SIZE / 2, top + 2, ProgramIcons.SIZE, ProgramIcons.SIZE,
                    ResourceLocation.fromNamespaceAndPath(JsComputers.MODID,
                            desktop.trashFull() ? "trash_full" : "trash"), "cde");
            return;
        }
        final ResourceLocation picture = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID,
                "textures/gui/cde/panel/" + control.name().toLowerCase(Locale.ROOT) + ".png");
        g.blit(picture, cx - PICTURE / 2, top, 0.0F, 0.0F, PICTURE, PICTURE, PICTURE, PICTURE);
        switch (control) {
            case CLOCK -> clockHands(g, cx, top + PICTURE / 2);
            case DATE -> day(g, cx, top + 11);
            default -> { }
        }
    }

    /** The hands that tell the world's own time, which is the one thing on the panel that moves. */
    private void clockHands(final GuiGraphics g, final int cx, final int cy) {
        final int minute = desktop.minuteOfDay();
        hand(g, cx, cy, (minute % 720) / 720.0, 6);
        hand(g, cx, cy, (minute % 60) / 60.0, 9);
    }

    /** One hand, from the middle of the face, {@code turn} of the way round from twelve. */
    private static void hand(final GuiGraphics g, final int cx, final int cy, final double turn, final int length) {
        final double angle = turn * Math.PI * 2.0;
        for (int step = 0; step <= length; step++) {
            final int x = cx + (int) Math.round(Math.sin(angle) * step);
            final int y = cy - (int) Math.round(Math.cos(angle) * step);
            g.fill(x, y, x + 1, y + 1, PALETTE.get().ink());
        }
    }

    /** The day of the world on the calendar page, under its red band. */
    private void day(final GuiGraphics g, final int cx, final int y) {
        final String day = Integer.toString(desktop.dayOfWorld());
        g.drawString(desktop.textFont(), day, cx - desktop.textFont().width(day) / 2, y, PALETTE.get().ink(), false);
    }

    /** The ink the clock's hands and the calendar day are drawn in. */
    private record Colours(int ink) {
    }
}
