/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.gui.layout.ComputerTerminalLayout;
import dev.jstech.computers.menu.ComputerTerminalMenu;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FormattedCharSequence;

/**
 * The Storage heading: what this machine's own disks hold, with the public/private slider for each disk
 * in the panel beside the grid.
 *
 * <p>The sliders used to be a band above the grid, which cost the grid a row on the one heading that most
 * wants them. They live where the detail of what is picked out lives on the other headings, because that
 * is what they are: the detail of the store this heading is showing.
 */
final class StorageTerminalTab extends AbstractTerminalTab {

    private static final int GRID_ROWS = ComputerTerminalLayout.GRID_ROWS;
    private static final int TOOLBAR_Y = ComputerTerminalLayout.TOOLBAR_Y;
    private static final int SORT_X = ComputerTerminalLayout.SORT_X;
    private static final int SORT_W = ComputerTerminalLayout.SORT_W;
    private static final int MOD_X = ComputerTerminalLayout.MOD_X;
    private static final int MOD_W = ComputerTerminalLayout.MOD_W;
    private static final int PANE_X = ComputerTerminalLayout.PANE_X;
    private static final int PANE_Y = ComputerTerminalLayout.PANE_Y;
    private static final int PANE_W = ComputerTerminalLayout.PANE_W;
    private static final int DEPOSIT_X = ComputerTerminalLayout.DEPOSIT_X;
    private static final int DEPOSIT_Y = ComputerTerminalLayout.DEPOSIT_Y;
    private static final int DEPOSIT_W = ComputerTerminalLayout.DEPOSIT_W;

    /* The slider band, panel-relative; the screen's drag math reads the very same numbers. */
    private static final int SLIDER_TRACK0_DY = ComputerTerminalLayout.SLIDER_TRACK0_DY;
    private static final int SLIDER_ROW_PITCH = ComputerTerminalLayout.SLIDER_ROW_PITCH;
    private static final int SLIDER_TRACK_H = ComputerTerminalLayout.SLIDER_TRACK_H;
    private static final int SLIDER_TRACK_LX = ComputerTerminalLayout.SLIDER_TRACK_LX;
    private static final int SLIDER_HANDLE_W = ComputerTerminalLayout.SLIDER_HANDLE_W;
    private static final int SLIDER_LABEL_DY = ComputerTerminalLayout.SLIDER_LABEL_DY;

    StorageTerminalTab(final ComputerTerminalScreen screen, final ComputerTerminalMenu menu) {
        super(screen, menu);
    }

    @Override
    public void renderTabBg(final GuiGraphics g, final int x, final int y,
                            final int cx, final int cy, final int cw,
                            final int mouseX, final int mouseY, final float partialTick) {
        gridBg(g, x, y, 0, GRID_ROWS);
        screen.paneBg(g, x, y);
        sliderBandBg(g, x, y);
    }

    @Override
    public void renderTabLabels(final GuiGraphics g, final int cx, final int cy, final int cw) {
        final int shown = visibleItems().size();
        final String t = GameText.resolve(
                (shown == 1 ? TerminalGridTexts.ONE_TYPE : TerminalGridTexts.TYPES).with(shown));
        g.drawString(font(), t, cx + cw - font().width(t), TOOLBAR_Y + 3, DIM(), false);
        g.drawCenteredString(font(),
                GameText.resolve(screen.sortByQuantity ? TerminalGridTexts.QUANTITY : TerminalGridTexts.NAME),
                SORT_X + SORT_W / 2, TOOLBAR_Y + 3, ACCENT());
        final String mod = screen.modFilter();
        g.drawCenteredString(font(),
                mod.isEmpty() ? GameText.resolve(TerminalGridTexts.MOD) : font().plainSubstrByWidth(mod, MOD_W - 6),
                MOD_X + MOD_W / 2, TOOLBAR_Y + 3, mod.isEmpty() ? DIM() : ACCENT());
        final boolean holding = !menu.getCarried().isEmpty();
        g.drawCenteredString(font(), GameText.resolve(TerminalGridTexts.STORE_ALL),
                DEPOSIT_X + DEPOSIT_W / 2 + 4, DEPOSIT_Y + 2, holding ? ACCENT() : DIM());
        sliderBandLabels(g);
    }

    // The public/private band

    private void sliderBandBg(final GuiGraphics g, final int x, final int y) {
        if (!menu.storageHasSlider()) {
            return;
        }
        final int trackX = x + PANE_X + SLIDER_TRACK_LX;
        final int trackW = PANE_W - SLIDER_TRACK_LX * 2;
        for (int d = 0; d < shownDisks(); d++) {
            sliderTrackBg(g, trackX, y + PANE_Y + SLIDER_TRACK0_DY + d * SLIDER_ROW_PITCH, trackW, d);
        }
    }

