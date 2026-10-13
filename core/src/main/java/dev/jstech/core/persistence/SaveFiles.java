/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.persistence;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;

/**
 * The files a world keeps its saved data in, one folder per dimension, and what is done with one a newer version of
 * its mod wrote: it is read as far as it can be and saved again as this version writes it, so the world goes on, and
 * the file as the newer version left it is kept beside it once, so going back to the newer version loses nothing.
 */
public final class SaveFiles {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String EXTENSION = ".dat";

    private SaveFiles() {
    }

    /** The folder a dimension's saved data lives in. */
    public static Path dataFolder(final ServerLevel level) {
        return DimensionType.getStorageFolder(level.dimension(), level.getServer().getWorldPath(LevelResource.ROOT))
                .resolve("data");
    }

    /** The file {@code name} is saved in, in {@code level}'s folder. */
    public static Path file(final ServerLevel level, final String name) {
        return dataFolder(level).resolve(name + EXTENSION);
    }

    /** Where the copy of {@code name} as version {@code version} of its layout left it is kept. */
    public static Path newerCopy(final ServerLevel level, final String name, final int version) {
        return dataFolder(level).resolve(name + ".newer-v" + version + EXTENSION);
    }

    /** Where the copy of {@code name} that could not be read at all is kept. */
    public static Path unreadableCopy(final ServerLevel level, final String name) {
        return dataFolder(level).resolve(name + ".unreadable" + EXTENSION);
    }

    /**
     * Keeps a copy of {@code name}, whose value could not be read at all in this version's layout, before it is saved
     * over with a fresh one. A copy already there is the one to keep, as the first is the closest to what was lost.
     */
    public static void keepUnreadableCopy(final ServerLevel level, final String name) {
        final Path saved = file(level, name);
        final Path copy = unreadableCopy(level, name);
        if (Files.exists(copy) || !Files.isRegularFile(saved)) {
            return;
        }
        try {
            Files.copy(saved, copy);
            LOGGER.warn("{} could not be read and is saved again from a fresh value; the file as it was is kept as {}",
                    saved.getFileName(), copy.getFileName());
        } catch (final IOException cannotCopy) {
            LOGGER.error("{} could not be read and could not be copied aside before it is saved again: {}",
                    saved.getFileName(), cannotCopy.getMessage());
        }
    }

    /**
     * Keeps a copy of {@code name}, which a newer version of its mod saved in version {@code version} of its layout,
     * before this version saves over it. The copy is made once: a copy already there is the one to keep.
     */
    public static void keepNewerCopy(final ServerLevel level, final String name, final int version) {
        final Path saved = file(level, name);
        final Path copy = newerCopy(level, name, version);
        if (Files.exists(copy) || !Files.isRegularFile(saved)) {
            return;
        }
        try {
            Files.copy(saved, copy);
            LOGGER.warn("{} was saved by a newer version of its mod; it is read as far as it can be, and the file as "
                    + "that version left it is kept as {}", saved.getFileName(), copy.getFileName());
        } catch (final IOException cannotCopy) {
            LOGGER.error("{} was saved by a newer version of its mod and could not be copied aside before it is "
                    + "saved again: {}", saved.getFileName(), cannotCopy.getMessage());
        }
    }
}
