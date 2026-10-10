/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.config;

import dev.jstech.core.JsCore;
import dev.jstech.core.config.format.ConfigFormatException;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * The settings files the Core keeps itself, in every format but TOML: where each lives, when it is read, and how it is
 * written, whole and at once, so a game stopped halfway through never leaves half a file.
 */
final class KeptConfigFiles {

    /** The folder beside a world its settings live in, the one NeoForge keeps a world's own settings in. */
    private static final String WORLD_FOLDER = "serverconfig";

    private KeptConfigFiles() {
    }

    static void register(final ConfigFile file) {
        switch (file.side()) {
            case CLIENT -> {
                // A player's settings exist only where there is a player: never on a dedicated server.
                if (FMLEnvironment.dist.isClient()) {
                    load(file, FMLPaths.CONFIGDIR.get());
                }
            }
            case COMMON -> load(file, FMLPaths.CONFIGDIR.get());
            case SERVER -> {
                NeoForge.EVENT_BUS.addListener(ServerAboutToStartEvent.class, event -> load(file,
                        event.getServer().getWorldPath(LevelResource.ROOT).resolve(WORLD_FOLDER)));
                NeoForge.EVENT_BUS.addListener(ServerStoppedEvent.class, event -> {
                    file.onSave(() -> { });
                    file.reset();
                });
            }
        }
    }

    /** Reads the file in {@code folder}, writing it when it is missing or was read with something to put right. */
    static void load(final ConfigFile file, final Path folder) {
        final Path path = folder.resolve(file.fileName());
        file.reset();
        file.onSave(() -> write(file, path));
        if (!Files.isRegularFile(path)) {
            write(file, path);
            return;
        }
        try {
            if (file.read(Files.readAllBytes(path)).rewrite()) {
                write(file, path);
            }
        } catch (final ConfigFormatException unreadable) {
            /*
             * A file nobody can read is not thrown away: it is kept beside the new one, so a person who broke a
             * long file by hand can mend it, and the game goes on with the defaults meanwhile.
             */
            final Path kept = freeKeptName(path, file.fileName());
            JsCore.LOGGER.warn("{} could not be read ({}); it is kept as {} and written again with the defaults",
                    path, unreadable.getMessage(), kept.getFileName());
            try {
                Files.move(path, kept);
            } catch (final IOException e) {
                JsCore.LOGGER.warn("{} could not be kept aside", path, e);
                return;
            }
            file.reset();
            write(file, path);
        } catch (final IOException e) {
            JsCore.LOGGER.warn("{} could not be read; the defaults are used", path, e);
        }
    }

    /** Writes the file whole: into a file beside it first, then over it in one move. */
    private static void write(final ConfigFile file, final Path path) {
        if (file.newer()) {
            return;
        }
        try {
            writeWhole(path, file.write());
        } catch (final IOException e) {
            JsCore.LOGGER.warn("{} could not be written", path, e);
        }
    }

    /** {@code bytes} into a file beside {@code path} first, then over it in one move, so no half file is ever seen. */
    static void writeWhole(final Path path, final byte[] bytes) throws IOException {
        final Path partial = path.resolveSibling(path.getFileName() + ".partial");
        Files.createDirectories(path.getParent());
        Files.write(partial, bytes);
        try {
            Files.move(partial, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (final AtomicMoveNotSupportedException notOnThisDisk) {
            Files.move(partial, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** The first of {@code name.unreadable}, {@code name.unreadable.1}, ... that is free, so no kept copy is lost. */
    private static Path freeKeptName(final Path path, final String fileName) {
        Path kept = path.resolveSibling(fileName + ".unreadable");
        for (int n = 1; Files.exists(kept); n++) {
            kept = path.resolveSibling(fileName + ".unreadable." + n);
        }
        return kept;
    }
}