    /** How many disks the panel has room to show, which is every one of them on any machine so far. */
    private int shownDisks() {
        return Math.min(menu.diskCount(), ComputerTerminalLayout.sliderRows());
    }

    private void sliderTrackBg(final GuiGraphics g, final int tx, final int ty, final int tw, final int disk) {
        final int permille = sliderValue(disk);
        final boolean empty = menu.diskCapacityWeight(disk) <= 0L;
        g.fill(tx, ty, tx + tw, ty + SLIDER_TRACK_H, TRACK());
        g.fill(tx, ty, tx + tw, ty + 1, LINE());
        if (empty) {
            return;
        }
        final int span = tw - SLIDER_HANDLE_W;
        final int handleX = tx + Math.round(span * (permille / 1000.0f));
        if (handleX > tx + 1) {
            g.fill(tx + 1, ty + 1, handleX, ty + SLIDER_TRACK_H - 1, GREEN());
        }
        if (handleX + SLIDER_HANDLE_W < tx + tw - 1) {
            g.fill(handleX + SLIDER_HANDLE_W, ty + 1, tx + tw - 1, ty + SLIDER_TRACK_H - 1, PANEL());
        }
        for (int i = 0; i <= 4; i++) {
            final int tickX = tx + Math.round(span * (i / 4.0f)) + SLIDER_HANDLE_W / 2;
            g.fill(tickX, ty + SLIDER_TRACK_H, tickX + 1, ty + SLIDER_TRACK_H + 1, LINE());
        }
        final int handleColor = screen.draggingSliderDisk == disk ? 0xFFFFFFFF : ACCENT();
        g.fill(handleX, ty - ComputerTerminalLayout.SLIDER_HANDLE_OVERHANG, handleX + SLIDER_HANDLE_W,
                ty + SLIDER_TRACK_H + ComputerTerminalLayout.SLIDER_HANDLE_OVERHANG, handleColor);
    }

    private void sliderBandLabels(final GuiGraphics g) {
        final int px = PANE_X + 6;
        final int right = PANE_X + PANE_W - 6;
        g.drawString(font(), GameText.resolve(TerminalGridTexts.THIS_MACHINES_DISKS), px, PANE_Y + 6, ACCENT(), false);
        if (!menu.storageHasSlider()) {
            final int below = lines(g, TerminalGridTexts.ALL_OFFERED, px, PANE_Y + 22, TEXT());
            final String store = menu.storageCapacity() <= 0 ? GameText.resolve(TerminalGridTexts.NO_DISK)
                    : fmt(menu.storageUsed()) + " / " + fmt(menu.storageCapacity());
            g.drawString(font(), store, px, below + 8, GREEN(), false);
            return;
        }
        for (int d = 0; d < shownDisks(); d++) {
            final int ty = PANE_Y + SLIDER_TRACK0_DY + d * SLIDER_ROW_PITCH;
            g.drawString(font(), GameText.resolve(TerminalGridTexts.DISK_LETTER.with(String.valueOf((char) ('A' + d)))),
                    px, ty - SLIDER_LABEL_DY, DIM(), false);
            if (menu.diskCapacityWeight(d) <= 0L) {
                final String empty = GameText.resolve(TerminalGridTexts.EMPTY);
                g.drawString(font(), empty, right - font().width(empty), ty - SLIDER_LABEL_DY, DIM(), false);
                continue;
            }
            final String readout = GameText.resolve(TerminalGridTexts.OFFERED.with(sliderValue(d) / 10));
            g.drawString(font(), readout, right - font().width(readout), ty - SLIDER_LABEL_DY, GREEN(), false);
        }
        // The word about dragging only where there is room left for all of it under the last track.
        final int footY = PANE_Y + SLIDER_TRACK0_DY + shownDisks() * SLIDER_ROW_PITCH + 6;
        final int rows = font().split(GameText.component(TerminalGridTexts.DRAG_HINT), PANE_W - 12).size();
        if (footY + rows * 10 > PANE_Y + ComputerTerminalLayout.PANE_H) {
            return;
        }
        lines(g, TerminalGridTexts.DRAG_HINT, px, footY, DIM());
    }

    /** Writes a sentence wrapped to the panel's width, and says where the line under it would go. */
    private int lines(final GuiGraphics g, final TextKey sentence, final int x, final int y, final int color) {
        int at = y;
        for (final FormattedCharSequence line : font().split(GameText.component(sentence), PANE_W - 12)) {
            g.drawString(font(), line, x, at, color, false);
            at += 10;
        }
        return at;
    }
}
