/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.rocket;

import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.space.client.flight.LandingMapsClient;
import dev.jstech.space.flight.FlightComputerBlockEntity;
import dev.jstech.space.flight.FlightPage;
import dev.jstech.space.flight.TransferPlan;
import dev.jstech.space.orbit.Orbit;
import dev.jstech.space.orbit.SolarSystemOrbits;
import dev.jstech.space.orbit.Transfers;
import dev.jstech.space.rocket.CabinReadings;
import dev.jstech.space.universe.SolarBody;
import dev.jstech.space.universe.SolarSystemWorlds;
import dev.jstech.space.universe.SphereMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/**
 * The Flight Computer's two open pages, as the approved display draws them. The transfer: the system round the Sun, its
 * rings of distance, the Overworld's and Mars' orbits in their colours and the worlds where they stand now, the
 * transfer's arc from where the world left will be at the window to where the destination will be on arrival; beside
 * it the plan's figures; under it the windows' tape, now and the next window marked, and what the plan says. The
 * landing: the map of the world the transfer goes to, drawn as the game's map item draws a world, the spot marked with
 * the ellipse the rocket comes down in round it, north, the scale, and where the spot is.
 */
@TextHolder
public final class FlightComputerPages {

    public static final TextKey TARGET = TextKey.of("jsspace.flight_computer.target", "TGT");
    public static final TextKey PHASE = TextKey.of("jsspace.flight_computer.phase", "PHASE");
    public static final TextKey DELTA_V_NOW = TextKey.of("jsspace.flight_computer.delta_v_now", "DV NOW");
    public static final TextKey DELTA_V_WINDOW = TextKey.of("jsspace.flight_computer.delta_v_window", "DV WIN");
    public static final TextKey ASSIST = TextKey.of("jsspace.flight_computer.assist", "ASSIST");
    public static final TextKey TIME_OF_FLIGHT = TextKey.of("jsspace.flight_computer.time_of_flight", "TOF");
    public static final TextKey ARRIVE = TextKey.of("jsspace.flight_computer.arrive", "ARRIVE");
    public static final TextKey SHIELD = TextKey.of("jsspace.flight_computer.shield", "SHIELD");
    public static final TextKey AVAILABLE = TextKey.of("jsspace.flight_computer.available", "AVAIL");
    public static final TextKey NONE = TextKey.of("jsspace.flight_computer.none", "NONE");
    public static final TextKey AEROBRAKE = TextKey.of("jsspace.flight_computer.aerobrake", "AERO");
    public static final TextKey BURN = TextKey.of("jsspace.flight_computer.burn", "BURN");
    public static final TextKey NO_READING = TextKey.of("jsspace.flight_computer.no_reading", "--");
    public static final TextKey SPEED = TextKey.of("jsspace.flight_computer.speed", "%s M/S");
    public static final TextKey MINUTES = TextKey.of("jsspace.flight_computer.minutes", "%s MIN");
    public static final TextKey PHASES = TextKey.of("jsspace.flight_computer.phases", "%s/%s");
    public static final TextKey DEPARTURE = TextKey.of("jsspace.flight_computer.departure", "DEP");
    public static final TextKey AT_ARRIVAL = TextKey.of("jsspace.flight_computer.at_arrival", "%s AT ARR");
    public static final TextKey NEXT_WINDOW = TextKey.of("jsspace.flight_computer.next_window", "NEXT WIN");
    public static final TextKey NOW = TextKey.of("jsspace.flight_computer.now", "NOW");
    public static final TextKey PLAN_IN_WINDOW = TextKey.of("jsspace.flight_computer.plan_in_window",
            "PLAN OK  IN WINDOW");
    public static final TextKey PLAN_WAIT = TextKey.of("jsspace.flight_computer.plan_wait", "PLAN OK  WAIT FOR WIN");
    public static final TextKey PLAN_ANY_TIME = TextKey.of("jsspace.flight_computer.plan_any_time",
            "PLAN OK  ANY TIME");
    public static final TextKey NO_PLAN = TextKey.of("jsspace.flight_computer.no_plan", "NO TRANSFER FROM HERE");
    public static final TextKey WINDOW_EARLIER = TextKey.of("jsspace.flight_computer.window_earlier", "WIN -");
    public static final TextKey WINDOW_LATER = TextKey.of("jsspace.flight_computer.window_later", "WIN +");
    public static final TextKey KEY_DELTA_V = TextKey.of("jsspace.flight_computer.key_delta_v", "DV");
    public static final TextKey KEY_TIME_OF_FLIGHT = TextKey.of("jsspace.flight_computer.key_time_of_flight", "TOF");
    public static final TextKey KEY_ASSIST = TextKey.of("jsspace.flight_computer.key_assist", "ASSIST");
    public static final TextKey KEY_AEROBRAKE = TextKey.of("jsspace.flight_computer.key_aerobrake", "AERO");
    public static final TextKey EXECUTE = TextKey.of("jsspace.flight_computer.execute", "EXEC");
    public static final TextKey ZOOM_IN = TextKey.of("jsspace.flight_computer.zoom_in", "ZOOM +");
    public static final TextKey ZOOM_OUT = TextKey.of("jsspace.flight_computer.zoom_out", "ZOOM -");
    public static final TextKey MARK = TextKey.of("jsspace.flight_computer.mark", "MARK");
    public static final TextKey CLEAR = TextKey.of("jsspace.flight_computer.clear", "CLEAR");
    public static final TextKey GUIDE = TextKey.of("jsspace.flight_computer.guide", "GUIDE");
    public static final TextKey HAZARD = TextKey.of("jsspace.flight_computer.hazard", "HAZARD");
    public static final TextKey GRID = TextKey.of("jsspace.flight_computer.grid", "GRID");
    public static final TextKey NORTH = TextKey.of("jsspace.flight_computer.north", "NORTH");
    public static final TextKey SET = TextKey.of("jsspace.flight_computer.set", "SET");
    public static final TextKey ELLIPSE = TextKey.of("jsspace.flight_computer.ellipse", "ELLIPSE %s X %s KM");
    public static final TextKey GUIDANCE = TextKey.of("jsspace.flight_computer.guidance", "GUIDE T1");
    public static final TextKey PLACE = TextKey.of("jsspace.flight_computer.place", "X %s  Z %s");
    public static final TextKey NORTH_LETTER = TextKey.of("jsspace.flight_computer.north_letter", "N");
    public static final TextKey BLOCKS = TextKey.of("jsspace.flight_computer.blocks", "%s BLK");
    public static final TextKey MAP_COMING = TextKey.of("jsspace.flight_computer.map_coming", "MAP COMING");
    public static final TextKey LATITUDE_NORTH = TextKey.of("jsspace.flight_computer.latitude_north", "%sN");
    public static final TextKey LATITUDE_SOUTH = TextKey.of("jsspace.flight_computer.latitude_south", "%sS");
    public static final TextKey LONGITUDE_EAST = TextKey.of("jsspace.flight_computer.longitude_east", "%sE");
    public static final TextKey LONGITUDE_WEST = TextKey.of("jsspace.flight_computer.longitude_west", "%sW");
    public static final TextKey WHERE = TextKey.of("jsspace.flight_computer.where", "%s  %s %s");
    /** The worlds as the computer's screen names them, short. */
    public static final TextKey OVERWORLD = TextKey.of("jsspace.flight_computer.world.overworld", "OVERWORLD");
    public static final TextKey MOON = TextKey.of("jsspace.flight_computer.world.moon", "MOON");
    public static final TextKey MARS = TextKey.of("jsspace.flight_computer.world.mars", "MARS");

