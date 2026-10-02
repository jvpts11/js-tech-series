/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import dev.jstech.computers.gui.layout.SoundfoundryLayout.Rect;
import dev.jstech.core.gui.layout.GuiLayout;
import dev.jstech.core.network.DataLine;
import dev.jstech.core.network.DataLink;
import dev.jstech.core.tier.HardwareEra;
import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;
import org.jetbrains.annotations.Nullable;

/**
 * Where everything of Soundfoundry's sharing window sits, the window NET opens: its tabs, the search, what was found
 * and on which computer, the download button, the songs on their way and the bar along the foot.
 *
 * <p>One plate in the same skin as the player, with a bar of its own. Positions are measured from its top-left corner.
 *
 * <p>This class is pure and carries no Minecraft dependency.
 */
public final class SoundfoundryShareLayout {

    public static final int WIDTH = 432;
    public static final int HEIGHT = 304;

    public static final int TABS_X = 6;
    public static final int TABS_Y = 16;
    public static final int TAB_H = 13;
    /** What a tab adds to its word, and the gap between two. */
    public static final int TAB_PAD = 14;
    public static final int TAB_GAP = 3;

    public static final Rect QUERY = new Rect(6, 34, 330, 13);
    public static final Rect SEARCH = new Rect(342, 34, 84, 13);
    public static final Rect RESULTS = new Rect(6, 52, 420, 102);
    public static final Rect HEADER = new Rect(7, 53, 418, 11);
    public static final int NAME_X = 10;
    public static final int SIZE_X = 222;
    public static final int FROM_X = 266;
    public static final int LINK_X = 340;
    public static final int SIGNAL_X = 396;
    public static final int ROW_TOP = 67;
    public static final int ROW_H = 11;
    public static final int ROWS = 8;
    /** How wide a name may run before the size column. */
    public static final int NAME_ROOM = SIZE_X - NAME_X - 6;

    public static final Rect DOWNLOAD = new Rect(6, 158, 84, 13);
    public static final Rect ADD_WHEN_DONE = new Rect(98, 160, 9, 9);
    public static final int ADD_WHEN_DONE_TEXT_X = 112;

    public static final int DOWNLOADS_LABEL_Y = 178;
    public static final Rect RULE = new Rect(6, 188, 420, 1);
    public static final int DOWNLOAD_TOP = 193;
    public static final int DOWNLOAD_H = 30;
    public static final int DOWNLOADS_SHOWN = 3;
    public static final int BAR_X = 10;
    public static final int BAR_W = 196;
    public static final int BAR_H = 7;
    public static final int AMOUNT_X = 212;
    public static final int RIGHT = 422;

    /** The whole list a tab of its own shows: the downloads, or the songs shared. */
    public static final Rect LIST = new Rect(6, 34, 420, 228);
    /** How many songs shared the list shows at once, a row each. */
    public static final int LIST_ROWS = 20;
    /** How many downloads the list shows at once, each as tall as one under the search. */
    public static final int LIST_DOWNLOADS = 7;
    public static final Rect CANCEL = new Rect(6, 266, 84, 13);
    public static final Rect CLEAR = new Rect(96, 266, 84, 13);
    /** Where the folder the songs shared are kept in is named, under their list. */
    public static final int FOLDER_TEXT_Y = 269;

    public static final Rect STATUS = new Rect(6, 286, 420, 12);
    public static final int STATUS_TEXT_Y = 288;

    private SoundfoundryShareLayout() {
    }

    /** The tabs, left to right, each as wide as its word and the pad. */
    public static List<Rect> tabs(final List<String> words, final ToIntFunction<String> width) {
        final List<Rect> out = new ArrayList<>(words.size());
        int x = TABS_X;
        for (final String word : words) {
            final int w = width.applyAsInt(word) + TAB_PAD;
            out.add(new Rect(x, TABS_Y, w, TAB_H));
            x += w + TAB_GAP;
        }
        return out;
    }

    /**
     * How many of a row's five bars are lit for a way whose slowest cable is {@code link}: as many as its songs come
     * fast, a cable with no speed of its own for songs counting as the Ethernet it goes at; none when it is unknown.
     */
    public static int signalOf(@Nullable final DataLink link) {
        if (link == null) {
            return 0;
        }
        if (link.line() == DataLine.BACKBONE && link.era() == HardwareEra.LEGACY) {
            return 4;
        }
        return link.line() == DataLine.HPC && link.era() == HardwareEra.STANDARD ? 5 : 2;
    }

