/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.client.crc;

import dev.jstech.core.client.machine.MachineArt;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.industrial.JsIndustrial;
import dev.jstech.industrial.crc.CrcLayout;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * How the Computational Research Centre's screen is drawn, as the approved design draws it: a research computer of
 * 1959, its face the grey enamel of the consoles of the time, bolted, with engraved words, mechanical counters, a
 * mercury thermometer and a register of small bulbs that flicker while it computes; its chart on squared paper, its
 * card an index card. Each takes the screen's pixels, as {@link MachineArt}'s parts do, which it draws with.
 */
@PaletteHolder
public final class CrcArt {

    /** Its colours. */
    public static final Palette<Colours> PALETTE = Palettes.declare(JsIndustrial.MODID, "machine/crc_screen",
            new Colours(rgb(176, 182, 186), rgb(34, 34, 40), rgb(40, 84, 196), rgb(196, 44, 36), rgb(40, 150, 60),
                    rgb(150, 146, 136), rgb(238, 232, 212), rgb(246, 242, 228), rgb(204, 218, 210),
                    rgb(176, 200, 190), rgb(252, 236, 190), rgb(104, 98, 84), rgb(60, 64, 70), rgb(40, 44, 50),
                    rgb(22, 22, 24), rgb(236, 236, 228), rgb(255, 120, 110), rgb(200, 60, 50), rgb(220, 120, 120),
                    rgb(226, 222, 210), rgb(200, 200, 192), rgb(178, 128, 72), rgb(84, 98, 118), rgb(110, 176, 226),
                    rgb(95, 224, 122), rgb(240, 178, 58), rgb(239, 106, 90), rgb(120, 126, 134)));
    /* The register's bulbs: the share lit in a frame of its flicker, and how many ticks a frame lasts. */
    private static final int LIT_IN_A_THOUSAND = 450;
    private static final int FLICKER_TICKS = 3;
    /* The thermometer's scale, every ten degrees. */
    private static final int SCALE_STEP = 10;
    private static final int RED_FROM = 90;

    private CrcArt() {
    }

    /** The console's face: grey enamel on steel, lit from above, a bolt at each corner. */
    public static void face(final GuiGraphics g, final int x0, final int y0, final int x1, final int y1) {
        final Colours c = PALETTE.get();
        final MachineArt.Colours m = MachineArt.PALETTE.get();
        final int top = MachineArt.mix(c.enamel(), m.white(), 0.08);
        final int bottom = MachineArt.mix(c.enamel(), m.black(), 0.08);
        for (int y = y0; y < y1; y++) {
            MachineArt.hline(g, x0, x1, y, MachineArt.mix(top, bottom, (y - y0) / (double) (y1 - y0)));
        }
        MachineArt.hline(g, x0, x1, y0, MachineArt.mix(c.enamel(), m.black(), 0.45));
        MachineArt.vline(g, x0, y0, y1, MachineArt.mix(c.enamel(), m.black(), 0.45));
        MachineArt.hline(g, x0, x1, y1 - 1, MachineArt.mix(c.enamel(), m.white(), 0.4));
        MachineArt.vline(g, x1 - 1, y0, y1, MachineArt.mix(c.enamel(), m.white(), 0.4));
        for (final int[] at : new int[][] {{x0, y0}, {x1 - 1, y0}, {x0, y1 - 1}, {x1 - 1, y1 - 1}}) {
            MachineArt.dot(g, at[0], at[1], m.ground());
        }
        MachineArt.bolt(g, x0 + 3, y0 + 3);
        MachineArt.bolt(g, x1 - 6, y0 + 3);
        MachineArt.bolt(g, x0 + 3, y1 - 6);
        MachineArt.bolt(g, x1 - 6, y1 - 6);
    }

