/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.eventlog.EventLevel;
import dev.jstech.computers.gui.layout.SectionDiagnosticsLayout;
import dev.jstech.computers.operation.payload.ClusterManagerStatePayload.WireAlert;
import dev.jstech.computers.operation.payload.ClusterManagerStatePayload.WireDiagnostics;
import dev.jstech.core.client.GameLocale;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.Texts;
import dev.jstech.core.client.gui.component.UiComponent;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.text.NumberFormat;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * A datacenter section's Diagnostics tab in the Cluster Manager (computers/docs/design/servers-and-racks.md): the last
 * minute's latency against the section's base, its throughput against the most it can move, and how many alerts stand;
 * the last hour minute by minute, the latency and the throughput each on its own scale; the alerts standing and for
 * how long; and, at the foot, the last time the latency went over twice its base. Where each part stands is
 * {@link SectionDiagnosticsLayout}'s.
 */
@TextHolder
final class SectionDiagnosticsView extends UiComponent {

    private final Supplier<WireDiagnostics> data;
    private final Supplier<Tones> toneSource;
    // The manager's colours as this frame reads them, so a palette reloaded since shows at once.
    private Tones tones;

    private static final TextKey LATENCY = TextKey.of("jsc.section_diagnostics.latency", "LATENCY");
    private static final TextKey THROUGHPUT = TextKey.of("jsc.section_diagnostics.throughput", "THROUGHPUT");
    private static final TextKey ALERTS = TextKey.of("jsc.section_diagnostics.alerts", "ALERTS");
    private static final TextKey TICKS = TextKey.of("jsc.section_diagnostics.ticks", "%s ticks");
    private static final TextKey NO_ANSWER = TextKey.of("jsc.section_diagnostics.no_answer", "--");
    private static final TextKey OF_BASE = TextKey.of("jsc.section_diagnostics.of_base", "%sx the base of %s");
    private static final TextKey BASE_ONLY = TextKey.of("jsc.section_diagnostics.base_only", "the base is %s");
    private static final TextKey NO_BASE = TextKey.of("jsc.section_diagnostics.no_base", "no servers to time");
    private static final TextKey PER_SECOND = TextKey.of("jsc.section_diagnostics.per_second", "%s it/s");
    private static final TextKey AT_MOST = TextKey.of("jsc.section_diagnostics.at_most", "of %s at most");
    private static final TextKey ONE_ERROR = TextKey.of("jsc.section_diagnostics.one_error", "1 error");
    private static final TextKey ERRORS = TextKey.of("jsc.section_diagnostics.errors", "%s errors");
    private static final TextKey ONE_WARNING = TextKey.of("jsc.section_diagnostics.one_warning", "1 warning");
    private static final TextKey WARNINGS = TextKey.of("jsc.section_diagnostics.warnings", "%s warnings");
    private static final TextKey BOTH = TextKey.of("jsc.section_diagnostics.both", "%s, %s");
    private static final TextKey ALL_CLEAR = TextKey.of("jsc.section_diagnostics.all_clear", "all clear");
    private static final TextKey LAST_HOUR = TextKey.of("jsc.section_diagnostics.last_hour", "THE LAST HOUR");
    private static final TextKey LATENCY_LINE = TextKey.of("jsc.section_diagnostics.latency_line", "latency (ticks)");
    private static final TextKey THROUGHPUT_LINE =
            TextKey.of("jsc.section_diagnostics.throughput_line", "throughput (it/s)");
    private static final TextKey HOUR_AGO = TextKey.of("jsc.section_diagnostics.hour_ago", "-60 min");
    private static final TextKey NOW = TextKey.of("jsc.section_diagnostics.now", "now");
    private static final TextKey LEVEL = TextKey.of("jsc.section_diagnostics.level", "LEVEL");
    private static final TextKey WHAT = TextKey.of("jsc.section_diagnostics.what", "WHAT");
    private static final TextKey SINCE = TextKey.of("jsc.section_diagnostics.since", "SINCE");
    private static final TextKey NO_ALERTS = TextKey.of("jsc.section_diagnostics.no_alerts", "No alerts standing");
    private static final TextKey MORE = TextKey.of("jsc.section_diagnostics.more", "+%s more");
    private static final TextKey SECONDS = TextKey.of("jsc.section_diagnostics.seconds", "%s s");
    private static final TextKey MINUTES = TextKey.of("jsc.section_diagnostics.minutes", "%s min");
    private static final TextKey HOURS = TextKey.of("jsc.section_diagnostics.hours", "%s h");
    private static final TextKey LAST_OVER = TextKey.of("jsc.section_diagnostics.last_over",
            "%s ago the latency was over twice the base for %s.");
    private static final TextKey TO_THE_LOG = TextKey.of("jsc.section_diagnostics.to_the_log",
            "Every alert also goes into the network's event log.");
    private static final float SMALL = 0.75f;
    private static final int TICKS_A_SECOND = 20;
    private static final int TICKS_A_MINUTE = 1200;
    private static final int TICKS_AN_HOUR = 72_000;
    /* The chart: its rows of dots, its columns (ten minutes apart), and the room over a line's highest point. */
    private static final int GRID_ROWS = 4;
    private static final int GRID_COLUMNS = 6;
    private static final double HEADROOM = 1.25;
    private static final int DOT = 2;
    private static final int SWATCH = 6;

