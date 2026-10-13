/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import dev.jstech.core.gui.layout.GuiLayout;

import java.util.List;
import java.util.function.ToIntFunction;

/**
 * Where everything sits on the volume control a system opens from the speaker on its panel, one arrangement per kind
 * of desktop. Every position is relative to the control's own corner, so the drawing and the clicks read the same
 * numbers, and every size follows the words in it, which are longer in some languages than in others.
 */
public final class VolumePopupLayout {

    /** The upright slider's travel, above the Mute box. */
    private static final int UPRIGHT_TOP = 18;
    private static final int ROW = 12;
    /** The Speakers row carries the speakers' names on a second line. */
    private static final int ROW_WITH_NAMES = 21;
    private static final int OUTPUTS_TALL = ROW + 1 + ROW_WITH_NAMES + 1 + ROW + 1;
    /** How far the thumb's centre keeps from each end of its groove, so the thumb never leaves it. */
    private static final int THUMB_MARGIN = 4;

    private VolumePopupLayout() {
    }

    /** The kinds of volume control, one per family of desktop. */
    public enum Look {
        /** A small raised popup with an upright slider and a Mute box: the older Frames. */
        CLASSIC,
        /** Quick settings: a slider with an arrow that opens the choice of output: the newest Frames. */
        QUICK,
        /** An applet with a header, a slider, its percentage, a mute button and the outputs listed: Plasma. */
        PLASMA,
        /** The system menu dropped from the top bar: GNOME. */
        SYSTEM_MENU,
        /** An applet with the volume written out, a mute switch and the outputs listed: Cinnamon. */
        APPLET,
        /** The classic popup with a button that opens the mixer: the period desktops of the Legacy era. */
        PERIOD,
        /**
         * A glass popup: the device button over an upright slider, its figure, a mute button and the Mixer link, the
         * outputs listed beside it when the device button is pressed: Frames 7.
         */
        GLASS,
        /** A dark flyout: the device row that opens the outputs under it, and a slider lying across: Frames 10. */
        FLYOUT
    }

    /** A rectangle relative to the control's corner; one of no size stands for something the look does not have. */
    public record Rect(int x, int y, int w, int h) {

        public static final Rect NONE = new Rect(0, 0, 0, 0);

        public boolean present() {
            return w > 0 && h > 0;
        }

        public boolean contains(final double px, final double py) {
            return present() && px >= x && px < x + w && py >= y && py < y + h;
        }

        public int right() {
            return x + w;
        }

        public int bottom() {
            return y + h;
        }
    }

    /**
     * The words the control shows, in the player's language.
     *
     * @param title    the line across its top, empty for a look without one
     * @param mute     the Mute box's or switch's words
     * @param heading  the words over the outputs
     * @param outputs  the three outputs, monitor, speakers and both
     * @param speakers the speakers' names, the line under the speakers' output
     * @param footer   the link or button at its foot that opens the sound settings
     */
    public record Labels(String title, String mute, String heading, List<String> outputs, String speakers,
                         String footer) {
    }

    /**
     * Where each part of one control sits.
     *
     * @param track   the slider's groove; {@code upright} says which way it runs
     * @param mute    what a click mutes on: the box and its words, a button, a switch's row or the speaker's picture
     * @param panel   a darker well the outputs sit in, where the look has one
     * @param rules   the heights of the lines drawn across it
     */
    public record Geometry(Look look, int width, int height, Rect title, Rect icon, Rect track, boolean upright,
                           Rect percent, Rect mute, Rect chevron, Rect heading, Rect panel, List<Rect> outputs,
                           Rect footer, List<Integer> rules) {

        /** The same places as a layout the audit reads: every part a click lands on is a solid box. */
        public GuiLayout toGuiLayout() {
            final GuiLayout l = new GuiLayout(width, height);
            l.box("track", track.x(), track.y(), track.w(), track.h());
            if (mute.present()) {
                l.box("mute", mute.x(), mute.y(), mute.w(), mute.h());
            }
            if (chevron.present()) {
                l.box("chevron", chevron.x(), chevron.y(), chevron.w(), chevron.h());
            }
            for (int i = 0; i < outputs.size(); i++) {
                final Rect row = outputs.get(i);
                l.box("output" + i, row.x(), row.y(), row.w(), row.h());
            }
            if (footer.present()) {
                l.box("footer", footer.x(), footer.y(), footer.w(), footer.h());
            }
            return l;
        }
    }

