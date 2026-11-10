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
 * Where the network boot's menu sits (computers/docs/design/operating-systems.md): the Legacy card's PXE agent, which
 * takes the whole screen with its addresses at the top and the Mirror's menu in the BIOS's blue under them, and the
 * Standard's HTTP Boot, which lists the Mirror's systems in the UEFI's own Boot Manager panel.
 *
 * <p>Positions are measured from the monitor's glass for the agent, and from the panel's corner for the HTTP Boot
 * list. This class is pure and carries no Minecraft dependency.
 */
public final class NetBootLayout {

    /* The agent's lines along the top, in the small type. */
    public static final int AGENT_X = 4;
    public static final int AGENT_Y = 5;
    public static final int AGENT_STEP = 8;
    public static final int AGENT_LINES = 4;
    public static final float SMALL = 0.75f;

    /* The Mirror's menu: a box in the BIOS's blue with its title across the top border. */
    public static final int BOX_X = 53;
    public static final int BOX_Y = 43;
    public static final int BOX_W = 279;
    public static final int BOX_H = 144;
    public static final int TITLE_RISE = 4;
    public static final int ROW_X = BOX_X + 6;
    public static final int ROW_W = BOX_W - 12;
    public static final int TEXT_INSET = 4;
    public static final int ROWS_TOP = BOX_Y + 12;
    public static final int ROW_PITCH = 11;
    public static final int ROWS = 8;
    public static final int RULE_Y = ROWS_TOP + ROWS * ROW_PITCH + 2;
    public static final int LOCAL_Y = RULE_Y + 3;
    public static final int NOTE_Y = LOCAL_Y + ROW_PITCH + 5;
    public static final int KEYS_Y = NOTE_Y + ROW_PITCH;

    /* The HTTP Boot list inside the UEFI's panel: its rows, the line under them, and INSTALL and BACK. */
    public static final int HTTP_ROWS_TOP = 20;
    public static final int HTTP_ROW_PITCH = 16;
    public static final int HTTP_ROWS = 8;
    public static final int HTTP_NOTE_ABOVE = 12;
    public static final int HTTP_BACK_X = 80;
    public static final int HTTP_BACK_W = 64;

    private NetBootLayout() {
    }

    /** The agent's screen at its fullest: four lines of addresses, the box, every row, the keys. */
    public static GuiLayout agent() {
        final GuiLayout l = new GuiLayout(FirmwareLayout.WIDTH, FirmwareLayout.HEIGHT);
        for (int i = 0; i < AGENT_LINES; i++) {
            l.text("agent" + i, AGENT_X, AGENT_Y + i * AGENT_STEP, 66, SMALL);
        }
        l.box("menu", BOX_X, BOX_Y, BOX_W, BOX_H);
        l.text("title", BOX_X + 40, BOX_Y - TITLE_RISE, 34, 1.0f);
        for (int i = 0; i < ROWS; i++) {
            l.text("row" + i, ROW_X + TEXT_INSET, ROWS_TOP + i * ROW_PITCH, 60, SMALL);
        }
        l.text("local", ROW_X + TEXT_INSET, LOCAL_Y, 30, SMALL);
        l.text("note", ROW_X + TEXT_INSET, NOTE_Y, 45, SMALL);
        l.text("keys", ROW_X + TEXT_INSET, KEYS_Y, 57, SMALL);
        return l;
    }
}