    /** Words cut into the enamel: dark, the light catching their lower lip; centred on {@code x} when asked. */
    public static void engrave(final GuiGraphics g, final Font font, final String text, final int x, final int y,
                               final boolean centre) {
        final Colours c = PALETTE.get();
        final MachineArt.Colours m = MachineArt.PALETTE.get();
        final int left = centre ? x - MachineArt.width(font, text) / 2 : x;
        MachineArt.text(g, font, text, left, y + 1, MachineArt.mix(c.enamel(), m.white(), 0.45));
        MachineArt.text(g, font, text, left, y, MachineArt.mix(c.enamel(), m.black(), 0.6));
    }

    /** Engraved words cut to {@code room} pixels. */
    public static void engrave(final GuiGraphics g, final Font font, final String text, final int x, final int y,
                               final int room) {
        engrave(g, font, MachineArt.fit(font, text, room), x, y, false);
    }

    /** A mechanical counter: a digit on each black wheel, the light catching its top; red digits when short. */
    public static void counter(final GuiGraphics g, final Font font, final int x, final int y, final String digits,
                               final boolean lacking) {
        final Colours c = PALETTE.get();
        final MachineArt.Colours m = MachineArt.PALETTE.get();
        for (int i = 0; i < digits.length(); i++) {
            final int wx = x + i * CrcLayout.DIGIT;
            MachineArt.fill(g, wx, y, wx + 5, y + CrcLayout.COUNTER_HEIGHT, c.wheel());
            MachineArt.hline(g, wx, wx + 5, y, MachineArt.mix(c.wheel(), m.white(), 0.2));
            MachineArt.hline(g, wx, wx + 5, y + CrcLayout.COUNTER_HEIGHT - 1, MachineArt.mix(c.wheel(), m.black(),
                    0.6));
            MachineArt.text(g, font, String.valueOf(digits.charAt(i)), wx + 1, y + 2, lacking ? c.digitShort()
                    : c.digit());
        }
        MachineArt.frame(g, x - 1, y - 1, x + digits.length() * CrcLayout.DIGIT, y + CrcLayout.COUNTER_HEIGHT + 1,
                MachineArt.mix(c.enamel(), m.black(), 0.5));
    }

    /** {@code value} as a counter's {@code wheels} digits: its last digits when it is more than they hold. */
    public static String digits(final long value, final int wheels) {
        final long most = (long) Math.pow(10, wheels);
        final String shown = String.valueOf(Math.floorMod(Math.max(0L, value), most));
        return "0".repeat(Math.max(0, wheels - shown.length())) + shown;
    }