    private static final int SX0 = FlightComputerFace.SX0;
    private static final int SY0 = FlightComputerFace.SY0;
    private static final int SX1 = FlightComputerFace.SX1;
    private static final int SY1 = FlightComputerFace.SY1;
    /* The system: the Sun's place, the rings of distance, and the Overworld's and Mars' orbits, in pixels. */
    private static final int SUN_X = SX0 + 96;
    private static final int SUN_Y = SY0 + 86;
    private static final int RING_STEP = 18;
    private static final int RING_MOST = 160;
    private static final int OVERWORLD_RING = 32;
    private static final int MARS_RING = 52;
    private static final int ARC_STEPS = 96;
    /* The plan's figures, the rows of the block beside the system, drawn small. */
    private static final int DATA_X0 = SX0 + 150;
    private static final int DATA_X1 = SX1 - 46;
    private static final int ROW = 12;
    private static final float SMALL = 0.8F;
    /* The windows' tape: its row, its ends, how many ticks it has, and where now stands along it. */
    private static final int TAPE_Y = SY1 - 26;
    private static final int TAPE_X0 = SX0 + 8;
    private static final int TAPE_X1 = SX1 - 8;
    private static final int TAPE_TICKS = 12;
    private static final double TAPE_NOW = 0.55;
    private static final double TAPE_SPAN = 2.2;
    /* The landing map's place on the screen, under the pages' names and over the two rows of figures. */
    private static final int MAP_X0 = SX0;
    private static final int MAP_Y0 = SY0 + 13;
    private static final int MAP_X1 = SX1;
    private static final int MAP_Y1 = SY1 - 28;
    private static final int GRID_STEP = 32;
    private static final int METRES_A_KILOMETRE = 1000;
    private static final int TICKS_A_SECOND = 20;
    private static final int SECONDS_A_MINUTE = 60;
    private static final int TICKS_A_MINUTE = TICKS_A_SECOND * SECONDS_A_MINUTE;

