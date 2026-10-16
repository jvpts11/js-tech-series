/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.effect;

import dev.jstech.core.config.ConfigFile;
import dev.jstech.core.config.ConfigKey;
import dev.jstech.core.config.ConfigSide;
import dev.jstech.core.config.format.ConfigFormats;

/**
 * How effects look on this player's game: how many particles the Core's effects may have at once, how hard the screen
 * shakes, how bright a flash is, and whether the air shimmers over hot things. The game's own particle setting (all,
 * decreased, minimal) thins them out as well.
 */
public final class EffectSettings {

    private static final String SECTION = "effects";
    private static final int LEAST_PARTICLES = 500;
    private static final int MOST_PARTICLES = 30_000;
    private static final int DEFAULT_PARTICLES = 6_000;

    /** How many particles at once. */
    public static final ConfigKey<Integer> PARTICLES = ConfigKey.whole(SECTION + ".particles", DEFAULT_PARTICLES)
            .range(LEAST_PARTICLES, MOST_PARTICLES)
            .comment("How many particles the effects of the series may have at once on this game. When there are "
                    + "this many, the farthest emit fewer.")
            .named("Particles at once").unit("particles");
    /** How hard the screen shakes. */
    public static final ConfigKey<Double> SHAKE = ConfigKey.number(SECTION + ".shake", 1.0).range(0.0, 1.0)
            .comment("How hard the screen shakes near an explosion or a heavy machine: 0 for not at all, 1 as made.")
            .named("Screen shake");
    /** How bright a flash of the screen is. */
    public static final ConfigKey<Double> FLASH = ConfigKey.number(SECTION + ".flash", 1.0).range(0.0, 1.0)
            .comment("How bright the screen flashes at an explosion or an arc: 0 for not at all, 1 as made.")
            .named("Screen flash");
    /** Whether the air shimmers over hot things and a shockwave bends the view. */
    public static final ConfigKey<Boolean> DISTORTION = ConfigKey.flag(SECTION + ".distortion", true)
            .comment("Whether the air shimmers over hot things and the shockwave of an explosion bends the view.")
            .named("Heat haze and shockwaves");
    /** The player's file. */
    public static final ConfigFile FILE = ConfigFile.builder("jstech-effects", ConfigSide.CLIENT, ConfigFormats.TOML)
            .comment("How effects look on this game.")
            .sectionComment(SECTION, "Particles, the screen's shake and flash, heat haze and shockwaves.")
            .sectionNamed(SECTION, "Effects")
            .key(PARTICLES, value -> particles = value)
            .key(SHAKE, value -> shake = value)
            .key(FLASH, value -> flash = value)
            .key(DISTORTION, value -> distortion = value)
            .build();

    private static volatile int particles = DEFAULT_PARTICLES;
    private static volatile double shake = 1.0;
    private static volatile double flash = 1.0;
    private static volatile boolean distortion = true;

    private EffectSettings() {
    }

    /** How many particles at once. */
    public static int particles() {
        return particles;
    }

    /** How hard the screen shakes, 0 to 1. */
    public static double shake() {
        return shake;
    }

    /** How bright a flash is, 0 to 1. */
    public static double flash() {
        return flash;
    }

    /** Whether heat haze and shockwaves bend the view. */
    public static boolean distortion() {
        return distortion;
    }
}
