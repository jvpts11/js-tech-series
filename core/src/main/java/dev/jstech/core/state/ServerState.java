/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.state;

import java.util.Objects;
import java.util.function.UnaryOperator;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * One value for the whole server, the same in every dimension, kept in the overworld's saved data. A synced one is
 * sent to every player.
 *
 * @param <T> the value kept
 */
public final class ServerState<T> extends CoreState<T> {

    private final SavedData.Factory<StateSave<T>> factory;

    /** The one key a value for the whole server is kept and sent under. */
    private static final Object WHOLE = new Object();

    ServerState(final Builder<T> builder) {
        super(builder);
        this.factory = StateSave.factory(id().toString(), codec(), layout(), this::defaultValue, value -> value);
    }

    /** How its file is made new and read back, to read it from a world's saved data other than the running one. */
    public SavedData.Factory<StateSave<T>> factory() {
        return this.factory;
    }

    public T get(final MinecraftServer server) {
        return file(server).value();
    }

    /** Keeps {@code value}; a value equal to the one kept changes nothing and sends nothing. */
    public void set(final MinecraftServer server, final T value) {
        Objects.requireNonNull(value, "value");
        final StateSave<T> file = file(server);
        if (!value.equals(file.value())) {
            file.replace(value);
            changed(WHOLE);
        }
    }

    /** Keeps what {@code change} makes of the value kept, and gives it back. */
    public T update(final MinecraftServer server, final UnaryOperator<T> change) {
        final T next = change.apply(get(server));
        set(server, next);
        return next;
    }

    @Override
    void sendTo(final ServerPlayer player) {
        send(player, get(player.server));
    }

    @Override
    void sendChange(final MinecraftServer server, final Object key) {
        final T value = get(server);
        for (final ServerPlayer player : server.getPlayerList().getPlayers()) {
            send(player, value);
        }
    }

    private StateSave<T> file(final MinecraftServer server) {
        return open(server.overworld(), this.factory);
    }
}
