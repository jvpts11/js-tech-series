/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.material;

import java.util.Objects;
import java.util.OptionalInt;

/**
 * How a material's ores look: the specks of one of the game's ores, taken off the game's rock and laid on any rock in
 * the material's colour, or in a colour of the ore's own (iron's ore is rusty, not the grey of its metal), and, for a
 * mineral of two colours, the hues of those specks that take the second one (the blue-green of chalcopyrite's
 * tarnish).
 *
 * @param pattern the game's ore whose specks it is drawn in
 * @param colour  the specks' colour, as {@code 0xRRGGBB}, when not the material's
 * @param second  the second colour, as {@code 0xRRGGBB}, or none
 * @param lowHue  the lowest hue (0 to 1) of the specks that take the second colour
 * @param highHue the highest
 */
public record OreLook(Pattern pattern, OptionalInt colour, OptionalInt second, double lowHue, double highHue) {

    /** The look of an ore unless its material says: the specks of the game's iron ore, in the material's colour. */
    public static final OreLook PLAIN = of(Pattern.IRON);

    public OreLook {
        Objects.requireNonNull(pattern, "pattern");
        Objects.requireNonNull(colour, "colour");
        Objects.requireNonNull(second, "second");
    }

    /** Specks of {@code pattern}, in the material's colour alone. */
    public static OreLook of(final Pattern pattern) {
        return new OreLook(pattern, OptionalInt.empty(), OptionalInt.empty(), 0.0, 0.0);
    }

    /** Specks of {@code pattern}, those of hue {@code lowHue} to {@code highHue} in {@code second}. */
    public static OreLook of(final Pattern pattern, final int second, final double lowHue, final double highHue) {
        return new OreLook(pattern, OptionalInt.empty(), OptionalInt.of(second & 0xFFFFFF), lowHue, highHue);
    }

    /** The same look, its specks in {@code rgb} rather than the material's colour. */
    public OreLook inColour(final int rgb) {
        return new OreLook(this.pattern, OptionalInt.of(rgb & 0xFFFFFF), this.second, this.lowHue, this.highHue);
    }

    /** The colour its specks are drawn in, for a material of colour {@code material}. */
    public int speckColour(final int material) {
        return this.colour.orElse(material);
    }

    /** The game's ores whose specks an ore can be drawn in. */
    public enum Pattern {
        COAL("coal_ore"),
        IRON("iron_ore"),
        COPPER("copper_ore"),
        GOLD("gold_ore"),
        REDSTONE("redstone_ore"),
        EMERALD("emerald_ore"),
        LAPIS("lapis_ore"),
        DIAMOND("diamond_ore");

        private final String texture;

        Pattern(final String texture) {
            this.texture = texture;
        }

        /** The game's rock that ore is drawn on, whose texels are not specks, in the game's namespace. */
        public static String rock(final boolean deepslate) {
            return deepslate ? "block/deepslate" : "block/stone";
        }

        /**
         * The game's texture of the ore, in its own namespace: the deepslate one for an ore drawn on deepslate, the
         * stone one on every other rock.
         */
        public String texture(final boolean deepslate) {
            return "block/" + (deepslate ? "deepslate_" : "") + this.texture;
        }
    }
}