    SectionDiagnosticsView(final Supplier<WireDiagnostics> data, final Supplier<Tones> toneSource) {
        this.data = data;
        this.toneSource = toneSource;
        this.tones = toneSource.get();
    }

    @Override
    public void render(final GuiGraphics g, final UiContext ctx) {
        tones = toneSource.get();
        final WireDiagnostics d = data.get();
        final SectionDiagnosticsLayout.Placed p = SectionDiagnosticsLayout.place(width(), height(), d.alerts().size());
        tiles(g, ctx, d, p);
        chart(g, ctx, d, p);
        alerts(g, ctx, d, p);
        notes(g, ctx, d, p);
    }

    /* The three numbers of the last minute, each in its tile with a line under it. */
    private void tiles(final GuiGraphics g, final UiContext ctx, final WireDiagnostics d,
                       final SectionDiagnosticsLayout.Placed p) {
        final int latency = d.latencyTenths();
        final String latencyValue = latency < 0 ? GameText.resolve(NO_ANSWER)
                : GameText.resolve(TICKS.with(tenths(latency)));
        final String latencySub;
        if (d.baseTenths() <= 0) {
            latencySub = GameText.resolve(NO_BASE);
        } else if (latency < 0) {
            latencySub = GameText.resolve(BASE_ONLY.with(tenths(d.baseTenths())));
        } else {
            latencySub = GameText.resolve(OF_BASE.with(tenths(latency * 10 / d.baseTenths()),
                    tenths(d.baseTenths())));
        }
        tile(g, ctx, 0, p, GameText.resolve(LATENCY), latencyValue, latencySub, latencyTone(ctx, d));
        tile(g, ctx, 1, p, GameText.resolve(THROUGHPUT), GameText.resolve(PER_SECOND.with(
                GameLocale.count(d.throughput()))), GameText.resolve(AT_MOST.with(
                GameLocale.count(d.maxThroughput()))), ctx.skin().text());
        final int errors = count(d.alerts(), EventLevel.ERROR);
        final int warnings = d.alerts().size() - errors;
        tile(g, ctx, 2, p, GameText.resolve(ALERTS), String.valueOf(d.alerts().size()), alertWords(errors, warnings),
                errors > 0 ? tones.bad() : warnings > 0 ? tones.busy() : tones.good());
    }

