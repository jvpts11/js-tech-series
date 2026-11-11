/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import net.minecraft.client.gui.GuiGraphics;

/**
 * How an installer's page writes a line: in the terminal font on the installers that ran in text, in the game's font on
 * the others, at the row pitch of each. Handed to whatever draws part of a page, so it writes the way the page does.
 */
interface IInstallerPen {

    /** Writes a line with its left edge at {@code x}. */
    void say(GuiGraphics g, String text, int x, int y, int colour);

    /** Writes a line ending at {@code rightEdge}. */
    void sayRight(GuiGraphics g, String text, int rightEdge, int y, int colour);

    /** How wide that line comes out. */
    int width(String text);

    /** A line cut to the room it has. */
    String fit(String text, int room);

    /** How far apart the page's rows sit. */
    int row();
}
