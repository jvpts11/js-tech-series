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
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * What makes the screens of the mods built on the Core easier on this player, kept in their own settings,
 * {@code jstech-accessibility.toml}: the size of the text apart from the GUI scale, in whole steps ({@link TextSizes}),
 * and less movement, so that whatever slides, turns, blinks or turns round is drawn still, where it ends. The styles
 * of high contrast and for each kind of colour blindness are chosen with the other styles
 * ({@code jstech-interface.toml}).
 */
@TextHolder
public final class AccessibilitySettings {

    private static final String TEXT = "text";
    private static final String SIZE = TEXT + ".size";
    private static final String MOTION = "motion";

    /** How large the text of the series' screens is, apart from the GUI scale. */
    public static final ConfigKey<String> TEXT_SIZE = ConfigKey.text(SIZE, TextSizes.NORMAL)
            .allowing(TextSizes.NORMAL, TextSizes.LARGER, TextSizes.LARGEST)
            .comment("How large the text of the series' screens, its settings and its notifications is, apart from "
                    + "the game's GUI scale: each step draws a letter's pixels a screen pixel larger, so it stays "
                    + "sharp, and what holds the text grows with it; the icons, the frames and the game's own buttons "
                    + "keep the GUI scale.")
            .named("Text size");
    /** The words of the sizes, as the settings screen shows them. */
    public static final TextKey NORMAL = TextKey.of("jscore.configuration.text.size.normal", "Normal");
    public static final TextKey LARGER = TextKey.of("jscore.configuration.text.size.larger", "Larger");
    public static final TextKey LARGEST = TextKey.of("jscore.configuration.text.size.largest", "Largest");
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
            .sectionComment(TEXT, "The text on the screens.")
            .sectionNamed(TEXT, "Text")
            .key(TEXT_SIZE, value -> textSteps = TextSizes.stepsOf(value))
            .sectionComment(MOTION, "Movement on the screens.")
            .sectionNamed(MOTION, "Movement")
            .key(REDUCE_MOTION, value -> reduceMotion = value)
            .build();

    private static volatile int textSteps;
    private static volatile boolean reduceMotion;

    private AccessibilitySettings() {
    }

    /** How many steps larger than the normal the player wants the text. */
    public static int textSteps() {
        return textSteps;
    }

    /** Whether the player asked for less movement. */
    public static boolean reduceMotion() {
        return reduceMotion;
    }
}
