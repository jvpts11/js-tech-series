/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Frames 10's Task View: the desktop dims and every window on the workspace that is up is laid out over it as a card,
 * its program's icon and its title over a live picture of it, the put-away ones too. A click on a card brings that
 * window up in front; a click anywhere else, or on the button again, puts the view away.
 *
 * <p>Its colours are {@code jsc:desktop/task_view}.
 */
@PaletteHolder
final class TaskView {

    private final DesktopState desktop;
    private boolean open;

    private static final int GAP = 8;
    private static final int TITLE_H = 11;
    private static final int MOST_CARD_W = 120;

    private static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "desktop/task_view",
            new Colours(0xB0000000, 0xFFFFFFFF, 0xFF2B2B2B, 0xFF0078D7));

    TaskView(final DesktopState desktop) {
        this.desktop = desktop;
    }

    boolean isOpen() {
        return open;
    }

    void toggle() {
        open = !open;
    }

    void close() {
        open = false;
    }

    void render(final GuiGraphics g, final int sw, final int tbY, final int lmx, final int lmy) {
        if (!open) {
            return;
        }
        final Colours c = PALETTE.get();
        g.fill(0, 0, sw, tbY, c.veil());
        final List<DesktopWindow> list = windows();
        if (list.isEmpty()) {
            Draw.textCentered(g, desktop.textFont(), GameText.resolve(DesktopTexts.NO_WINDOWS), sw / 2, tbY / 2 - 4,
                    c.ink());
            return;
        }
        final OsSkin skin = desktop.prefs().skin();
        for (int i = 0; i < list.size(); i++) {
            final DesktopWindow w = list.get(i);
            final int[] r = card(i, list.size(), sw, tbY);
            final boolean hot = lmx >= r[0] && lmx < r[0] + r[2] && lmy >= r[1] && lmy < r[1] + r[3];
            if (hot) {
                g.fill(r[0] - 2, r[1] - 2, r[0] + r[2] + 2, r[1] + r[3] + 2, c.hot());
            }
            final ResourceLocation icon = desktop.programIdFor(w.groupKey());
            ProgramIcons.draw(g, r[0], r[1], 9, 9, icon, desktop.icons());
            Draw.text(g, desktop.textFont(), desktop.textFont().plainSubstrByWidth(desktop.titleOf(w), r[2] - 12),
                    r[0] + 12, r[1] + 1, c.ink());
            final int ty = r[1] + TITLE_H;
            final int th = r[3] - TITLE_H;
            g.fill(r[0], ty, r[0] + r[2], ty + th, c.paper());
            w.renderThumbnail(g, desktop.textFont(), skin, r[0], ty, r[2], th, sw, desktop.view().height(),
                    desktop.view().panelReserve(), desktop.view().workAreaTop());
        }
    }

    /** A click while the view is up: a card brings its window to the front; anything else puts the view away. */
    boolean click(final double mx, final double my, final int sw, final int tbY) {
        if (!open) {
            return false;
        }
        final List<DesktopWindow> list = windows();
        for (int i = 0; i < list.size(); i++) {
            final int[] r = card(i, list.size(), sw, tbY);
            if (mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3]) {
                final DesktopWindow w = list.get(i);
                desktop.wm().restoreGroup(w.groupKey());
                desktop.wm().bringToFront(w);
                close();
                return true;
            }
        }
        // Its own button on the taskbar puts it away as it brought it up, so a click there is the taskbar's.
        if (my >= tbY) {
            return false;
        }
        close();
        return true;
    }

    /** The desktop-local centre of card {@code index}, or null when the view is not up or has no such card. */
    int[] cardPoint(final int index, final int sw, final int tbY) {
        final List<DesktopWindow> list = windows();
        if (!open || index < 0 || index >= list.size()) {
            return null;
        }
        final int[] r = card(index, list.size(), sw, tbY);
        return new int[] {r[0] + r[2] / 2, r[1] + r[3] / 2};
    }

    /** The windows the view lays out: every program window on the workspace that is up, dialogs aside. */
    private List<DesktopWindow> windows() {
        final List<DesktopWindow> out = new ArrayList<>();
        for (final DesktopWindow w : desktop.wm().all()) {
            if (!w.dialog() && w.on(desktop.wm().workspace())) {
                out.add(w);
            }
        }
        return out;
    }

    /** Card {@code index} of {@code count}, in a grid as near square as the cards allow, centred over the desktop. */
    private static int[] card(final int index, final int count, final int sw, final int tbY) {
        final int columns = (int) Math.ceil(Math.sqrt(count));
        final int rows = (count + columns - 1) / columns;
        final int cardW = Math.min(MOST_CARD_W, (sw - GAP * (columns + 1)) / columns);
        final int cardH = Math.min(cardW * 2 / 3 + TITLE_H, (tbY - GAP * (rows + 1)) / rows);
        final int gridW = columns * cardW + (columns - 1) * GAP;
        final int gridH = rows * cardH + (rows - 1) * GAP;
        final int col = index % columns;
        final int row = index / columns;
        return new int[] {(sw - gridW) / 2 + col * (cardW + GAP), (tbY - gridH) / 2 + row * (cardH + GAP), cardW,
                cardH};
    }

    /** The view's colours: the veil over the desktop, its ink, the ground under a picture, a card under the cursor. */
    private record Colours(int veil, int ink, int paper, int hot) {
    }
}
