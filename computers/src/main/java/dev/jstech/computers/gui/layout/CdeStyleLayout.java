/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import dev.jstech.computers.gui.layout.CdeFrontPanelLayout.Rect;
import dev.jstech.core.gui.layout.GuiLayout;

/**
 * Where CDE's Style Manager puts things inside its windows: the strip of pages, the Color page with its list of
 * palettes and the colours of the one picked, the Backdrop page with its list of patterns and a preview of the one
 * picked, and the Audio page with its volume scale, its Mute toggle and the outputs to play through.
 *
 * <p>Everything is measured from the top left of a window's content. The drawing and the click handling both ask
 * here, so what a player sees and what they hit are the same rectangle.
 */
public final class CdeStyleLayout {

    /**
     * The content of each window, which the window adds its frame and title bar around. The strip holds three pages,
     * each a page wide with the gap before it, and the gap after the last.
     */
    public static final int STRIP_W = 196;
    public static final int STRIP_H = 52;
    public static final int COLOR_W = 230;
    public static final int COLOR_H = 124;
    public static final int BACKDROP_W = 238;
    public static final int BACKDROP_H = 138;
    public static final int AUDIO_W = 196;
    public static final int AUDIO_H = 112;
    /** The side of a toggle's square, and how far its words stand from it. */
    public static final int TOGGLE = 7;
    public static final int TOGGLE_GAP = 4;

    /** How much a window's frame and title bar add to its content, across and down. */
    public static final int FRAME_W = 8;
    public static final int FRAME_H = 22;

    public static final int ROW_H = 11;
    public static final int PAGE_ICON = 24;
    public static final int BUTTON_W = 48;
    public static final int BUTTON_H = 14;

    /** How many colours a palette shows, four across and two down. */
    public static final int SWATCHES = 8;

    private static final int PAD = 4;
    private static final int PAGE_W = 60;
    private static final int PAGE_H = 44;
    private static final int COLOR_LIST_W = 84;
    private static final int BACKDROP_LIST_W = 86;
    private static final int SWATCH_W = 30;
    private static final int SWATCH_H = 20;
    private static final int SWATCH_GAP = 4;
    private static final int LABEL_H = 9;
    private static final int BUTTON_GAP = 8;
    /** How far the Audio page's content keeps from its sides. */
    private static final int AUDIO_PAD = 10;

    private CdeStyleLayout() {
    }

    /** One of the pages on the strip, counted from the left. */
    public static Rect page(final int index) {
        return new Rect(PAD + index * (PAGE_W + PAD), PAD, PAGE_W, PAGE_H);
    }

    /** The page under that point out of {@code count}, or -1 when the point is on none of them. */
    public static int pageAt(final double px, final double py, final int count) {
        for (int i = 0; i < count; i++) {
            if (page(i).holds(px, py)) {
                return i;
            }
        }
        return -1;
    }

    /** The well a page's list stands in, on the Color page or on the Backdrop page. */
    public static Rect list(final boolean colors, final int rows) {
        return new Rect(PAD, PAD, colors ? COLOR_LIST_W : BACKDROP_LIST_W, rows * ROW_H + PAD);
    }

    /** One line of that list. */
    public static Rect row(final boolean colors, final int index) {
        final Rect list = list(colors, index + 1);
        return new Rect(list.x() + 2, list.y() + 2 + index * ROW_H, list.w() - 4, ROW_H);
    }

    /** The line of that list under that point out of {@code count}, or -1 when the point is on none. */
    public static int rowAt(final boolean colors, final double px, final double py, final int count) {
        for (int i = 0; i < count; i++) {
            if (row(colors, i).holds(px, py)) {
                return i;
            }
        }
        return -1;
    }

    /** Where the name of the palette picked is written, above its colours. */
    public static Rect colorName() {
        return new Rect(rightOf(true), PAD, COLOR_W - rightOf(true) - PAD, LABEL_H);
    }

    /** One of the palette's colours, four across and two down. */
    public static Rect swatch(final int index) {
        final int top = PAD + LABEL_H + 3;
        return new Rect(rightOf(true) + (index % 4) * (SWATCH_W + SWATCH_GAP),
                top + (index / 4) * (SWATCH_H + SWATCH_GAP), SWATCH_W, SWATCH_H);
    }

    /** The preview of the pattern picked, drawn as the workspace will wear it. */
    public static Rect preview() {
        return new Rect(rightOf(false), PAD, BACKDROP_W - rightOf(false) - PAD, 84);
    }

    /** Where the Backdrop page says which workspace it is choosing for. */
    public static Rect forWorkspace() {
        final Rect preview = preview();
        return new Rect(preview.x(), preview.y() + preview.h() + 6, preview.w(), LABEL_H);
    }

    /**
     * One of the two buttons at the foot of a page: OK and Cancel on Color, Apply and Close on Backdrop. The first
     * is the default one, which Enter takes.
     */
    public static Rect button(final boolean colors, final int index) {
        final int width = colors ? COLOR_W : BACKDROP_W;
        final int height = colors ? COLOR_H : BACKDROP_H;
        final int left = rightOf(colors);
        final int centre = left + (width - left - PAD) / 2;
        final int x = index == 0 ? centre - BUTTON_W - BUTTON_GAP / 2 : centre + BUTTON_GAP / 2;
        return new Rect(x, height - PAD - BUTTON_H, BUTTON_W, BUTTON_H);
    }

