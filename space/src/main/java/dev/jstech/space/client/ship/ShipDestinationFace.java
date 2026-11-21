/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.ship;

import dev.jstech.core.text.GameText;
import dev.jstech.space.client.rocket.FlightComputerFace;
import dev.jstech.space.crew.CrewTexts;
import dev.jstech.space.ship.ShipDestination;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.Nullable;

/**
 * The face of a small ship's console on its destination page, as the approved page draws it, in the flight
 * computer's display: the world below along the top and where the ship comes down now; a place a left key, its kind
 * in its colour, its name and how far it is from there, the one chosen boxed, under the ship last; what choosing does;
 * and where the ship comes down. Away from the low orbit of a world, it says a place is picked there.
 */
public final class ShipDestinationFace {

    private static final int WHO_X = FlightComputerFace.SX0 + 8;
    private static final int NAME_X = FlightComputerFace.SX0 + 40;
    private static final int FAR_RIGHT = FlightComputerFace.SX1 - 56;
    /* Where a chosen row's box ends, on the right. */
    private static final int ROW_EDGE = FAR_RIGHT + 5;
    private static final int HEADER = 13;
    private static final int PAD_TOP = FlightComputerFace.SY1 - 16;
    private static final int KEY_LINE = FlightComputerFace.SY1 - 29;
    private static final int PAGE_LINE = FlightComputerFace.SY1 - 41;
    /* The two lines away from the low orbit of a world, and how far apart. */
    private static final int AWAY_LINE = FlightComputerFace.SY0 + 68;
    private static final int AWAY_STEP = 12;
    /* The right key that lets the place chosen go. */
    private static final int CLEAR_KEY = 4;

    private ShipDestinationFace() {
    }

    /** Draws the face across {@code w} by {@code h} as {@code view} says, the face's pixels stretched to fit. */
    public static void paint(final GuiGraphics g, final int w, final int h, final View view) {
        final Font font = Minecraft.getInstance().font;
        final FlightComputerFace.Colours c = FlightComputerFace.PALETTE.get();
        final ShipConsoleFace.Colours own = ShipConsoleFace.PALETTE.get();
        g.pose().pushPose();
        g.pose().scale(w / (float) FlightComputerFace.WIDTH, h / (float) FlightComputerFace.HEIGHT, 1);
        FlightComputerFace.bezel(g, font, c, CrewTexts.MAKER, false, ShipConsoleFace.TOP_KEYS);
        final int sx0 = FlightComputerFace.SX0;
        final int sy0 = FlightComputerFace.SY0;
        final int sx1 = FlightComputerFace.SX1;
        g.fill(sx0, sy0, sx1, FlightComputerFace.SY1, c.screen());
        ShipConsoleFace.top(g, font, true, view.pageIndex(), view.pages(), c);
        // The world below, and where the ship comes down now.
        final String over = GameText.resolve(CrewTexts.OVER);
        FlightComputerFace.text(g, font, over, WHO_X, sy0 + HEADER, c.cyan(), false);
        final boolean overAWorld = !view.world().isEmpty();
        FlightComputerFace.text(g, font, overAWorld ? view.world() : GameText.resolve(CrewTexts.NO_WORLD),
                WHO_X + font.width(over) + 6, sy0 + HEADER, overAWorld ? c.white() : c.engraveDim(), false);
        FlightComputerFace.text(g, font, view.below(), sx1 - 8 - font.width(view.below()), sy0 + HEADER,
                c.windowNext(), false);
        FlightComputerFace.hline(g, sx0 + 4, sx1 - 4, sy0 + HEADER + 10, own.rule());
        if (overAWorld) {
            for (int i = 0; i < view.page().size(); i++) {
                row(g, font, view.page().get(i), i, view.chosen() == i, view.pilots(), c, own);
            }
            FlightComputerFace.text(g, font, GameText.resolve(CrewTexts.PAGE.with(view.pageIndex() + 1,
                    view.pages())), WHO_X, PAGE_LINE, c.windowNext(), false);
            FlightComputerFace.text(g, font, GameText.resolve(CrewTexts.GOING_DOWN), WHO_X, KEY_LINE, c.engrave(),
                    false);
        } else {
            final int middle = (sx0 + sx1) / 2;
            FlightComputerFace.text(g, font, GameText.resolve(CrewTexts.PICKED_IN), middle, AWAY_LINE, c.engrave(),
                    true);
            FlightComputerFace.text(g, font, GameText.resolve(CrewTexts.PICKED_OVER), middle, AWAY_LINE + AWAY_STEP,
                    c.engrave(), true);
        }
        pad(g, font, view, c, own);
        final List<FlightComputerFace.Legend> legends = rightLegends(view);
        for (int i = 0; i < legends.size(); i++) {
            FlightComputerFace.legend(g, font, legends, i, false, c);
        }
        g.pose().popPose();
    }

