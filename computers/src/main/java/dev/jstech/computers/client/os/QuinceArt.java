/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The small pictures and the round shapes of Quince's desktops, drawn a pixel at a time as the systems they parody drew
 * them: the fruit of the menu bar, the bomb, the alerts' signs, the round-cornered buttons and dialogs, and the three
 * lights of a window.
 *
 * <p>The pictures are rows of letters, a letter a pixel, each letter a colour of this class's palette
 * ({@code jsc:quince/art}) or of the face the picture is drawn on.
 */
@PaletteHolder
public final class QuinceArt {

    /** The fruit at the left of the menu bar, nine rows: the leaf, the stem, and the fruit. */
    public static final String[] FRUIT = {
        "     LL ",
        "    SL  ",
        " XXXSXX ",
        "XXXXXXXX",
        "XXXXXXXX",
        "XXXXXXXX",
        "XXXXXXXX",
        " XXXXXX ",
        "  XXXX  "
    };
    /** The bomb of a classic system that went down, sixteen by sixteen. */
    public static final String[] BOMB = {
        "          #  #  ",
        "        #  ##   ",
        "       #   #  # ",
        "  ######  #     ",
        "  #....#        ",
        " ##########     ",
        " #########.##   ",
        "#########.####  ",
        "##########.###  ",
        "############### ",
        "############### ",
        "############### ",
        " #############  ",
        " #############  ",
        "  ###########   ",
        "    #######     "
    };
    /** The stop sign of an alert that refuses: the hand in its octagon, in one bit. */
    public static final String[] STOP = {
        "    ########    ",
        "   #........#   ",
        "  #..........#  ",
        " #............# ",
        "#..............#",
        "#...#....#.....#",
        "#...##..##.....#",
        "#....####......#",
        "#....####......#",
        "#...##..##.....#",
        "#...#....#.....#",
        "#..............#",
        " #............# ",
        "  #..........#  ",
        "   #........#   ",
        "    ########    "
    };
    /** Quince OS 9's stop sign, the hand white on red. */
    public static final String[] STOP_RED = {
        "    RRRRRRRR    ",
        "   RrrrrrrrrR   ",
        "  RrrrrrrrrrrR  ",
        " RrrrrrrrrrrrrR ",
        "RrrrrrrrrrrrrrrR",
        "Rrr.rr.rr.rrrrrR",
        "Rrr.rr.rr.rr.rrR",
        "Rrr.rr.rr.rr.rrR",
        "Rrr........r.rrR",
        "Rrr...........rR",
        "Rrrr..........rR",
        "Rrrrr........rrR",
        " Rrrrr......rrR ",
        "  RrrrrrrrrrrR  ",
        "   RrrrrrrrrR   ",
        "    RRRRRRRR    "
    };
    /** The caution sign of an alert that asks first, in one bit. */
    public static final String[] CAUTION = {
        "       ##       ",
        "      #..#      ",
        "      #..#      ",
        "     #....#     ",
        "     #.##.#     ",
        "    #..##..#    ",
        "    #..##..#    ",
        "   #...##...#   ",
        "   #...##...#   ",
        "  #....##....#  ",
        "  #..........#  ",
        " #.....##.....# ",
        " #.....##.....# ",
        "#..............#",
        "################"
    };
    /** Quince OS 9's caution sign, yellow. */
    public static final String[] CAUTION_YELLOW = {
        "       dd       ",
        "      dyyd      ",
        "      dyyd      ",
        "     dyyyyd     ",
        "     dy##yd     ",
        "    dyy##yyd    ",
        "    dyy##yyd    ",
        "   dyyy##yyyd   ",
        "   dyyy##yyyd   ",
        "  dyyyy##yyyyd  ",
        "  dyyyyyyyyyyd  ",
        " dyyyyy##yyyyyd ",
        " dyyyyy##yyyyyd ",
        "dyyyyyyyyyyyyyyd",
        "dddddddddddddddd"
    };

    /** How many rows the fruit is, down which Pulp's gel goes from light to deep. */
    private static final int FRUIT_ROWS = 9;
    /* The three lights: the first one's centre from the bar's left, the space between centres and their radius. */
    private static final int LIGHT_FIRST = 4;
    private static final int LIGHT_PITCH = 10;
    private static final int LIGHT_RADIUS = 3;

