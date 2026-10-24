/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.client.crc;

import static dev.jstech.industrial.crc.CrcLayout.BUTTON_HEIGHT;
import static dev.jstech.industrial.crc.CrcLayout.BUTTON_Y;
import static dev.jstech.industrial.crc.CrcLayout.CARD_BOTTOM;
import static dev.jstech.industrial.crc.CrcLayout.CARD_FLOW_BOTTOM;
import static dev.jstech.industrial.crc.CrcLayout.CARD_FLOW_TOP;
import static dev.jstech.industrial.crc.CrcLayout.CARD_LEFT;
import static dev.jstech.industrial.crc.CrcLayout.CARD_RIGHT;
import static dev.jstech.industrial.crc.CrcLayout.CARD_TOP;
import static dev.jstech.industrial.crc.CrcLayout.CHART_BOTTOM;
import static dev.jstech.industrial.crc.CrcLayout.CHART_LEFT;
import static dev.jstech.industrial.crc.CrcLayout.CHART_RIGHT;
import static dev.jstech.industrial.crc.CrcLayout.CHART_TOP;
import static dev.jstech.industrial.crc.CrcLayout.COMPUTING_Y;
import static dev.jstech.industrial.crc.CrcLayout.DIGIT;
import static dev.jstech.industrial.crc.CrcLayout.FACE_X;
import static dev.jstech.industrial.crc.CrcLayout.FACE_Y;
import static dev.jstech.industrial.crc.CrcLayout.FACE_WIDTH;
import static dev.jstech.industrial.crc.CrcLayout.INDEX_BOTTOM;
import static dev.jstech.industrial.crc.CrcLayout.INDEX_TOP;
import static dev.jstech.industrial.crc.CrcLayout.LINE;
import static dev.jstech.industrial.crc.CrcLayout.NOW_PLATE_MOST;
import static dev.jstech.industrial.crc.CrcLayout.NOW_PLATE_Y;
import static dev.jstech.industrial.crc.CrcLayout.NOW_WORD_Y;
import static dev.jstech.industrial.crc.CrcLayout.PLATE_PADDING;
import static dev.jstech.industrial.crc.CrcLayout.PLATE_X;
import static dev.jstech.industrial.crc.CrcLayout.PROGRESS_LAMPS;
import static dev.jstech.industrial.crc.CrcLayout.PROGRESS_PITCH;
import static dev.jstech.industrial.crc.CrcLayout.QUEUE_BUTTON_WIDTH;
import static dev.jstech.industrial.crc.CrcLayout.QUEUE_BUTTON_X;
import static dev.jstech.industrial.crc.CrcLayout.QUEUE_SHOWN;
import static dev.jstech.industrial.crc.CrcLayout.QUEUE_WORD_Y;
import static dev.jstech.industrial.crc.CrcLayout.QUEUE_Y;
import static dev.jstech.industrial.crc.CrcLayout.RESEARCH_BUTTON_WIDTH;
import static dev.jstech.industrial.crc.CrcLayout.RESEARCH_BUTTON_X;
import static dev.jstech.industrial.crc.CrcLayout.STATUS_PITCH;
import static dev.jstech.industrial.crc.CrcLayout.STATUS_ROOM;
import static dev.jstech.industrial.crc.CrcLayout.STATUS_Y;
import static dev.jstech.industrial.crc.CrcLayout.TALL_FACE_HEIGHT;

import dev.jstech.core.client.machine.MachineArt;
import dev.jstech.core.client.machine.MachineWords;
import dev.jstech.core.machine.MachineScreenTexts;
import dev.jstech.core.progression.KnowledgeGate;
import dev.jstech.core.progression.Knowledges;
import dev.jstech.core.research.DataKind;
import dev.jstech.core.research.DataKinds;
import dev.jstech.core.research.Discoveries;
import dev.jstech.core.research.Discovery;
import dev.jstech.core.research.Research;
import dev.jstech.core.research.ResearchCost;
import dev.jstech.core.research.ResearchDataItem;
import dev.jstech.core.research.ResearchMath;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import dev.jstech.industrial.crc.CrcCalls;
import dev.jstech.industrial.crc.CrcLayout;
import dev.jstech.industrial.crc.CrcReadings;
import dev.jstech.industrial.crc.CrcScreenTexts;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * The Research tab, as the approved design draws it: its plates, DISCOVERIES and its team's knowledge; the chart of
 * the discoveries ({@link CrcChart}); the card of the one picked on the chart, what it comes after, the data it asks
 * for against what its team holds, how far the computing is and how long the rest takes, and its two buttons, QUEUE
 * and RESEARCH; under the chart what its group works now, its progress, its pace and the data it spends; and the queue.
 */
