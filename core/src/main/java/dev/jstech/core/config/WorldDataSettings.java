/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.config;

import dev.jstech.core.config.format.ConfigFormatException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.neoforged.fml.loading.FMLConfig;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;

/**
 * The settings a world's data is loaded with. A world's data, its recipes, loot tables and the rest of its data packs,
 * is loaded as the world opens and before the world's own settings are read, so anything that reads a world's setting
 * while the data loads would see it at its default. Until the world's settings are read, a world's file is read for
 * the data straight from beside the world being opened, as it is on disk; a world that has none yet, such as one being
 * made, has the modpack's copy in the game's {@code defaultconfigs} folder, as it is about to start from; and with
 * neither, the defaults. Once the world runs, its data is loaded again only by {@code /reload}, which sees the
 * settings as they are.
 */
final class WorldDataSettings {

    /** Each file's values as read from disk for the world being opened, by the file's name. */
    private static final Map<String, Map<String, Object>> READ = new ConcurrentHashMap<>();
    /** The folder of the world being opened, from when the game takes hold of it until it lets go. */
    private static volatile @Nullable Path opening;

    private WorldDataSettings() {
    }

    /** The values {@code file} has for a world's data loading now. */
    static IConfigValues of(final ConfigFile file) {
        if (file.side() != ConfigSide.SERVER || ServerLifecycleHooks.getCurrentServer() != null) {
            return file;
        }
        return valuesOf(file, READ.computeIfAbsent(file.name(), name -> fromDisk(file, opening)));
    }

    /**
     * The values a world's file holds on disk for the world in {@code world}: the file beside it, else the modpack's
     * copy, else the defaults; with no world, the modpack's copy or the defaults, as a world being made starts with.
     */
    static IConfigValues onDisk(final ConfigFile file, final @Nullable Path world) {
        return valuesOf(file, fromDisk(file, world));
    }

    /** The game took hold of the world in {@code folder}, to open it. */
    static void opened(final Path folder) {
        opening = folder;
        READ.clear();
    }

    /** The game let go of the world in {@code folder}. */
    static void closed(final Path folder) {
        if (folder.equals(opening)) {
            opening = null;
        }
        READ.clear();
    }

    private static IConfigValues valuesOf(final ConfigFile file, final Map<String, Object> plain) {
        return new IConfigValues() {
            @Override
            public <T> T get(final ConfigKey<T> key) {
                return file.peek(key, plain);
            }
        };
    }

    private static Map<String, Object> fromDisk(final ConfigFile file, final @Nullable Path world) {
        Path path = world == null ? null : world.resolve(KeptConfigFiles.WORLD_FOLDER).resolve(file.fileName());
        if (path == null || !Files.isRegularFile(path)) {
            path = FMLPaths.GAMEDIR.get().resolve(FMLConfig.defaultConfigPath()).resolve(file.fileName());
        }
        if (!Files.isRegularFile(path)) {
            return Map.of();
        }
        try {
            return file.format().read(Files.readAllBytes(path));
        } catch (final IOException | ConfigFormatException e) {
            ConfigFiles.LOGGER.warn("{} could not be read for the world's data, which loads with its defaults", path,
                    e);
            return Map.of();
        }
    }
}