    /**
     * One control, laid out for its words.
     *
     * @param expanded whether the outputs are open, on a look that folds them behind an arrow
     * @param width    how wide a string is drawn, in pixels
     */
    public static Geometry of(final Look look, final boolean expanded, final Labels labels,
                              final ToIntFunction<String> width) {
        return switch (look) {
            case CLASSIC -> upright(look, labels, width, false);
            case PERIOD -> upright(look, labels, width, true);
            case QUICK -> quick(labels, width, expanded);
            case PLASMA -> plasma(labels, width);
            case SYSTEM_MENU -> systemMenu(labels, width, expanded);
            case APPLET -> applet(labels, width);
            case GLASS -> glass(labels, width, expanded);
            case FLYOUT -> flyout(labels, width, expanded);
        };
    }

    /** The volume, 0 to 100, a click or a drag at a point sets on the control's slider. */
    public static int volumeAt(final Geometry g, final double px, final double py) {
        final Rect t = g.track();
        final double at = g.upright()
                ? (t.bottom() - THUMB_MARGIN - py) / (t.h() - 2.0 * THUMB_MARGIN)
                : (px - t.x() - THUMB_MARGIN) / (t.w() - 2.0 * THUMB_MARGIN);
        return (int) Math.max(0, Math.min(100, Math.round(at * 100.0)));
    }

    /** Where the slider's thumb is centred for a volume: a height on an upright slider, else a distance across. */
    public static int thumbAt(final Geometry g, final int volume) {
        final Rect t = g.track();
        final int v = Math.max(0, Math.min(100, volume));
        return g.upright()
                ? t.bottom() - THUMB_MARGIN - Math.round(v * (t.h() - 2 * THUMB_MARGIN) / 100.0F)
                : t.x() + THUMB_MARGIN + Math.round(v * (t.w() - 2 * THUMB_MARGIN) / 100.0F);
    }

    /** The widest of the outputs and the speakers' names under them. */
    private static int widestOutput(final Labels labels, final ToIntFunction<String> width) {
        int widest = width.applyAsInt(labels.speakers());
        for (final String output : labels.outputs()) {
            widest = Math.max(widest, width.applyAsInt(output));
        }
        return widest;
    }

    /** The widest of the three outputs alone, for a look that lists them on one line each. */
    private static int widestName(final Labels labels, final ToIntFunction<String> width) {
        int widest = 0;
        for (final String output : labels.outputs()) {
            widest = Math.max(widest, width.applyAsInt(output));
        }
        return widest;
    }

    /* The three output rows from {@code top}, {@code x} in and {@code w} wide, one line each. */
    private static List<Rect> singleRows(final int x, final int top, final int w) {
        return List.of(new Rect(x, top, w, ROW), new Rect(x, top + ROW + 1, w, ROW),
                new Rect(x, top + 2 * (ROW + 1), w, ROW));
    }

    /* The three output rows from {@code top}, {@code x} in and {@code w} wide, the speakers' row two lines tall. */
    private static List<Rect> outputRows(final int x, final int top, final int w) {
        return List.of(new Rect(x, top, w, ROW), new Rect(x, top + ROW + 1, w, ROW_WITH_NAMES),
                new Rect(x, top + ROW + 1 + ROW_WITH_NAMES + 1, w, ROW));
    }

