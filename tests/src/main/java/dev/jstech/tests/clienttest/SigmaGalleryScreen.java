/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.computers.client.os.DesktopWindow;
import dev.jstech.computers.client.os.OsSkin;
import dev.jstech.computers.client.os.SigmaWindowApp;
import dev.jstech.computers.operation.payload.UiWindowPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * A program's window drawn alone on the screen in one system's look after another, so a screenshot shows how each
 * system draws the same widgets: the window's frame and title bar in that skin, and what the program put in it.
 */
public final class SigmaGalleryScreen extends Screen {

    private final SigmaWindowApp app;
    private OsSkin skin = OsSkin.fallback();

    /** The grey the screen is behind the window, which no system draws. */
    private static final int BACKDROP = 0xFF << 24 | 0x303840;

    public SigmaGalleryScreen(final UiWindowPayload window) {
        super(Component.literal(window.title()));
        this.app = new SigmaWindowApp(window.hostPos(), window);
    }

    /** Draws the window in that skin from now on. */
    public void show(final OsSkin shown) {
        this.skin = shown;
        this.app.applySkin(shown);
    }

    /** The window as drawn, for its widgets to be found and measured. */
    public SigmaWindowApp app() {
        return this.app;
    }

    /** The room the window's widgets are drawn in, as x, y, width and height. */
    public int[] room() {
        final int w = Math.min(this.width - 8, this.app.defaultWidth());
        final int h = Math.min(this.height - 8, this.app.defaultHeight());
        final int x = (this.width - w) / 2;
        final int y = (this.height - h) / 2;
        return new int[] {x + 4, y + DesktopWindow.TITLE_H + 4, w - 8, h - DesktopWindow.TITLE_H - 8};
    }

    @Override
    public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        g.fill(0, 0, this.width, this.height, BACKDROP);
        final int[] room = this.room();
        final int x = room[0] - 4;
        final int y = room[1] - DesktopWindow.TITLE_H - 4;
        final int w = room[2] + 8;
        final int h = room[3] + DesktopWindow.TITLE_H + 8;
        this.skin.windowFrame(g, x, y, w, h);
        this.skin.titleBar(g, x + 2, y + 2, w - 4, DesktopWindow.TITLE_H);
        g.drawString(this.font, this.app.title(), x + 6, y + 5, this.skin.titleText(), false);
        this.app.renderContent(g, this.font, room[0], room[1], room[2], room[3], mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
