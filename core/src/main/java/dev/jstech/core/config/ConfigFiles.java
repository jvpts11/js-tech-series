/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.config;

import dev.jstech.core.config.format.ConfigFormats;
import dev.jstech.core.log.CoreLogs;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Where settings files live and when they are read: the game's side of a {@link ConfigFile}, registered from the
 * constructor of the mod that owns it.
 *
 * <p>A TOML file is handed to NeoForge as one of its own settings files, so NeoForge's settings screen shows it,
 * NeoForge keeps a world's copy in step with the players and reads it again when somebody edits it. A file in any
 * other format the Core keeps itself: a player's and a common one in the game's config folder, read as the game
 * starts; a world's beside the world, read as it starts and reset as it stops. Either way, a file that does not exist
 * is written with the defaults and their comments, so a person always has one to edit; a world's that does not exist
 * yet starts as the modpack's copy in the game's {@code defaultconfigs} folder, when it has one.
 *
 * <p>A world's settings reach every player as they join and again whenever one changes ({@link ConfigSync}).
 */
public final class ConfigFiles {

    static final Logger LOGGER = CoreLogs.CONFIG.logger();

    /** Each mod's files, by its id, for the words its language file writes for them. */
    private static final Map<String, List<ConfigFile>> BY_MOD = new ConcurrentHashMap<>();
    /** Every file, by its name. */
    private static final Map<String, ConfigFile> BY_NAME = new ConcurrentHashMap<>();

    private ConfigFiles() {
    }

    /** Puts a mod's settings file in its place, to be read when its side reads it. */
    public static void register(final ConfigFile file, final IEventBus modEventBus, final ModContainer modContainer) {
        BY_MOD.computeIfAbsent(modContainer.getModId(), mod -> new CopyOnWriteArrayList<>()).add(file);
        BY_NAME.put(file.name(), file);
        file.reset();
        if (file.side() == ConfigSide.SERVER) {
            file.onAnyChange(key -> ConfigSync.changed(file));
        }
        if (file.format() == ConfigFormats.TOML) {
            SpecConfigFiles.register(file, modEventBus, modContainer);
        } else {
            KeptConfigFiles.register(file);
        }
    }

    /** The files a mod registered, in the order it registered them. */
    public static List<ConfigFile> of(final String modId) {
        return List.copyOf(BY_MOD.getOrDefault(modId, List.of()));
    }

    /** The file registered under {@code name} (without its extension), by whichever mod, or none. */
    public static Optional<ConfigFile> byName(final String name) {
        return Optional.ofNullable(BY_NAME.get(name));
    }

    /** Every file registered, by every mod. */
    public static List<ConfigFile> all() {
        final List<ConfigFile> out = new ArrayList<>();
        BY_MOD.values().forEach(out::addAll);
        return out;
    }

    /**
     * The values {@code file} has for a world's data loading now: what a condition in a recipe, a rule a loot table
     * asks or anything else read while data loads should read a setting from, rather than the file itself. A world's
     * data loads as the world opens, before the world's own settings are read, so a world's file is read for it from
     * beside the world, as it is on disk; any other file, and a world's once the world runs, is the file itself.
     */
    public static IConfigValues forData(final ConfigFile file) {
        return WorldDataSettings.of(file);
    }

    /**
     * The values a world's file holds on disk for the world in {@code world}, without reading it into the file: the
     * file beside the world, else the modpack's copy in the game's {@code defaultconfigs} folder, else the defaults;
     * with no world, the modpack's copy or the defaults, as a world being made starts with. What a world's data loads
     * with before the world runs ({@link #forData}).
     */
    public static IConfigValues onDisk(final ConfigFile file, final @Nullable Path world) {
        return WorldDataSettings.onDisk(file, world);
    }

    /**
     * The values a file of the game's config folder holds for what is decided as the game starts, before NeoForge reads
     * the file: what is registered, which is registered once. Read from the file on disk, or its defaults when there
     * is none yet; a change takes hold when the game starts again.
     */
    public static IConfigValues atStartup(final ConfigFile file) {
        return WorldDataSettings.inConfigFolder(file);
    }

    /** The game took hold of the world in {@code folder} to open it, before it loads the world's data. */
    @ApiStatus.Internal
    public static void worldOpened(final Path folder) {
        WorldDataSettings.opened(folder);
    }

    /** The game let go of the world in {@code folder}. */
    @ApiStatus.Internal
    public static void worldClosed(final Path folder) {
        WorldDataSettings.closed(folder);
    }

    /**
     * Reads a world's file, kept in a format other than TOML, from {@code folder} as a world starting does: a file the
     * world does not have yet starts as the modpack's copy in the game's {@code defaultconfigs} folder, when it has
     * one.
     */
    @ApiStatus.Internal
    public static void loadWorldFile(final ConfigFile file, final Path folder) {
        KeptConfigFiles.startFromModpack(file, folder);
        KeptConfigFiles.load(file, folder);
    }
}
