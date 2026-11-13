/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.rocket;

import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.space.JsSpace;
import dev.jstech.space.launch.LaunchCount;
import dev.jstech.space.launch.LaunchTexts;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The Flight Computer's LAUNCH page, open while its rocket stands on a launcher: the Launch Panel's count on the
 * capsule's own screen, the same count, worked from inside. Along its top the pad and its rocket, and whether a Launch
 * Panel by the pad works the count too; the count, the mission's clock and the holds; the sequence with where the count
 * is; the go and no-go board, what the pad has none of dark; the tanks; and along its foot what the count does or waits
 * for. Down the left the panel's switches (the MASTER key, the IGNITION switch with its guard, the deluge, the clamps,
 * HOLD), down the right its buttons (COUNT, RECYCLE, SCRUB, and LAUNCH under its cover till T-10).
 */
@TextHolder
@PaletteHolder
public final class FlightLaunchPage {

    public static final TextKey PANEL = TextKey.of("jsspace.flight_computer.launch.panel", "LCC-1");
    public static final TextKey NO_PANEL = TextKey.of("jsspace.flight_computer.launch.no_panel", "NO PANEL");
    public static final TextKey CLOCK = TextKey.of("jsspace.flight_computer.launch.clock", "T-%s");
    public static final TextKey MET = TextKey.of("jsspace.flight_computer.launch.met", "MET %s");
    public static final TextKey VEHICLE = TextKey.of("jsspace.flight_computer.launch.vehicle", "VEH");
    public static final TextKey PROPELLANT = TextKey.of("jsspace.flight_computer.launch.propellant", "PROP");
    public static final TextKey TOWER = TextKey.of("jsspace.flight_computer.launch.tower", "TOWER");
    public static final TextKey WEATHER = TextKey.of("jsspace.flight_computer.launch.weather", "WX");
    public static final TextKey MASTER_ARMED = TextKey.of("jsspace.flight_computer.launch.master_armed", "MSTR ARM");
    public static final TextKey MASTER_SAFE = TextKey.of("jsspace.flight_computer.launch.master_safe", "MSTR SAFE");
    public static final TextKey IGNITION_ARMED = TextKey.of("jsspace.flight_computer.launch.ignition_armed",
            "IGN ARM");
    public static final TextKey IGNITION_SAFE = TextKey.of("jsspace.flight_computer.launch.ignition_safe",
            "IGN SAFE");
    public static final TextKey DELUGE_AUTO = TextKey.of("jsspace.flight_computer.launch.deluge_auto", "DLG AUTO");
    public static final TextKey DELUGE_OFF = TextKey.of("jsspace.flight_computer.launch.deluge_off", "DLG OFF");
    public static final TextKey CLAMPS_AUTO = TextKey.of("jsspace.flight_computer.launch.clamps_auto", "CLMP AUTO");
    public static final TextKey CLAMPS_OFF = TextKey.of("jsspace.flight_computer.launch.clamps_off", "CLMP OFF");
    public static final TextKey KEY_COUNT = TextKey.of("jsspace.flight_computer.launch.key_count", "COUNT");
    public static final TextKey KEY_RECYCLE = TextKey.of("jsspace.flight_computer.launch.key_recycle", "RECYCLE");
    public static final TextKey RUNNING = TextKey.of("jsspace.flight_computer.launch.running", "COUNT RUNNING");
    public static final TextKey PRESS_LAUNCH = TextKey.of("jsspace.flight_computer.launch.press_launch",
            "PRESS LAUNCH BY T-6 OR THE COUNT HOLDS");
    public static final TextKey HOLDING_FOR_LAUNCH = TextKey.of("jsspace.flight_computer.launch.holding_for_launch",
            "HOLDING AT T-6 FOR LAUNCH");
    public static final TextKey HOLDING = TextKey.of("jsspace.flight_computer.launch.holding", "COUNT HOLDING");
    public static final TextKey SCRUBBED = TextKey.of("jsspace.flight_computer.launch.scrubbed", "SCRUBBED");
    public static final TextKey LIFTOFF = TextKey.of("jsspace.flight_computer.launch.liftoff", "LIFTOFF");
    public static final TextKey ARM_FIRST = TextKey.of("jsspace.flight_computer.launch.arm_first",
            "ARM MASTER AND IGNITION, THEN COUNT");
    public static final TextKey WAIT_GO = TextKey.of("jsspace.flight_computer.launch.wait_go", "WAITING FOR ALL GO");
    public static final TextKey READY = TextKey.of("jsspace.flight_computer.launch.ready", "READY TO COUNT");
    public static final Palette<Colours> PALETTE = Palettes.declare(JsSpace.MODID, "flight_computer_launch",
            new Colours(0xFF0C1418, 0xFF1D2A33, 0xFF4A5A66, 0xFF1E9A40, 0xFF0C2A16, 0xFF33240A, 0xFF3A0E0A,
                    0xFF14191D, 0xFF7AB8E8, 0xFFE8A060));
    /* The middle of the screen, between the side keys' legends. */
    private static final int MX0 = FlightComputerFace.SX0 + 62;
    private static final int MX1 = FlightComputerFace.SX1 - 62;
    private static final int SY0 = FlightComputerFace.SY0;
    /* A cell of the board, two to a row; where a tank's bar starts after its name, and how far it reaches. */
    private static final int CELL = (MX1 - MX0 - 2) / 2;
    private static final int CELL_STEP = 12;
    private static final int BAR_FROM = 22;
    private static final int BAR = 24;
    private static final int SECOND = LaunchCount.SECOND;
    private static final int HUNDRED = 100;