    private FlightComputerPages() {
    }

    /**
     * What {@code computer}'s face shows, its cabin reading {@code readings}: the page open, its keys' legends, the
     * plan, and the landing map once it has come.
     */
    public static FlightComputerFace.View view(final FlightComputerBlockEntity computer, final CabinReadings readings,
                                               final long tick) {
        final List<FlightComputerFace.Legend> left = new ArrayList<>();
        final List<FlightComputerFace.Legend> right = new ArrayList<>();
        final FlightPage page = computer.page();
        if (page == FlightPage.TRANSFER) {
            for (final ResourceLocation world : computer.destinations()) {
                left.add(new FlightComputerFace.Legend(name(world), world.equals(computer.target())
                        ? FlightComputerFace.Legend.State.CHOSEN : FlightComputerFace.Legend.State.LIVE, false));
            }
            left.add(off(WINDOW_EARLIER, false));
            left.add(off(WINDOW_LATER, false));
            for (final TextKey key : List.of(KEY_DELTA_V, KEY_TIME_OF_FLIGHT, KEY_ASSIST, KEY_AEROBRAKE, EXECUTE)) {
                right.add(off(key, false));
            }
        } else if (page == FlightPage.LANDING) {
            left.add(live(ZOOM_IN, computer.zoom() == FlightComputerBlockEntity.NEAR));
            left.add(live(ZOOM_OUT, computer.zoom() == FlightComputerBlockEntity.WIDE));
            left.add(live(MARK, computer.mark() != null));
            left.add(live(CLEAR, false));
            right.add(off(GUIDE, true));
            right.add(off(HAZARD, true));
            right.add(live(GRID, computer.grid()));
            right.add(off(NORTH, true));
            right.add(off(SET, true));
        }
        final LandingMapsClient.Shown map = LandingMapsClient.shown();
        return new FlightComputerFace.View(page, left, right, name(computer.target()), computer.plan(), tick,
                computer.mark(), computer.zoom(), computer.grid(),
                map != null && map.world().equals(computer.target()) ? map : null, readings);
    }

    /**
     * The block of the world the landing map at {@code (x, y)} of the face shows, in its own pixels, to mark there;
     * none off the map, or before it has come.
     */
    public static Optional<BlockPos> onMap(final FlightComputerFace.View view, final double x, final double y) {
        final LandingMapsClient.Shown map = view.map();
        if (map == null || x < MAP_X0 || x >= MAP_X1 || y < MAP_Y0 || y >= MAP_Y1) {
            return Optional.empty();
        }
        final BlockPos centre = centreOf(view);
        return Optional.of(new BlockPos(centre.getX() + (int) Math.round((x - (MAP_X0 + MAP_X1) / 2.0) * map.blocks()),
                0, centre.getZ() + (int) Math.round((y - (MAP_Y0 + MAP_Y1) / 2.0) * map.blocks())));
    }