    /**
     * The console's register: rows of small bulbs, a binary word that changes as it computes, still when it rests;
     * {@code tick} the screen's clock.
     */
    public static void register(final GuiGraphics g, final int x, final int y, final int columns, final int rows,
                                final float tick, final boolean computing) {
        final Colours c = PALETTE.get();
        final MachineArt.Colours m = MachineArt.PALETTE.get();
        final int frame = (int) (tick / FLICKER_TICKS);
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                final long seed = (column * 7L + row * 13L + frame * 5L) * 2654435761L % 1000L;
                final boolean on = computing ? seed < LIT_IN_A_THOUSAND : (column + row) % 3 == 0;
                final int bx = x + column * CrcLayout.REGISTER_PITCH;
                final int by = y + row * CrcLayout.REGISTER_PITCH;
                MachineArt.fill(g, bx - 1, by - 1, bx + 4, by + 4, c.bulbFrame());
                MachineArt.fill(g, bx, by, bx + 3, by + 3, on ? c.bulb() : c.bulbOff());
                if (on) {
                    MachineArt.dot(g, bx, by, m.white());
                }
            }
        }
    }

    /** A mercury thermometer behind glass, 0 to 120 degrees, its 100 marked in red, its scale's words beside it. */
    public static void thermometer(final GuiGraphics g, final Font font, final int x, final int y0, final int y1,
                                   final double celsius) {
        final Colours c = PALETTE.get();
        final MachineArt.Colours m = MachineArt.PALETTE.get();
        MachineArt.bezel(g, x - 3, y0 - 3, x + 9, y1 + 3, false);
        MachineArt.fill(g, x, y0, x + 6, y1, c.digit());
        for (int t = 0; t <= CrcLayout.THERMOMETER_MOST; t += SCALE_STEP) {
            MachineArt.hline(g, x + 4, x + 6, level(y0, y1, t), MachineArt.rgb(90, 90, 90));
        }
        MachineArt.hline(g, x, x + 6, level(y0, y1, 100), c.red());
        final int top = level(y0, y1, Math.clamp(celsius, 0.0, CrcLayout.THERMOMETER_MOST));
        final int mercury = celsius >= RED_FROM ? c.red() : c.mercury();
        MachineArt.fill(g, x + 2, top, x + 4, y1, mercury);
        MachineArt.fill(g, x + 1, y1, x + 5, y1 + 5, mercury);
        MachineArt.dot(g, x + 1, y1 + 1, MachineArt.mix(mercury, m.white(), 0.5));
        for (final int t : new int[] {100, 50, 0}) {
            MachineArt.text(g, font, String.valueOf(t), x + CrcLayout.SCALE_X - CrcLayout.THERMOMETER_X,
                    level(y0, y1, t) - 2, MachineArt.mix(c.enamel(), m.black(), 0.65));
        }
    }

    /** A push button with its word on it, raised: pale or dark, greyed when it would do nothing. */
    public static void pushButton(final GuiGraphics g, final Font font, final int x, final int y, final int w,
                                  final String label, final boolean dark, final boolean enabled) {
        final Colours c = PALETTE.get();
        final MachineArt.Colours m = MachineArt.PALETTE.get();
        final int colour = dark ? c.buttonDark() : c.schematic();
        final int body = enabled ? colour : MachineArt.mix(colour, c.enamel(), 0.6);
        MachineArt.drop(g, x, y, x + w, y + CrcLayout.BUTTON_HEIGHT - 1);
        MachineArt.shade(g, x, y, x + w, y + CrcLayout.BUTTON_HEIGHT - 1, body, 0.35, 0.45);
        final int ink = dark ? m.white() : MachineArt.mix(colour, m.black(), 0.7);
        MachineArt.textCentred(g, font, MachineArt.fit(font, label, w - 4), x + w / 2, y + 4,
                enabled ? ink : MachineArt.mix(ink, body, 0.5));
    }

    /** A node of the chart: a pen ring, filled whole, filled on its left half, or left open. */
    public static void node(final GuiGraphics g, final int x, final int y, final int colour, final Fill fill) {
        for (final int[] d : new int[][] {{-1, -2}, {0, -2}, {1, -2}, {-2, -1}, {2, -1}, {-2, 0}, {2, 0}, {-2, 1},
                {2, 1}, {-1, 2}, {0, 2}, {1, 2}}) {
            MachineArt.dot(g, x + d[0], y + d[1], colour);
        }
        if (fill == Fill.WHOLE) {
            MachineArt.fill(g, x - 1, y - 1, x + 2, y + 2, colour);
        } else if (fill == Fill.HALF) {
            MachineArt.fill(g, x - 1, y - 1, x + 1, y + 2, colour);
        }
    }

    /** The mark in pencil of a discovery its team cannot start yet. */
    public static void mark(final GuiGraphics g, final int x, final int y) {
        MachineArt.fill(g, x - 1, y - 1, x + 2, y + 1, PALETTE.get().pencil());
    }

    /** A green tick, what is known. */
    public static void tick(final GuiGraphics g, final int x, final int y) {
        for (final int[] d : new int[][] {{0, 1}, {1, 2}, {2, 1}, {3, 0}, {4, -1}}) {
            MachineArt.dot(g, x + d[0], y + d[1], PALETTE.get().green());
        }
    }

    /** A CRC as the Group tab draws it: three grey cabinets, the console at their end, its letter on top. */
    public static void cabinet(final GuiGraphics g, final Font font, final int x, final int y, final String letter,
                               final boolean self) {
        final Colours c = PALETTE.get();
        for (int k = 0; k < 3; k++) {
            final int kx = x + k * 13;
            MachineArt.shade(g, kx, y, kx + 13, y + CrcLayout.CABINET_HEIGHT, c.cabinet());
            MachineArt.fill(g, kx + 3, y + 3, kx + 10, y + 13, c.bay());
            for (int row = 0; row < 3; row++) {
                MachineArt.hline(g, kx + 4, kx + 9, y + 4 + row * 3, c.cabinetLight());
            }
            MachineArt.hline(g, kx + 3, kx + 10, y + 17, MachineArt.rgb(110, 112, 112));
        }
        final int edge = self ? c.blue() : c.cabinetFrame();
        MachineArt.frame(g, x - 1, y - 1, x + CrcLayout.CABINET_WIDTH + 1, y + CrcLayout.CABINET_HEIGHT + 1, edge);
        MachineArt.textCentred(g, font, letter, x + CrcLayout.CABINET_WIDTH / 2, y - 8, self ? c.blue() : c.ink());
    }

    /** An opaque colour from its channels. */
    public static int rgb(final int r, final int g, final int b) {
        return MachineArt.rgb(r, g, b);
    }

    /* The y of {@code celsius} on a thermometer from {@code y0} to {@code y1}. */
    private static int level(final int y0, final int y1, final double celsius) {
        final int span = y1 - y0 - 4;
        return y1 - 2 - (int) Math.round(span * celsius / CrcLayout.THERMOMETER_MOST);
    }

    /** How a node of the chart is filled. */
    public enum Fill {
        WHOLE,
        HALF,
        OPEN
    }

    /**
     * The screen's colours.
     *
     * @param enamel       the face's grey enamel, which engraving is mixed from
     * @param ink          the pen on paper
     * @param blue         the blue pen: what runs now, this CRC
     * @param red          the red pencil: the discovery picked, what is short, the thermometer's 100
     * @param green        a tick, what is known
     * @param pencil       the pencil: lanes, what cannot start yet
     * @param paper        the chart's squared paper
     * @param card         an index card, the table's ground
     * @param grid         the paper's squares
     * @param gridMajor    every fifth line of them
     * @param bulb         a lit bulb of the register
     * @param bulbOff      a dark bulb
     * @param bulbFrame    the bulbs' holder
     * @param bay          the dark behind a bay's slots, and a cabinet's window
     * @param wheel        a counter's wheel
     * @param digit        a counter's digit, and the thermometer's glass
     * @param digitShort   a counter's digit when what it counts falls short
     * @param mercury      the mercury, below 90 degrees
     * @param cardRule     an index card's red rule
     * @param schematic    the Group tab's drawing ground, and a pale button
     * @param cabinet      a cabinet on the drawing
     * @param cabinetLight the lights in a cabinet's window
     * @param cabinetFrame a cabinet's frame, when it is not this CRC
     * @param cable        the Research Link Cable when it is not dyed
     * @param lampRun      the RUN lamp, the COOLANT and HEAT PORT lamps, a CRC that works
     * @param lampWait     the WAIT lamp, the progress lamps, a CRC that waits
     * @param lampHot      the HOT lamp, a CRC with a fault
     * @param buttonDark   a dark button
     */
    public record Colours(int enamel, int ink, int blue, int red, int green, int pencil, int paper, int card,
                          int grid, int gridMajor, int bulb, int bulbOff, int bulbFrame, int bay, int wheel,
                          int digit, int digitShort, int mercury, int cardRule, int schematic, int cabinet,
                          int cabinetLight, int cabinetFrame, int cable, int lampRun, int lampWait, int lampHot,
                          int buttonDark) {
    }
}
