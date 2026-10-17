/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.persistor;

import dev.jstech.core.config.ConfigFile;
import dev.jstech.core.config.ConfigKey;
import dev.jstech.core.config.ConfigSide;
import dev.jstech.core.config.format.ConfigFormats;

/**
 * The Space Persistor's settings, {@code jstech-persistor.toml} beside the world, the server's alone: what each chunk
 * it keeps loaded costs in power, which can be nothing at all. Then it takes no energy, has no port and no Protection
 * tab.
 * How many chunks it keeps is the world's own setting, each owner's number and the server's budget.
 */
public final class PersistorSettings {

    public static final String PERSISTOR = "persistor";
    /** What each chunk kept loaded costs unless the settings say otherwise, in watts. */
    public static final int DEFAULT_WATTS_PER_CHUNK = 200;
    private static final int MOST_WATTS_PER_CHUNK = 1_000_000;

    /** What each chunk a Persistor keeps loaded costs, in watts; 0 for no energy at all. */
    public static final ConfigKey<Integer> WATTS_PER_CHUNK = ConfigKey.whole(PERSISTOR + ".watts_per_chunk",
            DEFAULT_WATTS_PER_CHUNK).range(0, MOST_WATTS_PER_CHUNK)
            .comment("What each chunk a Space Persistor keeps loaded costs in power, in watts (with J's Energy off, "
                    + "an FE a tick is 20 watts). 0 turns its energy use off entirely: it takes none, and has no port "
                    + "and no Protection tab.")
            .named("Power per chunk").unit("W");

    /** The file, each setting passed into what it sets. */
    public static final ConfigFile FILE = ConfigFile.builder("jstech-persistor", ConfigSide.SERVER, ConfigFormats.TOML)
            .comment("The Space Persistor: what the chunks it keeps loaded cost in power.",
                    "Values outside their range are pulled to the nearer end when the file is read.")
            .sectionComment(PERSISTOR, "The Space Persistor, the series' chunk loader.")
            .sectionNamed(PERSISTOR, "Space Persistor")
            .key(WATTS_PER_CHUNK, PersistorSettings::setWattsPerChunk)
            .build();

    private static volatile int wattsPerChunk = DEFAULT_WATTS_PER_CHUNK;

    private PersistorSettings() {
    }

    /** What each chunk kept loaded costs, in watts; 0 for none. */
    public static int wattsPerChunk() {
        return wattsPerChunk;
    }

    public static void setWattsPerChunk(final int watts) {
        wattsPerChunk = Math.max(0, watts);
    }

    /** Whether a Persistor spends energy at all. */
    public static boolean spendsEnergy() {
        return wattsPerChunk > 0;
    }
}