    /* The transfer page. */
    static void transfer(final GuiGraphics g, final Font font, final FlightComputerFace.View view,
                         final FlightComputerFace.Colours c) {
        g.fill(SX0, SY0 + 12, SX1, SY1, c.space());
        for (int r = RING_STEP; r < RING_MOST; r += RING_STEP) {
            circle(g, SUN_X, SUN_Y, r, c.ring(), false);
        }
        circle(g, SUN_X, SUN_Y, OVERWORLD_RING, mix(c.overworld(), c.space(), 0.35), false);
        circle(g, SUN_X, SUN_Y, MARS_RING, mix(c.mars(), c.space(), 0.35), false);
        final long tick = view.tick();
        final double overworld = Math.toDegrees(SolarSystemOrbits.EARTH.angleAt(tick));
        final double mars = Math.toDegrees(SolarSystemOrbits.MARS.angleAt(tick));
        final Optional<TransferPlan> plan = view.plan();
        if (plan.isPresent() && plan.get().windowed()) {
            arc(g, plan.get(), tick, c);
        }
        final int ox = (int) Math.round(atX(OVERWORLD_RING, overworld));
        final int oy = (int) Math.round(atY(OVERWORLD_RING, overworld));
        final int mx = (int) Math.round(atX(MARS_RING, mars));
        final int my = (int) Math.round(atY(MARS_RING, mars));
        // The worlds' names first, so the Sun and the worlds stand over them: the Overworld's and the Moon's beside
        // it, on its left where its right would run into the figures.
        final String overworldName = name(SolarSystemWorlds.OVERWORLD);
        final String moonName = name(SolarSystemWorlds.MOON);
        final boolean room = ox + 10 + Math.max(font.width(overworldName), font.width(moonName)) < DATA_X0 - 6;
        backed(g, font, overworldName, room ? ox + 10 : ox - 10 - font.width(overworldName), oy - 2, c.ink(),
                c.space());
        backed(g, font, moonName, room ? ox + 10 : ox - 10 - font.width(moonName), oy - 13, c.moon(), c.space());
        FlightComputerFace.text(g, font, name(SolarSystemWorlds.MARS), mx - 2 - font.width(
                name(SolarSystemWorlds.MARS)), my + 2, c.mars(), false);
        g.fill(SUN_X - 2, SUN_Y - 2, SUN_X + 3, SUN_Y + 3, c.sun());
        for (final int[] glow : new int[][] {{-3, 0}, {3, 0}, {0, -3}, {0, 3}}) {
            g.fill(SUN_X + glow[0], SUN_Y + glow[1], SUN_X + glow[0] + 1, SUN_Y + glow[1] + 1,
                    mix(c.sun(), c.space(), 0.5));
        }
        disc(g, ox, oy, 3, c.overworld(), c.white());
        circle(g, ox, oy, 7, mix(c.ink(), c.space(), 0.6), false);
        g.fill(ox + 5, oy - 6, ox + 7, oy - 4, c.moon());
        disc(g, mx, my, 2, c.mars(), c.white());
        if (plan.isEmpty()) {
            FlightComputerFace.text(g, font, GameText.resolve(NO_PLAN), SX1 - 8 - font.width(GameText.resolve(
                    NO_PLAN)), SY1 - 10, c.amber(), false);
            return;
        }
        figures(g, font, view, plan.get(), c);
        tape(g, font, plan.get(), c);
    }

