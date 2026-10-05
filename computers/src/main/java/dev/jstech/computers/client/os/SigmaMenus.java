/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.gui.layout.UiLayout;
import dev.jstech.computers.operation.payload.UiWindowPayload;
import dev.jstech.core.client.gui.component.Draw;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The menus a program's window opens over itself: the list of a combo box, a menu dropped from a menu bar and a
 * context menu. Each is a frame of the system's floating panels holding its entries one under another; an entry that
 * is only a dash is drawn as a line between groups, as menus have always done.
 */
final class SigmaMenus {

    /** The most entries a menu shows at once; a longer combo list is scrolled with the wheel. */
    static final int MOST_SHOWN = 10;
    /** An entry that is a line between groups rather than something to pick. */
    static final String SEPARATOR = "-";

    private SigmaMenus() {
    }

    /**
     * One menu's title on a menu bar.
     *
     * @param name what the menu is called
     * @param x    where its title starts
     * @param w    how wide its title is
     */
    record Title(String name, int x, int w) {
    }

    /**
     * An open menu: where it is, which entries it holds as their places in the owner's list, and which is picked.
     *
     * @param entries the entries' places in the owner's rows, counted from zero
     * @param picked  the entry shown as chosen, counted from zero, or -1
     */
    record Open(UiLayout.Rect rect, List<Integer> entries, int picked) {
    }

    /** The titles of a menu bar's menus across its strip, each menu once, in the order its first entry was added. */
    static List<Title> titles(final Font font, final UiWindowPayload.Widget bar, final UiLayout.Rect rect) {
        final List<Title> titles = new ArrayList<>();
        final List<String> named = new ArrayList<>();
        int at = rect.x() + 2;
        for (final String menu : bar.details()) {
            if (named.contains(menu)) {
                continue;
            }
            named.add(menu);
            final int wide = font.width(menu) + 10;
            titles.add(new Title(menu, at, wide));
            at += wide;
        }
        return titles;
    }

    /** The menu a popup is, laid out over the window, or null when the widget it belongs to is gone. */
    static Open open(final Font font, final SigmaUiState ui, final UiWindowPayload.Widget owner,
                     final UiLayout.Rect ownerRect, final int windowX, final int windowY, final int windowW,
                     final int windowH) {
        final List<Integer> entries = new ArrayList<>();
        int x;
        int y;
        int wide = 0;
        switch (ui.popup) {
            case COMBO -> {
                for (int i = 0; i < owner.rows().size(); i++) {
                    entries.add(i);
                }
                x = ownerRect.x();
                y = ownerRect.y() + ownerRect.h();
                wide = ownerRect.w();
            }
            case MENU -> {
                for (int i = 0; i < owner.rows().size(); i++) {
                    if (i < owner.details().size() && owner.details().get(i).equals(ui.popupMenu)) {
                        entries.add(i);
                    }
                }
                x = ownerRect.x() + 2;
                for (final Title title : titles(font, owner, ownerRect)) {
                    if (title.name().equals(ui.popupMenu)) {
                        x = title.x();
                    }
                }
                y = ownerRect.y() + ownerRect.h();
            }
            case CONTEXT -> {
                for (int i = 0; i < owner.rows().size(); i++) {
                    entries.add(i);
                }
                x = ui.popupX;
                y = ui.popupY;
            }
            default -> {
                return null;
            }
        }
        for (final int entry : entries) {
            wide = Math.max(wide, font.width(owner.row(entry)) + 24);
        }
        final int shown = Math.min(entries.size(), MOST_SHOWN);
        final int tall = Math.max(1, shown) * SigmaPainter.ROW_H + 4;
        x = Math.clamp(x, windowX, Math.max(windowX, windowX + windowW - wide));
        y = Math.clamp(y, windowY, Math.max(windowY, windowY + windowH - tall));
        final int picked = ui.popup == SigmaUiState.PopupKind.COMBO ? owner.number(4) - 1 : -1;
        return new Open(new UiLayout.Rect(owner.id(), x, y, wide, tall), entries, picked);
    }

    /** Draws an open menu, the entry under the pointer lit. */
    static void draw(final GuiGraphics g, final Font font, final OsSkin skin, final UiWindowPayload.Widget owner,
                     final Open menu, final int scroll, final int mouseX, final int mouseY) {
        final UiLayout.Rect rect = menu.rect();
        skin.windowFrame(g, rect.x(), rect.y(), rect.w(), rect.h());
        final int shown = Math.min(menu.entries().size(), MOST_SHOWN);
        for (int i = 0; i < shown && scroll + i < menu.entries().size(); i++) {
            final int entry = menu.entries().get(scroll + i);
            final int top = rect.y() + 2 + i * SigmaPainter.ROW_H;
            final String said = owner.row(entry);
            if (SEPARATOR.equals(said)) {
                g.fill(rect.x() + 3, top + SigmaPainter.ROW_H / 2, rect.x() + rect.w() - 3,
                        top + SigmaPainter.ROW_H / 2 + 1, skin.edge());
                continue;
            }
            final boolean lit = mouseX >= rect.x() && mouseX < rect.x() + rect.w() && mouseY >= top
                    && mouseY < top + SigmaPainter.ROW_H || entry == menu.picked();
            if (lit) {
                g.fill(rect.x() + 2, top, rect.x() + rect.w() - 2, top + SigmaPainter.ROW_H, skin.listSelect());
            }
            Draw.text(g, font, said, rect.x() + 10, top + 2, skin.listRowText(lit));
        }
    }

    /** The entry of an open menu at that point, as its place in the owner's rows, or -1 for none or a separator. */
    static int entryAt(final UiWindowPayload.Widget owner, final Open menu, final int scroll, final double mouseX,
                       final double mouseY) {
        final UiLayout.Rect rect = menu.rect();
        if (mouseX < rect.x() || mouseX >= rect.x() + rect.w() || mouseY < rect.y() + 2) {
            return -1;
        }
        final int row = scroll + (int) ((mouseY - rect.y() - 2) / SigmaPainter.ROW_H);
        if (row < 0 || row >= menu.entries().size() || row - scroll >= MOST_SHOWN) {
            return -1;
        }
        final int entry = menu.entries().get(row);
        return SEPARATOR.equals(owner.row(entry)) ? -1 : entry;
    }

    /** Whether a point is on an open menu at all. */
    static boolean contains(final Open menu, final double mouseX, final double mouseY) {
        final UiLayout.Rect rect = menu.rect();
        return mouseX >= rect.x() && mouseX < rect.x() + rect.w() && mouseY >= rect.y()
                && mouseY < rect.y() + rect.h();
    }
}