final class CrcResearchTab implements ICrcTab {

    private @Nullable ResourceLocation picked;
    private int laneScroll;
    /* How far along its years the chart is dragged: all the way at first, the most recent years in sight. */
    private int pan = CrcLayout.PAN_MOST;
    /* What a drag up or down has moved and not yet turned a lane by. */
    private double draggedDown;
    /* What the picked discovery opens, worked out once a pick: it reads every recipe. */
    private @Nullable ResourceLocation opensOf;
    private List<ItemStack> opens = List.of();

    private static final int NAME_ROOM = CARD_RIGHT - CARD_LEFT - 10;
    private static final int CARD_ROOM = CARD_RIGHT - CARD_LEFT - 6;
    private static final int NEEDS_SHOWN = 2;
    private static final int SOURCES_SHOWN = 2;
    private static final int SECONDS_A_MINUTE = 60;
    private static final int SECONDS_AN_HOUR = 3600;
    private static final int SECONDS_A_DAY = 86_400;

    @Override
    public void draw(final CrcView view) {
        final GuiGraphics g = view.g();
        final CrcArt.Colours c = CrcArt.PALETTE.get();
        final MachineArt.Colours m = MachineArt.PALETTE.get();
        CrcArt.face(g, view.x(FACE_X), view.y(FACE_Y), view.x(FACE_X + FACE_WIDTH), view.y(FACE_Y
                + TALL_FACE_HEIGHT));
        final CrcReadings read = view.read();
        final String team = read.team().isEmpty() ? GameText.resolve(CrcScreenTexts.NOBODY)
                : GameText.resolve(CrcScreenTexts.KNOWLEDGE_OF.with(read.team()));
        CrcTop.plates(view, MachineWords.upper(CrcScreenTexts.DISCOVERIES), MachineWords.upper(Text.literal(team)),
                m.classPlate(), m.classInk());
        CrcTop.lamp(view, CrcLayout.TOP_LAMP_X, c.lampRun(), view.crc().running(), MachineWords.upper(
                MachineScreenTexts.RUN), CrcLayout.TOP_RUN_ROOM);
        CrcTop.lamp(view, CrcLayout.TOP_HOT_X, c.lampHot(), view.hot(), MachineWords.upper(CrcScreenTexts.HOT),
                CrcLayout.TOP_HOT_ROOM);
        final Optional<ResourceLocation> now = view.crc().chosenDiscovery();
        final CrcChart chart = chart(view);
        if (this.picked == null || chart.node(this.picked).isEmpty() && Discoveries.byId(this.picked).isEmpty()) {
            this.picked = now.orElseGet(() -> firstOpen(view));
        }
        chart.draw(view, this.picked);
        card(view);
        now(view, now.flatMap(Discoveries::byId).orElse(null));
        queue(view);
    }

    @Override
    public boolean clicked(final CrcView view, final double x, final double y, final int button) {
        final Optional<CrcChart.Node> node = chart(view).nodeAt(x, y);
        if (node.isPresent()) {
            this.picked = node.get().discovery().id();
            return true;
        }
        final Optional<Discovery> chosen = picked(view);
        if (chosen.isEmpty()) {
            return false;
        }
        if (CrcLayout.on(x, y, QUEUE_BUTTON_X, BUTTON_Y, QUEUE_BUTTON_X + QUEUE_BUTTON_WIDTH, BUTTON_Y
                + BUTTON_HEIGHT) && queueable(view, chosen.get())) {
            CrcCalls.ENQUEUE.send(chosen.get().id());
            return true;
        }
        if (CrcLayout.on(x, y, RESEARCH_BUTTON_X, BUTTON_Y, RESEARCH_BUTTON_X + RESEARCH_BUTTON_WIDTH, BUTTON_Y
                + BUTTON_HEIGHT) && researchable(view, chosen.get())) {
            CrcCalls.CHOOSE.send(chosen.get().id());
            return true;
        }
        return false;
    }

