/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.studio;

import dev.jstech.core.client.gui.component.Panel;
import dev.jstech.core.client.gui.component.UiComponent;
import dev.jstech.core.gui.layout.StudioLayout;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * A page of the studio, reached by its tab: the things of the game, or one of the tools. It lays out its panels in the
 * body the studio gives it, draws them, takes the pointer over them, and holds its own buttons and fields, which the
 * studio draws and hands the keyboard to while the page is shown.
 */
abstract class StudioPage {

    protected final StudioScreen studio;
    protected final Panel panel = new Panel();
    protected StudioLayout.Regions regions = StudioLayout.of(StudioLayout.MIN_WIDTH, StudioLayout.MIN_HEIGHT,
            List.of());

    /** The room a button leaves round its label. */
    private static final int LABEL_ROOM = 8;

    protected StudioPage(final StudioScreen studio) {
        this.studio = studio;
    }

    /** What its tab says. */
    abstract TextKey tab();

    /** Draws it; what the pointer is on is added to {@code tip}. */
    abstract void render(GuiGraphics g, int mouseX, int mouseY, List<Component> tip);

    /** The way to what it shows, after the studio's name in the bar. */
    List<String> crumbs() {
        return List.of(GameText.resolve(this.tab()));
    }

    /** What it lists now, a line a row, as a test reads it. */
    List<String> lines() {
        return List.of();
    }

    /** Lays it out in {@code laid}, the studio's parts on this screen. */
    void layout(final StudioLayout.Regions laid) {
        this.regions = laid;
    }

    /** Its tab was opened. */
    void shown() {
    }

    /** A tick passed while it is shown. */
    void tick() {
    }

    /** The studio closes. */
    void closed() {
    }

    boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        return false;
    }

    boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollY) {
        return false;
    }

    boolean mouseDragged(final double mouseX, final double mouseY, final double dragX, final double dragY) {
        return false;
    }

    void mouseReleased() {
    }

    /** Its buttons and fields. */
    Panel panel() {
        return this.panel;
    }

    /** The font the studio writes in. */
    protected Font font() {
        return this.studio.studioFont();
    }

    /** Whether every one of {@code labels} fits a button {@code width} wide, so buttons stack when they do not. */
    protected boolean fits(final int width, final TextKey... labels) {
        for (final TextKey label : labels) {
            if (font().width(words(label)) + LABEL_ROOM > width) {
                return false;
            }
        }
        return true;
    }

    /** Has {@code component} stand at {@code rect}. */
    protected static void place(final UiComponent component, final StudioLayout.Rect rect) {
        component.setBounds(rect.x(), rect.y(), rect.width(), rect.height());
    }

    /** {@code key}'s words in the player's language. */
    protected static String words(final TextKey key) {
        return GameText.resolve(key);
    }
}
