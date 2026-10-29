/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.material;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * How a material's item looks in each form, drawn as the game loads: every form the game has a texture for comes from
 * that texture recoloured (the light and shade of each texel kept, only the colour changed to the material's), so no
 * texel of the game ships in the Core; the forms the game has no texture for are the Core's own templates, coloured
 * the same way. A material's colour gives a ramp: its shadows cooler and deeper, its lights warmer and paler.
 *
 * <p>Images are 16 by 16 texels, row by row, each an ARGB integer; 0 is empty. Pure: no game types.
 */
public final class FormArt {

    /** How wide and tall a texture is. */
    public static final int SIZE = 16;

    /* The light each digit of a template stands for, darkest first. */
    private static final double[] LEVELS = {0.06, 0.2, 0.34, 0.47, 0.6, 0.72, 0.84, 0.94, 1.0};
    /* The tones of a template drawn by hand, outline first, glint last. */
    private static final double[] TONES = {0.08, 0.3, 0.5, 0.66, 0.82, 0.96};
    private static final double MIDDLE = 0.6;
    private static final double COOL_HUE = 0.66;
    private static final double WARM_HUE = 0.14;
    private static final double DARK_MATERIAL = 0.45;
    private static final double DARK_MATERIAL_LIGHT = 0.82;
    private static final int OPAQUE = 0xFF << 24;

    /** The game's textures the forms it has are drawn from. */
    private static final Map<String, String> FROM_THE_GAME = Map.ofEntries(
            Map.entry("ingot", "textures/item/iron_ingot.png"), Map.entry("nugget", "textures/item/gold_nugget.png"),
            Map.entry("raw", "textures/item/raw_iron.png"), Map.entry("dust", "textures/item/glowstone_dust.png"),
            Map.entry("wire", "textures/item/string.png"), Map.entry("lens", "textures/item/ender_pearl.png"),
            Map.entry("rod", "textures/item/stick.png"), Map.entry("gem", "textures/item/emerald.png"),
            Map.entry("crystal", "textures/block/amethyst_cluster.png"),
            Map.entry("block", "textures/block/gold_block.png"));
    /* A gem's block is drawn from the emerald's block, a metal's from the gold's. */
    private static final String GEM_BLOCK = "textures/block/emerald_block.png";

    /*
     * The Core's own templates, in the light each digit stands for. A plate, a sheet and a foil lie across the whole
     * width, outlined dark at both their ends, so neither end reads as cut off by the slot's edge.
     */
    private static final Map<String, List<String>> LIGHT_TEMPLATES = Map.of(
            "plate", List.of("................", "................", "........22......", "......22461.....",
                    "....225664412...", "..2256655554412.", "2256656555554440", "1365556655555530",
                    "0355555655552220", "023355566522220.", "01333355222200..", "..0133322220....",
                    "....0131100.....", "......000.......", "................", "................"),
            "sheet", List.of("................", "................", "........22......", "......22461.....",
                    "....225664412...", "..2256655554412.", "2256656555554440", "1365556655555520",
                    "035555565555220.", "01335556652200..", "..0133552220....", "....0132200.....",
                    "......000.......", "................", "................", "................"),
            "foil", List.of("................", "................", ".......222......", ".....223361.....",
                    "...2275676412...", ".22556557754512.", "2557633455446760", "1388644554335220",
                    "023655687542200.", ".013346652200...", "...01332200.....", ".....0000.......",
                    "................", "................", "................", "................"),
            "frame", List.of(".....226612.....", "...2266666612...", ".22666611666612.", "2666601110166661",
                    "66611.2110.11666", "4666612111266662", "4416666116666122", "4400166666600222",
                    "4402211661112222", "4411111331111122", "4411101330111122", "44211.2330.11222",
                    "1222212331222220", ".01222233222200.", "...0122332200...", ".....013300....."),
            "gear", List.of("......2612......", "...2.2646102....", "..26126421261...", ".2646644466461..",
                    "..1644666442110.", ".2264622224211..", "266462111624661.", "6444621016244460",
                    "1624621126242210", "2116426662421110", ".02644222442100.", ".2642244422461..",
                    "..1611642116110.", "...11164210110..", "....0.16110.0...", ".......000......"),
            "spring", List.of("....2222........", "..2243331222....", ".233222223341...", "..120001112441..",
                    "...122221115530.", "..245366666440..", ".2344444433440..", "..000000011350..",
                    "..222222226650..", ".2435666644430..", "..133334432441..", "...100001112530.",
                    "..251222666640..", ".234444444430...", "..0133340000....", "....0000........"),
            "boule", List.of("................", "..........2222..", ".........266630.", "........2655320.",
                    ".......26753320.", "......267533220.", ".....267533220..", "....267533220...",
                    "...267533220....", "..267533220.....", ".265533220......", ".26533220.......",
                    ".2633220........", ".232220.........", "..0000..........", "................"));

    /* The bolt, drawn texel by texel in tones, as the stick is shaded. */
    private static final List<String> BOLT = List.of("................", "....00000000....", "...0555544440...",
            "...0444433320...", "...0434433220...", "...0434433220...", "...0222221110...", "....00000000....",
            ".....054320.....", ".....043210.....", ".....054320.....", ".....043210.....", ".....054320.....",
            ".....043210.....", ".....032110.....", "......0000......");

    /* The coil: the material's wire wound round a steel spool, the spool's steel the same for every material. */
    private static final List<String> COIL = List.of("................", "....aaaaaaaa....", "..aaeeeeeeddaa..",
            ".acddddddcccbba.", "..aaaaaaaaaaaa..", "...0554444320...", "...0333332210...", "...0554444320...",
            "...0333332210...", "...0554444320...", "...0333332210...", "..aaaaaaaaaaaa..", ".acddddddcccbba.",
            "..aaccccccbbaa..", "....aaaaaaaa....", "................");
    private static final Map<Character, Integer> SPOOL = Map.of('a', rgb(38, 40, 44), 'b', rgb(70, 74, 80),
            'c', rgb(104, 109, 116), 'd', rgb(138, 144, 152), 'e', rgb(176, 182, 190));

    /* The faint rainbow a wafer's face throws, across it from the top left. */
    private static final int[][] RAINBOW = {{255, 120, 120}, {255, 210, 110}, {140, 230, 140}, {110, 190, 255},
            {190, 140, 255}};
    private static final double RAINBOW_SHARE = 0.2;

    /* How far a texel of an ore has to be from its rock's, the three channels summed, to be a speck of it. */
    private static final int SPECK_DIFFERENCE = 40;
    /* A speck greyer than this keeps the mineral's main colour, whatever its hue. */
    private static final double SECOND_SATURATION = 0.25;
    /* A shining speck's darkest outline, as a share of the mineral's colour, which is at the specks' middle light. */
    private static final double SHINE_OUTLINE = 0.45;
    private static final double SHINE_MIDDLE = 0.5;

    private FormArt() {
    }

    /**
     * The game's texture {@code form} is drawn from, recoloured, as a path in the game's own namespace, or none when
     * the Core draws the form itself.
     */
    public static Optional<String> gameTexture(final String form, final boolean gem) {
        if ("block".equals(form) && gem) {
            return Optional.of(GEM_BLOCK);
        }
        return Optional.ofNullable(FROM_THE_GAME.get(form));
    }

    /** Whether the Core draws {@code form} itself, from a template of its own. */
    public static boolean drawnByTheCore(final String form) {
        return LIGHT_TEMPLATES.containsKey(form) || "bolt".equals(form) || "coil".equals(form)
                || "wafer".equals(form);
    }

    /** The forms the Core draws or recolours, every one it knows. */
    public static List<String> forms() {
        return List.of("ingot", "nugget", "block", "dust", "plate", "sheet", "wire", "rod", "bolt", "gear", "coil",
                "spring", "foil", "frame", "lens", "boule", "wafer", "crystal", "gem", "raw");
    }

    /**
     * A texture recoloured in a material's colour: each texel's light, taken between the texture's darkest and its
     * lightest and bent so its middle tone lands on the material's own colour, read through the material's ramp. A
     * dark material keeps its glints dark too. Only the first 16 by 16 texels are read, the first frame of a texture
     * that moves; the source is {@code width} texels wide.
     */
    public static int[] recolour(final int[] source, final int width, final int colour) {
        final Ramp ramp = new Ramp(colour);
        final boolean dark = Math.max(red(colour), Math.max(green(colour), blue(colour))) / 255.0 < DARK_MATERIAL;
        final double[] values = new double[SIZE * SIZE];
        int count = 0;
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                final int texel = source[y * width + x];
                if (alpha(texel) > 0) {
                    values[count++] = light(texel);
                }
            }
        }
        final int[] out = new int[SIZE * SIZE];
        if (count == 0) {
            return out;
        }
        final double[] sorted = Arrays.copyOf(values, count);
        Arrays.sort(sorted);
        final double lowest = sorted[0];
        final double span = Math.max(1.0, sorted[count - 1] - lowest);
        final double middle = Math.min(0.95, Math.max(0.05, (sorted[count / 2] - lowest) / span));
        final double gamma = Math.log(MIDDLE) / Math.log(middle);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                final int texel = source[y * width + x];
                if (alpha(texel) == 0) {
                    continue;
                }
                final double t = Math.pow((light(texel) - lowest) / span, gamma);
                final int tone = ramp.at(dark ? Math.min(t, DARK_MATERIAL_LIGHT) : t);
                out[y * SIZE + x] = (alpha(texel) << 24) | (tone & 0xFFFFFF);
            }
        }
        return out;
    }

    /**
     * A form the Core draws itself, in a material's colour.
     *
     * @throws IllegalArgumentException when the Core does not draw {@code form}
     */
    public static int[] drawn(final String form, final int colour) {
        final Ramp ramp = new Ramp(colour);
        final List<String> template = LIGHT_TEMPLATES.get(form);
        if (template != null) {
            return grid(template, LEVELS, ramp);
        }
        return switch (form) {
            case "bolt" -> grid(BOLT, TONES, ramp);
            case "coil" -> coil(ramp);
            case "wafer" -> wafer(ramp);
            default -> throw new IllegalArgumentException("the Core draws no " + form);
        };
    }

    /** The colour of light {@code lum} (0 to 1) in a material of colour {@code colour}, as the ramp makes it. */
    public static int ramp(final int colour, final double lum) {
        return new Ramp(colour).at(lum);
    }

    /**
     * The specks of a game's ore: the texels where it differs from the rock it is drawn on, as they are in the ore,
     * every other texel empty. Both are read 16 by 16; the ore is {@code oreWidth} texels wide, the rock
     * {@code rockWidth}.
     */
    public static int[] specks(final int[] ore, final int oreWidth, final int[] rock, final int rockWidth) {
        final int[] out = new int[SIZE * SIZE];
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                final int a = ore[y * oreWidth + x];
                final int b = rock[y * rockWidth + x];
                final int difference = Math.abs(red(a) - red(b)) + Math.abs(green(a) - green(b))
                        + Math.abs(blue(a) - blue(b));
                if (difference > SPECK_DIFFERENCE) {
                    out[y * SIZE + x] = a;
                }
            }
        }
        return out;
    }

    /**
     * An ore: the rock, {@code rockWidth} texels wide and read 16 by 16, with the specks laid on it in a mineral's
     * colour, or, when it has a {@code sheen}, shining with it ({@link #shine}). When the mineral has a second colour,
     * the specks whose hue lies from {@code lowHue} to {@code highHue} (0 to 1) and that are not grey take it instead:
     * the green of copper's patina.
     */
    public static int[] ore(final int[] rock, final int rockWidth, final int[] specks, final int colour,
                            final OptionalInt sheen, final OptionalInt second, final double lowHue,
                            final double highHue) {
        final int[] main = sheen.isPresent() ? shine(specks, colour, sheen.getAsInt()) : recolour(specks, SIZE, colour);
        final int[] alt = second.isPresent() ? recolour(specks, SIZE, second.getAsInt()) : main;
        final int[] out = new int[SIZE * SIZE];
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                final int i = y * SIZE + x;
                final int speck = specks[i];
                if (alpha(speck) == 0) {
                    out[i] = rock[y * rockWidth + x];
                    continue;
                }
                final double[] hsv = Hsv.of(red(speck) / 255.0, green(speck) / 255.0, blue(speck) / 255.0);
                final boolean secondHue = second.isPresent() && hsv[1] > SECOND_SATURATION && hsv[0] >= lowHue
                        && hsv[0] <= highHue;
                out[i] = secondHue ? alt[i] : main[i];
            }
        }
        return out;
    }

    /**
     * Specks, 16 by 16, drawn as a metallic or glassy mineral glints: each speck's light, taken between the specks'
     * darkest and brightest, gives its dark outline at a little under half of {@code colour}, its body at the colour,
     * and its highlights rising to {@code sheen}. A black mineral shows on a dark rock so, where the ramp of its colour
     * alone keeps it dark. Empty texels stay empty.
     */
    public static int[] shine(final int[] specks, final int colour, final int sheen) {
        double lowest = Double.MAX_VALUE;
        double highest = -Double.MAX_VALUE;
        for (final int speck : specks) {
            if (alpha(speck) > 0) {
                lowest = Math.min(lowest, light(speck));
                highest = Math.max(highest, light(speck));
            }
        }
        final double span = Math.max(1e-6, highest - lowest);
        final int[] out = new int[SIZE * SIZE];
        for (int i = 0; i < SIZE * SIZE; i++) {
            final int speck = specks[i];
            if (alpha(speck) == 0) {
                continue;
            }
            final double t = Math.max(0.0, Math.min(1.0, (light(speck) - lowest) / span));
            final int shone = t < SHINE_MIDDLE ? mix(scaled(colour, SHINE_OUTLINE), colour, t / SHINE_MIDDLE)
                    : mix(colour, sheen, (t - SHINE_MIDDLE) / (1 - SHINE_MIDDLE));
            out[i] = (alpha(speck) << 24) | (shone & 0xFFFFFF);
        }
        return out;
    }

    private static int[] grid(final List<String> rows, final double[] light, final Ramp ramp) {
        final int[] out = new int[SIZE * SIZE];
        for (int y = 0; y < SIZE; y++) {
            final String row = rows.get(y);
            for (int x = 0; x < SIZE; x++) {
                final char c = row.charAt(x);
                if (c != '.') {
                    out[y * SIZE + x] = ramp.at(light[c - '0']);
                }
            }
        }
        return out;
    }

    private static int[] coil(final Ramp ramp) {
        final int[] out = new int[SIZE * SIZE];
        for (int y = 0; y < SIZE; y++) {
            final String row = COIL.get(y);
            for (int x = 0; x < SIZE; x++) {
                final char c = row.charAt(x);
                final Integer steel = SPOOL.get(c);
                if (steel != null) {
                    out[y * SIZE + x] = steel;
                } else if (c != '.') {
                    out[y * SIZE + x] = ramp.at(TONES[c - '0']);
                }
            }
        }
        return out;
    }

    /*
     * A wafer seen almost from above: a round thin disc, its edge showing below, the grid of its dies on the mirror of
     * its face, and the faint rainbow a wafer's face throws.
     */
    private static int[] wafer(final Ramp ramp) {
        final boolean[] top = new boolean[SIZE * SIZE];
        final boolean[] rim = new boolean[SIZE * SIZE];
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                final double cx = x + 0.5;
                final double cy = y + 0.5;
                if (cy > 13.4) {
                    continue;
                }
                if (ellipse(cx, cy, 7.4) <= 1 && cy <= 12.4) {
                    top[y * SIZE + x] = true;
                } else if (ellipse(cx, cy, 8.4) <= 1) {
                    rim[y * SIZE + x] = true;
                }
            }
        }
        final int[] out = new int[SIZE * SIZE];
        for (int i = 0; i < SIZE * SIZE; i++) {
            if (rim[i]) {
                out[i] = ramp.at(TONES[1]);
            }
        }
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                if (!top[y * SIZE + x]) {
                    continue;
                }
                final boolean inner = in(top, x - 1, y) && in(top, x + 1, y) && in(top, x, y - 1) && in(top, x, y + 1);
                final boolean dieLine = x % 3 == 2 || y % 3 == 2;
                final boolean edgeLit = !in(top, x - 1, y) || !in(top, x, y - 1);
                final int tone = edgeLit ? 4 : !inner ? 3 : dieLine ? 2 : 3;
                final int base = ramp.at(TONES[tone]);
                final int[] band = RAINBOW[Math.min(4, Math.max(0, Math.floorDiv(x + y - 6, 4)))];
                out[y * SIZE + x] = OPAQUE | blend(red(base), band[0]) << 16 | blend(green(base), band[1]) << 8
                        | blend(blue(base), band[2]);
            }
        }
        for (final int[] glint : new int[][] {{4, 4}, {5, 3}, {4, 5}}) {
            out[glint[1] * SIZE + glint[0]] = ramp.at(TONES[5]);
        }
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                if (in(top, x, y) || in(rim, x, y)) {
                    continue;
                }
                if (in(top, x + 1, y) || in(rim, x + 1, y) || in(top, x, y + 1) || in(rim, x, y + 1)) {
                    out[y * SIZE + x] = ramp.at(TONES[1]);
                } else if (in(top, x - 1, y) || in(rim, x - 1, y) || in(top, x, y - 1) || in(rim, x, y - 1)) {
                    out[y * SIZE + x] = ramp.at(TONES[0]);
                }
            }
        }
        return out;
    }

    private static double ellipse(final double cx, final double cy, final double centreY) {
        return Math.pow((cx - 8.0) / 6.8, 2) + Math.pow((cy - centreY) / 5.9, 2);
    }

    private static boolean in(final boolean[] shape, final int x, final int y) {
        return x >= 0 && x < SIZE && y >= 0 && y < SIZE && shape[y * SIZE + x];
    }

    private static int blend(final int base, final int band) {
        return (int) (base * (1 - RAINBOW_SHARE) + band * RAINBOW_SHARE);
    }

    private static double light(final int texel) {
        return 0.3 * red(texel) + 0.59 * green(texel) + 0.11 * blue(texel);
    }

    private static int rgb(final int r, final int g, final int b) {
        return OPAQUE | r << 16 | g << 8 | b;
    }

    /* The colour {@code share} of the way from {@code from} to {@code to}, channel by channel. */
    private static int mix(final int from, final int to, final double share) {
        return rgb(channel(red(from) + (red(to) - red(from)) * share),
                channel(green(from) + (green(to) - green(from)) * share),
                channel(blue(from) + (blue(to) - blue(from)) * share));
    }

    private static int scaled(final int colour, final double share) {
        return rgb(channel(red(colour) * share), channel(green(colour) * share), channel(blue(colour) * share));
    }

    private static int channel(final double value) {
        return (int) Math.max(0, Math.min(255, Math.rint(value)));
    }

    private static int alpha(final int argb) {
        return argb >>> 24;
    }

    private static int red(final int argb) {
        return argb >> 16 & 0xFF;
    }

    private static int green(final int argb) {
        return argb >> 8 & 0xFF;
    }

    private static int blue(final int argb) {
        return argb & 0xFF;
    }

    /*
     * The ramp of one colour: the colour itself at the middle light; below it, deeper and turning cooler; above it,
     * paler and turning warmer, near white at the top. A grey keeps almost no colour in its shadows.
     */
    private static final class Ramp {

        private final double hue;
        private final double saturation;
        private final double value;
        private final boolean grey;

        private Ramp(final int colour) {
            final double[] hsv = Hsv.of(red(colour) / 255.0, green(colour) / 255.0, blue(colour) / 255.0);
            this.hue = hsv[0];
            this.saturation = hsv[1];
            this.value = hsv[2];
            this.grey = hsv[1] < 0.12;
        }

        private int at(final double lum) {
            final double h;
            final double s;
            final double v;
            if (lum < MIDDLE) {
                final double t = (MIDDLE - lum) / MIDDLE;
                h = toward(this.hue, COOL_HUE, 0.07 * t);
                s = Math.min(1.0, this.saturation + (this.grey ? 0.05 : 0.22) * t);
                v = this.value * (1 - 0.86 * t);
            } else {
                final double t = (lum - MIDDLE) / (1 - MIDDLE);
                h = toward(this.hue, WARM_HUE, 0.05 * t);
                s = this.saturation * (1 - 0.6 * t);
                v = this.value + (1 - this.value) * (0.75 * t * t);
            }
            final double[] rgb = Hsv.toRgb(h, Math.max(0.0, s), Math.max(0.0, Math.min(1.0, v)));
            return rgb((int) Math.rint(rgb[0] * 255), (int) Math.rint(rgb[1] * 255), (int) Math.rint(rgb[2] * 255));
        }

        private static double toward(final double from, final double target, final double amount) {
            final double d = mod1(target - from + 0.5) - 0.5;
            return mod1(from + Math.max(-amount, Math.min(amount, d)));
        }
    }

    private static double mod1(final double x) {
        return x - Math.floor(x);
    }

    /* Hue, saturation and value, as the colour conversions every drawing program shares reckon them. */
    private static final class Hsv {

        private Hsv() {
        }

        private static double[] of(final double r, final double g, final double b) {
            final double max = Math.max(r, Math.max(g, b));
            final double min = Math.min(r, Math.min(g, b));
            if (max == min) {
                return new double[] {0.0, 0.0, max};
            }
            final double s = (max - min) / max;
            final double rc = (max - r) / (max - min);
            final double gc = (max - g) / (max - min);
            final double bc = (max - b) / (max - min);
            final double h;
            if (r == max) {
                h = bc - gc;
            } else if (g == max) {
                h = 2.0 + rc - bc;
            } else {
                h = 4.0 + gc - rc;
            }
            return new double[] {mod1(h / 6.0), s, max};
        }

        private static double[] toRgb(final double h, final double s, final double v) {
            if (s == 0.0) {
                return new double[] {v, v, v};
            }
            final int i = (int) (h * 6.0);
            final double f = h * 6.0 - i;
            final double p = v * (1.0 - s);
            final double q = v * (1.0 - s * f);
            final double t = v * (1.0 - s * (1.0 - f));
            return switch (Math.floorMod(i, 6)) {
                case 0 -> new double[] {v, t, p};
                case 1 -> new double[] {q, v, p};
                case 2 -> new double[] {p, v, t};
                case 3 -> new double[] {p, q, v};
                case 4 -> new double[] {t, p, v};
                default -> new double[] {v, p, q};
            };
        }
    }
}
