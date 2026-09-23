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
 * How one {@link OsSkin.Form} draws a window and the controls in it: its shapes, and the colours of its own palette
 * for the parts every skin of that form shares. What differs from one skin of the form to the next (the window's
 * ground, its text, its accent) the chrome asks of the skin it is drawing for.
 *
 * <p>Each form is a design of its own, not a recolour of another, so each is its own class; the defaults here are
 * what most of them do.
 */
sealed interface IFormChrome permits BevelChrome, LunaChrome, FlatChrome, Kde2Chrome, Gnome1Chrome, MotifFormChrome {

    /** The form this chrome draws. */
    OsSkin.Form form();

    /** A 1px border or separator for panels and bands. */
    int edge(OsSkin skin);

    /** The fill of a content panel. */
    int panelFill(OsSkin skin);

    /** The title bar, lit when the window has focus; {@code left} and {@code right} are what its buttons take. */
    void titleBar(GuiGraphics g, OsSkin skin, int x, int y, int w, int h, boolean active, int left, int right);

    /** One button of a title bar, with its mark, and a pressed state so a click reads. */
    void control(GuiGraphics g, Font font, OsSkin skin, int x, int y, int bw, int bh, OsSkin.Control control,
                 boolean hovered, boolean pressed);

    /** A group panel. */
    void panel(GuiGraphics g, OsSkin skin, int x, int y, int w, int h);

    /** A push button's face; its label is written over it in {@link #buttonText}. */
    void button(GuiGraphics g, OsSkin skin, int x, int y, int w, int h, boolean hovered, boolean pressed,
                boolean primary);

    /** The border of a text field, over the field's ground. */
    void field(GuiGraphics g, OsSkin skin, int x, int y, int w, int h, boolean focused);

    /** A tab's face; its label is written over it in {@link #tabText}. */
    void tab(GuiGraphics g, OsSkin skin, int x, int y, int w, int h, boolean active);

    /** A scrollbar thumb. */
    void scrollThumb(GuiGraphics g, OsSkin skin, int x, int y, int w, int h);

    /** A status or footer bar. */
    void statusBar(GuiGraphics g, OsSkin skin, int x, int y, int w, int h);

    /** A window's body and its border, with the skin's rounding. Rounded corner pixels are left unpainted. */
    default void windowFrame(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w,
                             final int h) {
        final int t = frameThickness();
        ChromeShapes.roundedRect(g, x - t, y - t, w + t * 2, h + t * 2, skin.windowBorder(), skin.topRadius(),
                skin.bottomRadius());
        ChromeShapes.roundedRect(g, x, y, w, h, skin.windowBg(), skin.topRadius(), skin.bottomRadius());
    }

    /** Whether this is a dark variant of its form; only the flat form has one. */
    default boolean dark() {
        return false;
    }

    /** What a button's label is written in. */
    default int buttonText(final OsSkin skin, final boolean primary) {
        return skin.text();
    }

    /** What a tab's label is written in. */
    default int tabText(final OsSkin skin, final boolean active) {
        return skin.text();
    }

    /** Whether a selected list row carries a bar of the accent down its left side. */
    default boolean selectionBar() {
        return false;
    }

    /** The thickness of the window frame in pixels. */
    default int frameThickness() {
        return 1;
    }

    /** Whether a window title is centred on its bar. */
    default boolean titleCentered() {
        return false;
    }

    /** Whether a window's one way out sits at the left end of its bar, with minimise and maximise at the right. */
    default boolean menuAtLeft() {
        return false;
    }

    /** How many pixels a window's shadow reaches past it. */
    default int shadowSpread() {
        return 5;
    }

    /** How dark each layer of a window's shadow is, as an alpha. */
    default int shadowStrength() {
        return 0x12;
    }
}