    @Override
    public boolean scrolled(final CrcView view, final double x, final double y, final double amount) {
        if (!CrcLayout.on(x, y, CHART_LEFT, CHART_TOP, CHART_RIGHT, CHART_BOTTOM)) {
            return false;
        }
        final CrcChart chart = chart(view);
        this.laneScroll = Math.clamp(chart.scroll() + (amount > 0 ? -1 : 1), 0, Math.max(0, chart.laneCount()
                - CrcLayout.LANES_SHOWN));
        return true;
    }

    /*
     * A drag on the chart moves it as the hand moves: across, along the years; up or down, a lane at a time once it has
     * gone a lane's height, as the wheel turns them.
     */
    @Override
    public boolean dragged(final CrcView view, final double x, final double y, final double across,
                           final double down) {
        if (!CrcLayout.on(x, y, CHART_LEFT, CHART_TOP, CHART_RIGHT, CHART_BOTTOM)) {
            this.draggedDown = 0.0;
            return false;
        }
        this.pan = Math.clamp(this.pan - (int) Math.round(across), 0, CrcLayout.PAN_MOST);
        this.draggedDown += down;
        final int lanes = (int) (this.draggedDown / CrcLayout.LANE_HEIGHT);
        if (lanes != 0) {
            final CrcChart chart = chart(view);
            this.laneScroll = Math.clamp(chart.scroll() - lanes, 0, Math.max(0, chart.laneCount()
                    - CrcLayout.LANES_SHOWN));
            this.draggedDown -= lanes * CrcLayout.LANE_HEIGHT;
        }
        return true;
    }

    @Override
    public List<Component> tooltip(final CrcView view, final double x, final double y) {
        final Optional<CrcChart.Node> node = chart(view).nodeAt(x, y);
        if (node.isPresent()) {
            final Discovery discovery = node.get().discovery();
            final List<Component> lines = new ArrayList<>();
            lines.add(GameText.component(CrcScreenTexts.TIP_NODE.with(discovery.name().text(),
                    CrcChart.word(node.get().state()).text())));
            if (discovery.when() != null) {
                lines.add(GameText.component(discovery.when()));
            }
            return lines;
        }
        if (CrcLayout.on(x, y, QUEUE_BUTTON_X, BUTTON_Y, QUEUE_BUTTON_X + QUEUE_BUTTON_WIDTH, BUTTON_Y
                + BUTTON_HEIGHT)) {
            return List.of(GameText.component(CrcScreenTexts.TIP_QUEUE));
        }
        if (CrcLayout.on(x, y, RESEARCH_BUTTON_X, BUTTON_Y, RESEARCH_BUTTON_X + RESEARCH_BUTTON_WIDTH, BUTTON_Y
                + BUTTON_HEIGHT)) {
            return List.of(GameText.component(CrcScreenTexts.TIP_RESEARCH_NOW));
        }
        if (CrcLayout.on(x, y, CHART_LEFT, CHART_TOP, CHART_RIGHT, CHART_TOP + 9)) {
            final CrcChart chart = chart(view);
            if (chart.laneCount() > CrcLayout.LANES_SHOWN) {
                return List.of(GameText.component(CrcScreenTexts.MORE_LANES.with(chart.scroll() + 1,
                        chart.scroll() + CrcLayout.LANES_SHOWN, chart.laneCount())));
            }
        }
        return List.of();
    }

    /** {@code seconds} as a player reads a time to come: seconds, minutes, hours and minutes, or days and hours. */
    static Text duration(final double seconds) {
        final long whole = (long) Math.ceil(Math.max(0.0, seconds));
        if (whole < SECONDS_A_MINUTE) {
            return CrcScreenTexts.SECONDS.with(whole);
        }
        if (whole < SECONDS_AN_HOUR) {
            return CrcScreenTexts.MINUTES.with((whole + SECONDS_A_MINUTE - 1) / SECONDS_A_MINUTE);
        }
        if (whole < SECONDS_A_DAY) {
            return CrcScreenTexts.HOURS.with(whole / SECONDS_AN_HOUR, whole % SECONDS_AN_HOUR / SECONDS_A_MINUTE);
        }
        return CrcScreenTexts.DAYS.with(whole / SECONDS_A_DAY, whole % SECONDS_A_DAY / SECONDS_AN_HOUR);
    }