    /* The landing page. */
    static void landing(final GuiGraphics g, final Font font, final FlightComputerFace.View view,
                        final FlightComputerFace.Colours c) {
        g.fill(SX0, SY0 + 12, SX1, SY1, c.space());
        final LandingMapsClient.Shown map = view.map();
        if (map == null) {
            final String coming = GameText.resolve(MAP_COMING);
            FlightComputerFace.text(g, font, coming, (MAP_X0 + MAP_X1) / 2, (MAP_Y0 + MAP_Y1) / 2 - 4, c.cyan(),
                    true);
            return;
        }
        final BlockPos centre = centreOf(view);
        final int wide = MAP_X1 - MAP_X0;
        final int tall = MAP_Y1 - MAP_Y0;
        final double u = map.size() / 2.0 + (double) (centre.getX() - map.centreX()) / map.blocks() - wide / 2.0;
        final double v = map.size() / 2.0 + (double) (centre.getZ() - map.centreZ()) / map.blocks() - tall / 2.0;
        g.blit(map.texture(), MAP_X0, MAP_Y0, wide, tall, (float) u, (float) v, wide, tall, map.size(), map.size());
        if (view.grid()) {
            for (int x = MAP_X0 + GRID_STEP; x < MAP_X1; x += GRID_STEP) {
                FlightComputerFace.vline(g, x, MAP_Y0, MAP_Y1, c.backing());
            }
            for (int y = MAP_Y0 + GRID_STEP; y < MAP_Y1; y += GRID_STEP) {
                FlightComputerFace.hline(g, MAP_X0, MAP_X1, y, c.backing());
            }
        }
        final int kx = (MAP_X0 + MAP_X1) / 2;
        final int ky = (MAP_Y0 + MAP_Y1) / 2;
        if (view.mark() != null) {
            final double a = FlightComputerBlockEntity.ELLIPSE_LENGTH / 2.0 / map.blocks();
            final double b = FlightComputerBlockEntity.ELLIPSE_WIDTH / 2.0 / map.blocks();
            for (int i = 0; i < RING_MOST; i++) {
                if ((i / 4) % 2 == 1) {
                    continue;
                }
                final double t = 2 * Math.PI * i / RING_MOST;
                dot(g, (int) Math.round(kx + a * Math.cos(t)), (int) Math.round(ky + b * Math.sin(t)), c.cyan());
            }
            FlightComputerFace.hline(g, kx - 4, kx + 5, ky, c.white());
            FlightComputerFace.vline(g, kx, ky - 4, ky + 5, c.white());
            backed(g, font, GameText.resolve(MARK), kx + 8, ky - 13, c.white(), c.backing());
        }
        backed(g, font, GameText.resolve(WHERE.with(view.target(), latitude(centre, map), longitude(centre, map))),
                MAP_X0 + 44, MAP_Y0 + 4, c.white(), c.backing());
        north(g, font, c);
        scale(g, font, map, c);
        final int rows = SY1 - 24;
        FlightComputerFace.text(g, font, GameText.resolve(ELLIPSE.with(kilometres(
                FlightComputerBlockEntity.ELLIPSE_LENGTH), kilometres(FlightComputerBlockEntity.ELLIPSE_WIDTH))),
                SX0 + 6, rows, c.cyan(), false);
        FlightComputerFace.text(g, font, GameText.resolve(GUIDANCE), SX0 + 6, rows + 11, c.cyan(), false);
        final String place = GameText.resolve(PLACE.with(centre.getX(), centre.getZ()));
        FlightComputerFace.text(g, font, place, SX1 - 6 - font.width(place), rows + 11, c.white(), false);
    }

