/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * A window as a monitor's face in the world shows it: where the machine has it, its frame, its title and its
 * system's look, with no program behind it.
 *
 * <p>A real program opened only to be looked at from across the room would ask the machine for its data and, worse,
 * take the replies meant for the same program open on the player's own screen. This one asks nothing and answers
 * nothing: it is the window's place on the desktop, painted in the window colour of its system.
 */
final class MirroredWindowApp implements IDesktopApp {

    private final String title;
    private final int width;
    private final int height;
    private OsSkin skin = OsSkin.fallback();

    MirroredWindowApp(final String title, final int width, final int height) {
        this.title = title;
        this.width = width;
        this.height = height;
    }

    @Override
    public String title() {
        return title;
    }

    @Override
    public int defaultWidth() {
        return width;
    }

    @Override
    public int defaultHeight() {
        return height;
    }

    @Override
    public void applySkin(final OsSkin applied) {
        this.skin = applied;
    }

    @Override
    public void renderContent(final GuiGraphics graphics, final Font font, final int x, final int y, final int w,
                              final int h, final int mouseX, final int mouseY, final float partialTick) {
        graphics.fill(x, y, x + w, y + h, skin.windowBg());
    }
}
