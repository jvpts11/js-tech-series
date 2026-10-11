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
 * Pure layout of the Server Rack screen, drawn as the cabinet seen from the front: the rack-unit ruler down the left,
 * the two posts with the units between them (each mounted unit a faceplate with its own slot at the left end, its drive
 * caddies and gadget bays along it and its power button at the right end, an empty unit a blank panel), the card of the
 * selected unit on the right, and the player inventory below. The menu places its slots from these constants and the
 * screen draws from the same ones, so a moved control is caught by the unit test before it reaches the game.
 *
 * <p>All coordinates are relative to the panel's top-left corner. A slot's coordinates are where its item goes; its
 * frame is drawn one pixel out on every side.
 */
public final class ServerRackLayout {

    /*
     * Eight rack units of 18px plus a player inventory leave no room to spare: the cabinet summary rides in the header
     * instead of taking a line of its own, and the panel lands just inside the project's 256px height ceiling.
     */
    public static final int WIDTH = 268;
    public static final int HEIGHT = 254;

    /** The header strip and its title. */
    public static final int HEADER_X = 6;
    public static final int HEADER_Y = 6;
    public static final int HEADER_W = 256;
    public static final int HEADER_H = 16;
    public static final int TITLE_X = 10;
    public static final int TITLE_Y = 10;

    public static final int ROWS = 8;
    public static final int ROW_Y0 = 26;
    public static final int ROW_PITCH = 18;
    /** A slot's frame, the item's 16 pixels and a pixel of frame on each side. */
    public static final int SLOT = 18;

    /** The rack-unit ruler's labels down the left edge. */
    public static final int RULER_X = 9;
    /** The cabinet's back, from here to just past the right post. */
    public static final int CABINET_X = 8;
    /** Where the units begin and end between the posts: the left post stands just left of the first. */
    public static final int RAIL_L = 22;
    public static final int RAIL_R = 184;
    /** A post's width. */
    public static final int POST_W = 4;
    /** The unit's own slot, at the left end of its faceplate. */
    public static final int SERVER_X = RAIL_L + 3;
    /** The front-panel slots of a unit, five to a rack unit, from here along the faceplate. */
    public static final int FRONT_X = RAIL_L + 24;
    public static final int FRONT_SLOTS = 5;
    /**
     * The power button at the right end of a mounted computer's faceplate: its lamp at the left, its caption after it,
     * wide enough for the game's small type to clear its frame.
     */
    public static final int PWR_X = RAIL_R - 24;
    public static final int PWR_W = 22;
    public static final int PWR_H = 12;
    public static final int PWR_LAMP_DX = 3;
    public static final int PWR_LABEL_DX = 8;

    /** The card of the selected unit, right of the cabinet. */
    public static final int CARD_X = 192;
    public static final int CARD_Y = ROW_Y0;
    public static final int CARD_W = 68;
    public static final int CARD_H = 144;
    /** The card's lines: the first's top, and the step from one to the next. */
    public static final int CARD_LINE_Y = CARD_Y + 25;
    public static final int CARD_LINE_PITCH = 19;
    /** How many lines the card shows: state, boot, system, network, RAID and KVM. */
    public static final int CARD_LINES = 6;

    public static final int INV_X = 8;
    public static final int INV_Y = 174;
    public static final int HOTBAR_Y = INV_Y + 58;

    private ServerRackLayout() {
    }

    /** The top of rack unit {@code row}. */
    public static int rowY(final int row) {
        return ROW_Y0 + row * ROW_PITCH;
    }

    /** Where the item of a front-panel slot of {@code column} goes. */
    public static int frontItemX(final int column) {
        return FRONT_X + column * SLOT;
    }

    /** Where the item of a row's server slot goes. */
    public static int serverItemX() {
        return SERVER_X;
    }

    /** Where the items of a row's slots go. */
    public static int itemY(final int row) {
        return rowY(row) + 1;
    }

    /** The top of a unit's power button, centred on a unit {@code heightU} rack units tall topped at {@code row}. */
    public static int pwrY(final int row, final int heightU) {
        return rowY(row) + (heightU * ROW_PITCH - PWR_H) / 2;
    }

    /** The top of the card's line {@code index}: its label, with its value under it. */
    public static int cardLineY(final int index) {
        return CARD_LINE_Y + index * CARD_LINE_PITCH;
    }

    /**
     * The full element layout at its densest (eight 1U computers, every slot cabled), ready for the overlap and
     * out-of-bounds validators.
     */
    public static GuiLayout layout() {
        final GuiLayout l = new GuiLayout(WIDTH, HEIGHT);
        for (int row = 0; row < ROWS; row++) {
            l.slot("server" + row, serverItemX(), itemY(row));
            for (int column = 0; column < FRONT_SLOTS; column++) {
                l.slot("front" + row + "_" + column, frontItemX(column), itemY(row));
            }
            l.box("pwr" + row, PWR_X, pwrY(row, 1), PWR_W, PWR_H);
            l.text("ruler" + row, RULER_X, rowY(row) + 5, 2, 0.75f);           // "8U"
            l.text("pwrLabel" + row, PWR_X + PWR_LABEL_DX, pwrY(row, 1) + 3, 3, 0.75f);   // "PWR"
        }
        l.box("card", CARD_X, CARD_Y, CARD_W, CARD_H);
        l.text("cardUnit", CARD_X + 3, CARD_Y + 3, 2, 0.75f);                 // "8U"
        l.text("cardName", CARD_X + 3, CARD_Y + 10, 10, 1.0f);                // a name, clipped to the card
        for (int i = 0; i < CARD_LINES; i++) {
            l.text("cardLabel" + i, CARD_X + 3, cardLineY(i), 7, 0.75f);       // "NETWORK"
            l.text("cardValue" + i, CARD_X + 3, cardLineY(i) + 7, 10, 1.0f);   // "this unit", set right
        }
        l.playerInventory(INV_X, INV_Y);
        l.text("title", TITLE_X, TITLE_Y, 18, 1.0f);                          // "SUPERCOMPUTER RACK"
        final int pillChars = 19;                                             // "8/8U  THROTTLED 45%"
        l.text("statusPill", HEADER_X + HEADER_W - 4 - Math.round(pillChars * GuiLayout.GLYPH_WIDTH), TITLE_Y,
                pillChars, 1.0f);
        return l;
    }
}
