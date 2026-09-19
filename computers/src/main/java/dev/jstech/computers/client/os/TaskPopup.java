/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.gui.TaskbarGroups;
import dev.jstech.core.client.gui.component.Texts;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * The flyout that lists one program's windows over its button on the panel.
 *
 * <p>It is what a taskbar gives a player who has the same program open several times: the row of buttons
 * says a program is running, and this says which of its windows is which. A modern panel draws them as
 * cards with each window's live picture, because that is the only way of telling two documents apart at a
 * glance; a period one draws a list of titles with a Close all at the foot, because that is what such a
 * panel did and a thumbnail on it would be an anachronism.
 *
 * <p>Most of this class is timing, and the timing is the whole reason it is not simply drawn. It rises
 * after the cursor has rested on a program for a moment, so brushing past a button does not make menus
 * jump out; it follows the cursor straight across to the next program once it is up, because waiting
 * again for each neighbour reads as lag; and it goes away a moment after the cursor has left both it and
 * its program, so the trip from the button down to a card does not dismiss it halfway. One opened by a
 * click ignores all of that and stays until something puts it away, which is what a click should mean.
 *
 * <p>It holds that state itself rather than leaving it on the screen: what is showing, whether a click
 * pinned it, which program the cursor is over and since when. Every rectangle it draws is the rectangle
 * the clicks are tested against, so the card a player sees and the card they hit are the same box.
 */
final class TaskPopup {

    /** The program whose windows are showing, or null while nothing is. */
    @Nullable
    private String key;

    /** Whether a click opened it, in which case the cursor leaving does not close it. */
    private boolean sticky;

    /** The program the cursor is over and when it arrived, which is what the opening delay is measured on. */
    @Nullable
    private String hoverKey;
    private long hoverSince;

    /** When the cursor left both the popup and its program, or 0 while it has not. */
    private long leftAt;

    /*
     * A card carries a title line and a picture of the window under it; a list row carries an icon and a
     * title. The card is deliberately small: a desktop with six windows open has to fit six of them
     * side by side and still leave the panel's own ends clear.
     */
    private static final int CARD_W = 60;
    private static final int CARD_TITLE_H = 10;
    private static final int THUMB_H = 32;
    private static final int CARD_H = CARD_TITLE_H + THUMB_H + 4;
    private static final int POPUP_PAD = 3;
    private static final int LIST_W = 120;
    private static final int LIST_ROW_H = 11;

    /** How long the cursor rests on a program before it opens, and how long it may be away before it shuts. */
    private static final long HOVER_MS = 350L;
    private static final long LEAVE_MS = 300L;

    private final DesktopScreen desktop;

    TaskPopup(final DesktopScreen desktop) {
        this.desktop = desktop;
    }

    /** The program whose windows are showing, or null while nothing is. */
    @Nullable
    String key() {
        return key;
    }

    /** Whether the popup is actually up, which needs both a program and a window of its own to list. */
    boolean isOpen() {
        return key != null && !windows().isEmpty();
    }

    /** The titles it lists, in order, empty when it is not up. */
    List<String> titles() {
        final List<String> out = new ArrayList<>();
        for (final DesktopWindow w : windows()) {
            out.add(desktop.titleOf(w));
        }
        return out;
    }

    /** The desktop-local centre of the {@code index}-th card or row, where a click lands on it. */
    @Nullable
    int[] itemPoint(final int index) {
        final int[] r = itemRect(index);
        return r == null ? null : new int[] {r[0] + r[2] / 2, r[1] + r[3] / 2};
    }

    /** The desktop-local centre of the close box for the {@code index}-th window. */
    @Nullable
    int[] closePoint(final int index) {
        final int[] r = closeRect(index);
        return r == null ? null : new int[] {r[0] + r[2] / 2, r[1] + r[3] / 2};
    }

    /** Opens it on a program and pins it there: a click asked for it, so the cursor leaving must not shut it. */
    void openFor(final String program) {
        key = program;
        sticky = true;
        leftAt = 0;
    }

