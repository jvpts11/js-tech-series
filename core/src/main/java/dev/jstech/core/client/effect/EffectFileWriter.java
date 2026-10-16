/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.effect;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import dev.jstech.core.developer.DevSources;
import dev.jstech.core.effect.EffectDefinition;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * Writes an effect's file where its mod keeps its resources in its source, {@code assets/<namespace>/effects/}, as the
 * effects editor saves it, and has the game play it as written at once. A test writes elsewhere.
 */
public final class EffectFileWriter {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String ASSETS = "assets";

    private static volatile @Nullable Path into;

    private EffectFileWriter() {
    }

    /** Has effects be written under {@code root} in place of their mods' sources, or there again for none. */
    @ApiStatus.Internal
    public static void writeInto(final @Nullable Path root) {
        into = root;
    }

    /** Where the effect {@code id}'s file is written, if its mod's source is in this run. */
    public static Optional<Path> fileOf(final ResourceLocation id) {
        final Path root = into;
        final Optional<Path> resources = root != null ? Optional.of(root) : DevSources.resourcesOf(id.getNamespace());
        return resources.map(folder -> folder.resolve(ASSETS).resolve(id.getNamespace())
                .resolve(EffectFiles.path(id)));
    }

    /** Writes {@code definition} as the effect {@code id}'s file; where it was written. */
    public static Path write(final ResourceLocation id, final EffectDefinition definition) throws IOException {
        final Path file = fileOf(id).orElseThrow(() -> new IOException(id.getNamespace()));
        final JsonElement json = EffectDefinition.CODEC.encodeStart(JsonOps.INSTANCE, definition).getOrThrow();
        Files.createDirectories(file.getParent());
        Files.writeString(file, GSON.toJson(json) + "\n", StandardCharsets.UTF_8);
        EffectFiles.put(id, definition);
        return file;
    }
}
