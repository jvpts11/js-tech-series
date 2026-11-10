/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.os.install.InstallerFlow;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * An installer that draws its whole page and takes its own keys, rather than being a frame with pages in it: OpenBSD's
 * questions at the terminal and Linux Mint's window on its live desktop. The installer screen hands it everything.
 */
interface IOwnInstallerPage {

    /**
     * Takes the installer as the machine now has it.
     *
     * @param samePage whether it is still on the page it was, which keeps what was typed and chosen on it
     */
    void accept(InstallerFlow flow, boolean samePage);

    /** Draws the page over the glass, its top left corner at {@code (x, y)}. */
    void paint(GuiGraphics g, Font font, int x, int y, int ticksDone, int blink, int mouseX, int mouseY);

    /** A key pressed, other than Escape; answers whether it was taken. */
    boolean key(int key);

    /** A character typed; answers whether it was taken. */
    boolean typed(char letter);

    /** A click at that point of the screen; answers whether it landed on something. */
    boolean click(double mouseX, double mouseY);
}
