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
import org.jetbrains.annotations.Nullable;

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

    /**
     * The game's ores whose specks an ore can be drawn in: the specks of the ores of the stone, or the veins of the
     * Nether's quartz, for a mineral that fills cracks in its rock, as gypsum does.
     */
    public enum Pattern {
        COAL("coal_ore", null),
        IRON("iron_ore", null),
        COPPER("copper_ore", null),
        GOLD("gold_ore", null),
        REDSTONE("redstone_ore", null),
        EMERALD("emerald_ore", null),
        LAPIS("lapis_ore", null),
        DIAMOND("diamond_ore", null),
        /** Veins, not specks: the Nether quartz ore's, taken off the netherrack it alone is drawn on. */
        QUARTZ("nether_quartz_ore", "netherrack");

        private final String texture;
        /* The one rock the game draws the ore on, for an ore that has no deepslate kind; none for the stone's ores. */
        private final @Nullable String ownRock;

        Pattern(final String texture, @Nullable final String ownRock) {
            this.texture = texture;
            this.ownRock = ownRock;
        }

        /** The game's rock that ore is drawn on, whose texels are not specks, in the game's namespace. */
        public String rock(final boolean deepslate) {
            if (this.ownRock != null) {
                return "block/" + this.ownRock;
            }
            return deepslate ? "block/deepslate" : "block/stone";
        }

        /**
         * The game's texture of the ore, in its own namespace: the deepslate one for an ore drawn on deepslate, the
         * stone one on every other rock; an ore of one rock of its own, its one texture.
         */
        public String texture(final boolean deepslate) {
            return "block/" + (deepslate && this.ownRock == null ? "deepslate_" : "") + this.texture;
        }
    }
}