    private void tile(final GuiGraphics g, final UiContext ctx, final int index,
                      final SectionDiagnosticsLayout.Placed p, final String label, final String value,
                      final String under, final int valueColour) {
        final int tx = x() + index * (p.tileW() + SectionDiagnosticsLayout.TILE_GAP);
        final int ty = y();
        g.fill(tx, ty, tx + p.tileW(), ty + SectionDiagnosticsLayout.TILE_H, ctx.skin().fieldBg());
        Draw.outline(g, tx, ty, p.tileW(), SectionDiagnosticsLayout.TILE_H, ctx.skin().edge());
        final int room = p.tileW() - 6;
        small(g, ctx, clipSmall(ctx.font(), label, room), tx + 3, ty + 2, ctx.skin().dim());
        ctx.skin().drawText(g, ctx.font(), Texts.clip(ctx.font(), value, room), tx + 3, ty + 9, valueColour);
        small(g, ctx, clipSmall(ctx.font(), under, room), tx + 3, ty + 19, ctx.skin().dim());
    }

    /* The last hour: its label and legend, the chart with its dotted grid and two lines, and its time marks. */
    private void chart(final GuiGraphics g, final UiContext ctx, final WireDiagnostics d,
                       final SectionDiagnosticsLayout.Placed p) {
        final int left = x();
        final int right = x() + width();
        small(g, ctx, GameText.resolve(LAST_HOUR), left, y() + p.chartLabelY(), ctx.skin().dim());
        final String throughputWord = GameText.resolve(THROUGHPUT_LINE);
        final String latencyWord = GameText.resolve(LATENCY_LINE);
        final int throughputX = right - smallWidth(ctx.font(), throughputWord);
        final int latencyX = throughputX - SWATCH - 6 - smallWidth(ctx.font(), latencyWord);
        legend(g, ctx, latencyWord, latencyX - SWATCH - 2, y() + p.chartLabelY(), tones.busy());
        legend(g, ctx, throughputWord, throughputX - SWATCH - 2, y() + p.chartLabelY(), ctx.skin().accent());

        final int top = y() + p.chartY();
        final int bottom = top + p.chartH();
        ctx.skin().chartGround(g, left, top, width(), p.chartH());
        for (int row = 1; row < GRID_ROWS; row++) {
            final int gy = top + p.chartH() * row / GRID_ROWS;
            for (int gx = left + 1; gx < right - 1; gx += DOT) {
                g.fill(gx, gy, gx + 1, gy + 1, ctx.skin().chartLine());
            }
        }
        for (int column = 1; column < GRID_COLUMNS; column++) {
            final int gx = left + width() * column / GRID_COLUMNS;
            for (int gy = top + 1; gy < bottom - 1; gy += DOT) {
                g.fill(gx, gy, gx + 1, gy + 1, ctx.skin().chartLine());
            }
        }
        line(g, d.hourThroughput(), left + 1, top + 2, width() - 2, p.chartH() - 4, ctx.skin().accent());
        line(g, d.hourLatency(), left + 1, top + 2, width() - 2, p.chartH() - 4, tones.busy());
        small(g, ctx, GameText.resolve(HOUR_AGO), left, y() + p.axisY(), ctx.skin().dim());
        final String now = GameText.resolve(NOW);
        small(g, ctx, now, right - smallWidth(ctx.font(), now), y() + p.axisY(), ctx.skin().dim());
    }

