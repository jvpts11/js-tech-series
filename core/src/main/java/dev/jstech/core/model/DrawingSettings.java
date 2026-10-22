/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

import dev.jstech.core.config.ConfigFile;
import dev.jstech.core.config.ConfigKey;
import dev.jstech.core.config.ConfigSide;
import dev.jstech.core.config.format.ConfigFormats;

/**
 * How the Core draws on this player's game: whether things that look alike (models of one shape, the turning parts of
 * machines, the particles of effects) are drawn many at once by the graphics card, which costs the game far less than
 * drawing them one by one. Under a shader pack they are drawn one by one whatever this says, as the pack draws them.
 */
public final class DrawingSettings {

    private static final String SECTION = "drawing";

    /** Whether alike things are drawn many at once. */
    public static final ConfigKey<Boolean> INSTANCING = ConfigKey.flag(SECTION + ".instancing", true)
            .comment("Whether models of one shape, the turning parts of machines and the particles of effects are "
                    + "drawn many at once by the graphics card. Turn it off if they look wrong on this graphics card.")
            .named("Draw alike things together");
    /** The player's file. */
    public static final ConfigFile FILE = ConfigFile.builder("jstech-drawing", ConfigSide.CLIENT, ConfigFormats.TOML)
            .comment("How the series draws on this game.")
            .sectionComment(SECTION, "Drawing alike things many at once.")
            .sectionNamed(SECTION, "Drawing")
            .key(INSTANCING, value -> instancing = value)
            .build();

    private static volatile boolean instancing = true;

    private DrawingSettings() {
    }

    /** Whether alike things are drawn many at once, as the player set. */
    public static boolean instancing() {
        return instancing;
    }
}
