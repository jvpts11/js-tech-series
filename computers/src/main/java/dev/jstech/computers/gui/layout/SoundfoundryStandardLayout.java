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

/**
 * Where everything of the Standard Soundfoundry sits: the bar along its top, the sidebar with its pages, its playlists
 * and the Soundfoundry Server it streams from, the page itself, and the bar along its foot with the song playing, the
 * transport, the point in the song, where the sound comes out and how loud.
 *
 * <p>The window is drawn at whatever size it is handed, from the smallest it allows to the whole desktop: the sidebar
 * keeps its width, the bars keep their height and stretch, and the page fits as many albums and songs as it has room
 * for. Positions are measured from the window's top-left corner; those of a page from the page's own corner, just
 * past the sidebar under the top bar.
 *
 * <p>This class is pure and carries no Minecraft dependency.
 */
public final class SoundfoundryStandardLayout {

    public static final int DEFAULT_W = 630;
    public static final int DEFAULT_H = 410;
    public static final int MIN_W = 520;
    public static final int MIN_H = 320;

    public static final int TITLE_H = 18;
    public static final int SIDE_W = 150;
    public static final int FOOT_H = 56;
    /** The page's margin on either side. */
    public static final int PAD = 14;

    /* The sidebar, measured from the window's corner like the bars. */
    public static final int NAV_Y = 28;
    public static final int NAV_STEP = 16;
    public static final int NAV_GLYPH_X = 12;
    public static final int NAV_TEXT_X = 26;
    public static final Rect SIDE_RULE = new Rect(10, 80, 130, 1);
    public static final int PLAYLISTS_Y = 88;
    public static final Rect NEW_PLAYLIST = new Rect(130, 86, 12, 11);
    public static final int LIST_Y = 104;
    public static final int LIST_STEP = 14;
    public static final int SWATCH_X = 12;
    public static final int LIST_TEXT_X = 28;
    public static final int SERVER_BOX_W = 134;
    public static final int SERVER_BOX_H = 34;

    /* The home page. */
    public static final int GREETING_Y = 8;
    public static final int SECTION_Y = 32;
    public static final int CARD_Y = 46;
    public static final int CARD = 80;
    public static final int CARD_STEP = 114;
    public static final int CARD_TITLE_Y = 130;
    public static final int CARD_ARTIST_Y = 140;
    public static final int LISTS_Y = 160;
    public static final int LIST_ROW_Y = 174;
    public static final int LIST_ROW_STEP = 28;
    public static final int ROW_COVER = 22;

    /* An album's page and a playlist's. */
    public static final int HEAD_COVER = 96;
    public static final Rect ALBUM_COVER = new Rect(PAD, 10, HEAD_COVER, HEAD_COVER);
    public static final int HEAD_TEXT_X = 122;
    public static final int KIND_Y = 34;
    public static final int NAME_Y = 46;
    public static final int META_Y = 76;
    public static final int SOURCE_Y = 89;
    public static final Rect PLAY = new Rect(PAD, 116, 24, 24);
    public static final Rect SHUFFLE_ALBUM = new Rect(48, 122, 12, 12);
    public static final Rect DOWNLOAD_ALBUM = new Rect(70, 119, 100, 18);
    public static final int DOWNLOADED_X = 180;
    public static final int TRACKS_HEAD_Y = 152;
    public static final int TRACKS_Y = 168;
    public static final int TRACK_H = 13;

    /* The local files and the other lists. */
    public static final int BANNER_Y = 32;
    public static final int BANNER_H = 44;
    public static final int BUTTONS_Y = 84;
    public static final int IMPORT_W = 150;
    public static final int UPLOAD_W = 104;
    public static final int BUTTON_H = 18;
    public static final int LIST_HEAD_Y = 114;
    public static final int LIST_H = 14;
    /** How much higher everything under the banner sits when there is no banner. */
    public static final int NO_BANNER = BANNER_H + 8;
    public static final int ARTIST_X = 200;
    public static final int NUMBER_X = 24;
    public static final int TITLE_X = 36;
    /** Where the time column ends and the download arrows sit, from the page's right edge. */
    public static final int TIME_RIGHT = 40;
    public static final int ARROW_RIGHT = 26;

    /* The search. */
    public static final Rect SEARCH_BOX = new Rect(PAD, 36, 260, 16);
    public static final int SEARCH_ROWS_Y = 68;

    /* The foot. */
    public static final Rect FOOT_COVER = new Rect(8, 8, 40, 40);
    public static final int FOOT_TEXT_X = 56;
    public static final int FOOT_TITLE_Y = 14;
    public static final int FOOT_ARTIST_Y = 28;
    public static final int GLYPH_Y = 12;
    public static final int POSITION_Y = 39;
    public static final int TIME_Y = 36;

