/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.vehicle;

import com.mojang.serialization.Codec;
import dev.jstech.core.data.DataRegistry;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * Things of one sort by id: those its mods declare in code, and those the files of a data pack say, a file winning
 * over a declaration of its id; the files are sent to every player, so both sides read the same.
 *
 * @param <T> the sort
 */
final class VehicleCatalogue<T> {

    private final DataRegistry<T> files;
    private final Map<ResourceLocation, T> declared = new ConcurrentHashMap<>();

    VehicleCatalogue(final ResourceLocation id, final String folder, final Codec<T> codec) {
        this.files = DataRegistry.builder(id, folder, codec).synced().register();
    }

    T declare(final ResourceLocation id, final T value) {
        declared.put(Objects.requireNonNull(id, "id"), Objects.requireNonNull(value, "value"));
        return value;
    }

    Optional<T> get(final Level level, final ResourceLocation id) {
        final T file = files.entries(level).get(id);
        return file != null ? Optional.of(file) : Optional.ofNullable(declared.get(id));
    }

    /**
     * The one of {@code id} wherever it is known, for what has no level to ask by, as an item's name: the server's
     * files, else the player's game's, else the declared.
     */
    Optional<T> anywhere(final ResourceLocation id) {
        final T server = files.entries().get(id);
        if (server != null) {
            return Optional.of(server);
        }
        final T client = files.clientEntries().get(id);
        return client != null ? Optional.of(client) : Optional.ofNullable(declared.get(id));
    }

    /** Every one on the side {@code level} is on, by id: the files' over the declared. */
    Map<ResourceLocation, T> all(final Level level) {
        final Map<ResourceLocation, T> all = new TreeMap<>(declared);
        all.putAll(files.entries(level));
        return all;
    }

    /** Every one the player's game has, by id: the files the server sent over the declared. */
    Map<ResourceLocation, T> allOnClient() {
        final Map<ResourceLocation, T> all = new TreeMap<>(declared);
        all.putAll(files.clientEntries());
        return all;
    }

    DataRegistry<T> files() {
        return files;
    }

    Map<ResourceLocation, T> declared() {
        return Map.copyOf(declared);
    }
}
