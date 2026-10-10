/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli.msd;

import java.util.Locale;

/**
 * Where the diagnostics screen stands: whether the ports dialog is up, which button or port is picked, what the
 * player has just asked it to do, and how big the glass asking for it is.
 *
 * <p>All of it in one small piece of text, because that is what travels: the terminal asks the machine for a screen by
 * naming the state it wants, and the machine draws that state and hands the rows back, so the terminal keeps nothing of
 * the machine's hardware between one key and the next.
 *
 * <p>Pure, so what a key does to the state is held to account without a world.
 *
 * @param ports   whether the LPT and COM ports dialog is up over the main screen
 * @param picked  on the main screen the button picked, from nought, left to right and down; in the dialog the port
 * @param action  what was asked of the picked port: {@code enable}, {@code disable} or empty
 * @param columns how wide the glass asking for it is, since the machine draws the screen and not the glass
 * @param rows    how many rows that glass holds, for the same reason
 */
public record MsdState(boolean ports, int picked, String action, int columns, int rows) {

    /** What a path of this kind begins with, so the machine knows this screen is being asked for. */
    public static final String SCHEME = "msd:";

    /** How wide a glass is taken to be when the one asking has not said, which is what a terminal held. */
    public static final int DEFAULT_COLUMNS = 80;

    /** How many rows it is taken to hold for the same reason. */
    public static final int DEFAULT_ROWS = 24;

    /*
     * The largest glass a screen is drawn for. The size comes from the path the player's screen asks for, and the
     * machine draws every row of it, so a path naming two billion rows would have the server build them all.
     */
    static final int MOST_COLUMNS = 256;
    static final int MOST_ROWS = 128;

    /** The screen as it opens: the main buttons, the first one picked. */
    public static final MsdState OPENING = new MsdState(false, 0, "", DEFAULT_COLUMNS, DEFAULT_ROWS);

    public MsdState {
        picked = Math.max(0, picked);
        action = action == null ? "" : action.toLowerCase(Locale.ROOT);
        columns = columns <= 0 ? DEFAULT_COLUMNS : Math.min(MOST_COLUMNS, columns);
        rows = rows <= 0 ? DEFAULT_ROWS : Math.min(MOST_ROWS, rows);
    }

    /** The same screen with another button or port picked. */
    public MsdState picking(final int which) {
        return new MsdState(this.ports, which, "", this.columns, this.rows);
    }

    /** The ports dialog up over the main screen, with that port picked. */
    public MsdState openingPorts(final int port) {
        return new MsdState(true, port, "", this.columns, this.rows);
    }

    /** The dialog put away, the main screen back with the button that opened it picked. */
    public MsdState closingPorts(final int button) {
        return new MsdState(false, button, "", this.columns, this.rows);
    }

    /** The same screen with something asked of the picked port. */
    public MsdState asking(final String what) {
        return new MsdState(this.ports, this.picked, what, this.columns, this.rows);
    }

    /** The same screen with nothing asked, which is what it becomes once the machine has done it. */
    public MsdState done() {
        return new MsdState(this.ports, this.picked, "", this.columns, this.rows);
    }

    /** The same screen asked for on a glass of that size. */
    public MsdState on(final int howWide, final int howTall) {
        return new MsdState(this.ports, this.picked, this.action, howWide, howTall);
    }

    /** The state as a name a terminal can ask a machine for; it holds no slash, which would read as a folder. */
    public String path() {
        return SCHEME + (this.ports ? "ports" : "main") + ":" + this.picked + ":" + this.action + ":" + this.columns
                + ":" + this.rows;
    }

    /** The state that name stands for; anything it cannot read opens the screen where it opens. */
    public static MsdState of(final String path) {
        if (!names(path)) {
            return OPENING;
        }
        final String[] parts = path.substring(SCHEME.length()).split(":", -1);
        if (parts.length < 5) {
            return OPENING;
        }
        return new MsdState("ports".equals(parts[0]), whole(parts[1]), parts[2], whole(parts[3]), whole(parts[4]));
    }

    /** Whether a name asks for this screen at all. */
    public static boolean names(final String path) {
        return path != null && path.startsWith(SCHEME);
    }

    private static int whole(final String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (final NumberFormatException notANumber) {
            return 0;
        }
    }
}