    private FlightLaunchPage() {
    }

    /** Draws the page, showing {@code count}, in the face's own colours {@code c}. */
    public static void launch(final GuiGraphics g, final Font font, final LaunchCount.Shown count,
                              final FlightComputerFace.Colours c) {
        final Colours own = PALETTE.get();
        g.fill(FlightComputerFace.SX0, SY0, FlightComputerFace.SX1, FlightComputerFace.SY1, own.panel());
        top(g, font, count, c, own);
        final LaunchCount.Phase phase = count.phase();
        final boolean counted = phase != LaunchCount.Phase.SAFE && phase != LaunchCount.Phase.SCRUBBED;
        final boolean released = phase == LaunchCount.Phase.RELEASED;
        big(g, font, GameText.resolve(CLOCK.with(clock(released ? 0 : (count.left() + SECOND - 1) / SECOND))),
                MX0 + 6, SY0 + 30, counted ? c.green() : own.greenMid());
        FlightComputerFace.text(g, font, GameText.resolve(MET.with(clock(released ? count.met() / SECOND : 0))), MX0,
                SY0 + 53, c.cyan(), false);
        right(g, font, GameText.resolve(LaunchTexts.HOLDS.with(count.holds())), MX1, SY0 + 53, own.greenMid());
        FlightComputerFace.hline(g, MX0, MX1, SY0 + 64, own.edge());
        sequence(g, font, count, c, own);
        FlightComputerFace.hline(g, MX0, MX1, SY0 + 115, own.edge());
        FlightComputerFace.text(g, font, word(LaunchTexts.GO_NO_GO), MX0, SY0 + 118, own.dim(), false);
        if (count.vehicle() && (count.loaded() || count.oxidiser() + count.fuel() > 0)) {
            right(g, font, word(count.loaded() ? LaunchTexts.LOADED : LaunchTexts.TOPPING_OFF), MX1, SY0 + 118,
                    count.loaded() ? c.green() : c.amber());
        }
        board(g, font, count, c, own);
        FlightComputerFace.hline(g, MX0, MX1, SY0 + 178, own.edge());
        tanks(g, font, count, c, own);
        final TextKey status = status(count);
        FlightComputerFace.text(g, font, word(status), (FlightComputerFace.SX0 + FlightComputerFace.SX1) / 2,
                SY0 + 195, statusColour(status, c), true);
        launchKey(g, font, count, c, own);
    }

