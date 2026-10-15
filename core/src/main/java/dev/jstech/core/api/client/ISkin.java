/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.api.client;

import dev.jstech.core.client.gui.component.Draw;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.ApiStatus;

/**
 * The look every component draws through: the colours of a surface and the primitives a control is made of.
 * A skin is a design, not a palette: the computing mod's desktops each implement one in their own shape
 * language, and a component never paints a pixel of chrome itself, so the same button, field or list looks
 * like a Frames 95 control on one machine and like a flat one on another.
 *
 * <p>A mod is handed one wherever it draws inside a machine's screen, a desktop program or a component of a
 * program's window among them, and drawing through it is what makes what it draws look like the system the machine
 * runs. Coordinates are the screen's pixels, and colours opaque {@code 0xAARRGGBB}.
 *
 * <pre>{@code
 * public void draw(GuiGraphics g, Font font, ISkin skin, int x, int y, int w, int h, int mouseX, int mouseY) {
 *     skin.panel(g, x + 4, y + 4, w - 8, h - 24);
 *     skin.button(g, font, x + w - 64, y + h - 18, 60, 14, "Scan", false, false, true);
 *     Draw.text(g, font, "Ready", x + 8, y + 8, skin.text());
 * }
 * }</pre>
 */
public interface ISkin {

    /** How big a slot is with its frame, in pixels. */
    @ApiStatus.Experimental
    int SLOT_SIZE = 18;

    /** How wide a slider's handle is, in pixels. */
    @ApiStatus.Experimental
    int HANDLE_WIDTH = 8;

    // colours

    /** Primary text. */
    int text();

    /** Secondary text: captions, hints, placeholders. */
    int dim();

    /** The accent: selections, marks, the primary button, progress. */
    int accent();

    /** What a progress bar fills with: the accent, unless the look gives its bars a colour of their own. */
    default int progressFill() {
        return accent();
    }

    /** A 1px separator or border. */
    int edge();

    /** The background of a program's body. */
    int windowBg();

    /** The fill of a content panel. */
    int panelBg();

    /** The fill of a text field or an empty cell. */
    int fieldBg();

    /** The text typed in a field, over {@link #fieldBg()}: the primary text, unless the look's fields differ. */
    @ApiStatus.Experimental
    default int fieldText() {
        return text();
    }

    /** The background of a hovered row or cell. */
    int listHover();

    /** The text colour of a list row, given its selection state. */
    int listRowText(boolean selected);

    // primitives

    /** A group panel: a sunken well, a soft border or a hairline, per design. */
    void panel(GuiGraphics g, int x, int y, int w, int h);

    /** A push button, with its hovered and pressed states; the primary one is the default action. */
    void button(GuiGraphics g, Font font, int x, int y, int w, int h, String label, boolean hovered,
                boolean pressed, boolean primary);

    /** A text input field, lit when it has the keyboard. */
    void field(GuiGraphics g, int x, int y, int w, int h, boolean focused);

    /** One tab of a tab strip. */
    void tab(GuiGraphics g, Font font, int x, int y, int w, int h, String label, boolean active);

    /** The background of a list or grid row in its hovered and selected states; nothing when it is neither. */
    void listRow(GuiGraphics g, int x, int y, int w, int h, boolean hovered, boolean selected);

    /** A scrollbar thumb. */
    void scrollThumb(GuiGraphics g, int x, int y, int w, int h);

    /** A status or footer bar background. */
    void statusBar(GuiGraphics g, int x, int y, int w, int h);

    /** The body and border of a floating panel such as a popup. */
    void windowFrame(GuiGraphics g, int x, int y, int w, int h);

    /** A push button that may be unable to be pressed, which is drawn so; by default the button, faded over. */
    @ApiStatus.Experimental
    default void button(final GuiGraphics g, final Font font, final int x, final int y, final int w, final int h,
                        final String label, final boolean hovered, final boolean pressed, final boolean primary,
                        final boolean enabled) {
        button(g, font, x, y, w, h, label, hovered && enabled, pressed && enabled, primary);
        if (!enabled) {
            Draw.disabled(g, x, y, w, h);
        }
    }

    /** A whole window, a machine's or a dialog's; by default the frame of a floating panel. */
    @ApiStatus.Experimental
    default void window(final GuiGraphics g, final int x, final int y, final int w, final int h) {
        windowFrame(g, x, y, w, h);
    }

    /** An item slot whose item sits at {@code (x, y)}: the frame is drawn a pixel outside it, 18 pixels square. */
    @ApiStatus.Experimental
    default void slot(final GuiGraphics g, final int x, final int y) {
        field(g, x - 1, y - 1, SLOT_SIZE, SLOT_SIZE, false);
    }

    /** A check box {@code size} pixels square, ticked or not, lit when the pointer is over it. */
    @ApiStatus.Experimental
    default void checkbox(final GuiGraphics g, final Font font, final int x, final int y, final int size,
                          final boolean checked, final boolean hovered) {
        final int box = checked ? accent() : fieldBg();
        g.fill(x, y, x + size, y + size, box);
        Draw.outline(g, x, y, size, size, edge());
        if (checked) {
            Draw.checkMark(g, font, x, y, box);
        }
    }

