/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import dev.jstech.core.gui.layout.GuiLayout;

/**
 * The layout of the Workshop window, top to bottom: the card tabs, the station of the tab that is up with the details
 * panel at its right, the player's inventory band, and the status bar. Pure arithmetic, so a test can prove nothing
 * in the window overlaps and every cell of every station lies inside its area.
 *
 * <p>The band is the vanilla inventory shape the desktop lays the real slots over: three rows, a gap and the hotbar,
 * eighteen pixels a cell, inside a two-pixel frame. Station positions are measured from the top left of the station
 * area, the details panel's from its own.
 */
public final class WorkshopLayout {

    public static final int TAB_H = 13;
    public static final int PAD = 3;
    public static final int CELL = 18;
    public static final int STATION_TOP = TAB_H + PAD;
    public static final int STATION_H = 104;
    public static final int DETAILS_W = 152;
    public static final int LABEL_H = 10;
    public static final int STATUS_H = 12;
    public static final int INV_COLS = 9;
    public static final int INV_ROWS = 4;
    public static final int HOTBAR_GAP = 4;
    public static final int BAND_PAD = 2;
    public static final int BAND_H = BAND_PAD * 2 + INV_ROWS * CELL + HOTBAR_GAP;
    public static final int BAND_W = BAND_PAD * 2 + INV_COLS * CELL;
    public static final int MIN_W = 340;
    public static final int DEFAULT_W = 400;

    /** The crafting grid's corner and the result cell's, in the station area. */
    public static final int GRID_X = 10;
    public static final int GRID_Y = (STATION_H - 3 * CELL) / 2;
    public static final int RESULT_X = GRID_X + 3 * CELL + 30;
    public static final int RESULT_Y = GRID_Y + CELL;
    /** The furnace's input and output cells, and the arrow between them. */
    public static final int FURNACE_IN_X = 36;
    public static final int FURNACE_IN_Y = 30;
    public static final int FURNACE_ARROW_X = FURNACE_IN_X + CELL + 32;
    public static final int FURNACE_ARROW_W = 34;
    public static final int FURNACE_OUT_X = FURNACE_ARROW_X + FURNACE_ARROW_W + 10;
    /** The note under a cell that has no fuel or lapis beside it, two lines. */
    public static final int NOTE_Y = FURNACE_IN_Y + CELL + 4;
    /** The longest line of that note, in characters of the small text it is drawn in. */
    public static final int NOTE_CHARS = 14;
    public static final float NOTE_SCALE = 0.85f;
    /* The names the checks report the text elements by. */
    private static final String NOTE = "note";
    private static final String NAME_LABEL = "label";
    /** The enchanting item cell and the column of the three offers. */
    public static final int ENCHANT_ITEM_X = 28;
    public static final int ENCHANT_ITEM_Y = FURNACE_IN_Y;
    public static final int OFFERS_X = 90;
    public static final int OFFER_Y = 15;
    public static final int OFFER_H = 22;
    public static final int OFFER_GAP = 4;
    /** The anvil's name row and its row of cells. */
    public static final int NAME_Y = 16;
    public static final int NAME_LABEL_W = 30;
    public static final int NAME_LABEL_CHARS = 4;
    public static final int FIELD_H = 12;
    public static final int ANVIL_ROW_Y = 50;
    public static final int ANVIL_LEFT_X = 10;
    public static final int ANVIL_RIGHT_X = ANVIL_LEFT_X + CELL + 16;
    public static final int ANVIL_ARROW_X = ANVIL_RIGHT_X + CELL + 10;
    public static final int ANVIL_ARROW_W = 26;
    public static final int ANVIL_RESULT_X = ANVIL_ARROW_X + ANVIL_ARROW_W + 10;
    /** The details panel: the header, the first body line, the button row. */
    public static final int HEADER_H = 24;
    public static final int BODY_LINE = 8;
    public static final int BUTTON_H = 12;

    private WorkshopLayout() {
    }

    /** The content height the window needs: tabs, the station, the band with its label, the status bar. */
    public static int minContentHeight() {
        return STATION_TOP + STATION_H + PAD + LABEL_H + BAND_H + PAD + STATUS_H;
    }

    /** The station area's width in a window {@code contentWidth} wide: everything left of the details panel. */
    public static int stationWidth(final int contentWidth) {
        return detailsX(contentWidth) - PAD * 2;
    }