    /** The legends of its left keys from the top: the MASTER key, the IGNITION switch, the deluge, the clamps, HOLD. */
    public static List<FlightComputerFace.Legend> left(final LaunchCount.Shown count) {
        final boolean released = count.phase() == LaunchCount.Phase.RELEASED;
        return List.of(
                legend(word(count.master() ? MASTER_ARMED : MASTER_SAFE), released, count.master()),
                legend(word(count.ignition() ? IGNITION_ARMED : IGNITION_SAFE), released, count.ignition()),
                legend(word(count.deluge() ? DELUGE_AUTO : DELUGE_OFF), released, false),
                legend(word(count.clamps() ? CLAMPS_AUTO : CLAMPS_OFF), released, false),
                legend(word(LaunchTexts.HOLD), released, count.phase() == LaunchCount.Phase.HOLDING));
    }

    /** The legends of its right keys, from the top: COUNT, RECYCLE, SCRUB, none, and LAUNCH, which the page draws. */
    public static List<FlightComputerFace.Legend> right(final LaunchCount.Shown count) {
        final boolean released = count.phase() == LaunchCount.Phase.RELEASED;
        return List.of(
                legend(word(KEY_COUNT), released, count.phase() == LaunchCount.Phase.COUNTING),
                legend(word(KEY_RECYCLE), released, false),
                legend(word(LaunchTexts.SCRUB), released, false),
                new FlightComputerFace.Legend("", FlightComputerFace.Legend.State.OFF, false),
                new FlightComputerFace.Legend(word(LaunchTexts.LAUNCH), FlightComputerFace.Legend.State.OWN, false));
    }

    /* What the count does, or what it waits for, along the page's foot. */
    private static TextKey status(final LaunchCount.Shown count) {
        return switch (count.phase()) {
            case RELEASED -> LIFTOFF;
            case SCRUBBED -> SCRUBBED;
            case HOLDING -> count.left() == LaunchCount.LIGHT * SECOND && !count.launched() ? HOLDING_FOR_LAUNCH
                    : HOLDING;
            case COUNTING -> count.coverUp() && !count.launched() ? PRESS_LAUNCH : RUNNING;
            case SAFE -> !count.vehicle() ? LaunchTexts.NO_ROCKET : !count.master() || !count.ignition() ? ARM_FIRST
                    : !count.go() ? WAIT_GO : READY;
        };
    }

    private static int statusColour(final TextKey status, final FlightComputerFace.Colours c) {
        if (status == RUNNING || status == LIFTOFF) {
            return c.green();
        }
        if (status == SCRUBBED) {
            return c.hazard();
        }
        return status == ARM_FIRST || status == READY ? c.cyan() : c.amber();
    }

    /* The pad, by its name or its number, and its rocket; and who else works the count, the panel or the crew alone. */
    private static void top(final GuiGraphics g, final Font font, final LaunchCount.Shown count,
                            final FlightComputerFace.Colours c, final Colours own) {
        final String works = word(count.panel() ? PANEL : NO_PANEL);
        right(g, font, works, MX1, SY0 + 15, count.panel() ? c.green() : c.amber());
        final String pad = !count.padName().isEmpty() ? count.padName().toUpperCase(Locale.ROOT)
                : count.port() ? GameText.resolve(LaunchTexts.PAD.with(String.valueOf(count.padNumber())))
                : word(LaunchTexts.NO_PAD);
        final String rocket = count.rocketName().isEmpty() ? word(LaunchTexts.NO_ROCKET)
                : count.rocketName().toUpperCase(Locale.ROOT);
        // A long name is cut where it would reach the word on its right.
        FlightComputerFace.text(g, font, font.plainSubstrByWidth(pad + "  " + rocket, MX1 - MX0 - font.width(works)
                - 6), MX0, SY0 + 15, c.white(), false);
        FlightComputerFace.hline(g, MX0, MX1, SY0 + 26, own.edge());
    }