    /** The button under that point, or -1 when the point is on neither. */
    public static int buttonAt(final boolean colors, final double px, final double py) {
        for (int i = 0; i < 2; i++) {
            if (button(colors, i).holds(px, py)) {
                return i;
            }
        }
        return -1;
    }

    /** Where the Audio page writes Volume, over its scale. */
    public static Rect audioVolumeLabel() {
        return new Rect(AUDIO_PAD, AUDIO_PAD, AUDIO_W - 2 * AUDIO_PAD, LABEL_H);
    }

    /** The scale's trough; its value is written over the slider, in the band above the trough. */
    public static Rect audioScale() {
        return new Rect(AUDIO_PAD, AUDIO_PAD + LABEL_H + 12, AUDIO_W - 2 * AUDIO_PAD, 10);
    }

    /** The Mute toggle, its square and its words. */
    public static Rect audioMute() {
        final Rect scale = audioScale();
        return new Rect(AUDIO_PAD, scale.y() + scale.h() + 7, AUDIO_W / 2 - AUDIO_PAD, LABEL_H + 2);
    }

    /** Where the Audio page writes Output, over the two outputs. */
    public static Rect audioOutputLabel() {
        final Rect mute = audioMute();
        return new Rect(AUDIO_PAD, mute.y() + mute.h() + 5, AUDIO_W - 2 * AUDIO_PAD, LABEL_H);
    }

    /** One of the two output toggles, the monitor's (0) or the speakers' (1), side by side under Output. */
    public static Rect audioOutput(final int index) {
        final Rect label = audioOutputLabel();
        final int half = (AUDIO_W - 2 * AUDIO_PAD - 6) / 2;
        return new Rect(AUDIO_PAD + 6 + index * half, label.y() + label.h() + 3, half - 4, LABEL_H + 2);
    }

    /** The line across the page above its buttons. */
    public static int audioRule() {
        return AUDIO_H - PAD - BUTTON_H - 6;
    }

    /** OK (0) or Cancel (1) along the Audio page's foot, OK being the default one. */
    public static Rect audioButton(final int index) {
        final int centre = AUDIO_W / 2;
        final int x = index == 0 ? centre - BUTTON_W - BUTTON_GAP / 2 : centre + BUTTON_GAP / 2;
        return new Rect(x, AUDIO_H - PAD - BUTTON_H, BUTTON_W, BUTTON_H);
    }

    /** The Audio page's button under that point, or -1 when the point is on neither. */
    public static int audioButtonAt(final double px, final double py) {
        for (int i = 0; i < 2; i++) {
            if (audioButton(i).holds(px, py)) {
                return i;
            }
        }
        return -1;
    }

    /** The Audio page, as solids that may not overlap. */
    public static GuiLayout audioLayout() {
        final GuiLayout l = new GuiLayout(AUDIO_W, AUDIO_H);
        box(l, "volume", audioVolumeLabel());
        final Rect scale = audioScale();
        // The value is written in the band over the trough, so the scale takes that band as well.
        l.box("scale", scale.x(), scale.y() - 11, scale.w(), scale.h() + 11);
        box(l, "mute", audioMute());
        box(l, "output", audioOutputLabel());
        box(l, "monitor", audioOutput(0));
        box(l, "speakers", audioOutput(1));
        for (int i = 0; i < 2; i++) {
            final Rect r = audioButton(i);
            l.box("button_" + i, r.x() - 2, r.y() - 2, r.w() + 4, r.h() + 4);
        }
        return l;
    }

    /** The strip with that many pages, as solids that may not overlap. */
    public static GuiLayout stripLayout(final int pages) {
        final GuiLayout l = new GuiLayout(STRIP_W, STRIP_H);
        for (int i = 0; i < pages; i++) {
            final Rect r = page(i);
            l.box("page_" + i, r.x(), r.y(), r.w(), r.h());
        }
        return l;
    }

    /** The Color page with that many palettes. */
    public static GuiLayout colorLayout(final int palettes) {
        final GuiLayout l = new GuiLayout(COLOR_W, COLOR_H);
        box(l, "list", list(true, palettes));
        box(l, "name", colorName());
        for (int i = 0; i < SWATCHES; i++) {
            box(l, "swatch_" + i, swatch(i));
        }
        buttons(l, true);
        return l;
    }

    /** The Backdrop page with that many patterns. */
    public static GuiLayout backdropLayout(final int patterns) {
        final GuiLayout l = new GuiLayout(BACKDROP_W, BACKDROP_H);
        box(l, "list", list(false, patterns));
        box(l, "preview", preview());
        box(l, "for", forWorkspace());
        buttons(l, false);
        return l;
    }

    /** Where the right-hand side of a page begins, past its list. */
    private static int rightOf(final boolean colors) {
        return PAD + (colors ? COLOR_LIST_W : BACKDROP_LIST_W) + PAD + 2;
    }

    private static void buttons(final GuiLayout l, final boolean colors) {
        for (int i = 0; i < 2; i++) {
            // The default button wears a ring two pixels out, which must clear its neighbour as well.
            final Rect r = button(colors, i);
            l.box("button_" + i, r.x() - 2, r.y() - 2, r.w() + 4, r.h() + 4);
        }
    }

    private static void box(final GuiLayout l, final String name, final Rect r) {
        l.box(name, r.x(), r.y(), r.w(), r.h());
    }
}