    /** The content-local x of the details panel. */
    public static int detailsX(final int contentWidth) {
        return contentWidth - PAD - DETAILS_W;
    }

    /** The content-local top of the band frame. */
    public static int bandTop(final int contentHeight) {
        return contentHeight - STATUS_H - PAD - BAND_H;
    }

    /** The y of inventory row {@code row} inside the band frame; the hotbar sits a gap below the main rows. */
    public static int rowYOffset(final int row) {
        return row * CELL + (row >= 3 ? HOTBAR_GAP : 0);
    }

    /** The width of the offers' column in a station {@code stationWidth} wide. */
    public static int offerWidth(final int stationWidth) {
        return stationWidth - OFFERS_X - PAD;
    }

    /** The y of offer {@code offer} in the station. */
    public static int offerY(final int offer) {
        return OFFER_Y + offer * (OFFER_H + OFFER_GAP);
    }

    /** The solid regions of a window {@code contentWidth} by {@code contentHeight}, for the overlap check. */
    public static GuiLayout layout(final int contentWidth, final int contentHeight) {
        return new GuiLayout(contentWidth, contentHeight)
                .box("tabs", 0, 0, contentWidth, TAB_H)
                .box("station", PAD, STATION_TOP, stationWidth(contentWidth), STATION_H)
                .box("details", detailsX(contentWidth), STATION_TOP, DETAILS_W, STATION_H)
                .box("band", PAD, bandTop(contentHeight), BAND_W, BAND_H)
                .box("status", 0, contentHeight - STATUS_H, contentWidth, STATUS_H);
    }

    /*
     * The stations share one area and only one shows at a time, so each is checked on its own, measured from the
     * station's corner.
     */

    /** The crafting station: the grid, the arrow and the result. */
    public static GuiLayout crafting(final int stationWidth) {
        final GuiLayout layout = new GuiLayout(stationWidth, STATION_H);
        for (int i = 0; i < 9; i++) {
            layout.box("grid" + i, GRID_X + (i % 3) * CELL, GRID_Y + (i / 3) * CELL, CELL, CELL);
        }
        return layout.box("result", RESULT_X, RESULT_Y, CELL, CELL);
    }

    /** The furnace station: input, note, arrow and output. */
    public static GuiLayout furnace(final int stationWidth) {
        return new GuiLayout(stationWidth, STATION_H)
                .box("furnaceIn", FURNACE_IN_X, FURNACE_IN_Y, CELL, CELL)
                .box("furnaceArrow", FURNACE_ARROW_X, FURNACE_IN_Y + 5, FURNACE_ARROW_W, 8)
                .box("furnaceOut", FURNACE_OUT_X, FURNACE_IN_Y, CELL, CELL)
                .text(NOTE, PAD, NOTE_Y, NOTE_CHARS, NOTE_SCALE);
    }

    /** The enchanting station: the item, its note and the three offers. */
    public static GuiLayout enchanting(final int stationWidth) {
        final GuiLayout layout = new GuiLayout(stationWidth, STATION_H)
                .box("enchantItem", ENCHANT_ITEM_X, ENCHANT_ITEM_Y, CELL, CELL)
                .text(NOTE, PAD, NOTE_Y, NOTE_CHARS, NOTE_SCALE);
        for (int i = 0; i < 3; i++) {
            layout.box("offer" + i, OFFERS_X, offerY(i), offerWidth(stationWidth), OFFER_H);
        }
        return layout;
    }

    /** The anvil station: the name and the row of cells. */
    public static GuiLayout anvil(final int stationWidth) {
        return new GuiLayout(stationWidth, STATION_H)
                .text(NAME_LABEL, ANVIL_LEFT_X, NAME_Y + 2, NAME_LABEL_CHARS, 1.0f)
                .box("name", ANVIL_LEFT_X + NAME_LABEL_W, NAME_Y, stationWidth - ANVIL_LEFT_X - NAME_LABEL_W - PAD,
                        FIELD_H)
                .box("anvilLeft", ANVIL_LEFT_X, ANVIL_ROW_Y, CELL, CELL)
                .box("anvilRight", ANVIL_RIGHT_X, ANVIL_ROW_Y, CELL, CELL)
                .box("anvilArrow", ANVIL_ARROW_X, ANVIL_ROW_Y + 5, ANVIL_ARROW_W, 8)
                .box("anvilResult", ANVIL_RESULT_X, ANVIL_ROW_Y, CELL, CELL);
    }
}
