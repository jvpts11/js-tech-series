/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import dev.jstech.computers.gui.MonitorGlass;
import dev.jstech.core.gui.layout.GuiLayout;

/**
 * Pure layout model for the operating space a network machine draws: the whole monitor glass, with the
 * status bar across the top, the heading rail down the left and the player's own slots glued over the
 * bottom. It is the single source of the inventory slot positions, which {@code ComputerTerminalMenu}
 * consumes, so the validated layout covers the real slots rather than a drawing of them.
 *
 * <p>The glass is one size for every machine. It used to be a 244 by 230 window that grew by a band when
 * the host was a Mainframe, which threw away 140 columns of a screen every other system fills and made
 * the one interface in the mod that is a whole system read as an inventory panel.
 *
 * <p>Per-heading content (the popups, the console, the pattern lists) is drawn by the individual tab
 * renderers and is not modelled here; what is modelled is the frame they all share.
 */
public final class ComputerTerminalLayout {

    /** The monitor's glass, which is what this fills, asked of the one place that says how big it is. */
    public static final int WIDTH = MonitorGlass.WIDTH;
    public static final int HEIGHT = MonitorGlass.HEIGHT;

    /** The status bar across the top: which network, how big it is and what it is doing. */
    public static final int BAR_H = 14;

    public static final int RAIL_X = 0;
    public static final int RAIL_W = 62;
    public static final int TAB_Y0 = BAR_H + 2;
    public static final int TAB_H = 16;

    /** Where a heading's own content starts, six pixels clear of the rail's edge. */
    public static final int CONTENT_X = 68;
    public static final int CONTENT_Y = BAR_H;
    public static final int CONTENT_W = WIDTH - CONTENT_X - 6;

    /** The toolbar above the item grid: what is searched for, which mod, and the order. */
    public static final int TOOLBAR_Y = 18;
    public static final int TOOLBAR_H = 12;
    public static final int SEARCH_X = CONTENT_X;
    public static final int SEARCH_W = 104;
    public static final int MOD_X = 176;
    public static final int MOD_W = 34;
    public static final int SORT_X = 214;
    public static final int SORT_W = 40;

    /*
     * The item grid. Nine wide so it lines up with the player's own rows beneath it, which is the one
     * alignment a screen like this has to keep, and seven deep because the glass has the room for it.
     */
    public static final int GRID_X = CONTENT_X;
    public static final int GRID_Y = 34;
    public static final int GRID_COLS = 9;
    public static final int GRID_ROWS = 7;
    public static final int SLOT = 18;

    /** The panel beside the grid: everything the machine knows about the one thing that is selected. */
    public static final int PANE_X = 240;
    public static final int PANE_Y = 34;
    public static final int PANE_W = 138;
    public static final int PANE_H = 126;

    /*
     * The rule above the player's own slots, and the two things that sit under it. There are sixteen
     * pixels between the rule and the first row of slots, so the button in them is ten tall rather than
     * twelve: at twelve it sat on the rule with nothing between them, which is what it looked like.
     */
    public static final int INV_LINE_Y = 162;
    public static final int INV_LABEL_Y = 167;
    public static final int DEPOSIT_X = 308;
    public static final int DEPOSIT_Y = 165;
    public static final int DEPOSIT_W = 64;
    public static final int DEPOSIT_H = 10;

    /** The player's own slots, glued over the glass as the Network Interactor's are on a desktop. */
    public static final int INV_X = CONTENT_X;
    public static final int INV_Y = 178;
    public static final int HOTBAR_Y = 236;

    /** How many rows of the operations log the Ops heading shows, which its own hit test also reads. */
    public static final int OPS_ROWS = 6;

    /*
     * The two buttons at the foot of the panel. They live here rather than in the screen because the
     * screen draws them, the click reads them and a client test presses them: three places that once held
     * their own copy of the same number, which is how the last one came to be pressing empty glass.
     */
    public static final int PANE_BTN_Y = PANE_Y + 110;
    public static final int PANE_BTN_W = 60;
    public static final int PANE_BTN_H = 12;
    public static final int PANE_GET_X = PANE_X + 6;
    public static final int PANE_CRAFT_X = PANE_X + 72;