    private static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "quince/art", new Colours(
            0xFF000000, 0xFFFFFFFF, 0xFFC8C8C8, 0xFF555555, 0xFF9C1E1E, 0xFFE23838, 0xFFF2D35A,
            0xFF5EBD3E, 0xFF6B4A2B, 0xFF5EBD3E, 0xFFFFB900, 0xFFF78200, 0xFFE23838, 0xFF973999, 0xFF009CDF,
            0xFF7CB6F0, 0xFF2266C4, 0xFF3A3A3E,
            0xFFFF5F57, 0xFFFFBD2E, 0xFF28C940, 0xFFFFB0AA, 0xFFFFE2A0, 0xFFA0EAAA, 0xFFC8C8C8, 0xFF000000));

    private QuinceArt() {
    }

    /** This class's colours, as its palette has them now. */
    static Colours colours() {
        return PALETTE.get();
    }

    /**
     * Draws a picture of letters with its top left at {@code (x, y)}, a letter a pixel; a space is left clear, and a
     * letter the picture's colours do not know is drawn in {@code fallback}.
     */
    public static void sprite(final GuiGraphics g, final String[] rows, final int x, final int y, final int fallback) {
        final Colours c = colours();
        for (int row = 0; row < rows.length; row++) {
            final String line = rows[row];
            int run = 0;
            // Runs of one letter are drawn as one fill, which is most of any of these pictures.
            while (run < line.length()) {
                final char letter = line.charAt(run);
                int end = run + 1;
                while (end < line.length() && line.charAt(end) == letter) {
                    end++;
                }
                if (letter != ' ') {
                    g.fill(x + run, y + row, x + end, y + row + 1, c.ink(letter, fallback));
                }
                run = end;
            }
        }
    }

    /** The fruit at the left of a menu bar, in the face's colours: black, striped, gel blue or dark. */
    public static void fruit(final GuiGraphics g, final QuinceLook look, final int x, final int y, final boolean lit) {
        final Colours c = colours();
        for (int row = 0; row < FRUIT.length; row++) {
            final int body = switch (look) {
                case ONE_BIT -> lit ? c.white() : c.black();
                case PLATINUM -> lit ? c.white() : c.rainbow(row);
                case PULP -> ChromeShapes.blend(c.pulpTop(), c.pulpBottom(), row / (float) (FRUIT_ROWS - 1));
                case FLAT, QUINCE_OS -> c.darkFruit();
            };
            final String line = FRUIT[row];
            for (int col = 0; col < line.length(); col++) {
                final char letter = line.charAt(col);
                if (letter == ' ') {
                    continue;
                }
                final int ink = look == QuinceLook.PLATINUM && !lit
                        ? (letter == 'L' ? c.leaf() : letter == 'S' ? c.stem() : body)
                        : body;
                g.fill(x + col, y + row, x + col + 1, y + row + 1, ink);
            }
        }
    }

    /**
     * Text drawn as a classic system drew what could not be chosen: the letters with every other pixel of them
     * knocked out to the ground they are on.
     */
    public static void dimmed(final GuiGraphics g, final Font font, final String text, final int x, final int y,
                       final int ink, final int ground) {
        Draw.text(g, font, text, x, y, ink);
        final int w = font.width(text);
        for (int yy = y + 1; yy < y + 8; yy += 2) {
            for (int xx = x + 1; xx < x + w; xx += 2) {
                g.fill(xx, yy, xx + 1, yy + 1, ground);
            }
        }
    }

    /**
     * A rectangle with its corners stepped round by {@code r} pixels, {@code t} pixels thick, filled with
     * {@code inside} when that is not zero: the buttons and the default ring of the classic systems.
     */
    public static void rounded(final GuiGraphics g, final int x0, final int y0, final int x1, final int y1, final int ink,
                        final int r, final int t, final int inside) {
        if (inside != 0) {
            g.fill(x0 + 1, y0 + r, x1 - 1, y1 - r, inside);
            g.fill(x0 + r, y0 + 1, x1 - r, y1 - 1, inside);
            for (int k = 1; k < r; k++) {
                final int trim = k == r - 1 ? 1 : 0;
                g.fill(x0 + r - k + trim, y0 + k, x1 - r + k - trim, y1 - k, inside);
            }
        }
        for (int k = 0; k < t; k++) {
            final int a0 = x0 + k;
            final int b0 = y0 + k;
            final int a1 = x1 - k;
            final int b1 = y1 - k;
            g.fill(a0 + r, b0, a1 - r, b0 + 1, ink);
            g.fill(a0 + r, b1 - 1, a1 - r, b1, ink);
            g.fill(a0, b0 + r, a0 + 1, b1 - r, ink);
            g.fill(a1 - 1, b0 + r, a1, b1 - r, ink);
            for (int i = 0; i < r - 1; i++) {
                final int dx = r - 1 - i;
                final int dy = i + 1;
                dot(g, a0 + dx, b0 + dy, ink);
                dot(g, a1 - 1 - dx, b0 + dy, ink);
                dot(g, a0 + dx, b1 - 1 - dy, ink);
                dot(g, a1 - 1 - dx, b1 - 1 - dy, ink);
            }
        }
    }

    /** A rectangle with its corners rounded smoothly by {@code r} pixels, filled: the later systems' shapes. */
    public static void roundRect(final GuiGraphics g, final int x0, final int y0, final int x1, final int y1, final int r,
                          final int ink) {
        for (int y = y0; y < y1; y++) {
            final int cut = cut(y, y0, y1, r);
            g.fill(x0 + cut, y, x1 - cut, y + 1, ink);
        }
    }

    /** The outline of {@link #roundRect}, a pixel wide. */
    public static void roundFrame(final GuiGraphics g, final int x0, final int y0, final int x1, final int y1, final int r,
                           final int ink) {
        for (int y = y0; y < y1; y++) {
            final int cut = cut(y, y0, y1, r);
            if (y == y0 || y == y1 - 1) {
                g.fill(x0 + cut, y, x1 - cut, y + 1, ink);
            } else {
                dot(g, x0 + cut, y, ink);
                dot(g, x1 - 1 - cut, y, ink);
            }
        }
    }

    /** A filled circle of radius {@code r} round {@code (cx, cy)}, with its upper left lit when {@code light} is set. */
    public static void disc(final GuiGraphics g, final int cx, final int cy, final int r, final int ink, final int light) {
        for (int y = -r; y <= r; y++) {
            int from = Integer.MAX_VALUE;
            int to = Integer.MIN_VALUE;
            for (int x = -r; x <= r; x++) {
                if (x * x + y * y <= r * r + r * 0.6) {
                    from = Math.min(from, x);
                    to = Math.max(to, x);
                }
            }
            if (from <= to) {
                g.fill(cx + from, cy + y, cx + to + 1, cy + y + 1, ink);
            }
        }
        if (light != 0) {
            for (int y = -r; y < -r / 3; y++) {
                for (int x = -r + 1; x < r; x++) {
                    if (x * x + y * y <= (r - 1) * (r - 1)) {
                        dot(g, cx + x, cy + y, light);
                    }
                }
            }
        }
    }

    /** A raised face: lit along its top and left, shaded along its foot and right. */
    public static void bevel(final GuiGraphics g, final int x0, final int y0, final int x1, final int y1, final int face,
                      final int light, final int shade) {
        g.fill(x0, y0, x1, y1, face);
        g.fill(x0, y0, x1, y0 + 1, light);
        g.fill(x0, y0, x0 + 1, y1, light);
        g.fill(x0, y1 - 1, x1, y1, shade);
        g.fill(x1 - 1, y0, x1, y1, shade);
    }

    /**
     * A push button in the face's own shape, {@code w} wide and {@code h} tall from {@code (x, y)}: the round-cornered
     * outline of the classic systems with the ring of the default one round it, the gel of Pulp, or the later faces'
     * plain round button. The label is the caller's to write.
     */
    public static void button(final GuiGraphics g, final QuinceLook look, final int x, final int y, final int w, final int h,
                       final boolean primary, final boolean pressed) {
        final QuinceLook.Colours c = look.colours();
        switch (look) {
            case ONE_BIT -> {
                if (primary) {
                    rounded(g, x - 3, y - 3, x + w + 3, y + h + 3, c.frame(), 4, 2, 0);
                }
                rounded(g, x, y, x + w, y + h, c.frame(), 3, 1, pressed ? c.frame() : c.button());
            }
            case PLATINUM -> {
                if (primary) {
                    rounded(g, x - 3, y - 3, x + w + 3, y + h + 3, c.shadow(), 4, 2, 0);
                }
                rounded(g, x, y, x + w, y + h, c.shadow(), 3, 1, pressed ? c.shade() : c.button());
                g.fill(x + 2, y + 1, x + w - 2, y + 2, c.light());
                g.fill(x + 2, y + h - 2, x + w - 2, y + h - 1, c.stripe());
            }
            case PULP -> {
                final int top = primary ? c.defaultTop() : c.button();
                final int foot = primary ? c.defaultFoot() : c.buttonFoot();
                roundRect(g, x, y, x + w, y + h, h / 2, primary ? c.defaultRim() : c.frame());
                for (int yy = y + 1; yy < y + h - 1; yy++) {
                    final int shade = ChromeShapes.blend(top, foot, (yy - y) / (float) h);
                    g.fill(x + 2, yy, x + w - 2, yy + 1, pressed ? ChromeShapes.darken(shade) : shade);
                }
                g.fill(x + 4, y + 2, x + w - 4, y + 4, ChromeShapes.blend(top, c.light(), 0.6F));
            }
            case FLAT, QUINCE_OS -> {
                roundRect(g, x, y, x + w, y + h, 3, primary ? c.defaultRim() : c.frame());
                final int face = primary ? c.defaultTop() : c.button();
                roundRect(g, x + 1, y + 1, x + w - 1, y + h - 1, 2, pressed ? ChromeShapes.darken(face) : face);
            }
        }
    }

    /** What a button's label is written in on that face. */
    public static int buttonInk(final QuinceLook look, final boolean primary) {
        final QuinceLook.Colours c = look.colours();
        return primary ? c.defaultInk() : c.ink();
    }

    /**
     * A dialog's ground and frame: the double black frame of the Quince System, the bevelled Platinum box of Quince OS
     * 9 with its drop shadow, or a sheet of the later faces.
     */
    public static void dialog(final GuiGraphics g, final QuinceLook look, final int x0, final int y0, final int x1,
                       final int y1) {
        final QuinceLook.Colours c = look.colours();
        switch (look) {
            case ONE_BIT -> {
                g.fill(x0, y0, x1, y1, c.face());
                ChromeShapes.outline(g, x0, y0, x1 - x0, y1 - y0, c.frame());
                ChromeShapes.outline(g, x0 + 2, y0 + 2, x1 - x0 - 4, y1 - y0 - 4, c.frame());
                ChromeShapes.outline(g, x0 + 3, y0 + 3, x1 - x0 - 6, y1 - y0 - 6, c.frame());
            }
            case PLATINUM -> {
                g.fill(x0 + 3, y0 + 3, x1 + 3, y1 + 3, ChromeShapes.blend(0, c.shadow(), 0.3F));
                bevel(g, x0, y0, x1, y1, c.face(), c.light(), c.shade());
                ChromeShapes.outline(g, x0, y0, x1 - x0, y1 - y0, c.shadow());
                bevel(g, x0 + 2, y0 + 2, x1 - 2, y1 - 2, c.face(), c.light(), c.shade());
            }
            case PULP, FLAT, QUINCE_OS -> {
                roundRect(g, x0 - 1, y0 - 1, x1 + 1, y1 + 1, Math.max(2, look.round()), c.frame());
                roundRect(g, x0, y0, x1, y1, Math.max(1, look.round() - 1), c.face());
            }
        }
    }

    /**
     * A window's three lights from {@code (x, y)}: close, minimise and zoom, red, amber and green while the window is
     * in front and grey behind it; Pulp's are gel, lit at the top.
     */
    public static void lights(final GuiGraphics g, final QuinceLook look, final int x, final int y, final boolean active) {
        final Colours c = colours();
        for (int k = 0; k < 3; k++) {
            final int cx = x + LIGHT_FIRST + k * LIGHT_PITCH;
            final int face = active ? c.light(k) : c.idleLight();
            disc(g, cx, y + LIGHT_FIRST + 1, LIGHT_RADIUS, ChromeShapes.blend(face, c.lightRing(), 0.25F), 0);
            disc(g, cx, y + LIGHT_FIRST + 1, LIGHT_RADIUS - 1, face,
                    active && look == QuinceLook.PULP ? c.lightGleam(k) : 0);
        }
    }

    /** The left edge of the box the light {@code index} (close, minimise, zoom) answers clicks in, from {@code x}. */
    public static int lightLeft(final int x, final int index) {
        return x + LIGHT_FIRST + index * LIGHT_PITCH - LIGHT_RADIUS;
    }

    /** How wide the box a light answers clicks in is. */
    public static int lightSize() {
        return LIGHT_RADIUS * 2 + 1;
    }

    /**
     * A whole window of the face, from {@code (x0, y0)} to {@code (x1, y1)}, titled {@code title}, and answers the top
     * of its body: the Quince System's striped title with its close box, Platinum's pinstripes with the close, zoom and
     * collapse boxes, or the later faces' title with the three lights.
     */
    public static int window(final GuiGraphics g, final Font font, final QuinceLook look, final int x0, final int y0,
                             final int x1, final int y1, final String title, final boolean active) {
        final QuinceLook.Colours c = look.colours();
        switch (look) {
            case ONE_BIT -> {
                g.fill(x0 + 1, y1, x1 + 1, y1 + 1, c.shadow());
                g.fill(x1, y0 + 1, x1 + 1, y1 + 1, c.shadow());
                g.fill(x0, y0, x1, y1, c.face());
                ChromeShapes.outline(g, x0, y0, x1 - x0, y1 - y0, c.frame());
                g.fill(x0, y0 + 12, x1, y0 + 13, c.frame());
                final int tw = font.width(title);
                final int gap0 = (x0 + x1) / 2 - tw / 2 - 5;
                final int gap1 = (x0 + x1) / 2 + tw / 2 + 5;
                if (active) {
                    for (int y = y0 + 2; y < y0 + 11; y += 2) {
                        g.fill(x0 + 2, y, gap0, y + 1, c.stripe());
                        g.fill(gap1, y, x1 - 2, y + 1, c.stripe());
                    }
                    g.fill(x0 + 7, y0 + 2, x0 + 18, y0 + 11, c.face());
                    ChromeShapes.outline(g, x0 + 8, y0 + 2, 9, 9, c.frame());
                }
                Draw.textCentered(g, font, title, (x0 + x1) / 2, y0 + 3, c.titleInk());
                return y0 + 13;
            }
            case PLATINUM -> {
                g.fill(x0 + 2, y0 + 2, x1 + 2, y1 + 2, ChromeShapes.blend(0, c.shadow(), 0.3F));
                bevel(g, x0, y0, x1, y1, c.face(), c.light(), c.shade());
                ChromeShapes.outline(g, x0, y0, x1 - x0, y1 - y0, c.frame());
                if (active) {
                    for (int y = y0 + 3; y < y0 + 11; y += 2) {
                        g.fill(x0 + 4, y, x1 - 4, y + 1, c.stripe());
                        g.fill(x0 + 4, y + 1, x1 - 4, y + 2, c.light());
                    }
                    final int mid = (x0 + x1) / 2;
                    final int tw = font.width(title);
                    g.fill(mid - tw / 2 - 6, y0 + 2, mid + tw / 2 + 6, y0 + 12, c.face());
                    for (final int bx : new int[] {x0 + 6, x1 - 15, x1 - 27}) {
                        bevel(g, bx, y0 + 2, bx + 10, y0 + 12, c.face(), c.shade(), c.light());
                        bevel(g, bx + 1, y0 + 3, bx + 9, y0 + 11, c.track(), c.light(), c.shade());
                    }
                    g.fill(x1 - 13, y0 + 6, x1 - 7, y0 + 7, c.shadow());
                    ChromeShapes.outline(g, x1 - 24, y0 + 5, 7, 5, c.shadow());
                }
                Draw.textCentered(g, font, title, (x0 + x1) / 2, y0 + 3, active ? c.titleInk() : c.titleIdle());
                return y0 + 14;
            }
            default -> {
                final int r = look.round();
                g.fill(x0 + 3, y0 + 4, x1 + 3, y1 + 3, ChromeShapes.blend(0, c.shadow(), 0.27F));
                roundRect(g, x0, y0, x1, y1, r, c.frame());
                roundRect(g, x0 + 1, y0 + 1, x1 - 1, y1 - 1, Math.max(r - 1, 0), c.face());
                if (r == 0) {
                    ChromeShapes.vGradient(g, x0 + 1, y0 + 1, x1 - 1, y0 + 13, c.titleTop(), c.titleBottom());
                }
                g.fill(x0 + 1, y0 + 13, x1 - 1, y0 + 14, c.shade());
                lights(g, look, x0 + 4, y0 + 2, active);
                Draw.textCentered(g, font, title, (x0 + x1) / 2, y0 + 3, active ? c.titleInk() : c.titleIdle());
                return y0 + 14;
            }
        }
    }

    private static void dot(final GuiGraphics g, final int x, final int y, final int ink) {
        g.fill(x, y, x + 1, y + 1, ink);
    }

    /* How far a row of a round rectangle is cut in at each end by its corners. */
    private static int cut(final int y, final int y0, final int y1, final int r) {
        final int dy = Math.max(Math.max(y0 + r - 1 - y, y - (y1 - r)), 0);
        return dy == 0 ? 0 : r - (int) Math.sqrt(Math.max(0, r * r - dy * dy));
    }

    /**
     * The colours of the pictures and of the lights.
     *
     * @param black      a one-bit picture's black, and its {@code #}
     * @param white      a one-bit picture's white, and its {@code .}
     * @param grey       {@code g}: the bomb's fuse ground on Quince OS 9
     * @param dark       {@code d}: a coloured picture's outline
     * @param darkRed    {@code R}: the stop sign's rim
     * @param red        {@code r}: the stop sign
     * @param yellow     {@code y}: the caution sign
     * @param leaf       the fruit's leaf
     * @param stem       the fruit's stem
     * @param green      the fruit's first stripe
     * @param gold       its second
     * @param orange     its third
     * @param scarlet    its fourth
     * @param violet     its fifth
     * @param blue       its sixth
     * @param pulpTop    the gel fruit at its top
     * @param pulpBottom the gel fruit at its foot
     * @param darkFruit  the later faces' fruit
     * @param close      the close light
     * @param minimise   the minimise light
     * @param zoom       the zoom light
     * @param closeGleam the close light's gel gleam
     * @param minGleam   the minimise light's
     * @param zoomGleam  the zoom light's
     * @param idleLight  a light of a window behind
     * @param lightRing  what a light's rim is darkened toward
     */
    record Colours(int black, int white, int grey, int dark, int darkRed, int red, int yellow, int leaf, int stem,
                   int green, int gold, int orange, int scarlet, int violet, int blue, int pulpTop, int pulpBottom,
                   int darkFruit, int close, int minimise, int zoom, int closeGleam, int minGleam, int zoomGleam,
                   int idleLight, int lightRing) {

        /** The colour a picture's letter stands for, or {@code fallback} for one it does not know. */
        int ink(final char letter, final int fallback) {
            return switch (letter) {
                case '#' -> black;
                case '.' -> white;
                case 'g' -> grey;
                case 'd' -> dark;
                case 'R' -> darkRed;
                case 'r' -> red;
                case 'y' -> yellow;
                default -> fallback;
            };
        }

        /** The fruit's stripe at that row: green, green, gold, orange, red, red, violet, blue, blue. */
        int rainbow(final int row) {
            return switch (row) {
                case 0, 1 -> green;
                case 2 -> gold;
                case 3 -> orange;
                case 4, 5 -> scarlet;
                case 6 -> violet;
                default -> blue;
            };
        }

        /** The light at that place: close, minimise, zoom. */
        int light(final int index) {
            return index == 0 ? close : index == 1 ? minimise : zoom;
        }

        /** That light's gleam on Pulp's gel. */
        int lightGleam(final int index) {
            return index == 0 ? closeGleam : index == 1 ? minGleam : zoomGleam;
        }
    }
}
