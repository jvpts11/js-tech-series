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
 * Where everything of Soundfoundry's Legacy form sits: the player, with the playlist docked under it.
 *
 * <p>Both are plates of the same width with a bar of their own along the top, the way the players of the time were
 * built. Either can be folded to its bar alone, and the playlist can be put away. Every position here is measured from
 * the top-left corner of the plate it belongs to, so the same numbers serve the window wherever it stands, and the
 * screen and its test both read them from here.
 *
 * <p>This class is pure and carries no Minecraft dependency.
 */
public final class SoundfoundryLayout {

    /** How wide both plates are. */
    public static final int WIDTH = 276;
    /** How tall the player is, open. */
    public static final int PLAYER_H = 100;
    /** How tall the playlist is, open. */
    public static final int PLAYLIST_H = 150;
    /** How tall a plate folded to its bar is. */
    public static final int SHADE_H = 14;

    /* The bar along the top of a plate and its three buttons. */
    public static final Rect BAR = new Rect(1, 1, WIDTH - 2, 11);
    public static final Rect MINIMIZE = new Rect(WIDTH - 30, 3, 8, 7);
    public static final Rect SHADE = new Rect(WIDTH - 21, 3, 8, 7);
    public static final Rect CLOSE = new Rect(WIDTH - 12, 3, 8, 7);

    /* The player. */
    public static final Rect DISPLAY = new Rect(6, 16, 94, 42);
    public static final int STATUS_X = 11;
    public static final int STATUS_Y = 21;
    public static final int CLOCK_X = 26;
    public static final int CLOCK_Y = 20;
    public static final int BARS_X = 13;
    public static final int BARS_BOTTOM = 55;
    public static final int BARS = 19;
    public static final Rect SONG = new Rect(106, 16, 164, 13);
    public static final Rect KBPS = new Rect(106, 32, 22, 11);
    public static final int KBPS_LABEL_X = 131;
    public static final Rect KHZ = new Rect(160, 32, 16, 11);
    public static final int KHZ_LABEL_X = 179;
    public static final int MONO_X = 204;
    public static final int STEREO_X = 234;
    public static final int FORMAT_Y = 34;
    public static final Rect VOLUME = new Rect(106, 47, 72, 9);
    public static final Rect BALANCE = new Rect(184, 47, 36, 9);
    public static final Rect PL = new Rect(226, 46, 20, 11);
    public static final Rect NET = new Rect(248, 46, 22, 11);
    public static final Rect POSITION = new Rect(6, 61, 264, 9);
    public static final Rect PREVIOUS = new Rect(6, 74, 22, 18);
    public static final Rect PLAY = new Rect(30, 74, 22, 18);
    public static final Rect PAUSE = new Rect(54, 74, 22, 18);
    public static final Rect STOP = new Rect(78, 74, 22, 18);
    public static final Rect NEXT = new Rect(102, 74, 22, 18);
    public static final Rect EJECT = new Rect(128, 74, 20, 18);
    public static final Rect SHUFFLE = new Rect(154, 74, 44, 11);
    public static final Rect REPEAT = new Rect(200, 74, 40, 11);
    public static final int OUT_X = 155;
    public static final int OUT_Y = 88;
    public static final Rect MARK = new Rect(247, 78, 16, 10);
    /** How wide a slider's thumb is. */
    public static final int THUMB = 6;
    /** How wide the position bar's thumb is. */
    public static final int POSITION_THUMB = 12;

    /* The playlist. */
    public static final Rect LIST = new Rect(6, 16, 264, 110);
    public static final int ROW_X = 9;
    public static final int ROW_TOP = 18;
    public static final int ROW_H = 10;
    public static final int ROWS = 10;
    public static final int LENGTH_RIGHT = 254;
    public static final Rect SCROLLBAR = new Rect(262, 18, 6, 106);
    public static final Rect ADD = new Rect(6, 131, 30, 12);
    public static final Rect REMOVE = new Rect(40, 131, 30, 12);
    public static final Rect SELECT = new Rect(74, 131, 30, 12);
    public static final Rect MISC = new Rect(108, 131, 32, 12);
    public static final Rect LISTS = new Rect(144, 131, 32, 12);
    public static final Rect TOTAL = new Rect(182, 131, 88, 12);

