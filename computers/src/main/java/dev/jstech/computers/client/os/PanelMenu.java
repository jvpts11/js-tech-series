/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.Nullable;

/**
 * The panel's own menu, the one a right click on the bar clear of its entries opens on every desktop these imitate.
 * The Task Manager is one entry on it rather than the click's whole meaning, and every entry does something: none is
 * there for decoration. It is drawn in the desktop's own chrome, a pale plate on a dark panel and a dark one on a
 * pale panel, with a separator drawn as a rule.
 */
final class PanelMenu {

    private final DesktopScreen desktop;
    private boolean open;
    /* Where the menu was raised, in desktop-local coordinates. */
    private int x;
    private int y;

    private static final int W = 104;
    /** The rows in the order the menu shows them, which is a place on the screen and nothing more. */
    private static final List<Row> ROWS = List.of(Row.CASCADE, Row.SHOW_DESKTOP, Row.SEPARATOR, Row.TASK_MANAGER);

    PanelMenu(final DesktopScreen desktop) {
        this.desktop = desktop;
    }

    boolean isOpen() {
        return open;
    }

    /** Raises the menu at a point along a panel whose top is {@code panelY}, clamped so it stays on the desktop. */
    void open(final int atX, final int panelY) {
        final int h = height();
        final DesktopViewport view = desktop.view();
        open = true;
        x = Math.max(2, Math.min(view.width() - W - 2, atX));
        // Above a bottom panel, below a top one: the menu never covers the bar it came from.
        y = view.panelOnTop() ? panelY + DesktopScreen.TASKBAR_H + 2 : panelY - h - 2;
    }

    /** Draws the menu, lighting the entry under the pointer. */
    void render(final GuiGraphics g, final int hoverX, final int hoverY) {
        if (!open) {
            return;
        }
        final OsSkin skin = desktop.prefs().skin();
        final boolean light = desktop.lightOn(skin.text());
        final DesktopShellPalette.Colours c = DesktopShellPalette.get();
        final int bg = light ? c.darkMenuFill() : c.lightMenuFill();
        final int fg = light ? c.darkMenuInk() : c.lightMenuInk();
        final int h = height();
        g.fill(x - 1, y - 1, x + W + 1, y + h + 1, light ? c.darkMenuBorder() : c.lightMenuBorder());
        g.fill(x, y, x + W, y + h, bg);
        g.fill(x, y, x + W, y + 1, light ? c.darkMenuRule() : c.lightMenuTop());
        final int hover = rowAt(hoverX, hoverY);
        int iy = y + 1;
        for (int k = 0; k < ROWS.size(); k++) {
            if (ROWS.get(k) == Row.SEPARATOR) {
                g.fill(x + 4, iy + DeskMenu.ITEM_H / 2, x + W - 4, iy + DeskMenu.ITEM_H / 2 + 1,
                        light ? c.darkMenuRule() : c.lightMenuRule());
            } else {
                if (k == hover) {
                    g.fill(x + 1, iy, x + W - 1, iy + DeskMenu.ITEM_H, skin.accent());
                }
                g.drawString(desktop.textFont(), ROWS.get(k).words(), x + 4, iy + 2,
                        k == hover ? c.menuHoverInk() : fg, false);
            }
            iy += DeskMenu.ITEM_H;
        }
    }

    /**
     * The next click while the menu is up, wherever it lands: on an entry it runs it, anywhere else it only closes
     * the menu, which is what a menu does. Returns whether the menu was up to take it.
     */
    boolean click(final double mouseX, final double mouseY) {
        if (!open) {
            return false;
        }
        final int row = rowAt(mouseX, mouseY);
        open = false;
        if (row >= 0) {
            run(ROWS.get(row));
        }
        return true;
    }

    /** The desktop-local centre of the entry that reads {@code label}, or null when the menu is down or lacks it. */
    @Nullable
    int[] pointOf(final String label) {
        if (!open) {
            return null;
        }
        for (int i = 0; i < ROWS.size(); i++) {
            if (ROWS.get(i) != Row.SEPARATOR && ROWS.get(i).words().equals(label)) {
                return new int[] {x + W / 2, y + 1 + i * DeskMenu.ITEM_H + DeskMenu.ITEM_H / 2};
            }
        }
        return null;
    }

    private int height() {
        return ROWS.size() * DeskMenu.ITEM_H + 2;
    }

    /** The entry under a desktop-local point, or {@code -1}; a separator never answers. */
    private int rowAt(final double mx, final double my) {
        if (!open || mx < x || mx > x + W) {
            return -1;
        }
        final int rel = (int) Math.floor((my - (y + 1)) / (double) DeskMenu.ITEM_H);
        if (rel < 0 || rel >= ROWS.size() || ROWS.get(rel) == Row.SEPARATOR) {
            return -1;
        }
        return rel;
    }

    private void run(final Row row) {
        switch (row) {
            case CASCADE -> desktop.wm().cascade();
            case SHOW_DESKTOP -> desktop.wm().showDesktop();
            case TASK_MANAGER -> desktop.opener().openTaskManager();
            case SEPARATOR -> {
            }
        }
    }

    /** The menu's rows, top to bottom; a separator sits before the Task Manager. */
    private enum Row {
        CASCADE(DesktopTexts.CASCADE),
        SHOW_DESKTOP(DesktopTexts.SHOW_DESKTOP),
        SEPARATOR(null),
        TASK_MANAGER(DesktopTexts.TASK_MANAGER);

        @Nullable
        private final TextKey label;

        Row(@Nullable final TextKey label) {
            this.label = label;
        }

        /** What the row reads as in the player's language; a separator reads as nothing. */
        String words() {
            return this.label == null ? "" : GameText.resolve(this.label);
        }
    }
}