    /* The sequence, a line an event: the one the count is at marked, those done dim. */
    private static void sequence(final GuiGraphics g, final Font font, final LaunchCount.Shown count,
                                 final FlightComputerFace.Colours c, final Colours own) {
        final int[] at = {LaunchCount.START, LaunchCount.ARMS, LaunchCount.COVER, LaunchCount.LIGHT, 0};
        final TextKey[] what = {LaunchTexts.ARMS_RETRACT, LaunchTexts.ARMS_MOVING, LaunchTexts.GUARD_LIFTS,
            LaunchTexts.IGNITION_EVENT, LaunchTexts.RELEASE};
        final boolean counted = count.phase() != LaunchCount.Phase.SAFE
                && count.phase() != LaunchCount.Phase.SCRUBBED;
        final double seconds = count.phase() == LaunchCount.Phase.RELEASED ? -1 : count.left() / (double) SECOND;
        int now = -1;
        for (int i = 0; i < at.length; i++) {
            if (counted && seconds <= at[i]) {
                now = i;
            }
        }
        for (int i = 0; i < at.length; i++) {
            final int y = SY0 + 68 + i * 9;
            if (i == now) {
                g.fill(MX0, y - 1, MX1, y + 8, own.edge());
                FlightComputerFace.text(g, font, ">", MX0 + 1, y, c.amber(), false);
            }
            FlightComputerFace.text(g, font, "T-" + at[i], MX0 + 8, y, c.cyan(), false);
            FlightComputerFace.text(g, font, word(what[i]), MX0 + 38, y, i == now ? c.white() : i < now
                    ? own.greenMid() : c.green(), false);
        }
    }

    /* The go and no-go board, two to a row: each system's name and its state, in the panel's colours. */
    private static void board(final GuiGraphics g, final Font font, final LaunchCount.Shown count,
                              final FlightComputerFace.Colours c, final Colours own) {
        final TextKey[][] systems = {
            {VEHICLE, count.vehicle() ? LaunchTexts.GO : LaunchTexts.NO_GO},
            {PROPELLANT, count.loaded() ? LaunchTexts.GO : LaunchTexts.NO_GO},
            {TOWER, count.towerState()},
            {LaunchTexts.DELUGE, count.delugeState()},
            {WEATHER, count.storm() ? LaunchTexts.NO_GO : LaunchTexts.GO},
            {LaunchTexts.CREW, LaunchTexts.GO},
            {LaunchTexts.RANGE, LaunchTexts.GO},
            {LaunchTexts.CLAMPS, count.clamps() ? LaunchTexts.GO : LaunchTexts.OFF}};
        for (int i = 0; i < systems.length; i++) {
            final TextKey state = systems[i][1];
            final int x0 = MX0 + (i % 2) * (CELL + 2);
            final int y0 = SY0 + 128 + (i / 2) * CELL_STEP;
            final boolean good = state == LaunchTexts.GO || state == LaunchTexts.CLEAR;
            final int ground = good ? own.goGround() : state == LaunchTexts.NO_GO ? own.noGoGround()
                    : state == LaunchTexts.NONE ? own.offGround() : own.waitGround();
            final int ink = good ? c.green() : state == LaunchTexts.NO_GO ? c.hazard()
                    : state == LaunchTexts.NONE ? own.dim() : c.amber();
            g.fill(x0, y0, x0 + CELL, y0 + 11, ground);
            FlightComputerFace.frame(g, x0, y0, x0 + CELL, y0 + 11, state == LaunchTexts.NONE ? own.edge() : ink);
            final String said = word(state);
            // A name too long beside its state, in some language, is cut short of it.
            FlightComputerFace.text(g, font, font.plainSubstrByWidth(word(systems[i][0]), CELL - 9 - font.width(said)),
                    x0 + 3, y0 + 2, ink, false);
            right(g, font, said, x0 + CELL - 3, y0 + 2, ink);
        }
    }