    /** The right keys' legends: CLEAR on the last, for a pilot over a world. */
    public static List<FlightComputerFace.Legend> rightLegends(final View view) {
        if (!view.pilots() || view.world().isEmpty()) {
            return List.of();
        }
        final List<FlightComputerFace.Legend> out = new ArrayList<>();
        for (int i = 0; i < CLEAR_KEY; i++) {
            out.add(new FlightComputerFace.Legend("", FlightComputerFace.Legend.State.OFF, false));
        }
        out.add(new FlightComputerFace.Legend(GameText.resolve(CrewTexts.CLEAR), FlightComputerFace.Legend.State.LIVE,
                false));
        return out;
    }

    /** Which right key lets the place chosen go. */
    public static int clearKey() {
        return CLEAR_KEY;
    }

    /** The colour of a place of {@code kind} on the page; of under the ship, for none. */
    public static int colourOf(final ShipDestination.@Nullable Kind kind, final FlightComputerFace.Colours c) {
        if (kind == null) {
            return c.legendOff();
        }
        return switch (kind) {
            case DOCK -> c.magenta();
            case PAD -> c.green();
            case BED -> c.cyan();
            case LIFT_OFF -> c.amber();
        };
    }

    /* Row {@code i} beside its left key: its kind in its colour, its name, how far it is; boxed when chosen. */
    private static void row(final GuiGraphics g, final Font font, final Row row, final int i, final boolean chosen,
                            final boolean keyed, final FlightComputerFace.Colours c,
                            final ShipConsoleFace.Colours own) {
        final int y = FlightComputerFace.sideKeyY(i) + 2;
        if (chosen) {
            g.fill(WHO_X - 5, y - 3, ROW_EDGE, y + 11, own.chosenGround());
            FlightComputerFace.frame(g, WHO_X - 5, y - 3, ROW_EDGE, y + 11, c.white());
        }
        if (keyed) {
            FlightComputerFace.hline(g, FlightComputerFace.SX0, FlightComputerFace.SX0 + 4, y + 3, c.cyan());
        }
        FlightComputerFace.text(g, font, row.word(), WHO_X, y, colourOf(row.kind(), c), false);
        FlightComputerFace.text(g, font, row.name(), NAME_X, y, chosen ? c.white() : c.engrave(), false);
        FlightComputerFace.text(g, font, row.far(), FAR_RIGHT - font.width(row.far()), y, chosen ? c.white()
                : c.windowNext(), false);
    }

    /* Where the ship comes down: the place chosen and where it is, or under the ship. */
    private static void pad(final GuiGraphics g, final Font font, final View view, final FlightComputerFace.Colours c,
                            final ShipConsoleFace.Colours own) {
        final int x0 = FlightComputerFace.SX0 + 4;
        final int x1 = FlightComputerFace.SX1 - 4;
        g.fill(x0, PAD_TOP, x1, PAD_TOP + 12, own.padGround());
        FlightComputerFace.frame(g, x0, PAD_TOP, x1, PAD_TOP + 12, c.windowPast());
        final int y = PAD_TOP + 2;
        final boolean chosen = !view.downToAt().isEmpty();
        FlightComputerFace.text(g, font, GameText.resolve(CrewTexts.DOWN_TO.with(view.downTo())), x0 + 4, y,
                chosen ? c.green() : c.windowNext(), false);
        FlightComputerFace.text(g, font, view.downToAt(), x1 - 4 - font.width(view.downToAt()), y, c.white(), false);
    }

    /**
     * A place on the page.
     *
     * @param kind  what it is; none for under the ship
     * @param word  its column's word
     * @param name  its name
     * @param far   how far it is from where the ship comes down now
     * @param place the place itself; none for under the ship
     */
    public record Row(ShipDestination.@Nullable Kind kind, String word, String name, String far,
                      @Nullable ShipDestination place) {
    }

    /**
     * What the face shows.
     *
     * @param world     the name of the world below; empty away from the low orbit of one
     * @param below     where the ship comes down now, as the HUD writes it; empty when unknown
     * @param page      the rows of the page open
     * @param pageIndex which page is open, from nought
     * @param pages     how many pages there are
     * @param chosen    the row of the page open that is chosen, or -1
     * @param pilots    whether the player looking flies the ship, who chooses
     * @param downTo    the name of where the ship comes down
     * @param downToAt  where that is, or empty under the ship
     */
    public record View(String world, String below, List<Row> page, int pageIndex, int pages, int chosen,
                       boolean pilots, String downTo, String downToAt) {

        public View {
            page = List.copyOf(page);
        }
    }
}