    /** Puts it away, whatever opened it. */
    void dismiss() {
        key = null;
        sticky = false;
    }

    /**
     * Keeps it in step with the cursor, once a frame: it rises after the cursor has rested on an open
     * program for a moment, moves to the program the cursor moves to, and goes away a moment after the
     * cursor has left both it and its program. One opened by a click stays put instead.
     */
    void update(final int lmx, final int lmy, final int sw, final int tbY) {
        if (desktop.menuOrDialogOpen()) {
            if (!sticky) {
                key = null;
            }
            return;
        }
        if (key != null && desktop.windowsOf(key).isEmpty()) {
            dismiss();
        }
        if (desktop.panelOnTop()) {
            return; // the top bar lists no programs
        }
        final DesktopScreen.TaskStrip strip = desktop.taskButtons(sw);
        final int idx = lmy >= tbY ? strip.indexAt(lmx) : -1;
        final String under = idx >= 0 && strip.entries().get(idx).open() ? strip.entries().get(idx).key() : null;
        final long now = System.currentTimeMillis();
        if (under != null) {
            if (!under.equals(hoverKey)) {
                hoverKey = under;
                hoverSince = now;
            }
            /*
             * Once one is up, the cursor moving to a neighbouring program switches straight to it. Waiting
             * out the delay again at every button reads as the panel lagging behind the cursor.
             */
            final boolean switching = key != null && !sticky;
            if (thumbnails() && !under.equals(key) && (switching || now - hoverSince >= HOVER_MS)) {
                key = under;
                sticky = false;
                leftAt = 0;
            }
        } else {
            hoverKey = null;
        }
        if (key != null && !sticky) {
            final boolean over = key.equals(under) || inRect(lmx, lmy, rect(sw, tbY));
            if (over) {
                leftAt = 0;
            } else if (leftAt == 0) {
                leftAt = now;
            } else if (now - leftAt > LEAVE_MS) {
                key = null;
                leftAt = 0;
            }
        }
    }

    /** Draws it: cards with the windows' live pictures on a modern panel, a list of titles on a period one. */
    void render(final GuiGraphics g, final int tbY, final int sw, final int sh, final int lmx, final int lmy) {
        final int[] r = rect(sw, tbY);
        if (r == null) {
            return;
        }
        final OsSkin skin = desktop.panelSkin();
        skin.windowShadow(g, r[0], r[1], r[2], r[3]);
        skin.panel(g, r[0], r[1], r[2], r[3]);
        final List<DesktopWindow> list = windows();
        final ResourceLocation icon = desktop.programIdFor(key);
        if (thumbnails()) {
            drawCards(g, list, icon, skin, sw, sh, lmx, lmy);
            return;
        }
        drawRows(g, list, icon, skin, lmx, lmy);
    }

    /**
     * A click while it is up. On one of its windows it brings that window forward; on a close box it ends
     * that window; anywhere else it puts the popup away, and a click on the program's own entry that
     * opened it is that and nothing more.
     */
    boolean click(final double mx, final double my, final int button) {
        final int tbY = desktop.screenH() - DesktopScreen.TASKBAR_H;
        final int[] r = rect(desktop.screenW(), tbY);
        final List<DesktopWindow> list = windows();
        if (r == null) {
            dismiss();
            return false;
        }
        if (inRect(mx, my, r)) {
            return clickInside(mx, my, button, list);
        }
        final String was = key;
        final boolean pinned = sticky;
        dismiss();
        if (pinned && button == 0 && my >= tbY) {
            final DesktopScreen.TaskStrip strip = desktop.taskButtons(desktop.screenW());
            final int idx = strip.indexAt(mx);
            if (idx >= 0 && strip.entries().get(idx).key().equals(was)) {
                return true; // the entry that opened it closes it; nothing more
            }
        }
        return false;
    }