    private SoundfoundryStandardLayout() {
    }

    /** The window's top bar. */
    public static Rect titleBar(final int w) {
        return new Rect(0, 0, w, TITLE_H);
    }

    public static Rect minimize(final int w) {
        return new Rect(w - 54, 3, 14, 12);
    }

    public static Rect maximize(final int w) {
        return new Rect(w - 36, 3, 14, 12);
    }

    public static Rect close(final int w) {
        return new Rect(w - 18, 3, 14, 12);
    }

    /** The sidebar, between the two bars. */
    public static Rect sidebar(final int h) {
        return new Rect(0, TITLE_H, SIDE_W, h - TITLE_H - FOOT_H);
    }

    /** One of the three pages the sidebar opens, by its place. */
    public static Rect nav(final int index) {
        return new Rect(0, NAV_Y - 3 + index * NAV_STEP, SIDE_W, 14);
    }

    /** A playlist of the sidebar, by its place among those shown. */
    public static Rect playlist(final int index) {
        return new Rect(8, LIST_Y - 2 + index * LIST_STEP, SIDE_W - 16, LIST_STEP - 1);
    }

    /** How many playlists the sidebar shows above the server's box. */
    public static int playlistsShown(final int h) {
        final int room = serverBox(h).y() - 4 - (LIST_Y - 2);
        return Math.max(0, room / LIST_STEP);
    }

    /** The box naming the Soundfoundry Server streamed from, at the sidebar's foot. */
    public static Rect serverBox(final int h) {
        return new Rect(8, h - FOOT_H - SERVER_BOX_H - 6, SERVER_BOX_W, SERVER_BOX_H);
    }

    /** The page, between the sidebar and the window's right edge and between the two bars. */
    public static Rect page(final int w, final int h) {
        return new Rect(SIDE_W, TITLE_H, w - SIDE_W, h - TITLE_H - FOOT_H);
    }

    /** How many albums fit across a page of that width. */
    public static int cardsAcross(final int pageW) {
        return Math.max(1, (pageW - 2 * PAD + CARD_STEP - CARD) / CARD_STEP);
    }

    /** Where the second list of the home page starts, from the page's left edge. */
    public static int secondColumn(final int pageW) {
        return PAD + (pageW - 2 * PAD) * 236 / 452;
    }

    /** How many songs a list of the home page shows in that page height. */
    public static int homeRows(final int pageH) {
        return Math.max(0, (pageH - LIST_ROW_Y + LIST_ROW_STEP - ROW_COVER) / LIST_ROW_STEP);
    }

    /** How many rows of a table fit from {@code top} to the page's foot. */
    public static int rowsFrom(final int pageH, final int top, final int rowH) {
        return Math.max(0, (pageH - top - 2) / rowH);
    }

    /** The row of a table starting at {@code top} under a point of the page, or -1. */
    public static int rowAt(final double py, final int top, final int rowH, final int shown) {
        if (py < top - 2) {
            return -1;
        }
        final int row = (int) ((py - (top - 2)) / rowH);
        return row < shown ? row : -1;
    }

    /** The foot's bar. */
    public static Rect foot(final int w, final int h) {
        return new Rect(0, h - FOOT_H, w, FOOT_H);
    }

    /** The middle of the transport, from the window's left edge. */
    public static int transportCentre(final int w) {
        return w / 2;
    }

    /** The button that plays or pauses, measured from the foot's corner. */
    public static Rect playButton(final int w) {
        final int cx = transportCentre(w);
        return new Rect(cx - 10, 7, 20, 18);
    }

    public static Rect shuffleButton(final int w) {
        return new Rect(transportCentre(w) - 60, 10, 12, 11);
    }

    public static Rect previousButton(final int w) {
        return new Rect(transportCentre(w) - 34, 10, 10, 11);
    }

    public static Rect nextButton(final int w) {
        return new Rect(transportCentre(w) + 20, 10, 10, 11);
    }

    public static Rect repeatButton(final int w) {
        return new Rect(transportCentre(w) + 44, 10, 12, 11);
    }

    /** How wide the point in the song runs: the whole of it on a wide window, less on a narrow one. */
    public static int positionWidth(final int w) {
        return Math.clamp(w - 370, 120, 260);
    }

    /** The point in the song, which a click jumps to, measured from the foot's corner. */
    public static Rect position(final int w) {
        final int bw = positionWidth(w);
        return new Rect(transportCentre(w) - bw / 2, POSITION_Y - 2, bw, 7);
    }

    /** Where the sound comes out, measured from the foot's corner. */
    public static Rect output(final int w) {
        return new Rect(w - 128, 20, 44, 11);
    }

    /** How loud, measured from the foot's corner. */
    public static Rect volume(final int w) {
        return new Rect(w - 68, 22, 56, 9);
    }

