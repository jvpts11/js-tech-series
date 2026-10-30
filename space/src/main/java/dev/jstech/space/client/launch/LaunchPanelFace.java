/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.launch;

import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import dev.jstech.space.JsSpace;
import dev.jstech.space.launch.LaunchPanelBlockEntity;
import dev.jstech.space.launch.LaunchTexts;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The Launch Panel's console, drawn flat as the approved LCC-1: the bezel with its screws, the maker's line and the
 * pad's plate; the count in seven segments with the mission's clock and the holds; the sequence on its screen; the
 * HOLD and ABORT lamps; the go and no-go board of annunciators; the tanks on a small screen of bars; the DELUGE and
 * CLAMPS toggles, the IGNITION toggle under its red guard, the brass MASTER key and the LAUNCH button in its striped
 * well under its flip cover; the lit push buttons along the foot. All of it read live from the panel, drawn
 * {@value #WIDTH} by {@value #HEIGHT}.
 */
@PaletteHolder
public final class LaunchPanelFace {

    /** How big the console is drawn, in its own pixels. */
    public static final int WIDTH = 404;
    public static final int HEIGHT = 280;
    public static final Palette<Colours> PALETTE = Palettes.declare(JsSpace.MODID, "launch_panel", new Colours(
            0xFF030806, 0xFF7CFF7C, 0xFF0E2410, 0xFF38FF6A, 0xFF1E9A40, 0xFFFFB424, 0xFFFF4434, 0xFF38E0FF,
            0xFFEAF6F2, 0xFF17181B, 0xFFD8DADE, 0xFFC82818, 0xFFEE5A44, 0xFF7A1008, 0xFF1A1B1E, 0xFF2A2C30,
            0xFF4A4D54, 0xFF141518, 0xFF33363B, 0xFF3A3D43, 0xFF4F535A, 0xFF8A8D94, 0xFF5C5F66, 0xFF55585F,
            0xFF6E727A, 0xFF0B0C0E, 0xFF1C1E22, 0xFF2E3137, 0xFF10202A, 0xFF9A9EA6, 0xFF4C5058, 0xFF73777F,
            0xFFE4E8EE, 0xFFB4B8C0, 0xFF7C8088, 0xFFC8CCD2, 0xFF6A6E76, 0xFFB89030, 0xFFE8C860, 0xFF3A2A10,
            0xFFA8A8A8, 0xFFE8C040, 0xFF1A1A1A, 0xFF3A0A08, 0xFFFF8070, 0xFFE02818, 0xFFA01408, 0xFF8FD0FF,
            0xFFFFB060, 0xFFFFFFFF, 0xFF000000, 0x5AC81E14, 0xC8FF8C82, 0x96C82818));
    /* The places of its parts, in its own pixels, as the approved console lays them out. */
    private static final int[] TOGGLES_X = {36, 96, 158};
    private static final int TOGGLES_Y = 205;
    private static final int KEY_X = 212;
    private static final int KEY_Y = 196;
    private static final int LAUNCH_X = 262;
    private static final int LAUNCH_Y = 186;
    private static final int BUTTONS_X = 18;
    private static final int BUTTONS_STEP = 66;
    private static final int BUTTONS_Y = 250;
    private static final int BUTTON_W = 58;
    private static final int BUTTON_H = 26;
    private static final int SECOND = 20;
    private static final String SEGMENTS = "abcdefg";

    private LaunchPanelFace() {
    }

    /** Draws the console of {@code panel} with its top left at the pose's origin, {@code width} wide. */
    public static void paint(final GuiGraphics g, final Font font, final int width,
                             final LaunchPanelBlockEntity panel) {
        final Colours c = PALETTE.get();
        g.pose().pushPose();
        g.pose().scale(width / (float) WIDTH, width / (float) WIDTH, 1);
        bezel(g, font, c, panel);
        count(g, font, c, panel);
        sequence(g, font, c, panel);
        engrave(g, font, LaunchTexts.MASTER, 322, 22, c);
        annunciator(g, font, 322, 36, 66, 18, List.of(word(LaunchTexts.HOLD)), c.amber(),
                panel.phase() == LaunchPanelBlockEntity.Phase.HOLDING, c);
        annunciator(g, font, 322, 60, 66, 18, List.of(word(LaunchTexts.ABORT)), c.red(),
                panel.phase() == LaunchPanelBlockEntity.Phase.SCRUBBED, c);
        rule(g, 12, WIDTH - 12, 90, c);
        board(g, font, c, panel);
        tanks(g, font, c, panel);
        rule(g, 12, 312, 166, c);
        toggle(g, font, TOGGLES_X[0], TOGGLES_Y, panel.deluge(), LaunchTexts.AUTO, LaunchTexts.OFF,
                LaunchTexts.DELUGE, 0, c);
        toggle(g, font, TOGGLES_X[1], TOGGLES_Y, panel.clamps(), LaunchTexts.AUTO, LaunchTexts.OFF,
                LaunchTexts.CLAMPS, 0, c);
        toggle(g, font, TOGGLES_X[2], TOGGLES_Y, panel.ignition(), LaunchTexts.ARM, LaunchTexts.SAFE,
                LaunchTexts.IGNITION, panel.guard() ? 1 : 2, c);
        key(g, font, KEY_X, KEY_Y, panel.master(), c);
        plate(g, font, KEY_X + 9, 229, word(LaunchTexts.MASTER), c);
        launchButton(g, LAUNCH_X, LAUNCH_Y, panel.coverUp(), c);
        plate(g, font, LAUNCH_X + 14, 229, word(LaunchTexts.LAUNCH), c);
        rule(g, 12, WIDTH - 12, 245, c);
        final LaunchPanelBlockEntity.Phase phase = panel.phase();
        final boolean[] lit = {phase == LaunchPanelBlockEntity.Phase.HOLDING,
            phase == LaunchPanelBlockEntity.Phase.SCRUBBED,
            phase == LaunchPanelBlockEntity.Phase.HOLDING && panel.left() < LaunchPanelBlockEntity.COVER * SECOND,
            phase == LaunchPanelBlockEntity.Phase.COUNTING || phase == LaunchPanelBlockEntity.Phase.RELEASED};
        final TextKey[] names = {LaunchTexts.HOLD_COUNT, LaunchTexts.SCRUB, LaunchTexts.RECYCLE,
            LaunchTexts.COUNT_RUNNING};
        final int[] colours = {c.amber(), c.red(), c.cyan(), c.green()};
        for (int i = 0; i < names.length; i++) {
            litButton(g, font, BUTTONS_X + i * BUTTONS_STEP, BUTTONS_Y, BUTTON_W, BUTTON_H, lines(names[i]),
                    colours[i], lit[i], c);
        }
        final List<String> cover = lines(LaunchTexts.COVER);
        for (int i = 0; i < cover.size(); i++) {
            text(g, font, cover.get(i), 290, 255 + i * 9, c.engraveDim(), false);
        }
        g.pose().popPose();
    }

    /** The control of the console under {@code (x, y)}, in its own pixels, or -1 where none is. */
    public static int controlAt(final LaunchPanelBlockEntity panel, final double x, final double y) {
        for (int i = 0; i < 2; i++) {
            if (inside(x, y, TOGGLES_X[i] - 12, TOGGLES_Y - 22, 24, 34)) {
                return i == 0 ? LaunchPanelBlockEntity.DELUGE : LaunchPanelBlockEntity.CLAMPS;
            }
        }
        final int ix = TOGGLES_X[2];
        if (inside(x, y, ix - 14, TOGGLES_Y - 25, 30, 38)) {
            // Closed, the guard is lifted first; open, its lid closes it again and the switch below is thrown.
            return !panel.guard() || y < TOGGLES_Y - 18 ? LaunchPanelBlockEntity.GUARD
                    : LaunchPanelBlockEntity.IGNITION;
        }
        if (inside(x, y, KEY_X - 1, KEY_Y - 1, 20, 20)) {
            return LaunchPanelBlockEntity.MASTER;
        }
        if (inside(x, y, LAUNCH_X - 6, LAUNCH_Y - 6, 40, 40)) {
            return LaunchPanelBlockEntity.LAUNCH;
        }
        final int[] buttons = {LaunchPanelBlockEntity.HOLD, LaunchPanelBlockEntity.SCRUB,
            LaunchPanelBlockEntity.RECYCLE, LaunchPanelBlockEntity.RUN};
        for (int i = 0; i < buttons.length; i++) {
            if (inside(x, y, BUTTONS_X + i * BUTTONS_STEP, BUTTONS_Y, BUTTON_W, BUTTON_H)) {
                return buttons[i];
            }
        }
        return -1;
    }

    /* The bezel, its screws, the maker's line and the pad's plate with its rocket's name. */
    private static void bezel(final GuiGraphics g, final Font font, final Colours c,
                              final LaunchPanelBlockEntity panel) {
        g.fill(0, 0, WIDTH, HEIGHT, c.bezel());
        hline(g, 0, WIDTH, 0, c.bezelLit());
        vline(g, 0, 0, HEIGHT, c.bezelLit());
        hline(g, 0, WIDTH, HEIGHT - 1, c.bezelShade());
        vline(g, WIDTH - 1, 0, HEIGHT, c.bezelShade());
        frame(g, 3, 3, WIDTH - 3, HEIGHT - 3, c.bezelShade());
        hline(g, 4, WIDTH - 4, 4, c.rim());
        vline(g, 4, 4, HEIGHT - 4, c.rim());
        for (final int[] at : new int[][] {{8, 8}, {WIDTH - 12, 8}, {8, HEIGHT - 12}, {WIDTH - 12, HEIGHT - 12}}) {
            g.fill(at[0], at[1], at[0] + 4, at[1] + 4, c.bezelShade());
            g.fill(at[0] + 1, at[1] + 1, at[0] + 3, at[1] + 3, c.screw());
            g.fill(at[0] + 1, at[1] + 1, at[0] + 2, at[1] + 2, c.screwLit());
            g.fill(at[0] + 2, at[1] + 2, at[0] + 3, at[1] + 3, c.key());
        }
        text(g, font, word(LaunchTexts.TITLE), WIDTH / 2, 7, c.engrave(), true);
        final String pad = GameText.resolve(LaunchTexts.PAD.with("1")) + "  " + (panel.rocketName().isEmpty()
                ? word(LaunchTexts.NO_ROCKET) : panel.rocketName().toUpperCase(Locale.ROOT));
        final int w = font.width(pad) + 6;
        g.fill(WIDTH - 20 - w, 6, WIDTH - 20, 17, c.plate());
        text(g, font, pad, WIDTH - 17 - w, 7, c.plateInk(), false);
    }

    /* The count in seven segments, the mission's clock and the holds. */
    private static void count(final GuiGraphics g, final Font font, final Colours c,
                              final LaunchPanelBlockEntity panel) {
        engrave(g, font, LaunchTexts.COUNTDOWN, 16, 22, c);
        screen(g, 18, 36, 172, 80, c);
        final boolean released = panel.phase() == LaunchPanelBlockEntity.Phase.RELEASED;
        final int seconds = released ? 0 : (panel.left() + SECOND - 1) / SECOND;
        text(g, font, "T-", 24, 46, c.segOn(), false);
        int x = 36;
        for (final char ch : clock(seconds).toCharArray()) {
            if (ch == ':') {
                g.fill(x + 1, 46, x + 3, 48, c.segOn());
                g.fill(x + 1, 54, x + 3, 56, c.segOn());
                x += 5;
            } else {
                digit(g, x, 42, ch, c);
                x += 14;
            }
        }
        hline(g, 22, 168, 67, c.segOff());
        text(g, font, word(LaunchTexts.MET), 24, 70, c.cyan(), false);
        text(g, font, clock(released ? panel.met() / SECOND : 0), 44, 70, c.ink(), false);
        final String holds = GameText.resolve(LaunchTexts.HOLDS.with(panel.holds()));
        text(g, font, holds, 166 - font.width(holds), 70, c.greenMid(), false);
    }

    /* The sequence on its screen: done, now and next, by where the count is. */
    private static void sequence(final GuiGraphics g, final Font font, final Colours c,
                                 final LaunchPanelBlockEntity panel) {
        engrave(g, font, LaunchTexts.SEQUENCE, 184, 22, c);
        screen(g, 186, 36, 308, 80, c);
        final int[] at = {LaunchPanelBlockEntity.START, LaunchPanelBlockEntity.ARMS, LaunchPanelBlockEntity.COVER,
            LaunchPanelBlockEntity.LIGHT, 0};
        final TextKey[] what = {LaunchTexts.ARMS_RETRACT, LaunchTexts.ARMS_MOVING, LaunchTexts.GUARD_LIFTS,
            LaunchTexts.IGNITION_EVENT, LaunchTexts.RELEASE};
        final boolean counted = panel.phase() != LaunchPanelBlockEntity.Phase.SAFE
                && panel.phase() != LaunchPanelBlockEntity.Phase.SCRUBBED;
        final double seconds = panel.phase() == LaunchPanelBlockEntity.Phase.RELEASED ? -1 : panel.left()
                / (double) SECOND;
        int now = -1;
        for (int i = 0; i < at.length; i++) {
            if (counted && seconds <= at[i]) {
                now = i;
            }
        }
        for (int i = 0; i < at.length; i++) {
            final int y = 38 + i * 8;
            final boolean done = i < now;
            if (i == now) {
                g.fill(187, y - 1, 307, y + 8, c.sequenceNow());
                text(g, font, ">", 189, y, c.amber(), false);
            }
            text(g, font, "T-" + at[i], 196, y, done ? c.green() : c.cyan(), false);
            text(g, font, word(what[i]), 222, y, done ? c.green() : i == now ? c.ink() : c.greenMid(), false);
        }
    }

    /* The go and no-go board: one annunciator a system, its name and its state. */
    private static void board(final GuiGraphics g, final Font font, final Colours c,
                              final LaunchPanelBlockEntity panel) {
        engrave(g, font, LaunchTexts.GO_NO_GO, 16, 98, c);
        final double seconds = panel.left() / (double) SECOND;
        final boolean counting = panel.phase() == LaunchPanelBlockEntity.Phase.COUNTING
                || panel.phase() == LaunchPanelBlockEntity.Phase.HOLDING;
        final TextKey arms = counting && seconds <= LaunchPanelBlockEntity.COVER
                || panel.phase() == LaunchPanelBlockEntity.Phase.RELEASED ? LaunchTexts.CLEAR
                : counting && seconds <= LaunchPanelBlockEntity.ARMS ? LaunchTexts.MOVING : LaunchTexts.GO;
        final boolean propellant = panel.oxidiser() >= LaunchPanelBlockEntity.FULL
                && panel.fuel() >= LaunchPanelBlockEntity.FULL;
        final Object[][] systems = {
            {LaunchTexts.VEHICLE, panel.vehicle() ? LaunchTexts.GO : LaunchTexts.NO_GO},
            {LaunchTexts.PROPELLANT, propellant ? LaunchTexts.GO : LaunchTexts.NO_GO},
            {LaunchTexts.TOWER_ARMS, arms},
            {LaunchTexts.DELUGE, !panel.deluge() ? LaunchTexts.OFF : panel.delugeReady() ? LaunchTexts.GO
                : LaunchTexts.NO_GO},
            {LaunchTexts.WEATHER, panel.storm() ? LaunchTexts.NO_GO : LaunchTexts.GO},
            {LaunchTexts.CREW, LaunchTexts.GO},
            {LaunchTexts.RANGE, LaunchTexts.GO},
            {LaunchTexts.CLAMPS, panel.clamps() ? LaunchTexts.GO : LaunchTexts.OFF}};
        for (int i = 0; i < systems.length; i++) {
            final TextKey state = (TextKey) systems[i][1];
            final int colour = state == LaunchTexts.NO_GO ? c.red() : state == LaunchTexts.MOVING
                    || state == LaunchTexts.OFF ? c.amber() : c.green();
            annunciator(g, font, 18 + i % 4 * 72, 112 + i / 4 * 26, 64, 20, List.of(word((TextKey) systems[i][0]),
                    word(state)), colour, true, c);
        }
    }

    /* The tanks on a small screen: a bar each, its load and its pressure; and the loading's state under it. */
    private static void tanks(final GuiGraphics g, final Font font, final Colours c,
                              final LaunchPanelBlockEntity panel) {
        engrave(g, font, LaunchTexts.TANKS, 322, 98, c);
        screen(g, 324, 112, 388, 186, c);
        final int[] full = {panel.oxidiser(), panel.fuel()};
        final TextKey[] names = {LaunchTexts.OXIDISER, LaunchTexts.FUEL};
        final int[] bars = {c.lox(), c.methane()};
        final String[] pressures = {"2.1", "1.9"};
        for (int i = 0; i < 2; i++) {
            final int bx = 334 + i * 30;
            text(g, font, word(names[i]), bx + 5, 115, c.cyan(), true);
            frame(g, bx - 1, 125, bx + 11, 165, c.greenMid());
            final int top = 164 - (int) (38 * full[i] / 100.0);
            g.fill(bx + 1, top, bx + 10, 164, mix(bars[i], c.black(), 0.35));
            g.fill(bx + 1, top, bx + 4, 164, bars[i]);
            for (int k = 0; k < 5; k++) {
                hline(g, bx + 11, bx + 13, 126 + k * 9, c.greenMid());
            }
            text(g, font, full[i] + "%", bx + 5, 167, c.ink(), true);
            text(g, font, full[i] > 0 ? pressures[i] : "0.0", bx + 5, 176, c.green(), true);
        }
        final boolean loaded = full[0] >= LaunchPanelBlockEntity.FULL && full[1] >= LaunchPanelBlockEntity.FULL;
        annunciator(g, font, 324, 196, 64, 12, List.of(word(loaded ? LaunchTexts.LOADED : LaunchTexts.TOPPING_OFF)),
                loaded ? c.green() : c.amber(), panel.vehicle() && (loaded || full[0] + full[1] > 0), c);
    }

    /* A seven-segment digit. */
    private static void digit(final GuiGraphics g, final int x, final int y, final char ch, final Colours c) {
        final String on = switch (ch) {
            case '0' -> "abcdef";
            case '1' -> "bc";
            case '2' -> "abged";
            case '3' -> "abgcd";
            case '4' -> "fgbc";
            case '5' -> "afgcd";
            case '6' -> "afgedc";
            case '7' -> "abc";
            case '8' -> "abcdefg";
            case '9' -> "abfgcd";
            default -> "";
        };
        final int w = 11;
        final int h = 20;
        final int t = 2;
        final int[][] segs = {{x + t, y, x + w - t, y + t}, {x + w - t, y + t, x + w, y + h / 2 - 1},
            {x + w - t, y + h / 2 + 1, x + w, y + h - t}, {x + t, y + h - t, x + w - t, y + h},
            {x, y + h / 2 + 1, x + t, y + h - t}, {x, y + t, x + t, y + h / 2 - 1},
            {x + t, y + h / 2 - 1, x + w - t, y + h / 2 + 1}};
        for (int k = 0; k < segs.length; k++) {
            final boolean lit = on.indexOf(SEGMENTS.charAt(k)) >= 0;
            final int[] s = segs[k];
            g.fill(s[0], s[1], s[2], s[3], lit ? c.segOn() : c.segOff());
            if (lit) {
                g.fill(s[0], s[1], s[0] + 1, s[1] + 1, mix(c.segOn(), c.white(), 0.4));
            }
        }
    }

    /* A recessed screen, as the flight computer's: the raised rim, the dark gasket, the black glass. */
    private static void screen(final GuiGraphics g, final int x0, final int y0, final int x1, final int y1,
                               final Colours c) {
        g.fill(x0 - 4, y0 - 4, x1 + 4, y1 + 4, c.rim());
        hline(g, x0 - 4, x1 + 4, y0 - 4, c.bezelLit());
        vline(g, x0 - 4, y0 - 4, y1 + 4, c.bezelLit());
        hline(g, x0 - 4, x1 + 4, y1 + 3, c.bezelShade());
        vline(g, x1 + 3, y0 - 4, y1 + 4, c.bezelShade());
        g.fill(x0 - 2, y0 - 2, x1 + 2, y1 + 2, c.bezelShade());
        g.fill(x0, y0, x1, y1, c.screen());
    }

    /* A name plate: a dark plate with the name in light letters, centred on {@code cx}. */
    private static void plate(final GuiGraphics g, final Font font, final int cx, final int y, final String name,
                              final Colours c) {
        final int w = font.width(name) + 6;
        g.fill(cx - w / 2, y, cx - w / 2 + w, y + 11, c.plate());
        hline(g, cx - w / 2, cx - w / 2 + w, y + 10, c.rim());
        text(g, font, name, cx - w / 2 + 3, y + 2, c.plateInk(), false);
    }

    /* An annunciator: its legend lit in its colour on a face tinted by it; off, black, its legend barely there. */
    private static void annunciator(final GuiGraphics g, final Font font, final int x, final int y, final int w,
                                    final int h, final List<String> lines, final int colour, final boolean lit,
                                    final Colours c) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, c.bezelShade());
        final int ink;
        if (lit) {
            frame(g, x - 2, y - 2, x + w + 2, y + h + 2, mix(colour, c.bezel(), 0.72));
            g.fill(x, y, x + w, y + h, mix(colour, c.black(), 0.78));
            hline(g, x, x + w, y, mix(colour, c.black(), 0.45));
            ink = mix(colour, c.white(), 0.2);
        } else {
            g.fill(x, y, x + w, y + h, c.annunciatorOff());
            hline(g, x, x + w, y, c.annunciatorEdge());
            ink = c.annunciatorInk();
        }
        final int top = y + (h - (lines.size() * 9 - 1)) / 2;
        for (int i = 0; i < lines.size(); i++) {
            text(g, font, lines.get(i), x + w / 2, top + i * 9, ink, true);
        }
    }

    /* A bat-handle toggle on its riveted plate, thrown {@code up} or down, its positions engraved, its name plate;
     * {@code guard} 1 puts it under its red guard thrown open, 2 closed. */
    private static void toggle(final GuiGraphics g, final Font font, final int cx, final int cy, final boolean up,
                               final TextKey top, final TextKey bottom, final TextKey name, final int guard,
                               final Colours c) {
        g.fill(cx - 10, cy - 10, cx + 11, cy + 11, c.bezelShade());
        g.fill(cx - 9, cy - 9, cx + 10, cy + 10, c.key());
        hline(g, cx - 9, cx + 10, cy - 9, c.keyLit());
        vline(g, cx - 9, cy - 9, cy + 10, c.keyLit());
        for (final int[] r : new int[][] {{-7, -7}, {7, -7}, {-7, 7}, {7, 7}}) {
            dot(g, cx + r[0], cy + r[1], c.bezelShade());
            dot(g, cx + r[0] - 1, cy + r[1] - 1, c.keyLit());
        }
        for (int yy = -5; yy <= 5; yy++) {
            final int half = 5 - Math.max(0, Math.abs(yy) - 2);
            for (int xx = -half - 1; xx <= half + 1; xx++) {
                dot(g, cx + xx, cy + yy, xx + yy < -1 ? c.nutLit() : xx + yy > 2 ? c.nutDark() : c.nut());
            }
        }
        g.fill(cx - 1, cy - 1, cx + 2, cy + 2, c.shadow());
        final int sign = up ? -1 : 1;
        final int length = up ? 17 : 12;
        for (int k = 2; k < length; k++) {
            dot(g, cx + 2, cy + sign * k + 1, c.shadow());
        }
        for (int k = 1; k < length; k++) {
            dot(g, cx - 1, cy + sign * k, c.handleLit());
            dot(g, cx, cy + sign * k, c.handle());
            dot(g, cx + 1, cy + sign * k, c.handleDark());
        }
        final int tip = cy + sign * length;
        g.fill(cx - 2, tip - 1, cx + 3, tip + 2, c.tip());
        hline(g, cx - 2, cx + 2, tip - 1, c.white());
        dot(g, cx + 2, tip + 1, c.tipDark());
        hline(g, cx - 1, cx + 4, tip + 2, c.shadow());
        text(g, font, word(top), cx, cy - 32, up ? c.engrave() : c.engraveDim(), true);
        text(g, font, word(bottom), cx, cy + 13, up ? c.engraveDim() : c.engrave(), true);
        plate(g, font, cx, cy + 24, word(name), c);
        if (guard == 1) {
            for (final int side : new int[] {-13, 12}) {
                g.fill(cx + side, cy - 19, cx + side + 2, cy + 11, c.guard());
                vline(g, cx + side, cy - 19, cy + 11, c.guardLit());
            }
            g.fill(cx - 13, cy - 24, cx + 15, cy - 19, c.guard());
            hline(g, cx - 13, cx + 15, cy - 24, c.guardLit());
            hline(g, cx - 13, cx + 15, cy - 20, c.guardDark());
        } else if (guard == 2) {
            g.fill(cx - 13, cy - 13, cx + 14, cy + 12, c.guardGlass());
            frame(g, cx - 13, cy - 13, cx + 14, cy + 12, c.guard());
            hline(g, cx - 13, cx + 14, cy - 13, c.guardLit());
        }
    }

    /* A lit push button: its cap in the bezel's cut, its legend window lit in its colour when on. */
    private static void litButton(final GuiGraphics g, final Font font, final int x, final int y, final int w,
                                  final int h, final List<String> lines, final int colour, final boolean lit,
                                  final Colours c) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, c.bezelShade());
        g.fill(x, y, x + w, y + h, c.key());
        hline(g, x, x + w, y, c.keyLit());
        vline(g, x, y, y + h, c.keyLit());
        hline(g, x, x + w, y + h - 1, c.bezelShade());
        vline(g, x + w - 1, y, y + h, c.bezelShade());
        annunciator(g, font, x + 3, y + 3, w - 6, h - 7, lines, colour, lit, c);
    }

    /* The brass arming key, turned to ARM or to SAFE. */
    private static void key(final GuiGraphics g, final Font font, final int x, final int y, final boolean armed,
                            final Colours c) {
        g.fill(x - 1, y - 1, x + 19, y + 19, c.bezelShade());
        for (int yy = 0; yy < 18; yy++) {
            for (int xx = 0; xx < 18; xx++) {
                if (Math.hypot(xx - 8.5, yy - 8.5) <= 8.5) {
                    dot(g, x + xx, y + yy, Math.hypot(xx - 6, yy - 6) > 4 ? c.brass() : c.brassLit());
                }
            }
        }
        if (armed) {
            g.fill(x + 8, y + 2, x + 10, y + 16, c.keySlot());
            g.fill(x + 7, y - 4, x + 11, y + 3, c.keyMetal());
        } else {
            g.fill(x + 2, y + 8, x + 16, y + 10, c.keySlot());
        }
        final String safe = word(LaunchTexts.SAFE);
        text(g, font, safe, x - 3 - font.width(safe), y + 5, c.engraveDim(), false);
        text(g, font, word(LaunchTexts.ARM), x + 9, y - 14, c.engrave(), true);
    }

    /* The launch button in its striped well, under its flip cover of red glass while it is down. */
    private static void launchButton(final GuiGraphics g, final int x, final int y, final boolean coverUp,
                                     final Colours c) {
        g.fill(x - 6, y - 6, x + 34, y + 34, c.bezelShade());
        for (int yy = y - 5; yy < y + 33; yy++) {
            for (int xx = x - 5; xx < x + 33; xx++) {
                if (!(xx >= x - 1 && xx < x + 29 && yy >= y - 1 && yy < y + 29)) {
                    dot(g, xx, yy, Math.floorDiv(xx + yy, 3) % 2 == 0 ? c.hazard() : c.hazardDark());
                }
            }
        }
        g.fill(x, y, x + 28, y + 28, c.well());
        for (int yy = 4; yy < 24; yy++) {
            for (int xx = 4; xx < 24; xx++) {
                if (Math.hypot(xx - 13.5, yy - 13.5) <= 10.5) {
                    final double d = Math.hypot(xx - 10, yy - 10);
                    dot(g, x + xx, y + yy, d < 3 ? c.buttonLit() : d < 8 ? c.button() : c.buttonDark());
                }
            }
        }
        if (!coverUp) {
            g.fill(x - 2, y - 2, x + 30, y + 30, c.coverGlass());
            hline(g, x - 2, x + 30, y - 2, c.coverEdge());
            g.fill(x + 10, y - 5, x + 18, y - 2, c.keyLit());
        }
    }

    private static void engrave(final GuiGraphics g, final Font font, final TextKey key, final int x, final int y,
                                final Colours c) {
        text(g, font, word(key), x, y, c.engrave(), false);
    }

    private static void rule(final GuiGraphics g, final int x0, final int x1, final int y, final Colours c) {
        hline(g, x0, x1, y, c.bezelShade());
        hline(g, x0, x1, y + 1, c.bezelLit());
    }

    private static String clock(final int seconds) {
        return String.format(Locale.ROOT, "%02d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60);
    }

    private static String word(final TextKey key) {
        return GameText.resolve(key);
    }

    private static List<String> lines(final TextKey key) {
        return List.of(word(key).split("\n"));
    }

    private static void text(final GuiGraphics g, final Font font, final String text, final int x, final int y,
                             final int colour, final boolean centred) {
        Draw.text(g, font, text, centred ? x - font.width(text) / 2 : x, y, colour);
    }

    private static void hline(final GuiGraphics g, final int x0, final int x1, final int y, final int colour) {
        g.fill(x0, y, x1, y + 1, colour);
    }

    private static void vline(final GuiGraphics g, final int x, final int y0, final int y1, final int colour) {
        g.fill(x, y0, x + 1, y1, colour);
    }

    private static void dot(final GuiGraphics g, final int x, final int y, final int colour) {
        g.fill(x, y, x + 1, y + 1, colour);
    }

    private static void frame(final GuiGraphics g, final int x0, final int y0, final int x1, final int y1,
                              final int colour) {
        hline(g, x0, x1, y0, colour);
        hline(g, x0, x1, y1 - 1, colour);
        vline(g, x0, y0, y1, colour);
        vline(g, x1 - 1, y0, y1, colour);
    }

    private static boolean inside(final double x, final double y, final int x0, final int y0, final int w,
                                  final int h) {
        return x >= x0 && x < x0 + w && y >= y0 && y < y0 + h;
    }

    /* {@code a} toward {@code b} by {@code share}, its alpha kept. */
    private static int mix(final int a, final int b, final double share) {
        final int r = (int) Math.round((a >> 16 & 0xFF) + ((b >> 16 & 0xFF) - (a >> 16 & 0xFF)) * share);
        final int gr = (int) Math.round((a >> 8 & 0xFF) + ((b >> 8 & 0xFF) - (a >> 8 & 0xFF)) * share);
        final int bl = (int) Math.round((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * share);
        return a >>> 24 << 24 | r << 16 | gr << 8 | bl;
    }

    /**
     * The console's colours.
     *
     * @param screen          a screen's black glass
     * @param segOn           a lit segment of the count
     * @param segOff          an unlit one
     * @param green           what is done and go
     * @param greenMid        what is next, and the screens' marks
     * @param amber           what waits, holds or moves
     * @param red             what is no-go, scrubbed or aborted
     * @param cyan            the times and the names of figures
     * @param ink             what is now
     * @param plate           a name plate
     * @param plateInk        a name plate's letters
     * @param guard           the ignition's red guard
     * @param guardLit        its lit edge
     * @param guardDark       its shaded edge
     * @param shadow          a switch's shadow
     * @param bezel           the bezel
     * @param bezelLit        its lit edge
     * @param bezelShade      its shaded edge
     * @param rim             a screen's rim
     * @param key             a key's or a plate's face
     * @param keyLit          its lit edge
     * @param engrave         the engraved words
     * @param engraveDim      the words of what is not chosen
     * @param screw           a screw's head
     * @param screwLit        its lit side
     * @param annunciatorOff  an annunciator off
     * @param annunciatorEdge its top edge off
     * @param annunciatorInk  its legend off
     * @param sequenceNow     the line of the sequence now
     * @param nutLit          a switch's nut lit
     * @param nutDark         a switch's nut shaded
     * @param nut             a switch's nut
     * @param handleLit       a switch's handle lit
     * @param handle          a switch's handle
     * @param handleDark      a switch's handle shaded
     * @param tip             a switch's bat tip
     * @param tipDark         its shaded corner
     * @param brass           the key's brass
     * @param brassLit        its lit side
     * @param keySlot         the key's slot
     * @param keyMetal        the key's steel
     * @param hazard          the launch well's yellow stripes
     * @param hazardDark      its dark stripes
     * @param well            the launch button's well
     * @param buttonLit       the launch button lit
     * @param button          the launch button
     * @param buttonDark      its shaded edge
     * @param lox             the oxidiser's bar
     * @param methane         the fuel's bar
     * @param white           white
     * @param black           black
     * @param coverGlass      the launch button's red glass cover
     * @param coverEdge       its lit edge
     * @param guardGlass      the ignition guard closed
     */
    public record Colours(int screen, int segOn, int segOff, int green, int greenMid, int amber, int red, int cyan,
                          int ink, int plate, int plateInk, int guard, int guardLit, int guardDark, int shadow,
                          int bezel, int bezelLit, int bezelShade, int rim, int key, int keyLit, int engrave,
                          int engraveDim, int screw, int screwLit, int annunciatorOff, int annunciatorEdge,
                          int annunciatorInk, int sequenceNow, int nutLit, int nutDark, int nut, int handleLit,
                          int handle, int handleDark, int tip, int tipDark, int brass, int brassLit, int keySlot,
                          int keyMetal, int hazard, int hazardDark, int well, int buttonLit, int button,
                          int buttonDark, int lox, int methane, int white, int black, int coverGlass, int coverEdge,
                          int guardGlass) {
    }
}
