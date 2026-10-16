/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.light;

import dev.jstech.core.config.ConfigFile;
import dev.jstech.core.config.ConfigKey;
import dev.jstech.core.config.ConfigSide;
import dev.jstech.core.config.format.ConfigFormats;

/**
 * How light looks on this player's game: whether coloured light tints the world, whether what is held and what
 * burns light the way, how strong the glow of bright things is, and how far all of it reaches. Read on the player's
 * own game only; nothing of it changes the world.
 */
public final class LightingSettings {

    private static final String SECTION = "lighting";
    private static final double MOST_BLOOM = 2.0;
    private static final int LEAST_REACH = 16;
    private static final int MOST_REACH = 128;
    private static final int DEFAULT_REACH = 48;

    /** Whether coloured light tints the world. */
    public static final ConfigKey<Boolean> COLOURED = ConfigKey.flag(SECTION + ".coloured", true)
            .comment("Whether light of a colour tints the world it lights: a red alarm lamp turns its room red.")
            .named("Coloured light");
    /** Whether what is held, what burns and what glows lights the way. */
    public static final ConfigKey<Boolean> DYNAMIC = ConfigKey.flag(SECTION + ".dynamic", true)
            .comment("Whether a torch held, a mob on fire or a glowing creature lights the way as it moves. It is "
                    + "drawn on this game only, and changes nothing of the world.")
            .named("Dynamic light");
    /** How strong the glow of bright things is. */
    public static final ConfigKey<Double> BLOOM = ConfigKey.number(SECTION + ".bloom", 1.0).range(0.0, MOST_BLOOM)
            .comment("How strong the halo of bright lamps and glowing parts and the shaft of a spotlight are: 0 for "
                    + "none, 1 as made, 2 twice.")
            .named("Glow");
    /** How far coloured light, dynamic light and glow are drawn. */
    public static final ConfigKey<Integer> REACH = ConfigKey.whole(SECTION + ".reach", DEFAULT_REACH)
            .range(LEAST_REACH, MOST_REACH)
            .comment("How far from the player coloured light, dynamic light and glow are drawn, in blocks.")
            .named("Reach of light").unit("blocks");
    /** The player's file. */
    public static final ConfigFile FILE = ConfigFile.builder("jstech-lighting", ConfigSide.CLIENT, ConfigFormats.TOML)
            .comment("How light looks on this game.")
            .sectionComment(SECTION, "Coloured light, dynamic light, glow and how far they reach.")
            .sectionNamed(SECTION, "Lighting")
            .key(COLOURED, value -> coloured = value)
            .key(DYNAMIC, value -> dynamic = value)
            .key(BLOOM, value -> bloom = value)
            .key(REACH, value -> reach = value)
            .build();

    private static volatile boolean coloured = true;
    private static volatile boolean dynamic = true;
    private static volatile double bloom = 1.0;
    private static volatile int reach = DEFAULT_REACH;

    private LightingSettings() {
    }

    /** Whether coloured light tints the world. */
    public static boolean coloured() {
        return coloured;
    }

    /** Whether dynamic light is on. */
    public static boolean dynamic() {
        return dynamic;
    }

    /** How strong the glow is, 0 for none. */
    public static double bloom() {
        return bloom;
    }

    /** How far light is drawn, in blocks. */
    public static int reach() {
        return reach;
    }
}