    /** Which found song a point is on, or -1. */
    public static int resultAt(final double px, final double py) {
        if (px < RESULTS.x() || px >= RESULTS.right() || py < ROW_TOP - 1) {
            return -1;
        }
        final int row = (int) ((py - (ROW_TOP - 1)) / ROW_H);
        return row < ROWS ? row : -1;
    }

    /** Which row of the list of songs shared a point is on, or -1. */
    public static int listRowAt(final double px, final double py) {
        if (!LIST.contains(px, py) || py < LIST.y() + 3) {
            return -1;
        }
        final int row = (int) ((py - LIST.y() - 3) / ROW_H);
        return row < LIST_ROWS ? row : -1;
    }

    /** Where the download a row of the downloads tab shows starts, from the plate's top. */
    public static int listDownloadTop(final int row) {
        return LIST.y() + 4 + row * DOWNLOAD_H;
    }

    /** Which download of the downloads tab a point is on, or -1. */
    public static int listDownloadAt(final double px, final double py) {
        if (!LIST.contains(px, py) || py < LIST.y() + 3) {
            return -1;
        }
        final int row = (int) ((py - LIST.y() - 3) / DOWNLOAD_H);
        return row < LIST_DOWNLOADS ? row : -1;
    }

    /** Every part of the search tab, for a test that nothing overlaps or falls outside. */
    public static GuiLayout layout(final List<String> tabWords, final ToIntFunction<String> width) {
        final GuiLayout l = new GuiLayout(WIDTH, HEIGHT);
        final List<Rect> tabs = tabs(tabWords, width);
        for (int i = 0; i < tabs.size(); i++) {
            final Rect t = tabs.get(i);
            l.box("tab" + i, t.x(), t.y(), t.w(), t.h());
        }
        l.box("query", QUERY.x(), QUERY.y(), QUERY.w(), QUERY.h());
        l.box("search", SEARCH.x(), SEARCH.y(), SEARCH.w(), SEARCH.h());
        l.box("results", RESULTS.x(), RESULTS.y(), RESULTS.w(), RESULTS.h());
        l.box("download", DOWNLOAD.x(), DOWNLOAD.y(), DOWNLOAD.w(), DOWNLOAD.h());
        l.box("addWhenDone", ADD_WHEN_DONE.x(), ADD_WHEN_DONE.y(), ADD_WHEN_DONE.w(), ADD_WHEN_DONE.h());
        l.box("rule", RULE.x(), RULE.y(), RULE.w(), RULE.h());
        for (int i = 0; i < DOWNLOADS_SHOWN; i++) {
            l.box("bar" + i, BAR_X, DOWNLOAD_TOP + i * DOWNLOAD_H + 12, BAR_W, BAR_H);
        }
        l.box("status", STATUS.x(), STATUS.y(), STATUS.w(), STATUS.h());
        barButtons(l);
        return l;
    }

    /** Every part of the downloads tab, which the tab of songs shared lays out the same way, for the same test. */
    public static GuiLayout listLayout(final List<String> tabWords, final ToIntFunction<String> width) {
        final GuiLayout l = new GuiLayout(WIDTH, HEIGHT);
        final List<Rect> tabs = tabs(tabWords, width);
        for (int i = 0; i < tabs.size(); i++) {
            final Rect t = tabs.get(i);
            l.box("tab" + i, t.x(), t.y(), t.w(), t.h());
        }
        for (int i = 0; i < LIST_DOWNLOADS; i++) {
            l.box("bar" + i, BAR_X, listDownloadTop(i) + 12, BAR_W, BAR_H);
        }
        l.box("cancel", CANCEL.x(), CANCEL.y(), CANCEL.w(), CANCEL.h());
        l.box("clear", CLEAR.x(), CLEAR.y(), CLEAR.w(), CLEAR.h());
        l.box("status", STATUS.x(), STATUS.y(), STATUS.w(), STATUS.h());
        barButtons(l);
        return l;
    }

    private static void barButtons(final GuiLayout l) {
        for (final Rect button : new Rect[] {SoundfoundryLayout.minimize(WIDTH), SoundfoundryLayout.shade(WIDTH),
                SoundfoundryLayout.close(WIDTH)}) {
            l.box("bar" + button.x(), button.x(), button.y(), button.w(), button.h());
        }
    }
}
