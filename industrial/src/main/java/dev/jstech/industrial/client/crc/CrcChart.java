/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.client.crc;

import static dev.jstech.industrial.crc.CrcLayout.AXIS_Y;
import static dev.jstech.industrial.crc.CrcLayout.CHART_BOTTOM;
import static dev.jstech.industrial.crc.CrcLayout.CHART_LEFT;
import static dev.jstech.industrial.crc.CrcLayout.CHART_RIGHT;
import static dev.jstech.industrial.crc.CrcLayout.CHART_TOP;
import static dev.jstech.industrial.crc.CrcLayout.FIRST_YEAR;
import static dev.jstech.industrial.crc.CrcLayout.FUTURE_X;
import static dev.jstech.industrial.crc.CrcLayout.LANES_SHOWN;
import static dev.jstech.industrial.crc.CrcLayout.LANE_HEIGHT;
import static dev.jstech.industrial.crc.CrcLayout.LANE_TOP;
import static dev.jstech.industrial.crc.CrcLayout.LAST_YEAR;
import static dev.jstech.industrial.crc.CrcLayout.LEGEND_Y;
import static dev.jstech.industrial.crc.CrcLayout.YEAR_LEFT;
import static dev.jstech.industrial.crc.CrcLayout.YEAR_RIGHT;

import dev.jstech.core.client.machine.MachineArt;
import dev.jstech.core.client.machine.MachineWords;
import dev.jstech.core.research.Discovery;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import dev.jstech.industrial.crc.CrcLayout;
import dev.jstech.industrial.crc.CrcReadings;
import dev.jstech.industrial.crc.CrcScreenTexts;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * The Research tab's chart, as the approved design draws it: the discoveries there to research by the year each was
 * made in the real world, one lane for each field of science, the ones not made yet past the axis' end, under a
 * question mark. Each is a pen ring, filled for one its team knows (with a tick), blue for the one its group works
 * now, half filled for one begun, open for one it may start, and a pencil mark for one it may not start yet; a line
 * runs from what each comes after to it, whole once that is known. More fields than its four lanes turn with the wheel.
 */
final class CrcChart {

    private final List<Lane> lanes;
    private final List<Node> nodes;
    private final int scroll;

    /* The rows a node moves up and down by, to stand clear of another of its lane at the same year. */
    private static final int CROWD = 6;
    private static final int RAISE = -4;
    private static final int LOWER = 5;
    /* The ring a node is clicked and hovered in. */
    private static final int HIT = 3;
    private static final int YEAR_STEP = 10;
    private static final int LABELLED_STEP = 25;
    private static final int LABEL_ABOVE = 10;
    private static final int LABEL_BELOW = 4;

    private CrcChart(final List<Lane> lanes, final List<Node> nodes, final int scroll) {
        this.lanes = lanes;
        this.nodes = nodes;
        this.scroll = scroll;
    }

    /**
     * The chart of {@code discoveries} as {@code read} says their team stands on them, {@code now} the one its group
     * works, its lanes turned on by {@code scroll}.
     */
    static CrcChart of(final List<Discovery> discoveries, final CrcReadings read, final @Nullable ResourceLocation now,
                       final int scroll) {
        final List<TextKey> fields = new ArrayList<>();
        boolean fieldless = false;
        for (final Discovery discovery : discoveries) {
            if (discovery.field() == null) {
                fieldless = true;
            } else if (!fields.contains(discovery.field())) {
                fields.add(discovery.field());
            }
        }
        final List<Lane> lanes = new ArrayList<>();
        for (final TextKey field : fields) {
            lanes.add(new Lane(field));
        }
        if (fieldless) {
            lanes.add(new Lane(CrcScreenTexts.NO_FIELD));
        }
        final int turned = Math.clamp(scroll, 0, Math.max(0, lanes.size() - LANES_SHOWN));
        final List<Node> nodes = new ArrayList<>();
        final Map<Integer, Integer> lastX = new HashMap<>();
        final Map<Integer, Integer> lastLift = new HashMap<>();
        final List<Discovery> byYear = new ArrayList<>(discoveries);
        byYear.sort((a, b) -> Integer.compare(xOf(a), xOf(b)));
        for (final Discovery discovery : byYear) {
            final int lane = discovery.field() == null ? lanes.size() - 1 : fields.indexOf(discovery.field());
            final int shown = lane - turned;
            if (shown < 0 || shown >= LANES_SHOWN) {
                continue;
            }
            final int x = xOf(discovery);
            int lift = 0;
            if (lastX.containsKey(lane) && x - lastX.get(lane) < CROWD) {
                lift = lastLift.getOrDefault(lane, 0) <= 0 ? LOWER : RAISE;
            }
            lastX.put(lane, x);
            lastLift.put(lane, lift);
            nodes.add(new Node(discovery, x, CrcLayout.laneY(shown) + lift, stateOf(discovery, read, now),
                    read.share(discovery.id())));
        }
        return new CrcChart(lanes, nodes, turned);
    }