    /** How wide the song's title may run in the foot before the time. */
    public static int footTitleRoom(final int w) {
        return position(w).x() - 36 - FOOT_TEXT_X;
    }

    /** Every part of the window at that size, for the test that nothing overlaps. */
    public static GuiLayout layout(final int w, final int h) {
        final GuiLayout l = new GuiLayout(w, h);
        l.box("minimize", minimize(w).x(), minimize(w).y(), minimize(w).w(), minimize(w).h());
        l.box("maximize", maximize(w).x(), maximize(w).y(), maximize(w).w(), maximize(w).h());
        l.box("close", close(w).x(), close(w).y(), close(w).w(), close(w).h());
        for (int i = 0; i < 3; i++) {
            final Rect nav = nav(i);
            l.box("nav" + i, nav.x(), nav.y(), nav.w(), nav.h());
        }
        l.box("newPlaylist", NEW_PLAYLIST.x(), NEW_PLAYLIST.y(), NEW_PLAYLIST.w(), NEW_PLAYLIST.h());
        for (int i = 0; i < playlistsShown(h); i++) {
            final Rect list = playlist(i);
            l.box("playlist" + i, list.x(), list.y(), list.w(), list.h());
        }
        final Rect server = serverBox(h);
        l.box("server", server.x(), server.y(), server.w(), server.h());
        final Rect page = page(w, h);
        final int across = cardsAcross(page.w());
        for (int i = 0; i < across; i++) {
            l.box("card" + i, page.x() + PAD + i * CARD_STEP, page.y() + CARD_Y, CARD, CARD);
        }
        final int second = secondColumn(page.w());
        for (int i = 0; i < homeRows(page.h()); i++) {
            final int ry = page.y() + LIST_ROW_Y + i * LIST_ROW_STEP;
            l.box("network" + i, page.x() + PAD, ry, ROW_COVER, ROW_COVER);
            l.box("downloaded" + i, page.x() + second, ry, ROW_COVER, ROW_COVER);
        }
        final int fy = h - FOOT_H;
        l.box("footCover", FOOT_COVER.x(), fy + FOOT_COVER.y(), FOOT_COVER.w(), FOOT_COVER.h());
        for (final Rect button : new Rect[] {shuffleButton(w), previousButton(w), playButton(w), nextButton(w),
                repeatButton(w), position(w), output(w), volume(w)}) {
            l.box("foot" + button.x(), button.x(), fy + button.y(), button.w(), button.h());
        }
        return l;
    }

    /** Every part of an album's page at that size, for the same test. */
    public static GuiLayout albumLayout(final int w, final int h) {
        final Rect page = page(w, h);
        final GuiLayout l = new GuiLayout(w, h);
        l.box("cover", page.x() + ALBUM_COVER.x(), page.y() + ALBUM_COVER.y(), ALBUM_COVER.w(), ALBUM_COVER.h());
        l.box("play", page.x() + PLAY.x(), page.y() + PLAY.y(), PLAY.w(), PLAY.h());
        l.box("shuffle", page.x() + SHUFFLE_ALBUM.x(), page.y() + SHUFFLE_ALBUM.y(), SHUFFLE_ALBUM.w(),
                SHUFFLE_ALBUM.h());
        l.box("download", page.x() + DOWNLOAD_ALBUM.x(), page.y() + DOWNLOAD_ALBUM.y(), DOWNLOAD_ALBUM.w(),
                DOWNLOAD_ALBUM.h());
        final int shown = rowsFrom(page.h(), TRACKS_Y, TRACK_H);
        for (int i = 0; i < shown; i++) {
            l.box("arrow" + i, page.right() - ARROW_RIGHT, page.y() + TRACKS_Y + i * TRACK_H, 8, 8);
        }
        return l;
    }

    /** Every part of the page of local files at that size, with the banner saying there is no server or not. */
    public static GuiLayout localLayout(final int w, final int h, final boolean banner) {
        final Rect page = page(w, h);
        final GuiLayout l = new GuiLayout(w, h);
        final int lift = banner ? 0 : NO_BANNER;
        if (banner) {
            l.box("banner", page.x() + PAD, page.y() + BANNER_Y, page.w() - 2 * PAD, BANNER_H);
        }
        l.box("import", page.x() + PAD, page.y() + BUTTONS_Y - lift, IMPORT_W, BUTTON_H);
        l.box("upload", page.x() + PAD + IMPORT_W + 6, page.y() + BUTTONS_Y - lift, UPLOAD_W, BUTTON_H);
        l.box("rule", page.x() + PAD, page.y() + LIST_HEAD_Y + 10 - lift, page.w() - 2 * PAD, 1);
        return l;
    }
}