    /** A pace in points a second, as the face shows it: one decimal. */
    static String pace(final double points) {
        return String.format(Locale.ROOT, "%.1f", points);
    }

    /* The chart as the CRC's team stands now. */
    private CrcChart chart(final CrcView view) {
        final Level level = view.crc().getLevel();
        final List<Discovery> discoveries = level == null ? List.of() : Discoveries.existing(level);
        final CrcChart chart = CrcChart.of(discoveries, view.read(), view.crc().chosenDiscovery().orElse(null),
                this.laneScroll, this.pan);
        this.laneScroll = chart.scroll();
        this.pan = chart.pan();
        return chart;
    }

    private Optional<Discovery> picked(final CrcView view) {
        return this.picked == null ? Optional.empty() : Discoveries.byId(this.picked);
    }

    /*
     * The card of the discovery picked: its name, when it was made and what it opens on the index card; what it comes
     * after; the data it asks for against what the team holds; how far the computing is; and the buttons.
     */
    private void card(final CrcView view) {
        final GuiGraphics g = view.g();
        final Font font = view.font();
        final CrcArt.Colours c = CrcArt.PALETTE.get();
        final MachineArt.Colours m = MachineArt.PALETTE.get();
        MachineArt.fill(g, view.x(CARD_LEFT), view.y(CARD_TOP), view.x(CARD_RIGHT), view.y(CARD_BOTTOM),
                MachineArt.mix(c.enamel(), m.black(), 0.12));
        MachineArt.frame(g, view.x(CARD_LEFT), view.y(CARD_TOP), view.x(CARD_RIGHT), view.y(CARD_BOTTOM),
                MachineArt.mix(c.enamel(), m.black(), 0.35));
        CrcArt.engrave(g, font, MachineWords.upper(CrcScreenTexts.CHOSEN), view.x(CARD_LEFT + 3), view.y(CARD_TOP + 3),
                false);
        MachineArt.fill(g, view.x(CARD_LEFT + 3), view.y(INDEX_TOP), view.x(CARD_RIGHT - 3), view.y(INDEX_BOTTOM),
                c.card());
        MachineArt.hline(g, view.x(CARD_LEFT + 3), view.x(CARD_RIGHT - 3), view.y(INDEX_TOP + 9), c.cardRule());
        final Optional<Discovery> shown = picked(view);
        if (shown.isEmpty()) {
            text(view, CrcScreenTexts.NOTHING_CHOSEN.text(), CARD_LEFT + 6, INDEX_TOP + 2, c.pencil(), NAME_ROOM);
            buttons(view, null);
            return;
        }
        final Discovery discovery = shown.get();
        final CrcReadings read = view.read();
        final List<String> name = wrap(font, GameText.resolve(discovery.name()), NAME_ROOM);
        text(view, Text.literal(name.getFirst()), CARD_LEFT + 6, INDEX_TOP + 2, c.ink(), NAME_ROOM);
        if (name.size() > 1) {
            text(view, Text.literal(name.get(1)), CARD_LEFT + 6, INDEX_TOP + 12, c.ink(), NAME_ROOM);
        }
        final Text when = discovery.when() != null ? discovery.when().text() : discovery.year().isPresent()
                ? Text.literal(String.valueOf(discovery.year().getAsInt())) : CrcScreenTexts.NOT_MADE_YET.text();
        text(view, when, CARD_LEFT + 6, INDEX_TOP + 19, c.pencil(), NAME_ROOM);
        final List<ItemStack> opened = opens(view, discovery.id());
        if (!opened.isEmpty()) {
            final Text first = GameText.of(opened.getFirst().getHoverName());
            text(view, opened.size() == 1 ? CrcScreenTexts.UNLOCKS.with(first)
                    : CrcScreenTexts.UNLOCKS_MORE.with(first, opened.size() - 1), CARD_LEFT + 6, INDEX_TOP + 26,
                    c.blue(), NAME_ROOM);
        }
        flow(view, discovery);
        computing(view, discovery, read);
        buttons(view, discovery);
    }

