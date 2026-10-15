/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.notification;

import com.mojang.serialization.Codec;
import dev.jstech.core.config.ConfigFile;
import dev.jstech.core.config.ConfigKey;
import dev.jstech.core.config.ConfigSide;
import dev.jstech.core.config.format.ConfigFormats;
import java.util.List;

/**
 * What a player chose of their notifications, kept in their own settings, {@code jstech-notifications.toml}: where they
 * appear, whether they are not to be disturbed, the kinds and mods they silenced, and how many the history keeps. The
 * history's screen changes them as the player chooses; the settings screen shows them too.
 */
public final class NotificationSettings {

    /** The corners the notifications may appear in, as the file writes them. */
    public static final String TOP_RIGHT = "top_right";
    public static final String TOP_LEFT = "top_left";
    public static final String BOTTOM_RIGHT = "bottom_right";
    public static final String BOTTOM_LEFT = "bottom_left";

    private static final String SECTION = "notifications";
    private static final int FEWEST_KEPT = 10;
    private static final int MOST_KEPT = 1000;

    /** The corner of the screen they appear in. */
    public static final ConfigKey<String> PLACE = ConfigKey.of(SECTION + ".place", Codec.STRING, TOP_RIGHT)
            .allowing(TOP_RIGHT, TOP_LEFT, BOTTOM_RIGHT, BOTTOM_LEFT)
            .comment("The corner of the screen notifications appear in: top_right, top_left, bottom_right or "
                    + "bottom_left.")
            .named("Where they appear");

    /** Whether none comes on the screen or sounds; they are kept in the history all the same. */
    public static final ConfigKey<Boolean> DO_NOT_DISTURB = ConfigKey.flag(SECTION + ".do_not_disturb", false)
            .comment("Whether no notification comes on the screen or sounds; they are kept in the history all the "
                    + "same.")
            .named("Do not disturb");

    /** Whether not to be disturbed holds the game's own toasts too. */
    public static final ConfigKey<Boolean> HOLD_GAME_TOASTS = ConfigKey.flag(SECTION + ".hold_game_toasts", true)
            .comment("Whether, while not to be disturbed, the game's own toasts (advancements, recipes) are held too.")
            .named("Hold the game's toasts too").activeWhen(DO_NOT_DISTURB);

    /** The kinds the player silenced, by id. */
    public static final ConfigKey<List<String>> SILENCED_KINDS = ConfigKey.of(SECTION + ".silenced_kinds",
                    Codec.STRING.listOf(), List.<String>of())
            .comment("The kinds of notification that never come on the screen, by id.")
            .named("Silenced kinds");

    /** The mods the player silenced, by id. */
    public static final ConfigKey<List<String>> SILENCED_MODS = ConfigKey.of(SECTION + ".silenced_mods",
                    Codec.STRING.listOf(), List.<String>of())
            .comment("The mods whose notifications never come on the screen, by id.")
            .named("Silenced mods");

    /** How many notifications the history keeps. */
    public static final ConfigKey<Integer> HISTORY = ConfigKey.whole(SECTION + ".history",
                    NotificationBoard.DEFAULT_KEPT).range(FEWEST_KEPT, MOST_KEPT)
            .comment("How many notifications the history keeps; the oldest make way for new ones.")
            .named("History kept").unit("notifications");

    /** The player's file. */
    public static final ConfigFile FILE = ConfigFile.builder("jstech-notifications", ConfigSide.CLIENT,
                    ConfigFormats.TOML)
            .comment("How the notifications of the mods built on J's Core come on this game.")
            .sectionComment(SECTION, "Where notifications appear, what is silenced, and the history.")
            .sectionNamed(SECTION, "Notifications")
            .key(PLACE, value -> place = value)
            .key(DO_NOT_DISTURB, value -> doNotDisturb = value)
            .key(HOLD_GAME_TOASTS, value -> holdGameToasts = value)
            .key(SILENCED_KINDS, value -> silencedKinds = List.copyOf(value))
            .key(SILENCED_MODS, value -> silencedMods = List.copyOf(value))
            .key(HISTORY, value -> history = value)
            .build();

    private static volatile String place = TOP_RIGHT;
    private static volatile boolean doNotDisturb;
    private static volatile boolean holdGameToasts = true;
    private static volatile List<String> silencedKinds = List.of();
    private static volatile List<String> silencedMods = List.of();
    private static volatile int history = NotificationBoard.DEFAULT_KEPT;

    private NotificationSettings() {
    }

    /** The corner they appear in, as the file writes it. */
    public static String place() {
        return place;
    }

    public static boolean doNotDisturb() {
        return doNotDisturb;
    }

    public static boolean holdGameToasts() {
        return holdGameToasts;
    }

    public static List<String> silencedKinds() {
        return silencedKinds;
    }

    public static List<String> silencedMods() {
        return silencedMods;
    }

    public static int history() {
        return history;
    }
}
