/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.api.client;

import dev.jstech.core.api.client.ISkin;
import dev.jstech.core.api.client.SurfaceRenderer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * A program written in Java that opens a window on a computer's desktop, as the computers' own programs do.
 *
 * <p>It runs on the client of the player who opened it: the desktop draws its window's frame in the machine's
 * system and hands the program the room inside it, the system's skin and everything the player does there. It draws
 * either by itself, in {@link #draw}, or through a {@link SurfaceRenderer} it hands back from {@link #renderer()},
 * whose surface the desktop puts in the window at the frame rate the renderer allows. A window with a surface holds
 * video memory on the machine, as a paint program's or a game's does, and does not open on a machine whose video
 * memory is full.
 *
 * <p>Anyone else looking at the same desktop, another player at the machine or the monitor's face in the world, sees
 * a placeholder in the window, since what the program shows lives on its opener's client alone; unless
 * {@link #worldSafe()} says otherwise.
 */
@ApiStatus.Experimental
public abstract class DesktopProgram {

    /** What its window's title bar says. */
    public abstract String title();

    /** How wide its window's room is when it opens. */
    public int width() {
        return 320;
    }

    /** How tall its window's room is when it opens. */
    public int height() {
        return 200;
    }

    /** What draws its window, when it draws through a surface; null when it draws by itself. */
    @Nullable
    public SurfaceRenderer renderer() {
        return null;
    }

    /** Whether what it draws by itself may be shown to every viewer of the desktop, the world's monitor among them. */
    public boolean worldSafe() {
        return false;
    }

    /** Draws its window's room, when it draws by itself; a program with a renderer leaves this alone. */
    public void draw(final GuiGraphics graphics, final Font font, final ISkin skin, final int x, final int y,
                     final int width, final int height, final int mouseX, final int mouseY) {
    }

    /** A click in its window's room, in the desktop's pixels. */
    public void mouseClicked(final double mouseX, final double mouseY, final int button) {
    }

    /** A button let go over its window. */
    public void mouseReleased(final double mouseX, final double mouseY, final int button) {
    }

    /** The wheel turned over its window; above zero is up. */
    public boolean mouseScrolled(final double delta) {
        return false;
    }

    /** A key pressed while its window has the keyboard. */
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        return false;
    }

    /** A key let go while its window has the keyboard. */
    public boolean keyReleased(final int key, final int scanCode, final int modifiers) {
        return false;
    }

    /** A character typed while its window has the keyboard. */
    public boolean charTyped(final char typed) {
        return false;
    }

    /** Its window has closed. */
    public void closed() {
    }
}
