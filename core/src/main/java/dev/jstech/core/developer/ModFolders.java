/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * The folders a development run loads each mod from, as the build tool names them to the game: entries of
 * {@code modid%%folder}, joined by the system's path separator, in the {@code fml.modFolders} property or, in a run
 * an IDE starts, the {@code MOD_CLASSES} variable. A packed game has none, its mods being jars.
 */
public final class ModFolders {

    private static final String BETWEEN = "%%";

    private ModFolders() {
    }

    /** The folders of every mod this run loads from folders, by mod; none in a packed game. */
    public static Map<String, List<Path>> ofThisRun() {
        String written = System.getProperty("fml.modFolders");
        if (written == null || written.isBlank()) {
            written = System.getenv("MOD_CLASSES");
        }
        return written == null || written.isBlank() ? Map.of() : parse(written, File.pathSeparator);
    }

    /** The folders {@code written} names, by mod, in the order they are written; an entry without a mod is skipped. */
    public static Map<String, List<Path>> parse(final String written, final String separator) {
        final Map<String, List<Path>> byMod = new LinkedHashMap<>();
        for (final String entry : written.split(Pattern.quote(separator))) {
            final int between = entry.indexOf(BETWEEN);
            if (between <= 0 || between + BETWEEN.length() >= entry.length()) {
                continue;
            }
            byMod.computeIfAbsent(entry.substring(0, between).strip(), mod -> new ArrayList<>())
                    .add(Path.of(entry.substring(between + BETWEEN.length()).strip()));
        }
        byMod.replaceAll((mod, folders) -> List.copyOf(folders));
        return byMod;
    }
}