    /*
     * One of the chart's lines, on its own scale: the hour's minutes spread across the chart, the newest at its right
     * edge, its highest point a quarter under the top; a minute with nothing in it (-1) breaks the line.
     */
    private static void line(final GuiGraphics g, final List<Integer> values, final int x, final int y, final int w,
                             final int h, final int colour) {
        int most = 1;
        for (final int value : values) {
            most = Math.max(most, value);
        }
        final double top = most * HEADROOM;
        final int points = WireDiagnostics.MAX_MINUTES;
        final int first = points - values.size();
        int previous = Integer.MIN_VALUE;
        for (int i = 0; i < values.size(); i++) {
            final int value = values.get(i);
            if (value < 0) {
                previous = Integer.MIN_VALUE;
                continue;
            }
            final int px = x + (int) Math.round((double) w * (first + i) / (points - 1));
            final int py = y + h - (int) Math.round(h * value / top);
            if (previous == Integer.MIN_VALUE) {
                g.fill(px, py, px + 1, py + 1, colour);
            } else {
                final int fromX = x + (int) Math.round((double) w * (first + i - 1) / (points - 1));
                for (int lx = fromX + 1; lx <= px; lx++) {
                    final int ly = previous + (py - previous) * (lx - fromX) / Math.max(1, px - fromX);
                    final int was = previous + (py - previous) * (lx - 1 - fromX) / Math.max(1, px - fromX);
                    g.fill(lx, Math.min(was, ly), lx + 1, Math.max(was, ly) + 1, colour);
                }
            }
            previous = py;
        }
    }

    /* The table of the alerts standing: its label, its header, a row for each that fits and a count of the rest. */
    private void alerts(final GuiGraphics g, final UiContext ctx, final WireDiagnostics d,
                        final SectionDiagnosticsLayout.Placed p) {
        final int left = x();
        final int right = x() + width();
        small(g, ctx, GameText.resolve(ALERTS), left, y() + p.alertsLabelY(), ctx.skin().dim());
        final int headerY = y() + p.headerY();
        g.fill(left, headerY, right, headerY + SectionDiagnosticsLayout.HEADER_H, ctx.skin().panelBg());
        final int textY = headerY + 1;
        small(g, ctx, GameText.resolve(LEVEL), left + SectionDiagnosticsLayout.LEVEL_X, textY, ctx.skin().dim());
        small(g, ctx, GameText.resolve(WHAT), left + SectionDiagnosticsLayout.WHAT_X, textY, ctx.skin().dim());
        final int sinceX = right - SectionDiagnosticsLayout.SINCE_W;
        small(g, ctx, GameText.resolve(SINCE), sinceX, textY, ctx.skin().dim());
        if (p.rows() == 0) {
            return;
        }
        final int rowsY = y() + p.rowsY();
        final List<WireAlert> alerts = d.alerts();
        if (alerts.isEmpty()) {
            small(g, ctx, GameText.resolve(NO_ALERTS), left + SectionDiagnosticsLayout.LEVEL_X, rowsY,
                    ctx.skin().dim());
            return;
        }
        final boolean cut = alerts.size() > p.rows();
        final int shown = cut ? p.rows() - 1 : alerts.size();
        final int whatRoom = sinceX - (left + SectionDiagnosticsLayout.WHAT_X) - 4;
        for (int i = 0; i < shown; i++) {
            final WireAlert alert = alerts.get(i);
            final int ry = rowsY + i * SectionDiagnosticsLayout.ROW_H;
            final EventLevel level = EventLevel.byId(alert.level());
            small(g, ctx, GameText.resolve(level.label()), left + SectionDiagnosticsLayout.LEVEL_X, ry,
                    levelTone(ctx, level));
            small(g, ctx, clipSmall(ctx.font(), GameText.resolve(alert.what()), whatRoom),
                    left + SectionDiagnosticsLayout.WHAT_X, ry, ctx.skin().text());
            small(g, ctx, GameText.resolve(duration(alert.standing())), sinceX, ry, ctx.skin().dim());
        }
        if (cut) {
            small(g, ctx, GameText.resolve(MORE.with(alerts.size() - shown)), left + SectionDiagnosticsLayout.WHAT_X,
                    rowsY + shown * SectionDiagnosticsLayout.ROW_H, ctx.skin().dim());
        }
    }