    /** How many lanes it has, shown or not. */
    int laneCount() {
        return this.lanes.size();
    }

    /** The first lane it shows. */
    int scroll() {
        return this.scroll;
    }

    /** The node at ({@code x}, {@code y}), if any. */
    Optional<Node> nodeAt(final double x, final double y) {
        for (final Node node : this.nodes) {
            if (Math.abs(x - node.x()) <= HIT && Math.abs(y - node.y()) <= HIT) {
                return Optional.of(node);
            }
        }
        return Optional.empty();
    }

    /** The node of {@code id}, if it shows. */
    Optional<Node> node(final ResourceLocation id) {
        for (final Node node : this.nodes) {
            if (node.discovery().id().equals(id)) {
                return Optional.of(node);
            }
        }
        return Optional.empty();
    }

    /** Draws it, {@code picked} boxed in red pencil. */
    void draw(final CrcView view, final @Nullable ResourceLocation picked) {
        final GuiGraphics g = view.g();
        final Font font = view.font();
        final CrcArt.Colours c = CrcArt.PALETTE.get();
        MachineArt.bezel(g, view.x(CHART_LEFT - 3), view.y(CHART_TOP - 3), view.x(CHART_RIGHT + 3),
                view.y(CHART_BOTTOM + 3), true);
        MachineArt.fill(g, view.x(CHART_LEFT), view.y(CHART_TOP), view.x(CHART_RIGHT), view.y(CHART_BOTTOM),
                c.paper());
        for (int x = CHART_LEFT; x < CHART_RIGHT; x += 4) {
            MachineArt.vline(g, view.x(x), view.y(CHART_TOP), view.y(CHART_BOTTOM), (x - CHART_LEFT) % 20 == 0
                    ? c.gridMajor() : c.grid());
        }
        for (int y = CHART_TOP; y < CHART_BOTTOM; y += 4) {
            MachineArt.hline(g, view.x(CHART_LEFT), view.x(CHART_RIGHT), view.y(y), (y - CHART_TOP) % 20 == 0
                    ? c.gridMajor() : c.grid());
        }
        MachineArt.text(g, font, MachineArt.fit(font, MachineWords.upper(CrcScreenTexts.CHART), CHART_RIGHT
                - CHART_LEFT - 30), view.x(CHART_LEFT + 4), view.y(CHART_TOP + 2), c.ink());
        if (this.lanes.size() > LANES_SHOWN) {
            final String shown = (this.scroll + 1) + "-" + (this.scroll + LANES_SHOWN) + "/" + this.lanes.size();
            MachineArt.text(g, font, shown, view.x(CHART_RIGHT - 3) - MachineArt.width(font, shown),
                    view.y(CHART_TOP + 2), c.pencil());
        }
        if (this.lanes.isEmpty()) {
            MachineArt.textCentred(g, font, MachineArt.fit(font, GameText.resolve(CrcScreenTexts.NO_DISCOVERIES),
                    CHART_RIGHT - CHART_LEFT - 8), view.x((CHART_LEFT + CHART_RIGHT) / 2),
                    view.y((CHART_TOP + CHART_BOTTOM) / 2), c.pencil());
            return;
        }
        lanes(view);
        axis(view);
        lines(view);
        labels(view);
        for (final Node node : this.nodes) {
            drawNode(g, view.x(node.x()), view.y(node.y()), node.state());
        }
        if (picked != null) {
            node(picked).ifPresent(node -> MachineArt.frame(g, view.x(node.x() - 5), view.y(node.y() - 5),
                    view.x(node.x() + 6), view.y(node.y() + 6), c.red()));
        }
        legend(view);
    }