    /** The windows it lists, back to front: the program's, as many as the desktop has room for. */
    private List<DesktopWindow> windows() {
        if (key == null) {
            return List.of();
        }
        final List<DesktopWindow> mine = desktop.windowsOf(key);
        if (thumbnails()) {
            final int most = Math.max(1, (desktop.screenW() - 4 - POPUP_PAD) / (CARD_W + POPUP_PAD));
            return mine.size() > most ? mine.subList(0, most) : mine;
        }
        return mine;
    }

    /** Whether this panel shows the windows' live pictures rather than their titles. */
    private boolean thumbnails() {
        return desktop.popupShowsThumbnails();
    }

    /** Its box, desktop-local {x, y, w, h}, or null while it is not up. */
    @Nullable
    private int[] rect(final int sw, final int tbY) {
        final List<DesktopWindow> list = windows();
        if (list.isEmpty()) {
            return null;
        }
        final DesktopScreen.TaskStrip strip = desktop.taskButtons(sw);
        final int index = TaskbarGroups.indexOf(strip.entries(), key);
        if (index < 0) {
            return null;
        }
        final int center = strip.x()[index] + strip.w()[index] / 2;
        final int n = list.size();
        final int w;
        final int h;
        if (thumbnails()) {
            w = n * CARD_W + (n + 1) * POPUP_PAD;
            h = CARD_H + 2 * POPUP_PAD;
        } else {
            w = LIST_W;
            h = 2 + n * LIST_ROW_H + 5 + LIST_ROW_H + 2;
        }
        final int x = Math.max(2, Math.min(center - w / 2, sw - w - 2));
        final int y = desktop.panelOnTop() ? DesktopScreen.TASKBAR_H + 3 : tbY - 3 - h;
        return new int[] {x, y, w, h};
    }

    /** Its {@code index}-th card or row, desktop-local {x, y, w, h}, or null. */
    @Nullable
    private int[] itemRect(final int index) {
        final int[] r = rect(desktop.screenW(), desktop.screenH() - DesktopScreen.TASKBAR_H);
        if (r == null || index < 0 || index >= windows().size()) {
            return null;
        }
        if (thumbnails()) {
            return new int[] {r[0] + POPUP_PAD + index * (CARD_W + POPUP_PAD), r[1] + POPUP_PAD, CARD_W, CARD_H};
        }
        return new int[] {r[0] + 2, r[1] + 2 + index * LIST_ROW_H, r[2] - 4, LIST_ROW_H};
    }

    /** The close box of the {@code index}-th window: on a card its corner, on a list the Close all row. */
    @Nullable
    private int[] closeRect(final int index) {
        final int[] item = itemRect(index);
        if (item == null) {
            return null;
        }
        if (thumbnails()) {
            return new int[] {item[0] + CARD_W - 10, item[1] + 1, 9, 9};
        }
        return closeAllRect();
    }

    /** The "Close all" row of a list popup, or null on a modern panel. */
    @Nullable
    private int[] closeAllRect() {
        final int[] r = rect(desktop.screenW(), desktop.screenH() - DesktopScreen.TASKBAR_H);
        if (r == null || thumbnails()) {
            return null;
        }
        return new int[] {r[0] + 2, r[1] + 2 + windows().size() * LIST_ROW_H + 5, r[2] - 4, LIST_ROW_H};
    }

