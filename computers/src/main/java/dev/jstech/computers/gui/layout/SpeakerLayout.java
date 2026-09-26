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
 * Where everything sits on a speaker's screen: its header, the name field and the line under it, the computer and the
 * side it plays side by side, and how it plays across the foot.
 */
public final class SpeakerLayout {

    public static final int WIDTH = 190;
    public static final int HEIGHT = 124;

    public static final int NAME_LABEL_Y = 28;
    public static final int NAME_X = 8;
    public static final int NAME_Y = 36;
    public static final int NAME_H = 14;
    public static final int NOTE_Y = 54;

    public static final int TILE_Y = 66;
    public static final int TILE_W = 84;
    public static final int TILE_H = 22;
    public static final int COMPUTER_X = 8;
    public static final int CHANNEL_X = 98;

    public static final int PLAYS_Y = 94;
    public static final int PLAYS_W = WIDTH - 16;

    private SpeakerLayout() {
    }

    /** The screen, with its longest words in every place. */
    public static GuiLayout layout() {
        final GuiLayout l = new GuiLayout(WIDTH, HEIGHT)
                .box("nameField", NAME_X, NAME_Y, WIDTH - 16, NAME_H)
                .box("computerTile", COMPUTER_X, TILE_Y, TILE_W, TILE_H)
                .box("channelTile", CHANNEL_X, TILE_Y, TILE_W, TILE_H)
                .box("playsTile", NAME_X, PLAYS_Y, PLAYS_W, TILE_H);
        l.text("title", 12, 10, 7, 1.0f);                       // "SPEAKER"
        l.text("model", WIDTH - 12 - 9 * 6, 10, 9, 1.0f);      // "TONEWORKS", right-aligned
        l.text("nameLabel", 10, NAME_LABEL_Y, 4, 0.75f);        // "NAME"
        l.text("note", 10, NOTE_Y, 38, 0.75f);                  // "Programs find this speaker by its name"
        l.text("channel", CHANNEL_X + 3, TILE_Y + 11, 17, 0.75f);  // "Right, by position"
        l.text("plays", NAME_X + 3, PLAYS_Y + 11, 27, 0.75f);   // "22 kHz, bass and treble cut"
        return l;
    }
}