    /*
     * The public/private slider band, which lives in the panel beside the grid. These were written out in
     * both the heading that draws them and the screen that drags them, which is how the two came to
     * disagree; they are one set of numbers now.
     *
     * A disk's row is its name, then the track under it, and the handle stands proud of the track at each
     * end. The pitch has to clear all of that or the next disk's name is drawn across the handle above it.
     */
    public static final int SLIDER_TRACK0_DY = 30;
    public static final int SLIDER_ROW_PITCH = 22;
    public static final int SLIDER_TRACK_H = 7;
    public static final int SLIDER_TRACK_LX = 10;
    public static final int SLIDER_HANDLE_W = 3;
    /** How far the handle stands above the track and below it. */
    public static final int SLIDER_HANDLE_OVERHANG = 2;
    /**
     * How far above its track a disk's name is drawn.
     *
     * <p>Far enough that a name ends before the handle above it begins. A disk at nothing offered puts its
     * handle at the left end of the track, which is exactly where its own name is written, so a name that
     * reached down to the track would have the handle drawn through its last two rows of pixels.
     */
    public static final int SLIDER_LABEL_DY = 12;

    /** How many disks the panel has room to give a track to. */
    public static int sliderRows() {
        return (PANE_H - SLIDER_TRACK0_DY - 4) / SLIDER_ROW_PITCH;
    }

    /**
     * The panel a modal question is asked in, centred on the glass.
     *
     * <p>One size for the three that ask about a thing: what to take out of the network, what to have made,
     * and what an Operation was made of. They are the same shape because they are the same kind of question,
     * and a player should not have to find the buttons again each time.
     */
    public static final int POPUP_W = 204;
    public static final int POPUP_H = 178;

    private ComputerTerminalLayout() {
    }

    /** The frame every heading shares, which is the same frame on every machine. */
    public static GuiLayout layout() {
        final GuiLayout l = new GuiLayout(WIDTH, HEIGHT)
                .box("bar", 0, 0, WIDTH, BAR_H)
                .box("rail", RAIL_X, BAR_H, RAIL_W, HEIGHT - BAR_H)
                .box("search", SEARCH_X, TOOLBAR_Y, SEARCH_W, TOOLBAR_H)
                .box("mod", MOD_X, TOOLBAR_Y, MOD_W, TOOLBAR_H)
                .box("sort", SORT_X, TOOLBAR_Y, SORT_W, TOOLBAR_H)
                .box("pane", PANE_X, PANE_Y, PANE_W, PANE_H)
                .box("deposit", DEPOSIT_X, DEPOSIT_Y, DEPOSIT_W, DEPOSIT_H);
        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                l.box("net_" + row + "_" + col, GRID_X + col * SLOT, GRID_Y + row * SLOT, SLOT, SLOT);
            }
        }
        l.playerInventory(INV_X, INV_Y);
        return l;
    }

    /*
     * The rail down the left side. It scrolls rather than shrinking, so every entry keeps its full height
     * and its name whatever the host offers, and the five readings below are what say which entry is where.
     *
     * They are here rather than on the screen because four separate places asked the same question: the
     * drawing, the tooltips, the click and the wheel. Worked out separately they can disagree, and a rail
     * entry drawn in one row and clicked in another is the exact bug this layout model exists to prevent.
     */

    /** How tall the rail runs: from under the status bar to the bottom of the glass. */
    public static int railHeight() {
        return HEIGHT - TAB_Y0 - 2;
    }

    /** How many entries fit, never fewer than one: a rail with room for none would show nothing at all. */
    public static int visibleRows(final int railHeight) {
        return Math.max(1, railHeight / TAB_H);
    }

    /** How far the rail can scroll: zero when everything already fits. */
    public static int maxScroll(final int tabCount, final int visibleRows) {
        return Math.max(0, tabCount - visibleRows);
    }

    /** A scroll position brought back into range, which is what the wheel and the drawing both need. */
    public static int clampScroll(final int scroll, final int tabCount, final int visibleRows) {
        return Math.max(0, Math.min(scroll, maxScroll(tabCount, visibleRows)));
    }

    /** Where the entry shown in {@code row} starts, relative to the screen's top. */
    public static int rowY(final int row) {
        return TAB_Y0 + row * TAB_H;
    }

    /**
     * The row under a screen-local point, or -1 for anywhere off the rail. This is the same rectangle
     * {@link #rowY} draws, which is what keeps the entry a player sees and the entry they hit the same one.
     */
    public static int rowAt(final double localX, final double localY, final int visibleRows) {
        if (localX < RAIL_X || localX >= RAIL_X + RAIL_W) {
            return -1;
        }
        for (int row = 0; row < visibleRows; row++) {
            final int top = rowY(row);
            if (localY >= top && localY < top + TAB_H) {
                return row;
            }
        }
        return -1;
    }
}
