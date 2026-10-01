/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.config;

import dev.jstech.core.config.format.ConfigFormats;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;

/**
 * Where settings files live and when they are read: the game's side of a {@link ConfigFile}, registered from the
 * constructor of the mod that owns it.
 *
 * <p>A TOML file is handed to NeoForge as one of its own settings files, so NeoForge's settings screen shows it,
 * NeoForge keeps a world's copy in step with the players and reads it again when somebody edits it. A file in any
 * other format the Core keeps itself: a player's and a common one in the game's config folder, read as the game
 * starts; a world's beside the world, read as it starts and reset as it stops. Either way, a file that does not exist
 * is written with the defaults and their comments, so a person always has one to edit.
 */
public final class ConfigFiles {

    private ConfigFiles() {
    }

    /** Puts a mod's settings file in its place, to be read when its side reads it. */
    public static void register(final ConfigFile file, final IEventBus modEventBus, final ModContainer modContainer) {
        file.reset();
        if (file.format() == ConfigFormats.TOML) {
            SpecConfigFiles.register(file, modEventBus, modContainer);
        } else {
            KeptConfigFiles.register(file);
        }
    }
}
