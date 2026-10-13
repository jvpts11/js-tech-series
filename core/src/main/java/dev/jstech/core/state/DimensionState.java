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
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * One value for each dimension, kept in that dimension's own saved data. A synced one is sent to the players in the
 * dimension, and to a player again each time they arrive in another.
 *
 * @param <T> the value kept
 */
public final class DimensionState<T> extends CoreState<T> {

    private final SavedData.Factory<StateSave<T>> factory;

    DimensionState(final Builder<T> builder) {
        super(builder);
        this.factory = StateSave.factory(id().toString(), codec(), layout(), this::defaultValue, value -> value);
    }

    /** How its file is made new and read back, to read it from a world's saved data other than the running one. */
    public SavedData.Factory<StateSave<T>> factory() {
        return this.factory;
    }

    public T get(final ServerLevel level) {
        return file(level).value();
    }

    /** Keeps {@code value} for {@code level}; a value equal to the one kept changes nothing and sends nothing. */
    public void set(final ServerLevel level, final T value) {
        Objects.requireNonNull(value, "value");
        final StateSave<T> file = file(level);
        if (!value.equals(file.value())) {
            file.replace(value);
            changed(level.dimension());
        }
    }

    /** Keeps what {@code change} makes of {@code level}'s value, and gives it back. */
    public T update(final ServerLevel level, final UnaryOperator<T> change) {
        final T next = change.apply(get(level));
        set(level, next);
        return next;
    }

    @Override
    void sendTo(final ServerPlayer player) {
        send(player, get(player.serverLevel()));
    }

    @Override
    @SuppressWarnings("unchecked")
    void sendChange(final MinecraftServer server, final Object key) {
        final ServerLevel level = server.getLevel((ResourceKey<Level>) key);
        if (level == null) {
            return;
        }
        final T value = get(level);
        for (final ServerPlayer player : level.players()) {
            send(player, value);
        }
    }

    private StateSave<T> file(final ServerLevel level) {
        return open(level, this.factory);
    }
}
