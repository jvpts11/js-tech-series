/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Where a mod's own resources are kept in its source, in a development run: the folder the build copies them from,
 * not the copy the run loads, so what a tool of the studio writes there stays with the mod. A mod loaded from a jar
 * has none.
 */
public final class DevSources {

    private static final String BUILD = "build";
    private static final String RESOURCES = "resources";
    private static final String MAIN = "main";

    private DevSources() {
    }

    /** The folder {@code modid}'s resources are written in: {@code src/main/resources} of its project. */
    public static Optional<Path> resourcesOf(final String modid) {
        final List<Path> folders = ModFolders.ofThisRun().getOrDefault(modid, List.of());
        for (final Path folder : folders) {
            final Optional<Path> source = sourceOf(folder);
            if (source.isPresent()) {
                return source;
            }
        }
        return Optional.empty();
    }

    /**
     * The source of the resources a run loads from {@code folder}: {@code <project>/src/main/resources} for the
     * build's {@code <project>/build/resources/main}, the folder itself when it is a source; none for another.
     */
    public static Optional<Path> sourceOf(final Path folder) {
        final Path absolute = folder.toAbsolutePath().normalize();
        final int parts = absolute.getNameCount();
        if (parts >= 3 && absolute.getName(parts - 1).toString().equals(MAIN)
                && absolute.getName(parts - 2).toString().equals(RESOURCES)
                && absolute.getName(parts - 3).toString().equals(BUILD)) {
            final Path source = absolute.getParent().getParent().getParent().resolve("src").resolve(MAIN)
                    .resolve(RESOURCES);
            return Files.isDirectory(source) ? Optional.of(source) : Optional.empty();
        }
        if (absolute.endsWith(Path.of("src", MAIN, RESOURCES)) && Files.isDirectory(absolute)) {
            return Optional.of(absolute);
        }
        return Optional.empty();
    }
}