    /** The state of a discovery on the chart: what its team knows of it. */
    static State stateOf(final Discovery discovery, final CrcReadings read, final @Nullable ResourceLocation now) {
        final double share = read.share(discovery.id());
        if (share >= 1.0) {
            return State.KNOWN;
        }
        if (discovery.id().equals(now)) {
            return State.NOW;
        }
        if (share > 0.0) {
            return State.STARTED;
        }
        return read.ready(discovery) ? State.OPEN : State.NOT_YET;
    }

    /** The word of a state, as the legend and a tooltip say it. */
    static TextKey word(final State state) {
        return switch (state) {
            case KNOWN -> CrcScreenTexts.KNOWN;
            case NOW -> CrcScreenTexts.NOW_RUNNING;
            case STARTED -> CrcScreenTexts.STARTED;
            case OPEN -> CrcScreenTexts.OPEN;
            case NOT_YET -> CrcScreenTexts.NOT_YET;
        };
    }

    /* Each lane shown: its dotted top line in pencil and its field's name. */
    private void lanes(final CrcView view) {
        final CrcArt.Colours c = CrcArt.PALETTE.get();
        for (int i = 0; i < LANES_SHOWN && this.scroll + i < this.lanes.size(); i++) {
            final int top = LANE_TOP + i * LANE_HEIGHT;
            for (int x = CHART_LEFT + 2; x < CHART_RIGHT - 2; x += 3) {
                MachineArt.dot(view.g(), view.x(x), view.y(top), c.pencil());
            }
            MachineArt.text(view.g(), view.font(), MachineArt.fit(view.font(), MachineWords.upper(
                    this.lanes.get(this.scroll + i).field()), CHART_RIGHT - CHART_LEFT - 8), view.x(CHART_LEFT + 3),
                    view.y(top + 2), c.pencil());
        }
    }

    /* The years' axis: a tick every ten years, longer every fifty, the years every twenty-five, and the future. */
    private static void axis(final CrcView view) {
        final GuiGraphics g = view.g();
        final CrcArt.Colours c = CrcArt.PALETTE.get();
        MachineArt.hline(g, view.x(YEAR_LEFT), view.x(YEAR_RIGHT + 1), view.y(AXIS_Y), c.ink());
        for (int year = FIRST_YEAR; year <= LAST_YEAR; year += YEAR_STEP) {
            final int x = view.x(CrcLayout.yearX(year));
            MachineArt.vline(g, x, view.y(AXIS_Y), view.y(AXIS_Y + (year % 50 == 0 ? 3 : 2)), c.ink());
        }
        for (int year = FIRST_YEAR; year < LAST_YEAR; year += LABELLED_STEP) {
            MachineArt.textCentred(g, view.font(), String.valueOf(year), view.x(CrcLayout.yearX(year)),
                    view.y(AXIS_Y + 4), c.ink());
        }
        MachineArt.text(g, view.font(), "?", view.x(FUTURE_X - 1), view.y(AXIS_Y + 4), c.ink());
    }

    /* A line from what each discovery comes after to it: across, then up or down; whole once that is known. */
    private void lines(final CrcView view) {
        final CrcArt.Colours c = CrcArt.PALETTE.get();
        for (final Node node : this.nodes) {
            for (final ResourceLocation after : node.discovery().after()) {
                final Optional<Node> from = node(after);
                if (from.isEmpty()) {
                    continue;
                }
                final boolean whole = from.get().state() == State.KNOWN;
                final int y = from.get().y();
                final int x0 = Math.min(from.get().x(), node.x()) + 3;
                final int x1 = Math.max(from.get().x(), node.x()) - (y == node.y() ? 3 : 0);
                for (int x = x0; x < x1; x++) {
                    if (whole || x % 2 == 0) {
                        MachineArt.dot(view.g(), view.x(x), view.y(y), c.ink());
                    }
                }
                final int y0 = Math.min(y, node.y() + 3);
                final int y1 = Math.max(y, node.y() - 3);
                for (int down = y0; down < y1 && y != node.y(); down++) {
                    if (whole || down % 2 == 0) {
                        MachineArt.dot(view.g(), view.x(node.x()), view.y(down), c.ink());
                    }
                }
            }
        }
    }

