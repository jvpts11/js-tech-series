/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.gui.CdePalette;
import dev.jstech.computers.gui.layout.CdeFrontPanelLayout.Rect;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;

/**
 * How CDE opens a program: a subpanel that slides up out of the Applications control and lists every program
 * on the machine, each with its picture and the name this desktop gives it.
 *
 * <p>A subpanel is headed by what it is, stands right above the control it belongs to, and stays up until its
 * arrow is pressed again or something on it is chosen. It is drawn in the same relief as the panel it rises
 * from, so it reads as a piece of that panel pulled upward and not as a menu from another desktop.
 */
final class CdeLaunchers {

    private static final int WIDTH = 132;
    private static final int HEAD_H = 13;
    private static final int ROW_H = 16;
    private static final int PAD = 3;
    private static final String HEADING = "Applications";

    private final DesktopScreen desktop;

    CdeLaunchers(final DesktopScreen desktop) {
        this.desktop = desktop;
    }

    void render(final GuiGraphics g, final Rect control, final CdePalette p) {
        final List<DesktopScreen.Launcher> all = listed(control);
        final Rect box = box(control, all.size());
        MotifChrome.raised(g, box.x(), box.y(), box.w(), box.h(), p.window(), p);
        g.fill(box.x() + PAD, box.y() + PAD, box.x() + box.w() - PAD, box.y() + PAD + HEAD_H, p.active());
        g.drawString(desktop.textFont(), HEADING,
                box.x() + (box.w() - desktop.textFont().width(HEADING)) / 2, box.y() + PAD + 3, p.activeInk(), false);
        int y = box.y() + PAD + HEAD_H + 1;
        for (final DesktopScreen.Launcher launcher : all) {
            if (desktop.hoverIn(box.x() + PAD, y, box.w() - PAD * 2, ROW_H)) {
                MotifChrome.sunken(g, box.x() + PAD, y, box.w() - PAD * 2, ROW_H, p.inset(), p);
            }
            ProgramIcons.draw(g, box.x() + PAD + 2, y + 1, 14, 14, launcher.programId(), desktop.icons());
            g.drawString(desktop.textFont(), desktop.shorten(launcher.label(), 18), box.x() + PAD + 20, y + 4,
                    p.ink(), false);
            y += ROW_H;
        }
    }

    /** A click while the subpanel is up: a row starts its program, and anywhere else puts the subpanel away. */
    boolean click(final int mx, final int my, final Rect control) {
        final List<DesktopScreen.Launcher> all = listed(control);
        final Rect box = box(control, all.size());
        if (!box.holds(mx, my)) {
            return false;
        }
        final int row = (my - (box.y() + PAD + HEAD_H + 1)) / ROW_H;
        if (my >= box.y() + PAD + HEAD_H + 1 && row >= 0 && row < all.size()) {
            desktop.launchAt(row);
            desktop.closeLauncher();
        }
        return true;
    }

    /**
     * What the subpanel lists: every program, down to as many as stand between the control and the top of the
     * desktop. The row a click lands on is its place in this list, which is the launcher list's own order.
     */
    private List<DesktopScreen.Launcher> listed(final Rect control) {
        final List<DesktopScreen.Launcher> all = desktop.launcherList();
        final int room = Math.max(1, (control.y() - 6 - PAD * 2 - HEAD_H) / ROW_H);
        return all.size() <= room ? all : all.subList(0, room);
    }

    /** The subpanel's own rectangle: as tall as what it lists, standing on the head of its control. */
    private static Rect box(final Rect control, final int rows) {
        final int h = PAD * 2 + HEAD_H + 1 + rows * ROW_H;
        return new Rect(control.x() + control.w() - WIDTH, control.y() - h - 2, WIDTH, h);
    }
}
