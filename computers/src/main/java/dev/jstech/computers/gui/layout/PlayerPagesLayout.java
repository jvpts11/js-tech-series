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
 * Where the pages of an operating system a player wrote sit on the monitor's glass
 * (computers/docs/design/player-systems.md): its coming up, with its logo in the middle, the bar of its progress under
 * it, its last line under the bar and its name in a corner; and its panic, with its logo and message at the top, the
 * error under them, the system and its facts, and the reboot at the foot.
 *
 * <p>Positions are measured from the glass's corner. This class is pure and carries no Minecraft dependency.
 */
public final class PlayerPagesLayout {

    /* The coming up: the logo's box, the bar under it, the line under the bar, the name in the bottom left. */
    public static final int LOGO = 40;
    public static final int LOGO_Y = 80;
    public static final int BAR_W = 100;
    public static final int BAR_H = 3;
    public static final int BAR_Y = LOGO_Y + LOGO + 12;
    public static final int LINE_Y = BAR_Y + 9;
    public static final int NAME_X = 6;
    public static final int NAME_Y = FirmwareLayout.HEIGHT - 12;

    /* The panic: the logo beside the message, the message, the line under it, the error, the facts, the foot. */
    public static final int MARGIN = 12;
    public static final int PANIC_LOGO = 16;
    public static final int PANIC_LOGO_Y = 22;
    public static final int MESSAGE_X = MARGIN + PANIC_LOGO + 8;
    public static final int MESSAGE_Y = 24;
    public static final int STOPPED_Y = MESSAGE_Y + 12;
    public static final int TRACE_Y = 70;
    public static final int ROW = 8;
    public static final int TRACE_INDENT = 8;
    public static final int MOST_TRACE_ROWS = 12;
    public static final int FOOT_Y = FirmwareLayout.HEIGHT - 26;
    public static final float SMALL = 0.75f;

    private PlayerPagesLayout() {
    }

    /** The coming-up page at its fullest. */
    public static GuiLayout boot() {
        final GuiLayout l = new GuiLayout(FirmwareLayout.WIDTH, FirmwareLayout.HEIGHT);
        l.box("logo", (FirmwareLayout.WIDTH - LOGO) / 2, LOGO_Y, LOGO, LOGO);
        l.box("bar", (FirmwareLayout.WIDTH - BAR_W) / 2, BAR_Y, BAR_W, BAR_H);
        l.text("line", (FirmwareLayout.WIDTH - 200) / 2, LINE_Y, 40, SMALL);
        l.text("name", NAME_X, NAME_Y, 30, SMALL);
        return l;
    }

    /** The panic page at its fullest: the longest message, every row of the error, the facts and the foot. */
    public static GuiLayout panic() {
        final GuiLayout l = new GuiLayout(FirmwareLayout.WIDTH, FirmwareLayout.HEIGHT);
        l.box("logo", MARGIN, PANIC_LOGO_Y, PANIC_LOGO, PANIC_LOGO);
        l.text("message", MESSAGE_X, MESSAGE_Y, 54, 1.0f);
        l.text("stopped", MESSAGE_X, STOPPED_Y, 60, SMALL);
        for (int i = 0; i < MOST_TRACE_ROWS; i++) {
            l.text("trace" + i, MARGIN + (i == 0 ? 0 : TRACE_INDENT), TRACE_Y + i * ROW, 74, SMALL);
        }
        l.text("facts", MARGIN, TRACE_Y + (MOST_TRACE_ROWS + 1) * ROW, 74, SMALL);
        l.text("foot", MARGIN, FOOT_Y, 40, SMALL);
        return l;
    }
}
