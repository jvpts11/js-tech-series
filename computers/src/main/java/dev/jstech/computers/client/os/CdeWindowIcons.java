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
import dev.jstech.computers.gui.layout.CdeWindowIconLayout;
import dev.jstech.core.client.gui.component.Texts;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Where a window goes on CDE when it is put away: it becomes an icon on its own workspace, and a double click
 * on the icon brings it back.
 *
 * <p>CDE never had a list of open windows on its panel, so this is the whole of how a minimised window is
 * found again. The icons stand from the top left of the workspace in the order the windows were opened, each
 * one the program's picture on a raised tile with the window's name under it. They belong to the workspace the
 * window is on, so another workspace does not show them, and they sit under the open windows like everything
 * else on the desktop.
 */
final class CdeWindowIcons {

    private final DesktopScreen desktop;

    /** The icon the last click landed on and when, which is what tells a double click from two clicks. */
    private DesktopWindow lastClicked;
    private long lastClickAt;

    private static final long DOUBLE_CLICK_MS = 300L;
    private static final int NAME_PAD = 2;
    private static final String ELLIPSIS = "...";

    CdeWindowIcons(final DesktopScreen desktop) {
        this.desktop = desktop;
    }

    /** Draws one icon for each of those windows on a workspace that wide whose work area starts at {@code top}. */
    void render(final GuiGraphics g, final List<DesktopWindow> putAway, final int width, final int top,
                final CdePalette p) {
        for (int i = 0; i < putAway.size(); i++) {
            final DesktopWindow w = putAway.get(i);
            final Rect tile = CdeWindowIconLayout.tile(i, width, top);
            MotifChrome.raised(g, tile.x(), tile.y(), tile.w(), tile.h(), p.window(), p);
            ProgramIcons.draw(g, tile.x() + 3, tile.y() + 3, 16, 16, desktop.programIdFor(w.groupKey()),
                    desktop.icons());
            final Rect strip = CdeWindowIconLayout.name(i, width, top);
            final String name = fit(w.appKey(), strip.w() - NAME_PAD * 2);
            final int nameW = Texts.smallWidth(desktop.textFont(), name);
            final int nameX = strip.x() + (strip.w() - nameW) / 2;
            g.fill(nameX - NAME_PAD, strip.y(), nameX + nameW + NAME_PAD, strip.y() + strip.h(), p.window());
            Texts.small(g, desktop.textFont(), name, nameX, strip.y() + 2, p.ink());
        }
    }

    /**
     * A left click at that point: the window to bring back when it was the second click on one icon, and
     * nothing otherwise.
     *
     * @return whether the click landed on an icon at all, so the desktop under it leaves the click alone
     */
    boolean clicked(final double mx, final double my, final List<DesktopWindow> putAway, final int width,
                    final int top) {
        final int index = CdeWindowIconLayout.indexAt(mx, my, putAway.size(), width, top);
        if (index < 0) {
            lastClicked = null;
            return false;
        }
        final DesktopWindow w = putAway.get(index);
        final long now = System.currentTimeMillis();
        if (w == lastClicked && now - lastClickAt < DOUBLE_CLICK_MS) {
            lastClicked = null;
            desktop.focusOne(w);
        } else {
            lastClicked = w;
            lastClickAt = now;
        }
        return true;
    }

    /** A window's name in the small text, cut with an ellipsis when it is wider than its strip. */
    private String fit(final String name, final int room) {
        if (Texts.smallWidth(desktop.textFont(), name) <= room) {
            return name;
        }
        final int units = Math.max(1, Texts.smallFits(room) - desktop.textFont().width(ELLIPSIS));
        return desktop.textFont().plainSubstrByWidth(name, units) + ELLIPSIS;
    }
}