    /** A progress bar, filled {@code fraction} of the way, 0 to 1. */
    @ApiStatus.Experimental
    default void progress(final GuiGraphics g, final int x, final int y, final int w, final int h,
                          final double fraction) {
        panel(g, x, y, w, h);
        final int filled = (int) Math.round((w - 2) * Math.max(0.0, Math.min(1.0, fraction)));
        if (filled > 0) {
            g.fill(x + 1, y + 1, x + 1 + filled, y + h - 1, progressFill());
        }
    }

    /** A line between two parts, {@code w} pixels long. */
    @ApiStatus.Experimental
    default void separator(final GuiGraphics g, final int x, final int y, final int w) {
        g.fill(x, y, x + w, y + 1, edge());
    }

    /** A tooltip's ground and frame. */
    @ApiStatus.Experimental
    default void tooltip(final GuiGraphics g, final int x, final int y, final int w, final int h) {
        windowFrame(g, x, y, w, h);
    }

    /** Draws {@code text} in this look's font and manner (plain, or with a shadow where the look has one). */
    @ApiStatus.Experimental
    default void drawText(final GuiGraphics g, final Font font, final String text, final int x, final int y,
                          final int colour) {
        Draw.text(g, font, text, x, y, colour);
    }

    /** The same text at {@code scale}, anchored at its top-left corner. */
    @ApiStatus.Experimental
    default void drawText(final GuiGraphics g, final Font font, final String text, final int x, final int y,
                          final int colour, final float scale) {
        Draw.textScaled(g, font, text, x, y, colour, scale);
    }

    /** How wide {@code text} is in this look's font, at the font's own size. */
    @ApiStatus.Experimental
    default int textWidth(final Font font, final String text) {
        return font.width(text);
    }

    /** A slider: its track, its handle {@code fraction} of the way along, and its label over the track. */
    @ApiStatus.Experimental
    default void slider(final GuiGraphics g, final Font font, final int x, final int y, final int w, final int h,
                        final double fraction, final String label, final boolean hovered) {
        field(g, x, y, w, h, hovered);
        final int knob = Math.min(HANDLE_WIDTH, w);
        final int at = x + (int) Math.round((w - knob) * Math.max(0.0, Math.min(1.0, fraction)));
        panel(g, at, y, knob, h);
        final int tw = textWidth(font, label);
        drawText(g, font, label, x + (w - tw) / 2, y + (h - 8) / 2, text());
    }

    /** A switch, on or off: a track, and a knob at the end it is at. */
    @ApiStatus.Experimental
    default void switchControl(final GuiGraphics g, final int x, final int y, final int w, final int h,
                               final boolean on, final boolean hovered) {
        g.fill(x, y, x + w, y + h, on ? accent() : fieldBg());
        Draw.outline(g, x, y, w, h, hovered ? accent() : edge());
        final int knob = Math.max(2, h - 2);
        final int kx = on ? x + w - 1 - knob : x + 1;
        panel(g, kx, y + 1, knob, h - 2);
    }

    /** An option button {@code size} pixels square, chosen or not. */
    @ApiStatus.Experimental
    default void radio(final GuiGraphics g, final int x, final int y, final int size, final boolean chosen,
                       final boolean hovered) {
        g.fill(x, y, x + size, y + size, fieldBg());
        Draw.outline(g, x, y, size, size, hovered ? accent() : edge());
        if (chosen) {
            final int inset = Math.max(2, size / 3);
            g.fill(x + inset, y + inset, x + size - inset, y + size - inset, accent());
        }
    }

    /** A drop-down list's button: what is chosen, and a caret pointing down, or up while the list is open. */
    @ApiStatus.Experimental
    default void dropdown(final GuiGraphics g, final Font font, final int x, final int y, final int w, final int h,
                          final String label, final boolean open, final boolean hovered) {
        button(g, font, x, y, w, h, "", hovered || open, false, false);
        drawText(g, font, label, x + 5, y + (h - 8) / 2, text());
        final int cx = x + w - 10;
        final int cy = y + h / 2 - 1;
        for (int i = 0; i < 3; i++) {
            final int row = open ? cy + 1 - i : cy - 1 + i;
            g.fill(cx + i, row, cx + 6 - i, row + 1, text());
        }
    }

    /** The open list of a drop-down, or of what a field suggests. */
    @ApiStatus.Experimental
    default void listPopup(final GuiGraphics g, final int x, final int y, final int w, final int h) {
        windowFrame(g, x, y, w, h);
    }

    /** The colour of a line of an open list: the chosen one apart from the others. */
    @ApiStatus.Experimental
    default int popupText(final boolean chosen) {
        return chosen ? accent() : text();
    }

    /** The ground a chart is drawn on. */
    @ApiStatus.Experimental
    default void chartGround(final GuiGraphics g, final int x, final int y, final int w, final int h) {
        g.fill(x, y, x + w, y + h, fieldBg());
        Draw.outline(g, x, y, w, h, edge());
    }

    /** The colour of stored energy at its brightest, as a gauge fills. */
    @ApiStatus.Experimental
    default int energyColour() {
        return accent();
    }

    /** The colour of a chart's grid lines. */
    @ApiStatus.Experimental
    default int chartLine() {
        return edge();
    }

    /** The colour of a chart's {@code index}th series, the first ones distinct from each other. */
    @ApiStatus.Experimental
    default int seriesColour(final int index) {
        final int[] colours = {accent(), progressFill(), text(), dim()};
        return colours[Math.floorMod(index, colours.length)];
    }
}