    /* What it comes after, then each kind of data it asks for, down the card as far as there is room. */
    private static void flow(final CrcView view, final Discovery discovery) {
        final CrcArt.Colours c = CrcArt.PALETTE.get();
        final CrcReadings read = view.read();
        int y = CARD_FLOW_TOP;
        CrcArt.engrave(view.g(), view.font(), MachineWords.upper(CrcScreenTexts.NEEDS), view.x(CARD_LEFT + 3),
                view.y(y), false);
        y += 8;
        final List<ResourceLocation> after = discovery.after();
        if (after.isEmpty()) {
            text(view, CrcScreenTexts.NEEDS_NOTHING.text(), CARD_LEFT + 4, y, c.pencil(), CARD_ROOM);
            y += LINE + 1;
        }
        for (int i = 0; i < after.size() && i < NEEDS_SHOWN; i++) {
            if (i == NEEDS_SHOWN - 1 && after.size() > NEEDS_SHOWN) {
                text(view, CrcScreenTexts.MORE.with(after.size() - i), CARD_LEFT + 11, y, c.pencil(), CARD_ROOM);
            } else {
                final ResourceLocation id = after.get(i);
                final boolean known = read.known().contains(id);
                if (known) {
                    CrcArt.tick(view.g(), view.x(CARD_LEFT + 4), view.y(y + 1));
                }
                text(view, knowledgeName(id), CARD_LEFT + 11, y, known ? c.ink() : c.red(), CARD_ROOM - 8);
            }
            y += LINE + 1;
        }
        final Level level = view.crc().getLevel();
        final Optional<ResearchCost> cost = level == null ? Optional.empty() : Discoveries.cost(level,
                discovery.id());
        if (cost.isEmpty() || cost.get().data().isEmpty()) {
            text(view, CrcScreenTexts.NO_DATA_NEEDED.text(), CARD_LEFT + 3, y + 2, c.pencil(), CARD_ROOM);
            return;
        }
        final double share = read.share(discovery.id());
        double reach = 1.0;
        DataKind shortest = null;
        int shortBy = 0;
        for (final Map.Entry<ResourceLocation, Integer> asked : cost.get().data().entrySet()) {
            final Optional<DataKind> kind = DataKinds.byId(asked.getKey());
            final int count = asked.getValue();
            final int covered = Math.min(count, ResearchMath.spent(share, count) + read.held(asked.getKey()));
            if (y + 2 * LINE + 4 > CARD_FLOW_BOTTOM) {
                break;
            }
            CrcArt.engrave(view.g(), view.font(), MachineArt.fit(view.font(), MachineWords.upper(kind.map(
                    DataKind::name).map(TextKey::text).orElse(Text.literal(asked.getKey().getPath()))), CARD_ROOM),
                    view.x(CARD_LEFT + 3), view.y(y), false);
            y += 8;
            final boolean lacking = covered < count;
            CrcArt.counter(view.g(), view.font(), view.x(CARD_LEFT + 4), view.y(y), CrcArt.digits(covered,
                    Math.max(2, String.valueOf(count).length())), lacking);
            final int ofX = CARD_LEFT + 4 + Math.max(2, String.valueOf(count).length()) * DIGIT + 2;
            CrcArt.engrave(view.g(), view.font(), GameText.resolve(CrcScreenTexts.DATA_OF.with(count)),
                    view.x(ofX), view.y(y + 2), false);
            MachineArt.lamp(view.g(), view.x(CARD_RIGHT - 11), view.y(y + 1), lacking ? c.lampHot() : c.lampRun(),
                    true);
            y += 13;
            final double kindReach = covered / (double) count;
            if (kindReach < reach) {
                reach = kindReach;
                shortest = kind.orElse(null);
                shortBy = count - covered;
            }
        }
        if (shortest == null) {
            text(view, CrcScreenTexts.ENOUGH.text(), CARD_LEFT + 3, y, c.green(), CARD_ROOM);
            return;
        }
        if (y + LINE <= CARD_FLOW_BOTTOM + 2) {
            text(view, CrcScreenTexts.ENOUGH_TO.with((int) Math.floor(reach * 100), shortBy), CARD_LEFT + 3, y,
                    c.red(), CARD_ROOM);
            y += LINE;
        }
        for (final Text source : sources(shortest)) {
            if (y + LINE > CARD_FLOW_BOTTOM + 2) {
                break;
            }
            text(view, source, CARD_LEFT + 3, y, c.ink(), CARD_ROOM);
            y += LINE;
        }
    }