    private static Geometry upright(final Look look, final Labels labels, final ToIntFunction<String> width,
                                    final boolean withButton) {
        int w = Math.max(52, Math.max(width.applyAsInt(labels.title()) + 12, 19 + width.applyAsInt(labels.mute()) + 6));
        if (withButton) {
            w = Math.max(w, width.applyAsInt(labels.footer()) + 16);
        }
        final int h = withButton ? 120 : 102;
        final int bottom = h - (withButton ? 42 : 24);
        final int cx = w / 2;
        final Rect footer = withButton ? new Rect(5, bottom + 21, w - 10, 13) : Rect.NONE;
        return new Geometry(look, w, h, new Rect(0, 5, w, 8), Rect.NONE,
                new Rect(cx - 2, UPRIGHT_TOP, 5, bottom - UPRIGHT_TOP), true, Rect.NONE,
                new Rect(5, bottom + 4, w - 10, 14), Rect.NONE, Rect.NONE, Rect.NONE, List.of(), footer, List.of());
    }

    private static Geometry quick(final Labels labels, final ToIntFunction<String> width, final boolean expanded) {
        final int w = Math.max(Math.max(150, 4 + 6 + widestOutput(labels, width) + 10),
                8 + width.applyAsInt(labels.footer()) + 8);
        final Rect icon = new Rect(8, 9, 9, 9);
        final Rect track = new Rect(22, 9, w - 60, 8);
        final Rect chevron = new Rect(w - 26, 6, 18, 14);
        if (!expanded) {
            return new Geometry(Look.QUICK, w, 43, Rect.NONE, icon, track, false, Rect.NONE, icon, chevron, Rect.NONE,
                    Rect.NONE, List.of(), new Rect(8, 29, width.applyAsInt(labels.footer()), 10), List.of(25));
        }
        final int rowsTop = 42;
        final int rule = rowsTop + OUTPUTS_TALL + 1;
        return new Geometry(Look.QUICK, w, rule + 18, Rect.NONE, icon, track, false, Rect.NONE, icon, chevron,
                new Rect(8, 30, w - 16, 8), Rect.NONE, outputRows(4, rowsTop, w - 8),
                new Rect(8, rule + 4, width.applyAsInt(labels.footer()), 10), List.of(25, rule));
    }

    private static Geometry plasma(final Labels labels, final ToIntFunction<String> width) {
        final int buttonW = width.applyAsInt(labels.footer()) + 10;
        final int w = Math.max(160, Math.max(Math.max(width.applyAsInt(labels.title()) + 14,
                3 + 6 + widestOutput(labels, width) + 9), buttonW + 12));
        final int rowsTop = 47;
        final int buttonY = rowsTop + OUTPUTS_TALL + 3;
        return new Geometry(Look.PLASMA, w, buttonY + 13 + 5, new Rect(7, 5, w - 14, 8), new Rect(7, 20, 9, 9),
                new Rect(20, 21, w - 66, 8), false, new Rect(w - 44, 21, 22, 8), new Rect(w - 18, 18, 13, 13),
                Rect.NONE, new Rect(7, 36, w - 14, 8), Rect.NONE, outputRows(3, rowsTop, w - 6),
                new Rect(w - 6 - buttonW, buttonY, buttonW, 13), List.of(15));
    }

    private static Geometry systemMenu(final Labels labels, final ToIntFunction<String> width, final boolean expanded) {
        final int w = Math.max(Math.max(150, 4 + 15 + widestOutput(labels, width) + 9),
                9 + width.applyAsInt(labels.footer()) + 9);
        final Rect icon = new Rect(8, 9, 9, 9);
        final Rect track = new Rect(22, 9, w - 60, 8);
        final Rect chevron = new Rect(w - 26, 6, 18, 14);
        if (!expanded) {
            return new Geometry(Look.SYSTEM_MENU, w, 45, Rect.NONE, icon, track, false, Rect.NONE, icon, chevron,
                    Rect.NONE, Rect.NONE, List.of(), new Rect(9, 31, width.applyAsInt(labels.footer()), 9),
                    List.of(26));
        }
        final int wellTop = 26;
        final int rowsTop = wellTop + 14;
        final int rule = rowsTop + OUTPUTS_TALL + 7;
        return new Geometry(Look.SYSTEM_MENU, w, rule + 19, Rect.NONE, icon, track, false, Rect.NONE, icon, chevron,
                new Rect(9, wellTop + 3, w - 18, 8), new Rect(4, wellTop, w - 8, 14 + OUTPUTS_TALL + 1),
                outputRows(4, rowsTop, w - 8), new Rect(9, rule + 5, width.applyAsInt(labels.footer()), 9),
                List.of(rule));
    }

