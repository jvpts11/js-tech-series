/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.rocket;

import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.space.JsSpace;
import dev.jstech.space.client.flight.LandingMapsClient;
import dev.jstech.space.flight.FlightComputerBlockEntity;
import dev.jstech.space.flight.FlightPage;
import dev.jstech.space.flight.TransferPlan;
import dev.jstech.space.rocket.CabinLamp;
import dev.jstech.space.rocket.CabinReadings;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The Flight Computer's face as the approved display draws it, flat, in pixels of its own {@value #WIDTH} by
 * {@value #HEIGHT}: the bezel with its screws, the raised rim and gasket round the screen, the soft keys along the top
 * and both sides with their night lighting, the fail lamp and the light sensor, the brightness and contrast rockers,
 * the data plate and the night and day knob; on the screen, the pages' names along its top, the open page
 * ({@link FlightComputerPages}) and its keys' legends. The face on a capsule's panel and on the lander's cabin wall
 * shows the mission's clock until the rocket's own computer is joined to it.
 */
@TextHolder
@PaletteHolder
public final class FlightComputerFace {

    /** The face's size in its own pixels. */
    public static final int WIDTH = 372;
    public static final int HEIGHT = 280;
    public static final TextKey MAKER = TextKey.of("jsspace.flight_computer.maker", "FCC-2  FLIGHT COMPUTER");
    public static final TextKey FAIL = TextKey.of("jsspace.flight_computer.fail", "FAIL");
    public static final TextKey BRIGHTNESS = TextKey.of("jsspace.flight_computer.brightness", "BRT");
    public static final TextKey CONTRAST = TextKey.of("jsspace.flight_computer.contrast", "CON");
    public static final TextKey PLATE = TextKey.of("jsspace.flight_computer.plate", "P/N 7104-220 S/N 31");
    public static final TextKey KNOB_OFF = TextKey.of("jsspace.flight_computer.knob_off", "OFF");
    public static final TextKey KNOB_NIGHT = TextKey.of("jsspace.flight_computer.knob_night", "NT");
    public static final TextKey KNOB_DAY = TextKey.of("jsspace.flight_computer.knob_day", "DAY");
    public static final TextKey LEFT_KEY = TextKey.of("jsspace.flight_computer.left_key", "L%s");
    public static final TextKey RIGHT_KEY = TextKey.of("jsspace.flight_computer.right_key", "R%s");
    public static final TextKey CLOCK = TextKey.of("jsspace.flight_computer.clock", "MET");
    public static final TextKey SYSTEM_PAGE = TextKey.of("jsspace.flight_computer.page.sys", "SYS");
    public static final TextKey TRANSFER_PAGE = TextKey.of("jsspace.flight_computer.page.xfer", "XFER");
    public static final TextKey LANDING_PAGE = TextKey.of("jsspace.flight_computer.page.land", "LAND");
    public static final TextKey STAGES_PAGE = TextKey.of("jsspace.flight_computer.page.stg", "STG");
    public static final TextKey RADIO_PAGE = TextKey.of("jsspace.flight_computer.page.comm", "COMM");
    /** The pages, in the order of the keys along the top. */
    public static final List<TextKey> PAGES = List.of(SYSTEM_PAGE, TRANSFER_PAGE, LANDING_PAGE, STAGES_PAGE,
            RADIO_PAGE);
    public static final Palette<Colours> PALETTE = Palettes.declare(JsSpace.MODID, "flight_computer", new Colours(
            0xFF030806, 0xFF38FF6A, 0xFF38E0FF, 0xFFEAF6F2, 0xFFFFB424, 0xFF2A2C30, 0xFF4A4D54, 0xFF141518,
            0xFF33363B, 0xFF3A3D43, 0xFF4F535A, 0xFF9EE8B0, 0xFF8A8D94, 0xFF5C5F66, 0xFF55585F, 0xFF6E727A,
            0xFF08090A, 0xFF4A3214, 0xFF9A6A2A, 0xFF1A2230, 0xFF5A7090, 0xFF3E4147, 0xFF1C1D20, 0xFF0A0F17,
            0xFF1A2636, 0xFFA8C8E8, 0xFFFFE070, 0xFF3A7AD0, 0xFFD05A30, 0xFFC8C8C8, 0xFFFF48DE, 0xFF4A5260,
            0xFF5E6878, 0xFF24602A, 0xFF3AA040, 0xC80A0F17, 0xFF2A6070));
    /** The screen inside the bezel. */
    public static final int SX0 = 46;
    public static final int SY0 = 36;
    public static final int SX1 = WIDTH - 46;
    public static final int SY1 = HEIGHT - 38;

    private static final int SIDE_KEYS = 5;
    private static final int TOP_KEY_WIDTH = 24;
    private static final int KEY_WIDTH = 18;
    private static final int KEY_HEIGHT = 11;
    private static final int SIDE_KEY_STEP = 30;
    private static final int KNOB = 7;
    /* A legend's line pointing at its key, and how far a click beside a legend still finds it. */
    private static final int POINTER = 4;
    private static final int LEGEND_FROM = 5;

    private FlightComputerFace() {
    }

    /** Draws the face across {@code w} by {@code h} as {@code readings} say, the face's pixels stretched to fit. */
    public static void paint(final GuiGraphics g, final int w, final int h, final CabinReadings readings) {
        paint(g, w, h, View.cabin(readings));
    }

    /**
     * Draws the face across {@code w} by {@code h} as {@code view} says, the face's pixels stretched to fit: its page,
     * the pages' names along the top, the open one boxed, and its keys' legends.
     */
    public static void paint(final GuiGraphics g, final int w, final int h, final View view) {
        final Font font = Minecraft.getInstance().font;
        g.pose().pushPose();
        g.pose().scale(w / (float) WIDTH, h / (float) HEIGHT, 1);
        final Colours c = PALETTE.get();
        bezel(g, font, c, view.readings().lit(CabinLamp.MASTER_ALARM));
        g.fill(SX0, SY0, SX1, SY1, c.screen());
        if (view.page() == FlightPage.TRANSFER) {
            FlightComputerPages.transfer(g, font, view, c);
        } else if (view.page() == FlightPage.LANDING) {
            FlightComputerPages.landing(g, font, view, c);
        } else if (view.page() == FlightPage.STAGES) {
            FlightComputerStages.stages(g, font, view, c);
        } else {
            final String clock = GameText.resolve(CLOCK);
            text(g, font, clock, SX0 + 8, SY1 - 11, c.cyan(), false);
            text(g, font, view.readings().timer(), SX0 + 14 + font.width(clock), SY1 - 11, c.white(), false);
        }
        for (int i = 0; i < PAGES.size(); i++) {
            final FlightPage page = FlightPage.KEYS.get(i);
            final String name = GameText.resolve(PAGES.get(i));
            final int x = topKeyX(i) + TOP_KEY_WIDTH / 2;
            final boolean open = page == view.page();
            if (open) {
                frame(g, x - font.width(name) / 2 - 2, SY0 + 1, x + (font.width(name) + 1) / 2 + 2, SY0 + 11,
                        c.white());
            }
            text(g, font, name, x, SY0 + 2, open ? c.white() : page.open() ? c.cyan() : c.legendOff(), true);
        }
        for (int i = 0; i < SIDE_KEYS; i++) {
            legend(g, font, view.left(), i, true, c);
            legend(g, font, view.right(), i, false, c);
        }
        g.pose().popPose();
    }

    /**
     * The key at {@code (x, y)} of the face, in its own pixels, or -1: a key's cap, or the legend beside it, by the
     * numbers {@link FlightComputerBlockEntity#press} takes.
     */
    public static int keyAt(final Font font, final View view, final double x, final double y) {
        for (int i = 0; i < PAGES.size(); i++) {
            if (x >= topKeyX(i) - 1 && x < topKeyX(i) + TOP_KEY_WIDTH + 1 && y >= SY0 - 21 && y < SY0 + 12) {
                return FlightComputerBlockEntity.TOP + i;
            }
        }
        for (int i = 0; i < SIDE_KEYS; i++) {
            final int top = sideKeyY(i) - 1;
            if (y < top || y >= top + KEY_HEIGHT + 2) {
                continue;
            }
            final int left = font.width(i < view.left().size() ? view.left().get(i).text() : "");
            if (x >= SX0 - 29 && x < SX0 + LEGEND_FROM + left + 2) {
                return FlightComputerBlockEntity.LEFT + i;
            }
            final int right = font.width(i < view.right().size() ? view.right().get(i).text() : "");
            if (x >= SX1 - LEGEND_FROM - right - 2 && x < SX1 + 29) {
                return FlightComputerBlockEntity.RIGHT + i;
            }
        }
        return -1;
    }

    /* The bezel, flat, with more of a display's parts, and its fail lamp lit when {@code failing}. */
    private static void bezel(final GuiGraphics g, final Font font, final Colours c, final boolean failing) {
        g.fill(0, 0, WIDTH, HEIGHT, c.bezel());
        hline(g, 0, WIDTH, 0, c.bezelLit());
        vline(g, 0, 0, HEIGHT, c.bezelLit());
        hline(g, 0, WIDTH, HEIGHT - 1, c.bezelShade());
        vline(g, WIDTH - 1, 0, HEIGHT, c.bezelShade());
        // A second edge just inside the outer one, as the bezel's face sits a step below its frame.
        frame(g, 3, 3, WIDTH - 3, HEIGHT - 3, c.bezelShade());
        hline(g, 4, WIDTH - 4, 4, c.rim());
        vline(g, 4, 4, HEIGHT - 4, c.rim());
        for (final int[] at : new int[][] {{8, 8}, {WIDTH - 12, 8}, {8, HEIGHT - 12}, {WIDTH - 12, HEIGHT - 12}}) {
            screw(g, at[0], at[1], c);
        }
        // The raised rim round the screen, then its dark gasket.
        g.fill(SX0 - 6, SY0 - 6, SX1 + 6, SY1 + 6, c.rim());
        hline(g, SX0 - 6, SX1 + 6, SY0 - 6, c.bezelLit());
        vline(g, SX0 - 6, SY0 - 6, SY1 + 6, c.bezelLit());
        hline(g, SX0 - 6, SX1 + 6, SY1 + 5, c.bezelShade());
        vline(g, SX1 + 5, SY0 - 6, SY1 + 6, c.bezelShade());
        g.fill(SX0 - 3, SY0 - 3, SX1 + 3, SY1 + 3, c.bezelShade());
        g.fill(SX0 - 2, SY0 - 2, SX1 + 2, SY1 + 2, c.gasket());
        // The maker's line, the fail lamp, the light sensor.
        text(g, font, GameText.resolve(MAKER), WIDTH / 2, 7, c.engrave(), true);
        g.fill(SX0 - 2, 6, SX0 + 22, 16, c.bezelShade());
        g.fill(SX0 - 1, 7, SX0 + 21, 15, failing ? c.amber() : c.failGlass());
        text(g, font, GameText.resolve(FAIL), SX0 + 10, 7, failing ? c.screen() : c.failText(), true);
        g.fill(SX1 - 8, 7, SX1 - 1, 14, c.bezelShade());
        g.fill(SX1 - 7, 8, SX1 - 2, 13, c.sensor());
        g.fill(SX1 - 6, 9, SX1 - 5, 10, c.sensorShine());
        for (int i = 0; i < PAGES.size(); i++) {
            key(g, topKeyX(i), SY0 - 20, TOP_KEY_WIDTH, c);
        }
        for (int i = 0; i < SIDE_KEYS; i++) {
            final int y = SY0 + 24 + i * SIDE_KEY_STEP;
            key(g, SX0 - 28, y, KEY_WIDTH, c);
            key(g, SX1 + 10, y, KEY_WIDTH, c);
            text(g, font, GameText.resolve(LEFT_KEY.with(i + 1)), SX0 - 41, y + 2, c.engraveDim(), false);
            text(g, font, GameText.resolve(RIGHT_KEY.with(i + 1)), SX1 + 31, y + 2, c.engraveDim(), false);
        }
        // Along the bottom: brightness and contrast rockers, the data plate, the night and day knob.
        rocker(g, font, SX0 - 4, SY1 + 13, GameText.resolve(BRIGHTNESS), c);
        rocker(g, font, SX0 + 36, SY1 + 13, GameText.resolve(CONTRAST), c);
        g.fill(SX0 + 92, SY1 + 16, SX0 + 200, SY1 + 28, c.plate());
        frame(g, SX0 + 92, SY1 + 16, SX0 + 200, SY1 + 28, c.bezelShade());
        text(g, font, GameText.resolve(PLATE), SX0 + 146, SY1 + 18, c.plateText(), true);
        knob(g, font, SX1 - 22, SY1 + 23, c);
    }

    /*
     * The legend of side key {@code i}, on the left or the right, as the approved face writes them: its action with a
     * short line pointing at the key, a chosen one white and boxed, one not in use dim; over the landing map, on its
     * own dark ground.
     */
    private static void legend(final GuiGraphics g, final Font font, final List<Legend> legends, final int i,
                               final boolean left, final Colours c) {
        if (i >= legends.size() || legends.get(i).text().isEmpty()) {
            return;
        }
        final Legend shown = legends.get(i);
        final int y = sideKeyY(i) + 2;
        final int colour = switch (shown.state()) {
            case CHOSEN -> c.white();
            case LIVE -> c.cyan();
            case OFF -> c.legendOff();
        };
        final int width = font.width(shown.text());
        final int x = left ? SX0 + LEGEND_FROM : SX1 - LEGEND_FROM - width;
        if (shown.backed()) {
            g.fill(x - 2, y - 1, x + width + 1, y + 9, c.backing());
        }
        if (left) {
            hline(g, SX0, SX0 + POINTER, y + 3, colour);
        } else {
            hline(g, SX1 - POINTER, SX1, y + 3, colour);
        }
        text(g, font, shown.text(), x, y, colour, false);
        if (shown.state() == Legend.State.CHOSEN) {
            frame(g, x - 2, y - 1, x + width + 2, y + 9, c.white());
        }
    }

    private static int sideKeyY(final int i) {
        return SY0 + 24 + i * SIDE_KEY_STEP;
    }

    private static int topKeyX(final int i) {
        final int step = (SX1 - SX0 - 16) / PAGES.size();
        return SX0 + 8 + i * step + (step - TOP_KEY_WIDTH) / 2;
    }

    /* A soft key, flat: its cap, a lit edge above, a dark one below, and the green bar of the night lighting. */
    private static void key(final GuiGraphics g, final int x, final int y, final int w, final Colours c) {
        g.fill(x - 1, y - 1, x + w + 1, y + KEY_HEIGHT + 1, c.bezelShade());
        g.fill(x, y, x + w, y + KEY_HEIGHT, c.key());
        hline(g, x, x + w, y, c.keyLit());
        hline(g, x, x + w, y + KEY_HEIGHT - 1, c.bezelShade());
        vline(g, x + w - 1, y, y + KEY_HEIGHT, c.bezelShade());
        hline(g, x + w / 2 - 3, x + w / 2 + 3, y + 4, c.nightLit());
    }

    private static void screw(final GuiGraphics g, final int x, final int y, final Colours c) {
        g.fill(x, y, x + 4, y + 4, c.bezelShade());
        g.fill(x + 1, y + 1, x + 3, y + 3, c.screw());
        g.fill(x + 1, y + 1, x + 2, y + 2, c.screwLit());
        g.fill(x + 2, y + 2, x + 3, y + 3, c.key());
    }

    /* A flat rocker, up and down: its two halves, an arrow on each, its name beside it. */
    private static void rocker(final GuiGraphics g, final Font font, final int x, final int y, final String name,
                               final Colours c) {
        g.fill(x - 1, y - 1, x + 11, y + 17, c.bezelShade());
        g.fill(x, y, x + 10, y + 16, c.key());
        hline(g, x, x + 10, y, c.keyLit());
        hline(g, x, x + 10, y + 7, c.bezelShade());
        hline(g, x, x + 10, y + 8, c.keyLit());
        for (int k = 0; k < 3; k++) {
            hline(g, x + 4 - k, x + 6 + k, y + 2 + k, c.engrave());
            hline(g, x + 4 - k, x + 6 + k, y + 13 - k, c.engrave());
        }
        text(g, font, name, x + 14, y + 4, c.engrave(), false);
    }

    /* The night and day knob, flat: its cap, its pointer at day, the three positions with their ticks. */
    private static void knob(final GuiGraphics g, final Font font, final int x, final int y, final Colours c) {
        for (int yy = y - KNOB; yy <= y + KNOB; yy++) {
            for (int xx = x - KNOB; xx <= x + KNOB; xx++) {
                final double d = Math.hypot(xx - x, yy - y);
                if (d <= KNOB) {
                    final int colour = d > KNOB - 1 ? c.bezelShade() : (xx - x) + (yy - y) < -3 ? c.keyLit()
                            : c.key();
                    g.fill(xx, yy, xx + 1, yy + 1, colour);
                }
            }
        }
        hline(g, x + 1, x + KNOB - 1, y, c.white());
        position(g, font, x, y, KNOB_OFF, 210, -2, c);
        position(g, font, x, y, KNOB_NIGHT, 150, -6, c);
        position(g, font, x, y, KNOB_DAY, 0, -4, c);
    }

    private static void position(final GuiGraphics g, final Font font, final int x, final int y, final TextKey name,
                                 final double degrees, final int lift, final Colours c) {
        final double a = Math.toRadians(degrees);
        final int tx = (int) Math.round(x + (KNOB + 2) * Math.cos(a));
        final int ty = (int) Math.round(y - (KNOB + 2) * Math.sin(a));
        g.fill(tx, ty, tx + 1, ty + 1, c.engrave());
        final String word = GameText.resolve(name);
        if (degrees > 90) {
            text(g, font, word, tx - 2 - font.width(word), ty + lift, c.engrave(), false);
        } else {
            text(g, font, word, tx + 3, ty + lift, c.engrave(), false);
        }
    }

    /* Text at {@code (x, y)}, or centred on {@code x}; for the pages too. */
    static void text(final GuiGraphics g, final Font font, final String text, final int x, final int y,
                     final int colour, final boolean centred) {
        Draw.text(g, font, text, centred ? x - font.width(text) / 2 : x, y, colour);
    }

    static void hline(final GuiGraphics g, final int x0, final int x1, final int y, final int colour) {
        g.fill(x0, y, x1, y + 1, colour);
    }

    static void vline(final GuiGraphics g, final int x, final int y0, final int y1, final int colour) {
        g.fill(x, y0, x + 1, y1, colour);
    }

    static void frame(final GuiGraphics g, final int x0, final int y0, final int x1, final int y1,
                      final int colour) {
        hline(g, x0, x1, y0, colour);
        hline(g, x0, x1, y1 - 1, colour);
        vline(g, x0, y0, y1, colour);
        vline(g, x1 - 1, y0, y1, colour);
    }

    /**
     * The colours of the Flight Computer's face.
     *
     * @param screen      the screen's ground
     * @param green       what is in hand
     * @param cyan        the keys' legends and the names of figures
     * @param white       a figure, and a page open
     * @param amber       what wants an eye, and the fail lamp lit
     * @param bezel       the bezel
     * @param bezelLit    the bezel's lit edges
     * @param bezelShade  the bezel's dark edges
     * @param rim         the raised rim round the screen
     * @param key         a key's cap
     * @param keyLit      a key's lit edge
     * @param nightLit    a key's night lighting
     * @param engrave     what is engraved on the bezel
     * @param engraveDim  the keys' numbers engraved
     * @param screw       a screw's head
     * @param screwLit    a screw's lit side
     * @param gasket      the gasket round the screen
     * @param failGlass   the fail lamp's glass unlit
     * @param failText    the fail lamp's word unlit
     * @param sensor      the light sensor's window
     * @param sensorShine the light sensor's shine
     * @param plate       the data plate
     * @param plateText   the data plate's words
     * @param space       space behind the system's drawing, and behind the landing page
     * @param ring        the rings of distance round the Sun, and a frame on the screen
     * @param ink         a world's name and a mark drawn on the system
     * @param sun         the Sun
     * @param overworld   the Overworld and its orbit
     * @param mars        Mars and its orbit
     * @param moon        the Moon
     * @param magenta     now, on the windows' tape
     * @param track       the windows' tape
     * @param trackTick   a tick of the windows' tape
     * @param windowPast  a window gone by
     * @param windowNext  the next window
     * @param backing     the dark ground of words over the landing map
     * @param legendOff   a legend of a key not in use, and a page not open
     */
    public record Colours(int screen, int green, int cyan, int white, int amber, int bezel, int bezelLit,
                          int bezelShade, int rim, int key, int keyLit, int nightLit, int engrave, int engraveDim,
                          int screw, int screwLit, int gasket, int failGlass, int failText, int sensor,
                          int sensorShine, int plate, int plateText, int space, int ring, int ink, int sun,
                          int overworld, int mars, int moon, int magenta, int track, int trackTick, int windowPast,
                          int windowNext, int backing, int legendOff) {
    }

    /**
     * A legend beside a side key.
     *
     * @param text   what it says
     * @param state  how it is shown
     * @param backed whether it stands on its own dark ground, over a map
     */
    public record Legend(String text, State state, boolean backed) {

        /** How a legend is shown: in use, chosen, or not in use. */
        public enum State {
            LIVE, CHOSEN, OFF
        }
    }

    /**
     * What the face shows.
     *
     * @param page        the page open; none for the face on a capsule's panel, which shows the clock
     * @param left        the left keys' legends, from the top
     * @param right       the right keys' legends, from the top
     * @param target      the name of the world the transfer goes to
     * @param plan        the transfer planned now, if one is
     * @param tick        the game's time, for where the worlds stand
     * @param mark        the spot marked to land on, if one is
     * @param zoom        how near the landing map is drawn
     * @param grid        whether the landing map has its grid
     * @param map         the landing map, once it has come
     * @param stages      the rocket's stages, where each goes and the one chosen
     * @param readings    what the cabin reads
     */
    public record View(@Nullable FlightPage page, List<Legend> left, List<Legend> right, String target,
                       Optional<TransferPlan> plan, long tick, @Nullable BlockPos mark, int zoom, boolean grid,
                       LandingMapsClient.@Nullable Shown map, FlightComputerStages.Shown stages,
                       CabinReadings readings) {

        public View {
            left = List.copyOf(left);
            right = List.copyOf(right);
        }

        /** The face on a capsule's panel: the clock, no page open. */
        public static View cabin(final CabinReadings readings) {
            return new View(null, List.of(), List.of(), "", Optional.empty(), 0, null,
                    FlightComputerBlockEntity.WIDE, false, null, FlightComputerStages.Shown.NONE, readings);
        }
    }
}