    /* How far the computing is, of how many points, and how long the rest takes at its team's pace. */
    private static void computing(final CrcView view, final Discovery discovery, final CrcReadings read) {
        final CrcArt.Colours c = CrcArt.PALETTE.get();
        final MachineArt.Colours m = MachineArt.PALETTE.get();
        final Level level = view.crc().getLevel();
        final long points = level == null ? 0L : Discoveries.cost(level, discovery.id()).map(ResearchCost::points)
                .orElse(0L);
        final double share = read.share(discovery.id());
        CrcArt.engrave(view.g(), view.font(), MachineWords.upper(CrcScreenTexts.COMPUTING), view.x(CARD_LEFT + 3),
                view.y(COMPUTING_Y), false);
        final int percent = (int) Math.floor(share * 100);
        final String digits = CrcArt.digits(percent, percent >= 100 ? 3 : 2);
        CrcArt.counter(view.g(), view.font(), view.x(CARD_LEFT + 4), view.y(COMPUTING_Y + 8), digits, false);
        CrcArt.engrave(view.g(), view.font(), GameText.resolve(CrcScreenTexts.PERCENT_OF.with(
                String.format(Locale.ROOT, "%,d", points))), view.x(CARD_LEFT + 6 + digits.length() * DIGIT),
                view.y(COMPUTING_Y + 10), false);
        final Text left;
        if (share >= 1.0) {
            left = CrcScreenTexts.MADE_ALREADY.text();
        } else if (read.pace() > 0.0F && discovery.id().equals(view.crc().chosenDiscovery().orElse(null))) {
            left = CrcScreenTexts.AT_PACE.with(pace(read.pace()), duration((1.0 - share) * points / read.pace()));
        } else {
            left = CrcScreenTexts.NO_PACE.text();
        }
        text(view, left, CARD_LEFT + 3, COMPUTING_Y + 21, MachineArt.mix(c.enamel(), m.black(), 0.6), CARD_ROOM);
    }

    /* QUEUE and RESEARCH, each greyed when it would do nothing. */
    private void buttons(final CrcView view, final @Nullable Discovery discovery) {
        CrcArt.pushButton(view.g(), view.font(), view.x(QUEUE_BUTTON_X), view.y(BUTTON_Y), QUEUE_BUTTON_WIDTH,
                MachineWords.upper(CrcScreenTexts.QUEUE), false, discovery != null && queueable(view, discovery));
        CrcArt.pushButton(view.g(), view.font(), view.x(RESEARCH_BUTTON_X), view.y(BUTTON_Y), RESEARCH_BUTTON_WIDTH,
                MachineWords.upper(CrcScreenTexts.RESEARCH), true, discovery != null
                        && researchable(view, discovery));
    }

