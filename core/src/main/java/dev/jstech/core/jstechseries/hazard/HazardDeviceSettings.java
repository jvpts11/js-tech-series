/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.hazard;

import com.mojang.serialization.Codec;
import dev.jstech.core.config.ConfigFile;
import dev.jstech.core.config.ConfigKey;
import dev.jstech.core.config.ConfigSide;
import dev.jstech.core.config.format.ConfigFormats;

/**
 * Where a player wants the hazard devices they carry on their HUD and how big, kept in their own settings,
 * {@code jstech-hazard-devices.toml}: a corner of the screen for each device, where the devices of a corner stack from
 * it inwards, and its size as a share of the size it was drawn at.
 */
public final class HazardDeviceSettings {

    /** The corners a device may be in, as the file writes them. */
    public static final String TOP_RIGHT = "top_right";
    public static final String TOP_LEFT = "top_left";
    public static final String BOTTOM_RIGHT = "bottom_right";
    public static final String BOTTOM_LEFT = "bottom_left";

    private static final String SECTION = "devices";
    private static final int SMALLEST = 50;
    private static final int LARGEST = 300;
    private static final int DRAWN = 100;

    public static final ConfigKey<String> GEIGER_PLACE = place("geiger_counter", BOTTOM_LEFT)
            .comment("The Geiger counter's corner of the screen: top_right, top_left, bottom_right or bottom_left.")
            .named("The Geiger counter: where");
    public static final ConfigKey<Integer> GEIGER_SIZE = size("geiger_counter")
            .comment("The Geiger counter's size, in percent of the size it was drawn at.")
            .named("The Geiger counter: size");
    public static final ConfigKey<String> DOSIMETER_PLACE = place("pocket_dosimeter", BOTTOM_LEFT)
            .comment("The dosimeter's corner of the screen: top_right, top_left, bottom_right or bottom_left.")
            .named("The dosimeter: where");
    public static final ConfigKey<Integer> DOSIMETER_SIZE = size("pocket_dosimeter")
            .comment("The dosimeter's size, in percent of the size it was drawn at.")
            .named("The dosimeter: size");
    public static final ConfigKey<String> MAP_PLACE = place("hazard_map", BOTTOM_RIGHT)
            .comment("The hazard map's corner of the screen: top_right, top_left, bottom_right or bottom_left.")
            .named("The hazard map: where");
    public static final ConfigKey<Integer> MAP_SIZE = size("hazard_map")
            .comment("The hazard map's size, in percent of the size it was drawn at.")
            .named("The hazard map: size");

    /** The player's file. */
    public static final ConfigFile FILE = ConfigFile.builder("jstech-hazard-devices", ConfigSide.CLIENT,
                    ConfigFormats.TOML)
            .comment("Where the hazard devices of the mods built on J's Core show on this game's HUD, and how big.")
            .sectionComment(SECTION, "A corner and a size for each device carried.")
            .sectionNamed(SECTION, "Hazard devices")
            .key(GEIGER_PLACE, value -> geigerPlace = value)
            .key(GEIGER_SIZE, value -> geigerSize = value)
            .key(DOSIMETER_PLACE, value -> dosimeterPlace = value)
            .key(DOSIMETER_SIZE, value -> dosimeterSize = value)
            .key(MAP_PLACE, value -> mapPlace = value)
            .key(MAP_SIZE, value -> mapSize = value)
            .build();

    private static volatile String geigerPlace = BOTTOM_LEFT;
    private static volatile int geigerSize = DRAWN;
    private static volatile String dosimeterPlace = BOTTOM_LEFT;
    private static volatile int dosimeterSize = DRAWN;
    private static volatile String mapPlace = BOTTOM_RIGHT;
    private static volatile int mapSize = DRAWN;

    private HazardDeviceSettings() {
    }

    /** The corner {@code device} shows in, as the file writes it. */
    public static String place(final HazardDevice device) {
        return switch (device) {
            case GEIGER_COUNTER -> geigerPlace;
            case POCKET_DOSIMETER -> dosimeterPlace;
            case HAZARD_MAP -> mapPlace;
        };
    }

    /** How big {@code device} shows, as a share of the size it was drawn at: 1 as drawn. */
    public static float size(final HazardDevice device) {
        final int percent = switch (device) {
            case GEIGER_COUNTER -> geigerSize;
            case POCKET_DOSIMETER -> dosimeterSize;
            case HAZARD_MAP -> mapSize;
        };
        return percent / (float) DRAWN;
    }

    private static ConfigKey<String> place(final String device, final String corner) {
        return ConfigKey.of(SECTION + "." + device + "_place", Codec.STRING, corner)
                .allowing(TOP_RIGHT, TOP_LEFT, BOTTOM_RIGHT, BOTTOM_LEFT);
    }

    private static ConfigKey<Integer> size(final String device) {
        return ConfigKey.whole(SECTION + "." + device + "_size", DRAWN).range(SMALLEST, LARGEST).unit("%");
    }
}
