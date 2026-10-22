/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.component;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FastColor;

/**
 * A scale: a value along a bar coloured in bands, with its marks and their words under it, as a body's dose or its
 * core's warmth is read off a scale painted in its bands. The bar is the style's track; what it shows is read every
 * frame from its reading. The value fills the bands up to it, or stands as a pointer over the whole bar.
 */
public final class ScaleView extends UiComponent {

    private final Supplier<Reading> reading;
    private final boolean pointer;
    private final float textScale;

    /** How tall the bar is, its edges included. */
    public static final int BAR = 7;
    /** How far a mark's line reaches past the bar, above it and under it. */
    public static final int TICK = 2;
    /** How tall the pointer stands over the bar, its point touching it. */
    public static final int POINTER = 5;
    /** The room between a mark's line and its words. */
    public static final int WORDS_GAP = 1;
    /* The pointer's head is this many rows, a pixel wider each row down. */
    private static final int HEAD = 4;

    /**
     * @param reading   what it shows, read each frame
     * @param pointer   whether the value stands as a pointer over the whole bar rather than filling it
     * @param textScale the size the marks' words are drawn at
     */
    public ScaleView(final Supplier<Reading> reading, final boolean pointer, final float textScale) {
        this.reading = Objects.requireNonNull(reading, "reading");
        this.pointer = pointer;
        this.textScale = textScale;
    }

    /** The room a scale keeps over its bar: its pointer's, or its marks' lines'. */
    public static int above(final boolean pointer) {
        return pointer ? POINTER : TICK;
    }

    @Override
    public void render(final GuiGraphics g, final UiContext ctx) {
        final Reading now = reading.get();
        final int top = y() + above(pointer);
        final int inner = Math.max(0, width() - 2);
        ctx.skin().progress(g, x(), top, width(), BAR, 0);
        final int reach = pointer ? inner : at(inner, now.value());
        for (final Band band : now.bands()) {
            final int from = at(inner, band.from());
            final int to = Math.min(reach, at(inner, band.to()));
            for (int column = from; column < to; column++) {
                final float along = to - from <= 1 ? 0 : (column - from) / (float) (at(inner, band.to()) - from - 1);
                g.fill(x() + 1 + column, top + 1, x() + 2 + column, top + BAR - 1,
                        FastColor.ARGB32.lerp(Math.clamp(along, 0, 1), band.start(), band.end()));
            }
        }
        final int ink = ctx.skin().text();
        // A mark's words that would run into the last ones drawn are left out, the line of the mark kept.
        int wordsEnd = Integer.MIN_VALUE;
        for (final Mark mark : now.marks()) {
            final int mx = x() + 1 + at(inner, mark.at());
            g.fill(mx, top - TICK, mx + 1, top + BAR + TICK, ink);
            if (!mark.text().isEmpty()) {
                final int tw = (int) Math.ceil(ctx.skin().textWidth(ctx.font(), mark.text()) * textScale);
                final int tx = Math.clamp(mx - tw / 2, x(), Math.max(x(), right() - tw));
                if (tx > wordsEnd) {
                    ctx.skin().drawText(g, ctx.font(), mark.text(), tx, top + BAR + TICK + WORDS_GAP, ink, textScale);
                    wordsEnd = tx + tw + WORDS_GAP;
                }
            }
        }
        if (pointer) {
            final int vx = x() + 1 + at(inner, now.value());
            for (int row = 0; row < HEAD; row++) {
                g.fill(vx - row, top - POINTER + row, vx + row + 1, top - POINTER + row + 1, now.pointer());
            }
            g.fill(vx, top - 1, vx + 1, top + BAR, now.pointer());
        }
    }

    /* Where a share of the bar falls, in pixels from its inside's left. */
    private static int at(final int inner, final double share) {
        return (int) Math.round(inner * Math.clamp(share, 0, 1));
    }

    /**
     * What a scale shows: its value, its bands and its marks, each standing at a share of the bar, from 0 at its left
     * to 1 at its right.
     *
     * @param value   where the value stands
     * @param bands   the bar's bands, left to right
     * @param marks   the marks on it, each with its words, which may be none
     * @param pointer the pointer's colour, for a scale whose value stands as one
     */
    public record Reading(double value, List<Band> bands, List<Mark> marks, int pointer) {

        /** Nothing to show: an empty bar. */
        public static final Reading EMPTY = new Reading(0, List.of(), List.of(), 0);

        public Reading {
            bands = List.copyOf(bands);
            marks = List.copyOf(marks);
        }
    }

    /**
     * A band of a scale's bar, coloured from its left to its right.
     *
     * @param from  where it starts, a share of the bar
     * @param to    where it ends
     * @param start its colour at its left
     * @param end   its colour at its right
     */
    public record Band(double from, double to, int start, int end) {

        /** A band of one colour. */
        public static Band of(final double from, final double to, final int colour) {
            return new Band(from, to, colour, colour);
        }
    }

    /**
     * A mark of a scale.
     *
     * @param at   where it stands, a share of the bar
     * @param text its words under it, or none
     */
    public record Mark(double at, String text) {

        public Mark {
            Objects.requireNonNull(text, "text");
        }
    }
}