    /*
     * Under the chart: what the group works now on its plate, ten lamps of how far its team is and the counter of
     * it; the group's pace and when it ends; and the data it spends.
     */
    private static void now(final CrcView view, final @Nullable Discovery now) {
        final GuiGraphics g = view.g();
        final Font font = view.font();
        final CrcArt.Colours c = CrcArt.PALETTE.get();
        final MachineArt.Colours m = MachineArt.PALETTE.get();
        final CrcReadings read = view.read();
        CrcArt.engrave(g, font, MachineWords.upper(CrcScreenTexts.NOW), view.x(PLATE_X), view.y(NOW_WORD_Y), false);
        final Text named = now == null ? CrcScreenTexts.NOTHING_NOW.text() : now.year().isPresent()
                ? CrcScreenTexts.NAMED_YEAR.with(now.name().text(), now.year().getAsInt()) : now.name().text();
        final int w = MachineArt.plate(g, font, view.x(PLATE_X), view.y(NOW_PLATE_Y), MachineArt.fit(font,
                MachineWords.upper(named), NOW_PLATE_MOST - PLATE_PADDING), m.aluminium(), m.plateInk());
        final double share = now == null ? 0.0 : read.share(now.id());
        final int lampsX = PLATE_X + w + 6;
        for (int k = 0; k < PROGRESS_LAMPS; k++) {
            MachineArt.lamp(g, view.x(lampsX + k * PROGRESS_PITCH), view.y(NOW_PLATE_Y + 2), c.lampWait(),
                    k < (int) Math.floor(share * PROGRESS_LAMPS));
        }
        CrcArt.counter(g, font, view.x(lampsX + 92), view.y(NOW_PLATE_Y + 1), CrcArt.digits(Math.min(99,
                (long) Math.floor(share * 100)), 2), false);
        CrcArt.engrave(g, font, "%", view.x(lampsX + 105), view.y(NOW_PLATE_Y + 3), false);
        final Text first;
        final Text second;
        if (now == null) {
            first = CrcScreenTexts.NOT_COMPUTING.text();
            second = Text.EMPTY;
        } else {
            first = paceLine(view, now, share);
            second = spendLine(view, now);
        }
        CrcArt.engrave(g, font, MachineWords.upper(first), view.x(PLATE_X), view.y(STATUS_Y), STATUS_ROOM);
        CrcArt.engrave(g, font, MachineWords.upper(second), view.x(PLATE_X), view.y(STATUS_Y + STATUS_PITCH),
                STATUS_ROOM);
    }

    /* The group's pace on what runs now and when it ends, or why it does not go on. */
    private static Text paceLine(final CrcView view, final Discovery now, final double share) {
        final CrcReadings read = view.read();
        final Research.Outcome outcome = read.outcome().orElse(null);
        if (outcome == Research.Outcome.NEEDS_DATA) {
            return CrcScreenTexts.WAITING_FOR_DATA.text();
        }
        if (outcome == Research.Outcome.LOCKED) {
            return CrcScreenTexts.WAITING_FOR_NEEDS.text();
        }
        if (outcome == Research.Outcome.ABSENT) {
            return CrcScreenTexts.NOT_THERE.text();
        }
        if (read.pace() <= 0.0F) {
            return CrcScreenTexts.NOT_COMPUTING.text();
        }
        final Level level = view.crc().getLevel();
        final long points = level == null ? 0L : Discoveries.cost(level, now.id()).map(ResearchCost::points)
                .orElse(0L);
        final Text ends = duration((1.0 - share) * points / read.pace());
        final int crcs = read.group().members().size();
        return crcs > 1 ? CrcScreenTexts.GROUP_PACE.with(crcs, pace(read.pace()), ends)
                : CrcScreenTexts.ALONE_PACE.with(pace(read.pace()), ends);
    }

    /* The data what runs now spends: the first kind it asks for, one every so much of the way, and how many held. */
    private static Text spendLine(final CrcView view, final Discovery now) {
        final Level level = view.crc().getLevel();
        final Optional<ResearchCost> cost = level == null ? Optional.empty() : Discoveries.cost(level, now.id());
        if (cost.isEmpty() || cost.get().data().isEmpty()) {
            return CrcScreenTexts.NO_DATA_NEEDED.text();
        }
        final Map.Entry<ResourceLocation, Integer> first = cost.get().data().entrySet().iterator().next();
        final Text kind = DataKinds.byId(first.getKey()).map(DataKind::name).map(TextKey::text)
                .orElse(Text.literal(first.getKey().getPath()));
        final String every = String.format(Locale.ROOT, "%.1f", 100.0 / first.getValue()).replace(".0", "");
        return CrcScreenTexts.SPENT_EVERY.with(kind, every, view.read().held(first.getKey()));
    }

    /* The queue, under the card: the first ones, and how many more. */
    private static void queue(final CrcView view) {
        final CrcArt.Colours c = CrcArt.PALETTE.get();
        CrcArt.engrave(view.g(), view.font(), MachineWords.upper(CrcScreenTexts.QUEUE), view.x(CARD_LEFT),
                view.y(QUEUE_WORD_Y), false);
        final List<ResourceLocation> queue = view.crc().queue();
        if (queue.isEmpty()) {
            text(view, CrcScreenTexts.QUEUE_EMPTY.text(), CARD_LEFT, QUEUE_Y, c.pencil(), CARD_RIGHT - CARD_LEFT);
            return;
        }
        for (int i = 0; i < queue.size() && i < QUEUE_SHOWN; i++) {
            final Text line = i == QUEUE_SHOWN - 1 && queue.size() > QUEUE_SHOWN
                    ? CrcScreenTexts.MORE.with(queue.size() - i)
                    : CrcScreenTexts.QUEUE_LINE.with(i + 1, Discoveries.byId(queue.get(i)).map(Discovery::name)
                    .map(TextKey::text).orElse(Text.literal(queue.get(i).toString())));
            text(view, line, CARD_LEFT, QUEUE_Y + i * LINE, c.ink(), CARD_RIGHT - CARD_LEFT);
        }
    }

