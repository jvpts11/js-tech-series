/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.gui.TrashItem;
import dev.jstech.computers.gui.layout.CdeFrontPanelLayout.Rect;
import dev.jstech.computers.gui.layout.TrashLayout;
import dev.jstech.core.client.gui.component.ContextMenu;
import dev.jstech.core.client.gui.component.Texts;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The trash shown as icons, the way the Linux file managers and CDE's Trash Can show it: each thing as the picture of
 * the program that opens it, with its name under it, in rows across a view that scrolls.
 *
 * <p>The view is a rectangle in desktop pixels, and the cells come from {@link TrashLayout}, so what is drawn and what
 * a click finds are the same squares.
 */
final class TrashIcons {

    /** The colours an icon view writes in: its ground, its text, and a selected name's fill and text. */
    record Inks(int ground, int text, int pickedFill, int pickedText) {
    }

    private static final int ICON = 16;

    private TrashIcons() {
    }

    /** Draws every item that is scrolled into view. */
    static void render(final GuiGraphics g, final Font font, final TrashApp app, final Rect view, final Inks inks,
                       final int mouseX, final int mouseY) {
        final List<TrashItem> items = app.items();
        final int first = app.scroll() * TrashLayout.columns(view);
        final int last = Math.min(items.size(), first + TrashLayout.columns(view) * TrashLayout.rowsShown(view));
        for (int i = first; i < last; i++) {
            final TrashItem item = items.get(i);
            final Rect cell = TrashLayout.cell(view, i, app.scroll());
            final boolean picked = app.isSelected(item);
            if (!picked && cell.holds(mouseX, mouseY)) {
                g.fill(cell.x() + 2, cell.y(), cell.x() + cell.w() - 2, cell.y() + cell.h() - 1, 0x22000000);
            }
            ProgramIcons.draw(g, cell.x() + (cell.w() - ICON) / 2, cell.y() + 3, ICON, ICON, app.iconOf(item),
                    app.iconSet());
            final String name = fit(font, item.name(), cell.w() - 4);
            final int nameW = Texts.smallWidth(font, name);
            final int nx = cell.x() + (cell.w() - nameW) / 2;
            final int ny = cell.y() + 22;
            if (picked) {
                g.fill(nx - 2, ny - 1, nx + nameW + 2, ny + 8, inks.pickedFill());
            }
            TrashApp.write(g, font, name, nx, ny, picked ? inks.pickedText() : inks.text(),
                    picked ? inks.pickedFill() : inks.ground(), Texts.SMALL);
        }
    }

    /** The item under a point of the view, or -1 for the bare view. */
    static int indexAt(final TrashApp app, final Rect view, final double mouseX, final double mouseY) {
        if (!view.holds(mouseX, mouseY)) {
            return -1;
        }
        final int first = app.scroll() * TrashLayout.columns(view);
        final int last = Math.min(app.items().size(), first + TrashLayout.columns(view) * TrashLayout.rowsShown(view));
        for (int i = first; i < last; i++) {
            if (TrashLayout.cell(view, i, app.scroll()).holds(mouseX, mouseY)) {
                return i;
            }
        }
        return -1;
    }

    /** The middle of item {@code index}'s picture, or null when it is scrolled out of view. */
    @Nullable
    static int[] centre(final TrashApp app, final Rect view, final int index) {
        final Rect cell = TrashLayout.cell(view, index, app.scroll());
        if (cell.y() < view.y() || cell.y() + cell.h() > view.y() + view.h()) {
            return null;
        }
        return new int[] {cell.x() + cell.w() / 2, cell.y() + 3 + ICON / 2};
    }

    /** How many rows the view can be scrolled down. */
    static int scrollLimit(final TrashApp app, final Rect view) {
        final int cols = TrashLayout.columns(view);
        final int rows = (app.items().size() + cols - 1) / cols;
        return Math.max(0, rows - TrashLayout.rowsShown(view));
    }

    /** What a menu offers, top to bottom. */
    static List<String> labels(final ContextMenu menu) {
        final List<String> out = new ArrayList<>();
        for (final ContextMenu.Item entry : menu.items()) {
            out.add(entry.label());
        }
        return out;
    }

    /** The middle of a menu's entry with that label, or null. */
    @Nullable
    static int[] pointOf(final ContextMenu menu, final String label) {
        final int index = labels(menu).indexOf(label);
        return index < 0 ? null : menu.itemCenter(index);
    }

    /** A name in the small text, cut with dots when it is wider than its cell. */
    private static String fit(final Font font, final String name, final int room) {
        return Texts.smallWidth(font, name) <= room ? name : Texts.clip(font, name, Texts.smallFits(room));
    }
}
