/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.config;

import dev.jstech.core.client.motion.MotionClock;
import dev.jstech.core.config.ConfigFile;
import dev.jstech.core.config.ConfigFiles;
import dev.jstech.core.config.ConfigKey;
import dev.jstech.core.config.ConfigSide;
import dev.jstech.core.config.format.ConfigFormats;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;

/**
 * A player's own settings for the computers, {@code jscomputers-client.toml} beside the game's options: how the
 * desktops move and whether they draw their own pointer. Read on the player's game only.
 */
public final class ComputersClientConfig {

    public static final ConfigKey<Boolean> REDUCE_MOTION = ConfigKey.flag("client.reduce_motion", false)
            .comment("Whether every desktop acts at once: no window, menu or boot moves, everything is drawn where it "
                            + "ends.",
                    "Each system's own settings page turns its motions off one by one; this turns them all off for "
                            + "this player.")
            .named("Reduce motion");

    public static final ConfigKey<Boolean> DESKTOP_CURSORS = ConfigKey.flag("client.desktop_cursors", true)
            .comment("Whether the desktops draw their own pointer over the monitor's glass: each system's arrow, and "
                    + "its hourglass, ring or watch while it is busy. Off, the game's own pointer stays.")
            .named("Desktop cursors");

    public static final ConfigKey<Boolean> OUTSIDE_COMPONENTS = ConfigKey.flag("client.outside_components", false)
            .comment("Whether this game draws the components of programs' windows that reach outside the game, such "
                            + "as one showing a web address or a file on this computer.",
                    "Off, each shows a placeholder naming it, whatever the server allows.")
            .named("Components reaching outside the game");

    public static final ConfigFile FILE = ConfigFile.builder("jscomputers-client", ConfigSide.CLIENT,
                    ConfigFormats.TOML)
            .comment("How the computers of J's Computers look on this player's game.")
            .sectionNamed("client", "Client")
            .key(REDUCE_MOTION, MotionClock::setReduced)
            .key(DESKTOP_CURSORS)
            .key(OUTSIDE_COMPONENTS)
            .build();

    private ComputersClientConfig() {
    }

    /** Puts the file beside the game's options, where the player's game reads it. */
    public static void register(final IEventBus modEventBus, final ModContainer modContainer) {
        ConfigFiles.register(FILE, modEventBus, modContainer);
    }

    /** Whether this game draws components that reach outside the game, rather than a placeholder. */
    public static boolean outsideComponents() {
        return FILE.get(OUTSIDE_COMPONENTS);
    }

    /** Whether the desktops draw their own pointer over the glass. */
    public static boolean desktopCursors() {
        return FILE.get(DESKTOP_CURSORS);
    }
}