    /* The two tanks: each its name, a bar of how full it is, and the share. */
    private static void tanks(final GuiGraphics g, final Font font, final LaunchCount.Shown count,
                              final FlightComputerFace.Colours c, final Colours own) {
        final int half = (MX1 - MX0) / 2;
        final TextKey[] names = {LaunchTexts.OXIDISER, LaunchTexts.FUEL};
        final int[] colours = {own.oxidiser(), own.fuel()};
        final int[] full = {count.oxidiser(), count.fuel()};
        final int y = SY0 + 182;
        for (int i = 0; i < 2; i++) {
            final int x = MX0 + i * half;
            FlightComputerFace.text(g, font, word(names[i]), x, y, colours[i], false);
            g.fill(x + BAR_FROM, y + 2, x + BAR_FROM + BAR, y + 6, own.edge());
            g.fill(x + BAR_FROM, y + 2, x + BAR_FROM + BAR * full[i] / HUNDRED, y + 6, colours[i]);
            right(g, font, full[i] + "%", x + half - 4, y, c.white());
        }
    }

    /* LAUNCH beside its key: dim under its cover till T-10, then lit red as the panel's button. */
    private static void launchKey(final GuiGraphics g, final Font font, final LaunchCount.Shown count,
                                  final FlightComputerFace.Colours c, final Colours own) {
        final String launch = word(LaunchTexts.LAUNCH);
        final int w = font.width(launch);
        final int y = FlightComputerFace.sideKeyY(4) + 2;
        final int x = FlightComputerFace.SX1 - 6 - w;
        if (count.coverUp()) {
            g.fill(x - 2, y - 1, x + w + 1, y + 9, own.noGoGround());
            FlightComputerFace.text(g, font, launch, x, y, c.hazard(), false);
            FlightComputerFace.frame(g, x - 2, y - 1, x + w + 2, y + 9, c.hazard());
        } else {
            FlightComputerFace.text(g, font, launch, x, y, own.dim(), false);
            FlightComputerFace.hline(g, FlightComputerFace.SX1 - 4, FlightComputerFace.SX1, y + 3, own.dim());
        }
    }

    /* A key's legend: dim once the rocket is away, white and boxed when what it sets is chosen. */
    private static FlightComputerFace.Legend legend(final String text, final boolean off, final boolean chosen) {
        final FlightComputerFace.Legend.State state = off ? FlightComputerFace.Legend.State.OFF
                : chosen ? FlightComputerFace.Legend.State.CHOSEN : FlightComputerFace.Legend.State.LIVE;
        return new FlightComputerFace.Legend(text, state, false);
    }

    /* Text at twice the face's size, as the count is the biggest thing on the panel. */
    private static void big(final GuiGraphics g, final Font font, final String text, final int x, final int y,
                            final int colour) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(2, 2, 1);
        FlightComputerFace.text(g, font, text, 0, 0, colour, false);
        g.pose().popPose();
    }

    /* Text whose right edge is {@code x}. */
    private static void right(final GuiGraphics g, final Font font, final String text, final int x, final int y,
                              final int colour) {
        FlightComputerFace.text(g, font, text, x - font.width(text), y, colour, false);
    }

    private static String clock(final int seconds) {
        return String.format(Locale.ROOT, "%02d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60);
    }

    private static String word(final TextKey key) {
        return GameText.resolve(key);
    }

    /**
     * The page's own colours, beside the flight computer's.
     *
     * @param panel      the screen's ground on this page
     * @param edge       a rule across the page, the line the count is at, and a tank's empty bar
     * @param dim        a heading, what the pad has none of, and LAUNCH under its cover
     * @param greenMid   what is done, the holds, and the count not running
     * @param goGround   the ground of a system that is go
     * @param waitGround the ground of a system that wants an eye
     * @param noGoGround the ground of a system that is no-go, and of LAUNCH lit
     * @param offGround  the ground of what the pad has none of
     * @param oxidiser   the oxidiser's tank
     * @param fuel       the fuel's tank
     */
    public record Colours(int panel, int edge, int dim, int greenMid, int goGround, int waitGround, int noGoGround,
                          int offGround, int oxidiser, int fuel) {
    }
}