    /* The foot: the last time the latency went over twice its base, if it ever did, and where every alert goes. */
    private void notes(final GuiGraphics g, final UiContext ctx, final WireDiagnostics d,
                       final SectionDiagnosticsLayout.Placed p) {
        if (p.notes() == 0) {
            return;
        }
        int ny = y() + p.notesY();
        if (d.lastOverAgo() >= 0L) {
            small(g, ctx, clipSmall(ctx.font(), GameText.resolve(LAST_OVER.with(duration(d.lastOverAgo()),
                    duration(d.lastOverTicks()))), width()), x(), ny, ctx.skin().dim());
            ny += SectionDiagnosticsLayout.LINE_H;
        }
        small(g, ctx, clipSmall(ctx.font(), GameText.resolve(TO_THE_LOG), width()), x(), ny, ctx.skin().dim());
    }

    private void legend(final GuiGraphics g, final UiContext ctx, final String word, final int x, final int y,
                        final int colour) {
        g.fill(x, y + 3, x + SWATCH, y + 4, colour);
        small(g, ctx, word, x + SWATCH + 2, y, ctx.skin().dim());
    }

    /* The latency's colour: fine near its base, busy past half again, bad past twice. */
    private int latencyTone(final UiContext ctx, final WireDiagnostics d) {
        if (d.latencyTenths() < 0 || d.baseTenths() <= 0) {
            return ctx.skin().text();
        }
        if (d.latencyTenths() > 2 * d.baseTenths()) {
            return tones.bad();
        }
        return d.latencyTenths() * 2 > 3 * d.baseTenths() ? tones.busy() : tones.good();
    }

    private int levelTone(final UiContext ctx, final EventLevel level) {
        return switch (level) {
            case ERROR -> tones.bad();
            case WARN -> tones.busy();
            case INFO -> ctx.skin().dim();
        };
    }

    private static String alertWords(final int errors, final int warnings) {
        if (errors == 0 && warnings == 0) {
            return GameText.resolve(ALL_CLEAR);
        }
        final Text errorWords = errors == 1 ? ONE_ERROR.text() : ERRORS.with(errors);
        final Text warningWords = warnings == 1 ? ONE_WARNING.text() : WARNINGS.with(warnings);
        if (warnings == 0) {
            return GameText.resolve(errorWords);
        }
        return GameText.resolve(errors == 0 ? warningWords : BOTH.with(errorWords, warningWords));
    }

    private static int count(final List<WireAlert> alerts, final EventLevel level) {
        int count = 0;
        for (final WireAlert alert : alerts) {
            if (alert.level() == level.id()) {
                count++;
            }
        }
        return count;
    }

    /* A stretch of game time as the table and the notes say it: seconds under a minute, minutes under an hour. */
    private static Text duration(final long ticks) {
        if (ticks < TICKS_A_MINUTE) {
            return SECONDS.with(ticks / TICKS_A_SECOND);
        }
        if (ticks < TICKS_AN_HOUR) {
            return MINUTES.with(ticks / TICKS_A_MINUTE);
        }
        return HOURS.with(ticks / TICKS_AN_HOUR);
    }

    /* Tenths as a number with one decimal, the way the game's language writes it: 38 is "3.8" in English. */
    private static String tenths(final int tenths) {
        final NumberFormat format = NumberFormat.getNumberInstance(GameLocale.locale());
        format.setMinimumFractionDigits(1);
        format.setMaximumFractionDigits(1);
        return format.format(tenths / 10.0);
    }

    private static void small(final GuiGraphics g, final UiContext ctx, final String text, final int x, final int y,
                              final int colour) {
        ctx.skin().drawText(g, ctx.font(), text, x, y, colour, SMALL);
    }

    private static int smallWidth(final Font font, final String text) {
        return Math.round(font.width(text) * SMALL);
    }

    private static String clipSmall(final Font font, final String text, final int pixels) {
        return Texts.clip(font, text, (int) (pixels / SMALL));
    }

    /**
     * The manager's colours the tab takes: what is fine, what is busy, and what is wrong.
     *
     * @param good fine: a latency near its base, no alerts
     * @param busy busy: a latency past half again its base, a warning, the latency's line
     * @param bad  wrong: a latency past twice its base, an error
     */
    record Tones(int good, int busy, int bad) {
    }
}
