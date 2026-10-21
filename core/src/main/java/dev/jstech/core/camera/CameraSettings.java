/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.camera;

import dev.jstech.core.config.ConfigFile;
import dev.jstech.core.config.ConfigKey;
import dev.jstech.core.config.ConfigSide;
import dev.jstech.core.config.format.ConfigFormats;

/**
 * How the camera behaves on this player's game: whether it follows the turn of a vehicle or a moving structure the
 * player rides, and what views of cameras in the world drawn into screens cost: how many at once, how often each is
 * drawn again, and how many pixels wide.
 */
public final class CameraSettings {

    private static final String SECTION = "camera";
    private static final int MOST_FEEDS = 8;
    private static final int DEFAULT_FEEDS = 2;
    private static final int MOST_RATE = 60;
    private static final int DEFAULT_RATE = 10;
    private static final int LEAST_RESOLUTION = 64;
    private static final int MOST_RESOLUTION = 1024;
    private static final int DEFAULT_RESOLUTION = 256;

    /** Whether the camera follows a ridden vehicle's turn. */
    public static final ConfigKey<Boolean> CHASE = ConfigKey.flag(SECTION + ".chase", true)
            .comment("Whether, riding a vehicle or a moving structure, the camera behind follows its turn and rolls "
                    + "with it, and the view in the first person rolls with it too.")
            .named("Follow what is ridden");
    /** How many views of cameras in the world are drawn at once. */
    public static final ConfigKey<Integer> FEEDS = ConfigKey.whole(SECTION + ".feeds", DEFAULT_FEEDS)
            .range(0, MOST_FEEDS)
            .comment("How many cameras in the world are drawn into the screens that show them at once; the nearest "
                    + "screens are drawn first, and 0 draws none.")
            .named("Camera views at once").unit("views");
    /** How many times a second each view is drawn again. */
    public static final ConfigKey<Integer> FEED_RATE = ConfigKey.whole(SECTION + ".feed_rate", DEFAULT_RATE)
            .range(1, MOST_RATE)
            .comment("How many times a second each camera's view is drawn again.")
            .named("Camera view rate").unit("/s");
    /** How many pixels wide each view is drawn. */
    public static final ConfigKey<Integer> FEED_RESOLUTION = ConfigKey.whole(SECTION + ".feed_resolution",
                    DEFAULT_RESOLUTION).range(LEAST_RESOLUTION, MOST_RESOLUTION)
            .comment("How many pixels wide each camera's view is drawn; its height follows the screen showing it.")
            .named("Camera view width").unit("pixels");
    /** The player's file. */
    public static final ConfigFile FILE = ConfigFile.builder("jstech-camera", ConfigSide.CLIENT, ConfigFormats.TOML)
            .comment("How the camera behaves on this game.")
            .sectionComment(SECTION, "Following what is ridden, and the views of cameras shown on screens.")
            .sectionNamed(SECTION, "Camera")
            .key(CHASE, value -> chase = value)
            .key(FEEDS, value -> feeds = value)
            .key(FEED_RATE, value -> feedRate = value)
            .key(FEED_RESOLUTION, value -> feedResolution = value)
            .build();

    private static volatile boolean chase = true;
    private static volatile int feeds = DEFAULT_FEEDS;
    private static volatile int feedRate = DEFAULT_RATE;
    private static volatile int feedResolution = DEFAULT_RESOLUTION;

    private CameraSettings() {
    }

    /** Whether the camera follows a ridden vehicle's turn. */
    public static boolean chase() {
        return chase;
    }

    /** How many camera views are drawn at once. */
    public static int feeds() {
        return feeds;
    }

    /** How many times a second each view is drawn again. */
    public static int feedRate() {
        return feedRate;
    }

    /** How many pixels wide each view is drawn. */
    public static int feedResolution() {
        return feedResolution;
    }
}
