/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.state;

import com.mojang.serialization.Codec;
import dev.jstech.core.persistence.ISaveUpgrade;
import dev.jstech.core.persistence.SaveFiles;
import dev.jstech.core.persistence.SaveLayout;
import dev.jstech.core.team.CoreTeams;
import java.util.Objects;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * Something a mod keeps with a world, declared once: a value for the whole server ({@link ServerState}), one for each
 * dimension ({@link DimensionState}), one for each player ({@link PlayerState}) or one for each team ({@link
 * TeamState}). It is saved with the world in a file of its own, in the version of its layout, and an older file is
 * brought up to today's by the steps the state declares.
 *
 * <p>Declared with a builder, the scope chosen last, and kept as a constant; then registered once, while the game
 * loads, with {@link CoreStates#register}:
 *
 * <pre>{@code
 * ServerState<Integer> LAUNCHES = CoreState.builder(id("launches"), Codec.INT, 0)
 *         .synced(ByteBufCodecs.VAR_INT)
 *         .server();
 * }</pre>
 *
 * <p>Values are read and changed on the server's thread, and are best immutable: a state holds what it is given and
 * is told of a change by being given the next value. A state declared {@code synced} also sends each player the value
 * that is theirs to see (the server's, their dimension's, their own, their team's) when it changes and when they join,
 * and {@link #client()} reads it on their game.
 *
 * @param <T> the value kept
 */
public abstract sealed class CoreState<T> permits ServerState, DimensionState, PlayerState, TeamState {

    private final ResourceLocation id;
    private final Codec<T> codec;
    private final T defaultValue;
    private final SaveLayout layout;
    private final String fileName;
    private final @Nullable StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec;
    private volatile boolean registered;

    CoreState(final Builder<T> builder) {
        this.id = builder.id;
        this.codec = builder.codec;
        this.defaultValue = builder.defaultValue;
        this.layout = builder.layout.build();
        this.fileName = builder.fileName;
        this.streamCodec = builder.streamCodec;
    }

    /**
     * Starts declaring a state.
     *
     * @param id           its name, which also names its file unless another is given
     * @param codec        what writes its value to the save
     * @param defaultValue its value before anything is kept: for a player's or a team's, the value of each one
     */
    public static <T> Builder<T> builder(final ResourceLocation id, final Codec<T> codec, final T defaultValue) {
        return new Builder<>(id, codec, defaultValue);
    }

    public ResourceLocation id() {
        return this.id;
    }

    public Codec<T> codec() {
        return this.codec;
    }

    /** The value before anything is kept; for a player's or a team's state, the value of each one. */
    public T defaultValue() {
        return this.defaultValue;
    }

    /** The layout its file is written in. */
    public SaveLayout layout() {
        return this.layout;
    }

    /** The name of its file in a world's saved data, without the extension. */
    public String fileName() {
        return this.fileName;
    }

    /** Whether it sends each player the value that is theirs to see. */
    public boolean synced() {
        return this.streamCodec != null;
    }

    /**
     * On a player's game, the value this player's game was last sent: the server's, their dimension's, their own or
     * their team's, as the state's scope says. The default until one comes, and always for a state not synced.
     */
    public T client() {
        return ClientStates.get(this);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + this.id + "]";
    }

    /** Sends {@code player} every value of this state that is theirs to see, as they join or arrive. */
    abstract void sendTo(ServerPlayer player);

    /** Sends the value under {@code key}, which changed, to every player whose it is to see. */
    abstract void sendChange(MinecraftServer server, Object key);

    void markRegistered() {
        this.registered = true;
    }

    /** The file of this state in {@code level}'s saved data, opened through {@code factory}. */
    <V> StateSave<V> open(final ServerLevel level, final SavedData.Factory<StateSave<V>> factory) {
        if (!this.registered) {
            throw new IllegalStateException(this.id + " is used before it was registered with CoreStates.register");
        }
        final StateSave<V> save = level.getDataStorage().computeIfAbsent(factory, this.fileName);
        final int newer = save.takeNewer();
        if (newer > 0) {
            SaveFiles.keepNewerCopy(level, this.fileName, newer);
        }
        return save;
    }

    /** The value under {@code key} changed: it is sent at the end of the tick to whoever sees it. */
    void changed(final Object key) {
        if (synced()) {
            StateSync.changed(this, key);
        }
    }

    void send(final ServerPlayer player, final T value) {
        // A player whose connection agreed on no payloads, one a test made, is sent nothing.
        if (synced() && player.connection.hasChannel(StateSyncPayload.TYPE)) {
            PacketDistributor.sendToPlayer(player, new StateSyncPayload(this, value));
        }
    }

    @SuppressWarnings("unchecked")
    void writeValue(final RegistryFriendlyByteBuf buf, final Object value) {
        Objects.requireNonNull(this.streamCodec, "a state not synced is never sent").encode(buf, (T) value);
    }

    T readValue(final RegistryFriendlyByteBuf buf) {
        return Objects.requireNonNull(this.streamCodec, "a state not synced is never sent").decode(buf);
    }

    /**
     * Declares a state a part at a time; the scope, chosen last, makes it.
     *
     * @param <T> the value kept
     */
    public static final class Builder<T> {

        private final ResourceLocation id;
        private final Codec<T> codec;
        private final T defaultValue;
        private final SaveLayout.Builder layout;
        private String fileName;
        private @Nullable StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec;

        private Builder(final ResourceLocation id, final Codec<T> codec, final T defaultValue) {
            this.id = Objects.requireNonNull(id, "id");
            this.codec = Objects.requireNonNull(codec, "codec");
            this.defaultValue = Objects.requireNonNull(defaultValue, "defaultValue");
            this.layout = SaveLayout.builder(id.toString());
            this.fileName = id.getNamespace() + "_" + id.getPath().replace('/', '_');
        }

        /** The name of its file in a world's saved data, without the extension, when not the one its id gives. */
        public Builder<T> fileName(final String name) {
            if (name == null || name.isBlank() || name.contains("/") || name.contains("\\") || name.contains(":")) {
                throw new IllegalArgumentException("a state's file name is one word, without a folder: " + name);
            }
            this.fileName = name;
            return this;
        }

        /** Today's version of its layout, counted from 1; raised when an older file needs a step to be read. */
        public Builder<T> version(final int layout) {
            this.layout.version(layout);
            return this;
        }

        /**
         * The step that takes its file's value from version {@code from} of the layout to the next. A file from before
         * the state had versions is at version 0, and its step is handed the whole file as it was saved.
         */
        public Builder<T> upgrade(final int from, final ISaveUpgrade step) {
            this.layout.upgrade(from, step);
            return this;
        }

        /** Sends each player the value that is theirs to see, written by {@code codec}. */
        public Builder<T> synced(final StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
            this.streamCodec = Objects.requireNonNull(codec, "codec");
            return this;
        }

        /** One value for the whole server, in every dimension. */
        public ServerState<T> server() {
            return new ServerState<>(this);
        }

        /** One value for each dimension. */
        public DimensionState<T> dimension() {
            return new DimensionState<>(this);
        }

        /** One value for each player, kept whether they are online or not. */
        public PlayerState<T> player() {
            return new PlayerState<>(this);
        }

        /** One value for each team, as {@link CoreTeams} says who is on which. */
        public TeamState<T> team() {
            return new TeamState<>(this);
        }
    }
}