    /* The plan's figures in their block beside the system. */
    private static void figures(final GuiGraphics g, final Font font, final FlightComputerFace.View view,
                                final TransferPlan plan, final FlightComputerFace.Colours c) {
        final String unknown = GameText.resolve(NO_READING);
        final String[][] rows = {
            {GameText.resolve(TARGET), view.target()},
            {GameText.resolve(PHASE), plan.windowed() ? GameText.resolve(PHASES.with(Math.round(plan.phase()),
                Math.round(plan.lead()))) : unknown},
            {GameText.resolve(DELTA_V_NOW), GameText.resolve(SPEED.with(Math.round(plan.deltaVNow())))},
            {GameText.resolve(DELTA_V_WINDOW), GameText.resolve(SPEED.with(Math.round(plan.deltaVWindow())))},
            {GameText.resolve(ASSIST), GameText.resolve(NONE)},
            {GameText.resolve(TIME_OF_FLIGHT), GameText.resolve(MINUTES.with(Math.max(1, plan.travel()
                / TICKS_A_MINUTE)))},
            {GameText.resolve(ARRIVE), GameText.resolve(plan.aerobraking() ? AEROBRAKE : BURN)},
            {GameText.resolve(SHIELD), unknown},
            {GameText.resolve(AVAILABLE), unknown}};
        final int[] colours = {colourOf(plan.to(), c), c.white(), c.white(), c.green(), c.white(), c.white(),
            c.white(), c.white(), c.white()};
        final int bottom = SY0 + 21 + rows.length * ROW;
        g.fill(DATA_X0 - 4, SY0 + 14, DATA_X1 + 3, bottom, c.space());
        FlightComputerFace.frame(g, DATA_X0 - 4, SY0 + 14, DATA_X1 + 3, bottom, c.ring());
        g.pose().pushPose();
        g.pose().translate(DATA_X0, SY0 + 18, 0);
        g.pose().scale(SMALL, SMALL, 1);
        final int across = Math.round((DATA_X1 - DATA_X0) / SMALL);
        for (int i = 0; i < rows.length; i++) {
            final int y = Math.round(i * ROW / SMALL);
            FlightComputerFace.text(g, font, rows[i][0], 0, y, c.cyan(), false);
            FlightComputerFace.text(g, font, rows[i][1], across - font.width(rows[i][1]), y, colours[i], false);
        }
        g.pose().popPose();
    }

    /* The windows' tape, now and the windows on it, how long to the next and what the plan says. */
    private static void tape(final GuiGraphics g, final Font font, final TransferPlan plan,
                             final FlightComputerFace.Colours c) {
        g.fill(SX0, TAPE_Y - 18, SX1, SY1, c.space());
        final String status;
        if (plan.windowed()) {
            final int span = TAPE_X1 - TAPE_X0;
            g.fill(TAPE_X0, TAPE_Y, TAPE_X1, TAPE_Y + 5, c.track());
            for (int k = 0; k < TAPE_TICKS; k++) {
                FlightComputerFace.vline(g, TAPE_X0 + k * span / (TAPE_TICKS - 1), TAPE_Y, TAPE_Y + 5,
                        c.trackTick());
            }
            final double ticks = plan.between() * TAPE_SPAN;
            final double open = Transfers.WINDOW / Math.PI * plan.between() / ticks * span;
            for (int k = -1; k <= 1; k++) {
                final double at = TAPE_NOW + (plan.toWindow() + (double) k * plan.between()) / ticks;
                if (at < 0 || at > 1) {
                    continue;
                }
                final int x = TAPE_X0 + (int) Math.round(at * span);
                g.fill(x, TAPE_Y, Math.min(TAPE_X1, x + Math.max(2, (int) Math.round(open))), TAPE_Y + 5,
                        k == 0 ? c.windowNext() : c.windowPast());
            }
            final int now = TAPE_X0 + (int) Math.round(TAPE_NOW * span);
            FlightComputerFace.vline(g, now, TAPE_Y - 2, TAPE_Y + 8, c.magenta());
            FlightComputerFace.text(g, font, GameText.resolve(NOW), now - 22, TAPE_Y + 7, c.magenta(), false);
            final String next = GameText.resolve(NEXT_WINDOW);
            FlightComputerFace.text(g, font, next, TAPE_X0, TAPE_Y - 14, c.cyan(), false);
            FlightComputerFace.text(g, font, clock(plan.toWindow()), TAPE_X0 + font.width(next) + 6, TAPE_Y - 14,
                    c.white(), false);
            status = GameText.resolve(plan.inWindow() ? PLAN_IN_WINDOW : PLAN_WAIT);
        } else {
            status = GameText.resolve(PLAN_ANY_TIME);
        }
        FlightComputerFace.text(g, font, status, SX1 - 8 - font.width(status), SY1 - 10, c.green(), false);
    }

