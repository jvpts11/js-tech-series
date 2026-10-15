/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.component;

import dev.jstech.core.client.gui.logic.ChartMath;
import java.util.List;
import java.util.Objects;
import java.util.function.DoubleFunction;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphics;

/**
 * A chart of a machine's numbers, as the game draws its debug graphs: lines over time with their legend and a limit,
 * bars side by side with their values, or a pie of shares with its legend. What it shows is read every frame, so a
 * chart of a running machine moves with it; its colours are the look's series.
 */
public final class ChartView extends UiComponent {

    private final Kind kind;
    private final String title;
    private final Supplier<List<Series>> data;
    private final DoubleFunction<String> valueText;
    private final String fromLabel;
    private final String toLabel;
    private double limit = Double.NaN;
    private String limitLabel = "";

    /** The room at the chart's left for its scale's numbers, and under it for its labels. */
    private static final int AXIS = 24;
    private static final int FOOT = 10;
    private static final int TITLE_ROW = 12;
    private static final int GRID_LINES = 4;
    private static final int DOT = 2;
    private static final float SMALL = 0.75f;

    /**
     * @param kind      lines, bars or a pie
     * @param title     what the chart is called, drawn at its top
     * @param data      its series: for lines each has its values over time, for bars and a pie each is one value
     * @param valueText how a value is written ({@code "%.1f kW"})
     * @param fromLabel what the time at the left is called, for lines ({@code "-10 min"})
     * @param toLabel   what the time at the right is called ({@code "now"})
     */
    public ChartView(final Kind kind, final String title, final Supplier<List<Series>> data,
                     final DoubleFunction<String> valueText, final String fromLabel, final String toLabel) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.title = Objects.requireNonNull(title, "title");
        this.data = Objects.requireNonNull(data, "data");
        this.valueText = Objects.requireNonNull(valueText, "valueText");
        this.fromLabel = Objects.requireNonNull(fromLabel, "fromLabel");
        this.toLabel = Objects.requireNonNull(toLabel, "toLabel");
    }

    /** Draws a dashed line at {@code value}, a limit the lines should stay under, with what it is called. */
    public ChartView limit(final double value, final String label) {
        limit = value;
        limitLabel = Objects.requireNonNull(label, "label");
        return this;
    }

    @Override
    public void render(final GuiGraphics g, final UiContext ctx) {
        ctx.skin().chartGround(g, x(), y(), width(), height());
        ctx.skin().drawText(g, ctx.font(), title, x() + 4, y() + 3, ctx.skin().text());
        final List<Series> series = data.get();
        switch (kind) {
            case LINE -> lines(g, ctx, series);
            case BAR -> bars(g, ctx, series);
            case PIE -> pie(g, ctx, series);
        }
    }

    private void lines(final GuiGraphics g, final UiContext ctx, final List<Series> series) {
        final int px0 = x() + AXIS;
        final int py0 = y() + TITLE_ROW + 4;
        final int px1 = right() - 4;
        final int py1 = bottom() - FOOT - 2;
        double most = Double.isNaN(limit) ? 0 : limit;
        for (final Series one : series) {
            for (final double value : one.values()) {
                most = Math.max(most, value);
            }
        }
        final double top = ChartMath.niceMax(most);
        for (int k = 0; k <= GRID_LINES; k++) {
            final int gy = py0 + (py1 - py0) * k / GRID_LINES;
            for (int gx = px0; gx < px1; gx += DOT) {
                g.fill(gx, gy, gx + 1, gy + 1, ctx.skin().chartLine());
            }
        }
        small(g, ctx, valueText.apply(top), px0 - 3, py0 - 3, true);
        small(g, ctx, valueText.apply(0), px0 - 3, py1 - 3, true);
        small(g, ctx, fromLabel, px0, py1 + 3, false);
        small(g, ctx, toLabel, px1, py1 + 3, true);
        if (!Double.isNaN(limit)) {
            final int ly = py1 - (int) Math.round((py1 - py0) * limit / top);
            for (int lx = px0; lx < px1; lx += 4) {
                g.fill(lx, ly, Math.min(px1, lx + 2), ly + 1, ctx.skin().seriesColour(3));
            }
            small(g, ctx, limitLabel, px1, ly - 7, true);
        }
        for (int s = 0; s < series.size(); s++) {
            final double[] values = series.get(s).values();
            final int colour = ctx.skin().seriesColour(s);
            int previous = Integer.MIN_VALUE;
            for (int px = px0; px < px1 && values.length > 0; px++) {
                final int index = (int) ((px - px0) / (double) (px1 - px0) * (values.length - 1) + 0.5);
                final int py = py1 - (int) Math.round((py1 - py0) * values[index] / top);
                if (previous == Integer.MIN_VALUE) {
                    g.fill(px, py, px + 1, py + 1, colour);
                } else {
                    g.fill(px, Math.min(previous, py), px + 1, Math.max(previous, py) + 1, colour);
                }
                previous = py;
            }
            legend(g, ctx, s, series.get(s).name(), right() - 4 - (series.size() - s) * 44, y() + 4);
        }
    }

    private void bars(final GuiGraphics g, final UiContext ctx, final List<Series> series) {
        final int base = bottom() - FOOT - 2;
        final int top = y() + TITLE_ROW + 8;
        double most = 0;
        for (final Series one : series) {
            most = Math.max(most, one.value());
        }
        final double scale = ChartMath.niceMax(most);
        final int count = Math.max(1, series.size());
        final int slot = (width() - 8) / count;
        final int barWidth = Math.max(3, slot - 5);
        final int colour = ctx.skin().seriesColour(0);
        for (int i = 0; i < series.size(); i++) {
            final int bx = x() + 4 + i * slot + (slot - barWidth) / 2;
            final int by = base - (int) Math.round((base - top) * series.get(i).value() / scale);
            g.fill(bx, by, bx + barWidth, base, colour);
            g.fill(bx + barWidth - Math.max(1, barWidth / 4), by, bx + barWidth, base, darker(colour));
            g.fill(bx, by, bx + barWidth, by + 1, lighter(colour));
            small(g, ctx, valueText.apply(series.get(i).value()), bx + barWidth / 2, by - 7, null);
            small(g, ctx, series.get(i).name(), bx + barWidth / 2, base + 3, null);
        }
        g.fill(x() + 4, base, right() - 4, base + 1, ctx.skin().chartLine());
    }

    private void pie(final GuiGraphics g, final UiContext ctx, final List<Series> series) {
        final double[] values = new double[series.size()];
        double total = 0;
        for (int i = 0; i < values.length; i++) {
            values[i] = series.get(i).value();
            total += Math.max(0, values[i]);
        }
        final List<double[]> slices = ChartMath.slices(values);
        final int legendRows = series.size();
        final int r = Math.max(4, Math.min(width() - 12, height() - TITLE_ROW - legendRows * 8 - 10) / 2);
        final int cx = x() + width() / 2;
        final int cy = y() + TITLE_ROW + 4 + r;
        for (int dy = -r; dy <= r; dy++) {
            int runStart = Integer.MIN_VALUE;
            int runSlice = -1;
            for (int dx = -r; dx <= r + 1; dx++) {
                final int slice = dx > r ? -1 : ChartMath.sliceAt(dx + 0.5, dy + 0.5, 0, 0, r, slices);
                if (slice != runSlice) {
                    if (runSlice >= 0) {
                        g.fill(cx + runStart, cy + dy, cx + dx, cy + dy + 1, ctx.skin().seriesColour(runSlice));
                    }
                    runSlice = slice;
                    runStart = dx;
                }
            }
        }
        for (int i = 0; i < series.size(); i++) {
            final int share = total == 0 ? 0 : (int) Math.round(100 * Math.max(0, values[i]) / total);
            final int ly = cy + r + 6 + i * 8;
            g.fill(x() + 5, ly + 1, x() + 9, ly + 5, ctx.skin().seriesColour(i));
            small(g, ctx, series.get(i).name() + " " + share + "%", x() + 12, ly, false);
        }
    }

    private void legend(final GuiGraphics g, final UiContext ctx, final int index, final String name, final int x,
                        final int y) {
        g.fill(x, y + 1, x + 3, y + 4, ctx.skin().seriesColour(index));
        small(g, ctx, name, x + 5, y, false);
    }

    /* Small text: from its left, from its right ({@code right} true), or centred ({@code right} null). */
    private static void small(final GuiGraphics g, final UiContext ctx, final String text, final int x, final int y,
                              final Boolean right) {
        final int tw = Math.round(ctx.skin().textWidth(ctx.font(), text) * SMALL);
        final int tx = right == null ? x - tw / 2 : right ? x - tw : x;
        ctx.skin().drawText(g, ctx.font(), text, tx, y, ctx.skin().dim(), SMALL);
    }

    /* The colour at half its brightness, its alpha kept. */
    private static int darker(final int argb) {
        return argb >>> 24 << 24 | argb >> 1 & 0x7F7F7F;
    }

    /* The colour halfway to white, its alpha kept. */
    private static int lighter(final int argb) {
        return argb >>> 24 << 24 | ((argb & 0xFEFEFE) >> 1) + 0x808080;
    }

    /** What a chart draws. */
    public enum Kind {
        /** Lines over time. */
        LINE,
        /** Bars side by side. */
        BAR,
        /** A pie of shares. */
        PIE
    }

    /**
     * One series: a line's values over time, or a bar's or a slice's single value.
     *
     * @param name   what it is called in the legend or under its bar
     * @param values its values; a bar or a slice has one
     */
    public record Series(String name, double[] values) {

        public Series {
            values = values.clone();
        }

        /** A bar or a slice. */
        public static Series of(final String name, final double value) {
            return new Series(name, new double[] {value});
        }

        /** Its values, a copy. */
        @Override
        public double[] values() {
            return values.clone();
        }

        /** Its one value, or its first. */
        public double value() {
            return values.length == 0 ? 0 : values[0];
        }
    }
}