    /*
     * Frames 7's: a column with the device button at its head, the slider standing under it, its figure, the mute
     * button and the Mixer link; pressed, the device button lists the outputs to the column's left, so the column
     * keeps its place against the edge.
     */
    private static Geometry glass(final Labels labels, final ToIntFunction<String> width, final boolean expanded) {
        final int names = widestName(labels, width);
        final int footerW = width.applyAsInt(labels.footer());
        final int colW = Math.max(Math.max(64, names + 26), footerW + 10);
        final int listW = expanded ? names + 22 : 0;
        final int x0 = listW;
        final Rect device = new Rect(x0 + 5, 5, colW - 10, 18);
        final Rect track = new Rect(x0 + colW / 2 - 2, 30, 5, 64);
        final Rect percent = new Rect(x0 + 5, 99, colW - 10, 8);
        final Rect mute = new Rect(x0 + colW / 2 - 8, 110, 16, 13);
        final Rect footer = new Rect(x0 + (colW - footerW) / 2, 128, footerW, 9);
        final List<Rect> rows = expanded ? singleRows(2, 5, listW - 4) : List.of();
        return new Geometry(Look.GLASS, listW + colW, 142, Rect.NONE, Rect.NONE, track, true, percent, mute, device,
                Rect.NONE, new Rect(x0, 0, colW, 142), rows, footer, List.of());
    }

    /*
     * Frames 10's: the device row across the top, the outputs opening under it, and the slider row with the speaker
     * (which mutes) at its left and the figure at its right.
     */
    private static Geometry flyout(final Labels labels, final ToIntFunction<String> width, final boolean expanded) {
        final int w = Math.max(170, Math.max(widestName(labels, width) + 30, widestOutput(labels, width) + 18));
        final Rect device = new Rect(6, 6, w - 12, 14);
        final List<Rect> rows = expanded ? singleRows(6, 22, w - 12) : List.of();
        final int sliderY = expanded ? 22 + 3 * (ROW + 1) + 6 : 28;
        final Rect icon = new Rect(9, sliderY, 9, 9);
        return new Geometry(Look.FLYOUT, w, sliderY + 19, Rect.NONE, icon, new Rect(24, sliderY, w - 60, 8), false,
                new Rect(w - 30, sliderY, 22, 8), icon, device, Rect.NONE, Rect.NONE, rows, Rect.NONE, List.of());
    }

    private static Geometry applet(final Labels labels, final ToIntFunction<String> width) {
        final int w = Math.max(Math.max(150, 8 + width.applyAsInt(labels.footer()) + 8),
                Math.max(Math.max(width.applyAsInt(labels.title()) + 16, 3 + 9 + widestOutput(labels, width) + 8),
                        8 + width.applyAsInt(labels.mute()) + 8 + 16 + 8));
        final int rowsTop = 61;
        final int rule = rowsTop + OUTPUTS_TALL + 1;
        return new Geometry(Look.APPLET, w, rule + 17, new Rect(8, 5, w - 16, 8), new Rect(8, 18, 9, 9),
                new Rect(22, 19, w - 30, 8), false, Rect.NONE, new Rect(3, 31, w - 6, 12), Rect.NONE,
                new Rect(8, 50, w - 16, 8), Rect.NONE, outputRows(3, rowsTop, w - 6),
                new Rect(8, rule + 4, width.applyAsInt(labels.footer()), 9), List.of(46, rule));
    }
}