    /*
     * The transfer's arc, dotted: from where the world left will be when the window opens to where the destination
     * will be on arrival, half an ellipse round the Sun; the departure and the arrival ringed with dashes.
     */
    private static void arc(final GuiGraphics g, final TransferPlan plan, final long tick,
                            final FlightComputerFace.Colours c) {
        final Orbit leaving = plan.from() == SolarBody.MARS ? SolarSystemOrbits.MARS : SolarSystemOrbits.EARTH;
        final int from = leaving == SolarSystemOrbits.MARS ? MARS_RING : OVERWORLD_RING;
        final int to = from == MARS_RING ? OVERWORLD_RING : MARS_RING;
        final double departure = Math.toDegrees(leaving.angleAt(tick + plan.toWindow()));
        final double e = (double) (to - from) / (to + from);
        final double semi = (to + from) / 2.0;
        for (int i = 0; i < ARC_STEPS; i++) {
            if ((i / 3) % 2 == 1) {
                continue;
            }
            final double nu = Math.PI * i / ARC_STEPS;
            final double r = semi * (1 - e * e) / (1 + e * Math.cos(nu));
            final double angle = departure + Math.toDegrees(nu);
            dot(g, (int) Math.round(atX(r, angle)), (int) Math.round(atY(r, angle)), c.white());
        }
        circle(g, (int) Math.round(atX(from, departure)), (int) Math.round(atY(from, departure)), 4,
                mix(c.ink(), c.space(), 0.2), true);
        circle(g, (int) Math.round(atX(to, departure + 180)), (int) Math.round(atY(to, departure + 180)), 3,
                colourOf(plan.to(), c), true);
    }

    /* North's arrow over the map's corner. */
    private static void north(final GuiGraphics g, final Font font, final FlightComputerFace.Colours c) {
        final int nx = SX1 - 46;
        final int ny = MAP_Y0 + 18;
        g.fill(nx - 4, ny - 2, nx + 12, ny + 13, c.backing());
        FlightComputerFace.vline(g, nx, ny, ny + 10, c.white());
        dot(g, nx - 1, ny + 1, c.white());
        dot(g, nx + 1, ny + 1, c.white());
        FlightComputerFace.text(g, font, GameText.resolve(NORTH_LETTER), nx + 3, ny + 3, c.white(), false);
    }

    /* The scale's bar: a round count of blocks, a bar as long as that on the map. */
    private static void scale(final GuiGraphics g, final Font font, final LandingMapsClient.Shown map,
                              final FlightComputerFace.Colours c) {
        final int blocks = map.blocks() >= FlightComputerBlockEntity.WIDE_BLOCKS ? 10_000 : 1_000;
        final int bar = blocks / map.blocks();
        final int sx = MAP_X0 + 44;
        final int sy = MAP_Y1 - 8;
        final String label = GameText.resolve(BLOCKS.with(blocks));
        g.fill(sx - 3, sy - 4, sx + bar + 8 + font.width(label), sy + 5, c.backing());
        g.fill(sx, sy, sx + bar, sy + 2, c.white());
        FlightComputerFace.text(g, font, label, sx + bar + 4, sy - 3, c.white(), false);
    }

    /* Where the map looks: the spot marked, or the world's middle. */
    private static BlockPos centreOf(final FlightComputerFace.View view) {
        return view.mark() == null ? BlockPos.ZERO : view.mark();
    }

    private static String latitude(final BlockPos at, final LandingMapsClient.Shown map) {
        final double degrees = SphereMap.latitude(at.getZ(), map.width());
        return GameText.resolve((degrees >= 0 ? LATITUDE_NORTH : LATITUDE_SOUTH).with(tenths(Math.abs(degrees))));
    }

    private static String longitude(final BlockPos at, final LandingMapsClient.Shown map) {
        final double degrees = SphereMap.longitude(at.getX(), map.width());
        return GameText.resolve((degrees >= 0 ? LONGITUDE_EAST : LONGITUDE_WEST).with(tenths(Math.abs(degrees))));
    }