    private SoundfoundryLayout() {
    }

    /** A rectangle within a plate. */
    public record Rect(int x, int y, int w, int h) {

        /** Whether a point measured from the plate's corner falls inside it. */
        public boolean contains(final double px, final double py) {
            return px >= x && px < x + w && py >= y && py < y + h;
        }

        public int right() {
            return x + w;
        }

        public int bottom() {
            return y + h;
        }
    }

    /** How tall the player stands, open or folded to its bar. */
    public static int playerHeight(final boolean shaded) {
        return shaded ? SHADE_H : PLAYER_H;
    }

    /** How tall the playlist stands, put away, open or folded to its bar. */
    public static int playlistHeight(final boolean shown, final boolean shaded) {
        return !shown ? 0 : shaded ? SHADE_H : PLAYLIST_H;
    }

    /** Which visible row of the list a point of the playlist is on, or -1 for none. */
    public static int rowAt(final double px, final double py) {
        if (px < LIST.x() || px >= SCROLLBAR.x() || py < ROW_TOP) {
            return -1;
        }
        final int row = (int) ((py - ROW_TOP) / ROW_H);
        return row < ROWS ? row : -1;
    }

    /** Where a slider's thumb stands for a value from 0 to 1, measured from the slider's left end. */
    public static int thumbAt(final Rect slider, final int thumb, final double value) {
        return (int) Math.round((slider.w() - thumb) * Math.clamp(value, 0.0, 1.0));
    }

    /** The value from 0 to 1 a point along a slider stands for, the thumb's middle being where it was taken. */
    public static double valueAt(final Rect slider, final int thumb, final double px) {
        return Math.clamp((px - slider.x() - thumb / 2.0) / (slider.w() - thumb), 0.0, 1.0);
    }

    /** Every part of both plates, open, for a test that nothing overlaps or falls outside. */
    public static GuiLayout layout() {
        final GuiLayout l = new GuiLayout(WIDTH, PLAYER_H + PLAYLIST_H);
        l.box("minimize", MINIMIZE.x(), MINIMIZE.y(), MINIMIZE.w(), MINIMIZE.h());
        l.box("shade", SHADE.x(), SHADE.y(), SHADE.w(), SHADE.h());
        l.box("close", CLOSE.x(), CLOSE.y(), CLOSE.w(), CLOSE.h());
        for (final Named part : new Named[] {
                new Named("display", DISPLAY), new Named("song", SONG), new Named("kbps", KBPS),
                new Named("khz", KHZ), new Named("volume", VOLUME), new Named("balance", BALANCE),
                new Named("pl", PL), new Named("net", NET), new Named("position", POSITION),
                new Named("previous", PREVIOUS), new Named("play", PLAY), new Named("pause", PAUSE),
                new Named("stop", STOP), new Named("next", NEXT), new Named("eject", EJECT),
                new Named("shuffle", SHUFFLE), new Named("repeat", REPEAT), new Named("mark", MARK)}) {
            l.box(part.name(), part.rect().x(), part.rect().y(), part.rect().w(), part.rect().h());
        }
        l.text("mono", MONO_X, FORMAT_Y, 4, 1.0F);
        l.text("stereo", STEREO_X, FORMAT_Y, 6, 1.0F);
        final int top = PLAYER_H;
        l.box("listMinimize", MINIMIZE.x(), top + MINIMIZE.y(), MINIMIZE.w(), MINIMIZE.h());
        l.box("listShade", SHADE.x(), top + SHADE.y(), SHADE.w(), SHADE.h());
        l.box("listClose", CLOSE.x(), top + CLOSE.y(), CLOSE.w(), CLOSE.h());
        for (final Named part : new Named[] {
                new Named("list", LIST), new Named("add", ADD), new Named("remove", REMOVE),
                new Named("select", SELECT), new Named("misc", MISC), new Named("lists", LISTS),
                new Named("total", TOTAL)}) {
            l.box(part.name(), part.rect().x(), top + part.rect().y(), part.rect().w(), part.rect().h());
        }
        return l;
    }

    /** A part and what it is called, for the audit. */
    private record Named(String name, Rect rect) {
    }
}
