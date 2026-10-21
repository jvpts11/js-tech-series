/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.studio;

import dev.jstech.core.developer.DevSources;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;

/**
 * The files of a thing's parts, as the studio names them and opens them: by where they stand among a mod's resources,
 * looked for in the mod's source in this run, written by hand or by the data generation.
 */
public final class StudioFiles {

    private static final String ASSETS = "assets/";
    private static final String DATA = "data/";
    /** The folder the data generation writes beside the one written by hand. */
    private static final Path GENERATED = Path.of("src", "generated", "resources");

    private StudioFiles() {
    }

    /** The file of {@code file}'s mod that the player's game loads it from: {@code assets/<mod>/<path>}. */
    public static StudioSheet.File asset(final ResourceLocation file) {
        return new StudioSheet.File(file.getNamespace(), ASSETS + file.getNamespace() + "/" + file.getPath());
    }

    /** The file of {@code file}'s mod that the server loads it from: {@code data/<mod>/<path>}. */
    public static StudioSheet.File data(final ResourceLocation file) {
        return new StudioSheet.File(file.getNamespace(), DATA + file.getNamespace() + "/" + file.getPath());
    }

    /** Where {@code file} is written in the mod's source in this run, by hand or generated; none when nowhere. */
    public static Optional<Path> sourceOf(final StudioSheet.File file) {
        final Optional<Path> resources = DevSources.resourcesOf(file.modid());
        if (resources.isEmpty()) {
            return Optional.empty();
        }
        final Path written = resources.get().resolve(file.path());
        if (Files.isRegularFile(written)) {
            return Optional.of(written);
        }
        // src/main/resources sits beside src/generated/resources in the same project.
        final Path project = resources.get().getParent().getParent().getParent();
        final Path generated = project.resolve(GENERATED).resolve(file.path());
        return Files.isRegularFile(generated) ? Optional.of(generated) : Optional.empty();
    }

    /** Opens {@code file} in what the computer opens such files with; whether it was there to open. */
    public static boolean open(final StudioSheet.File file) {
        final Optional<Path> source = sourceOf(file);
        source.ifPresent(path -> Util.getPlatform().openFile(path.toFile()));
        return source.isPresent();
    }
}
