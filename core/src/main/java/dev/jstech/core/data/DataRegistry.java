/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import dev.jstech.core.JsCore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Consumer;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.world.level.Level;

/**
 * Values read from a datapack's JSON files: every file under {@code data/<namespace>/<folder>/} is one value, known
 * by the file's id ({@code data/jsc/recipe_machines/vanilla.json} is {@code jsc:vanilla}), read with the registry's
 * codec. The files are read again whenever the server reloads its data, so a pack can change them while the world
 * runs; a file that does not read is left out, with a line in the log saying why.
 *
 * <p>A synced registry is sent to every player as they join and after every reload, and their game keeps it apart
 * from the server's, so a screen reads it on the player's side with {@link #clientEntries()} or, where either side
 * may ask, {@link #entries(Level)}. Declared once, beside what reads it:
 *
 * <pre>{@code
 * DataRegistry<Shade> SHADES = DataRegistry.builder(id("shades"), "shades", Shade.CODEC).synced().register();
 * }</pre>
 *
 * @param <T> what each file holds
 */
public final class DataRegistry<T> {

    private final ResourceLocation id;
    private final String folder;
    private final Codec<T> codec;
    private final boolean synced;
    private final List<Consumer<Map<ResourceLocation, T>>> listeners;
    private volatile Map<ResourceLocation, T> serverValues = Map.of();
    private volatile Map<ResourceLocation, T> clientValues = Map.of();
    private volatile long version;

    private static final Gson GSON = new GsonBuilder().create();

    private DataRegistry(final Builder<T> builder) {
        this.id = builder.id;
        this.folder = builder.folder;
        this.codec = builder.codec;
        this.synced = builder.synced;
        this.listeners = List.copyOf(builder.listeners);
    }

    /** Starts declaring the values in the files under {@code folder}, each read with {@code codec}. */
    public static <T> Builder<T> builder(final ResourceLocation id, final String folder, final Codec<T> codec) {
        return new Builder<>(id, folder, codec);
    }

    /** What the registry is known by, in the log and over the network. */
    public ResourceLocation id() {
        return this.id;
    }

    /** The folder of {@code data/<namespace>/} its files are in. */
    public String folder() {
        return this.folder;
    }

    /** Whether it is sent to the players. */
    public boolean synced() {
        return this.synced;
    }

    /** The values the server read, by file id, in the order of the ids. */
    public Map<ResourceLocation, T> entries() {
        return this.serverValues;
    }

    /** The value the server read from the file {@code file}. */
    public Optional<T> get(final ResourceLocation file) {
        return Optional.ofNullable(this.serverValues.get(file));
    }

    /** The values the server sent this player's game, by file id; empty for a registry that is not synced. */
    public Map<ResourceLocation, T> clientEntries() {
        return this.clientValues;
    }

    /** The values on the side {@code level} is on. */
    public Map<ResourceLocation, T> entries(final Level level) {
        return level.isClientSide() ? this.clientValues : this.serverValues;
    }

    /** How many times the server has read the files, or been handed values. */
    public long version() {
        return this.version;
    }

    /** Reads every file of the registry {@code manager} can see, as a reload does. */
    public void reload(final ResourceManager manager, final HolderLookup.Provider registries) {
        final Map<ResourceLocation, JsonElement> files = new LinkedHashMap<>();
        SimpleJsonResourceReloadListener.scanDirectory(manager, this.folder, GSON, files);
        load(files, registries);
    }

    /** Hands the server values that came from no file: what a test does without a datapack. */
    public void replace(final Map<ResourceLocation, T> values) {
        this.serverValues = Collections.unmodifiableMap(new TreeMap<>(values));
        this.version++;
        this.listeners.forEach(listener -> listener.accept(this.serverValues));
    }

    void load(final Map<ResourceLocation, JsonElement> files, final HolderLookup.Provider registries) {
        final RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registries);
        final Map<ResourceLocation, T> read = new TreeMap<>();
        for (final Map.Entry<ResourceLocation, JsonElement> file : files.entrySet()) {
            final DataResult<T> value = this.codec.parse(ops, file.getValue());
            value.resultOrPartial(problem -> JsCore.LOGGER.warn("The {} file {} does not read and is left out: {}",
                    this.id, file.getKey(), problem)).ifPresent(parsed -> read.put(file.getKey(), parsed));
        }
        replace(read);
        JsCore.LOGGER.debug("Read {} {} files", read.size(), this.id);
    }

    /* The server's values as the players are sent them. */
    CompoundTag encode(final HolderLookup.Provider registries) {
        final RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, registries);
        final CompoundTag entries = new CompoundTag();
        for (final Map.Entry<ResourceLocation, T> entry : this.serverValues.entrySet()) {
            this.codec.encodeStart(ops, entry.getValue())
                    .resultOrPartial(problem -> JsCore.LOGGER.warn("The {} value {} cannot be sent: {}", this.id,
                            entry.getKey(), problem))
                    .ifPresent(tag -> entries.put(entry.getKey().toString(), tag));
        }
        return entries;
    }

    /* What the server sent, kept on the player's game. */
    void acceptFromServer(final CompoundTag entries, final HolderLookup.Provider registries) {
        final RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, registries);
        final Map<ResourceLocation, T> read = new TreeMap<>();
        for (final String key : entries.getAllKeys()) {
            final ResourceLocation file = ResourceLocation.tryParse(key);
            if (file == null) {
                continue;
            }
            this.codec.parse(ops, entries.get(key))
                    .resultOrPartial(problem -> JsCore.LOGGER.warn("The {} value {} from the server does not read: {}",
                            this.id, file, problem))
                    .ifPresent(value -> read.put(file, value));
        }
        this.clientValues = Collections.unmodifiableMap(read);
    }

    /* The player left the server: what it sent goes with it. */
    void forgetClientValues() {
        this.clientValues = Map.of();
    }

    /** Declares a registry of datapack files one property at a time. */
    public static final class Builder<T> {

        private final ResourceLocation id;
        private final String folder;
        private final Codec<T> codec;
        private final List<Consumer<Map<ResourceLocation, T>>> listeners = new ArrayList<>();
        private boolean synced;

        private Builder(final ResourceLocation id, final String folder, final Codec<T> codec) {
            this.id = Objects.requireNonNull(id, "id");
            this.folder = Objects.requireNonNull(folder, "folder");
            this.codec = Objects.requireNonNull(codec, "codec");
        }

        /** It is sent to every player as they join and after every reload. */
        public Builder<T> synced() {
            this.synced = true;
            return this;
        }

        /** Hears every new set of values the server reads, on the server, by file id in the order of the ids. */
        public Builder<T> onReload(final Consumer<Map<ResourceLocation, T>> listener) {
            this.listeners.add(Objects.requireNonNull(listener, "listener"));
            return this;
        }

        /**
         * The registry, read whenever the server loads its data.
         *
         * @throws IllegalStateException when a registry of that id was declared already
         */
        public DataRegistry<T> register() {
            final DataRegistry<T> registry = new DataRegistry<>(this);
            DataRegistries.add(registry);
            return registry;
        }
    }
}