    /** The cards of a modern panel: an icon and a shortened title over a live picture of the window. */
    private void drawCards(final GuiGraphics g, final List<DesktopWindow> list, final ResourceLocation icon,
                           final OsSkin skin, final int sw, final int sh, final int lmx, final int lmy) {
        for (int i = 0; i < list.size(); i++) {
            final DesktopWindow w = list.get(i);
            final int[] card = itemRect(i);
            if (card == null) {
                continue;
            }
            final boolean hot = inRect(lmx, lmy, card);
            if (hot) {
                g.fill(card[0], card[1], card[0] + card[2], card[1] + card[3], skin.listHover());
            }
            ProgramIcons.draw(g, card[0] + 2, card[1] + 1, 8, 8, icon, desktop.icons());
            // The title gives up room for the close box, which only appears on the card under the cursor.
            final int titleW = CARD_W - 12 - (hot ? 10 : 2);
            final String title = desktop.textFont().plainSubstrByWidth(desktop.titleOf(w), Texts.smallFits(titleW));
            Texts.small(g, desktop.textFont(), title, card[0] + 12, card[1] + 2, skin.text());
            if (hot) {
                drawCardClose(g, skin, i, lmx, lmy);
            }
            final int tx = card[0] + 3;
            final int ty = card[1] + CARD_TITLE_H + 1;
            final int tw = CARD_W - 6;
            OsSkin.outline(g, tx - 1, ty - 1, tw + 2, THUMB_H + 2, skin.edge());
            g.fill(tx, ty, tx + tw, ty + THUMB_H, skin.fieldBg());
            w.renderThumbnail(g, desktop.textFont(), skin, tx, ty, tw, THUMB_H, sw, sh,
                    desktop.panelReserve(), desktop.workAreaTop());
        }
    }

    /** The close box in a card's corner, red under the cursor so it is clear what the click will do. */
    private void drawCardClose(final GuiGraphics g, final OsSkin skin, final int index,
                               final int lmx, final int lmy) {
        final int[] close = closeRect(index);
        if (close == null) {
            return;
        }
        final boolean over = inRect(lmx, lmy, close);
        if (over) {
            g.fill(close[0], close[1], close[0] + close[2], close[1] + close[3], 0xFFC04A3E);
        }
        g.drawString(desktop.textFont(), "x", close[0] + 2, close[1],
                over ? 0xFFFFFFFF : skin.text(), false);
    }

    /** The rows of a period panel: an icon and the window's title, dimmed while that window is put away. */
    private void drawRows(final GuiGraphics g, final List<DesktopWindow> list, final ResourceLocation icon,
                          final OsSkin skin, final int lmx, final int lmy) {
        for (int i = 0; i < list.size(); i++) {
            final DesktopWindow w = list.get(i);
            final int[] row = itemRect(i);
            if (row == null) {
                continue;
            }
            if (inRect(lmx, lmy, row)) {
                g.fill(row[0], row[1], row[0] + row[2], row[1] + row[3], skin.listHover());
            }
            ProgramIcons.draw(g, row[0] + 2, row[1] + 1, 9, 9, icon, desktop.icons());
            g.drawString(desktop.textFont(),
                    desktop.textFont().plainSubstrByWidth(desktop.titleOf(w), row[2] - 16),
                    row[0] + 14, row[1] + 2, w.minimized() ? skin.dim() : skin.text(), false);
        }
        final int[] all = closeAllRect();
        if (all == null) {
            return;
        }
        g.fill(all[0] + 2, all[1] - 3, all[0] + all[2] - 2, all[1] - 2, skin.edge());
        if (inRect(lmx, lmy, all)) {
            g.fill(all[0], all[1], all[0] + all[2], all[1] + all[3], skin.listHover());
        }
        g.drawString(desktop.textFont(), "Close all", all[0] + 14, all[1] + 2, 0xFFC04A3E, false);
    }

    /** A click landing inside the popup: a close box, a window, the Close all row, or nothing at all. */
    private boolean clickInside(final double mx, final double my, final int button,
                                final List<DesktopWindow> list) {
        if (button != 0) {
            return true;
        }
        for (int i = 0; i < list.size(); i++) {
            if (thumbnails() && inRect(mx, my, closeRect(i))) {
                desktop.closeOne(list.get(i));
                if (key != null && desktop.windowsOf(key).isEmpty()) {
                    dismiss();
                }
                return true;
            }
            if (inRect(mx, my, itemRect(i))) {
                desktop.focusOne(list.get(i));
                dismiss();
                return true;
            }
        }
        if (inRect(mx, my, closeAllRect()) && key != null) {
            desktop.closeAllOf(key);
            dismiss();
        }
        return true;
    }

    private static boolean inRect(final double mx, final double my, @Nullable final int[] r) {
        return r != null && mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3];
    }
}
