/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui;

import dev.jstech.core.config.ConfigFile;
import dev.jstech.core.config.ConfigKey;
import dev.jstech.core.config.ConfigSide;
import dev.jstech.core.config.format.ConfigFormats;

/**
 * What makes the screens of the mods built on the Core easier on this player, kept in their own settings,
 * {@code jstech-accessibility.toml}: less movement, so that whatever slides, turns, blinks or turns round is drawn
 * still, where it ends. The styles of high contrast and for each kind of colour blindness are chosen with the other
 * styles ({@code jstech-interface.toml}).
 */
public final class AccessibilitySettings {

    private static final String MOTION = "motion";

    /** Whether everything that moves on the screens is drawn still. */
    public static final ConfigKey<Boolean> REDUCE_MOTION = ConfigKey.flag(MOTION + ".reduce", false)
            .comment("Whether what moves on the screens of the series (what slides open, the rings and bars that "
                    + "turn round while something waits, what blinks, the turning 3D pictures) is drawn still, "
                    + "where it ends.")
            .named("Less movement");
    /** The player's file. */
    public static final ConfigFile FILE = ConfigFile.builder("jstech-accessibility", ConfigSide.CLIENT,
                    ConfigFormats.TOML)
            .comment("What makes the screens of the mods built on J's Core easier on this game.")
            .sectionComment(MOTION, "Movement on the screens.")
            .sectionNamed(MOTION, "Movement")
            .key(REDUCE_MOTION, value -> reduceMotion = value)
            .build();

    private static volatile boolean reduceMotion;

    private AccessibilitySettings() {
    }

    /** Whether the player asked for less movement. */
    public static boolean reduceMotion() {
        return reduceMotion;
    }
}