    private static String tenths(final double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static String kilometres(final int metres) {
        return tenths(metres / (double) METRES_A_KILOMETRE);
    }

    /* How long {@code ticks} is, as hours, minutes and seconds of the real time. */
    private static String clock(final long ticks) {
        final long seconds = ticks / TICKS_A_SECOND;
        return String.format(Locale.ROOT, "%02d:%02d:%02d", seconds / 3600, seconds / SECONDS_A_MINUTE
                % SECONDS_A_MINUTE, seconds % SECONDS_A_MINUTE);
    }

    /* The name of the world {@code id}, as the computer's screen writes it, short. */
    private static String name(final ResourceLocation id) {
        final Optional<SolarBody> body = SolarSystemWorlds.bodyOf(id);
        if (body.isEmpty()) {
            return id.getPath().toUpperCase(Locale.ROOT);
        }
        return GameText.resolve(switch (body.get()) {
            case OVERWORLD -> OVERWORLD;
            case MOON -> MOON;
            case MARS -> MARS;
        });
    }

    private static int colourOf(final SolarBody body, final FlightComputerFace.Colours c) {
        return switch (body) {
            case OVERWORLD -> c.overworld();
            case MOON -> c.moon();
            case MARS -> c.mars();
        };
    }

    private static FlightComputerFace.Legend live(final TextKey key, final boolean chosen) {
        return new FlightComputerFace.Legend(GameText.resolve(key), chosen ? FlightComputerFace.Legend.State.CHOSEN
                : FlightComputerFace.Legend.State.LIVE, true);
    }

    private static FlightComputerFace.Legend off(final TextKey key, final boolean backed) {
        return new FlightComputerFace.Legend(GameText.resolve(key), FlightComputerFace.Legend.State.OFF, backed);
    }

    private static void backed(final GuiGraphics g, final Font font, final String text, final int x, final int y,
                               final int colour, final int ground) {
        g.fill(x - 2, y - 1, x + font.width(text) + 1, y + 9, ground);
        FlightComputerFace.text(g, font, text, x, y, colour, false);
    }

    private static double atX(final double r, final double degrees) {
        return SUN_X + r * Math.cos(Math.toRadians(degrees));
    }

    private static double atY(final double r, final double degrees) {
        return SUN_Y - r * Math.sin(Math.toRadians(degrees));
    }

    /* A circle of dots, clipped to the screen under the pages' names; dashed in threes where {@code dashed}. */
    private static void circle(final GuiGraphics g, final int cx, final int cy, final int r, final int colour,
                               final boolean dashed) {
        final int n = Math.max(24, r * 7);
        for (int i = 0; i < n; i++) {
            if (dashed && (i / 3) % 2 == 1) {
                continue;
            }
            final double a = 2 * Math.PI * i / n;
            final int x = (int) Math.round(cx + r * Math.cos(a));
            final int y = (int) Math.round(cy - r * Math.sin(a));
            if (x >= SX0 && x < SX1 && y >= SY0 + 12 && y < SY1 - 36) {
                dot(g, x, y, colour);
            }
        }
    }

    /* A world as a disc, lit on its upper left by {@code lit}. */
    private static void disc(final GuiGraphics g, final int cx, final int cy, final int r, final int colour,
                             final int lit) {
        for (int y = -r; y <= r; y++) {
            for (int x = -r; x <= r; x++) {
                if (x * x + y * y <= r * r + r * 0.6) {
                    dot(g, cx + x, cy + y, x + y < 0 ? mix(colour, lit, 0.35) : colour);
                }
            }
        }
    }

    private static void dot(final GuiGraphics g, final int x, final int y, final int colour) {
        g.fill(x, y, x + 1, y + 1, colour);
    }

    private static int mix(final int a, final int b, final double t) {
        final int r = (int) Math.round((a >> 16 & 0xFF) * (1 - t) + (b >> 16 & 0xFF) * t);
        final int gr = (int) Math.round((a >> 8 & 0xFF) * (1 - t) + (b >> 8 & 0xFF) * t);
        final int bl = (int) Math.round((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
        return 0xFF << 24 | r << 16 | gr << 8 | bl;
    }
}