    /*
     * Each node's name, and how far it is for one begun: above it, or below when another's name or its lane's is in
     * the way; left out when neither has room, for its tooltip to say.
     */
    private void labels(final CrcView view) {
        final CrcArt.Colours c = CrcArt.PALETTE.get();
        final List<int[]> taken = new ArrayList<>();
        for (int i = 0; i < LANES_SHOWN && this.scroll + i < this.lanes.size(); i++) {
            final int top = LANE_TOP + i * LANE_HEIGHT;
            final int w = Math.min(CHART_RIGHT - CHART_LEFT - 8, MachineArt.width(view.font(), MachineWords.upper(
                    this.lanes.get(this.scroll + i).field())));
            taken.add(new int[] {CHART_LEFT + 3, top + 2, CHART_LEFT + 3 + w, top + 2 + CrcLayout.SMALL_TEXT});
        }
        for (final Node node : this.nodes) {
            final String name = GameText.resolve(node.discovery().name());
            final String label = node.state() == State.NOW || node.state() == State.STARTED
                    ? GameText.resolve(CrcScreenTexts.NAMED_SHARE.with(name, (int) Math.floor(node.share() * 100)))
                    : name;
            final int w = MachineArt.width(view.font(), label);
            final int x = Math.clamp(node.x() - w / 2, CHART_LEFT + 2, CHART_RIGHT - 2 - w);
            int[] box = {x, node.y() - LABEL_ABOVE, x + w, node.y() - LABEL_ABOVE + CrcLayout.SMALL_TEXT};
            if (clashes(box, taken)) {
                box = new int[] {x, node.y() + LABEL_BELOW, x + w, node.y() + LABEL_BELOW + CrcLayout.SMALL_TEXT};
            }
            if (clashes(box, taken) || box[1] < CHART_TOP + 9 || box[3] > AXIS_Y) {
                continue;
            }
            taken.add(box);
            final int colour = node.state() == State.NOW ? c.blue() : node.state() == State.NOT_YET ? c.pencil()
                    : c.ink();
            MachineArt.text(view.g(), view.font(), label, view.x(box[0]), view.y(box[1]), colour);
        }
    }

    /* The legend under the axis: a node of each state and its word. */
    private static void legend(final CrcView view) {
        final CrcArt.Colours c = CrcArt.PALETTE.get();
        int x = CHART_LEFT + 7;
        for (final State state : State.values()) {
            drawNode(view.g(), view.x(x), view.y(LEGEND_Y), state);
            final String word = GameText.resolve(word(state));
            MachineArt.text(view.g(), view.font(), word, view.x(x + 5), view.y(LEGEND_Y - 2), c.ink());
            x += 5 + MachineArt.width(view.font(), word) + 9;
        }
    }

    /* A node of {@code state} at the screen's ({@code x}, {@code y}). */
    private static void drawNode(final GuiGraphics g, final int x, final int y, final State state) {
        final CrcArt.Colours c = CrcArt.PALETTE.get();
        switch (state) {
            case KNOWN -> {
                CrcArt.node(g, x, y, c.ink(), CrcArt.Fill.WHOLE);
                CrcArt.tick(g, x + 3, y - 4);
            }
            case NOW -> CrcArt.node(g, x, y, c.blue(), CrcArt.Fill.WHOLE);
            case STARTED -> CrcArt.node(g, x, y, c.ink(), CrcArt.Fill.HALF);
            case OPEN -> CrcArt.node(g, x, y, c.ink(), CrcArt.Fill.OPEN);
            case NOT_YET -> CrcArt.mark(g, x, y);
        }
    }

    /* Whether {@code box} overlaps any of {@code taken}. */
    private static boolean clashes(final int[] box, final List<int[]> taken) {
        for (final int[] other : taken) {
            if (box[0] < other[2] && other[0] < box[2] && box[1] < other[3] && other[1] < box[3]) {
                return true;
            }
        }
        return false;
    }

    /* Where a discovery stands across: its year, or past the axis' end for one not made yet. */
    private static int xOf(final Discovery discovery) {
        return discovery.year().isPresent() ? CrcLayout.yearX(discovery.year().getAsInt()) : FUTURE_X;
    }

    /** A discovery's state, as its team stands on it. */
    enum State {
        KNOWN,
        NOW,
        STARTED,
        OPEN,
        NOT_YET
    }

    /** A lane: a field of science. */
    private record Lane(TextKey field) {
    }

    /**
     * A discovery on the chart.
     *
     * @param discovery the discovery
     * @param x         its node across, on the screen from its left
     * @param y         its node down
     * @param state     its state
     * @param share     how much of it its team has made
     */
    record Node(Discovery discovery, int x, int y, State state, double share) {
    }
}
