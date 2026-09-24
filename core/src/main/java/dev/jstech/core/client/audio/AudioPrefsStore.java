/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.audio;

import dev.jstech.core.JsCore;
import dev.jstech.core.audio.AudioPrefs;
import dev.jstech.core.audio.AudioPrefsJson;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.neoforged.fml.loading.FMLPaths;

/**
 * The player's sound preferences, read from their file the first time they are asked for and written back each time
 * they change. The file sits with the game's own options, since it is the player's and not any world's.
 */
public final class AudioPrefsStore {

    private static final String FILE_NAME = "jstech-audio.json";

    private static AudioPrefs prefs;

    private AudioPrefsStore() {
    }

    /** The preferences in force, read from the file on first use. */
    public static synchronized AudioPrefs prefs() {
        if (prefs == null) {
            prefs = read();
        }
        return prefs;
    }

    /** Writes the preferences to their file; a file that cannot be written is said in the log and left as it was. */
    public static synchronized void save() {
        try {
            Files.writeString(file(), AudioPrefsJson.write(prefs()), StandardCharsets.UTF_8);
        } catch (final IOException e) {
            JsCore.LOGGER.warn("The sound preferences could not be written to {}", file(), e);
        }
    }

    private static AudioPrefs read() {
        final Path file = file();
        if (!Files.isRegularFile(file)) {
            return new AudioPrefs();
        }
        try {
            return AudioPrefsJson.read(Files.readString(file, StandardCharsets.UTF_8));
        } catch (final IOException e) {
            JsCore.LOGGER.warn("The sound preferences could not be read from {}; using the defaults", file, e);
            return new AudioPrefs();
        }
    }

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve(FILE_NAME);
    }
}