    /* What {@code id} opens, worked out again only when the pick changes. */
    private List<ItemStack> opens(final CrcView view, final ResourceLocation id) {
        final Level level = view.crc().getLevel();
        if (!id.equals(this.opensOf) && level != null) {
            this.opensOf = id;
            this.opens = KnowledgeGate.opens(level, id);
        }
        return this.opens;
    }

    /* Whether QUEUE does anything for {@code discovery}: its team has not made it, and it is not chosen or queued. */
    private static boolean queueable(final CrcView view, final Discovery discovery) {
        return !view.read().team().isEmpty() && !view.read().made(discovery.id())
                && !discovery.id().equals(view.crc().chosenDiscovery().orElse(null))
                && !view.crc().queue().contains(discovery.id());
    }

    /* Whether RESEARCH does anything for {@code discovery}: its team may start it, and it is not chosen already. */
    private static boolean researchable(final CrcView view, final Discovery discovery) {
        return !view.read().team().isEmpty() && !view.read().made(discovery.id()) && view.read().ready(discovery)
                && !discovery.id().equals(view.crc().chosenDiscovery().orElse(null));
    }

    /* The first discovery its team may start, to show before anything is picked; none when there is none. */
    private static @Nullable ResourceLocation firstOpen(final CrcView view) {
        final Level level = view.crc().getLevel();
        if (level == null) {
            return null;
        }
        for (final Discovery discovery : Discoveries.existing(level)) {
            if (CrcChart.stateOf(discovery, view.read(), null) == CrcChart.State.OPEN) {
                return discovery.id();
            }
        }
        return null;
    }

    /* What gives data of {@code kind}, as its items say: "From the X-ray Diffractometer". */
    private static List<Text> sources(final @Nullable DataKind kind) {
        final List<Text> sources = new ArrayList<>();
        if (kind == null) {
            return sources;
        }
        for (final Item item : BuiltInRegistries.ITEM) {
            if (item instanceof ResearchDataItem data && data.dataKind().equals(kind) && data.from().isPresent()
                    && sources.size() < SOURCES_SHOWN) {
                sources.add(data.from().get().text());
            }
        }
        return sources;
    }

    /* The name of a piece of knowledge something comes after: a discovery's, a knowledge's, or its id. */
    private static Text knowledgeName(final ResourceLocation id) {
        return Discoveries.byId(id).map(Discovery::name).map(TextKey::text)
                .or(() -> Knowledges.byId(id).map(known -> known.name().text()))
                .orElse(Text.literal(id.getPath()));
    }

    /* {@code text} cut to {@code room} pixels at ({@code x}, {@code y}) of the screen. */
    private static void text(final CrcView view, final Text text, final int x, final int y, final int colour,
                             final int room) {
        MachineArt.text(view.g(), view.font(), MachineArt.fit(view.font(), GameText.resolve(text), room),
                view.x(x), view.y(y), colour);
    }

    /* {@code text} in lines of at most {@code room} pixels at the face's small size. */
    private static List<String> wrap(final Font font, final String text, final int room) {
        final List<String> lines = new ArrayList<>();
        final StringBuilder line = new StringBuilder();
        for (final String word : text.split(" ")) {
            final String longer = line.isEmpty() ? word : line + " " + word;
            if (!line.isEmpty() && MachineArt.width(font, longer) > room) {
                lines.add(line.toString());
                line.setLength(0);
                line.append(word);
            } else {
                line.setLength(0);
                line.append(longer);
            }
        }
        if (!line.isEmpty() || lines.isEmpty()) {
            lines.add(line.toString());
        }
        return lines;
    }
}
