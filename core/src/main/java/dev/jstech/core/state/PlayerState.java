/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.state;

import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.UnaryOperator;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * One value for each player, all kept in one file of the overworld's saved data, so a player's value is read and
 * changed whether they are online or not: what a player earned while away waits here for them. A player whose value
 * is the default takes no room in the file. A synced one is sent to each player alone, their own value and no other.
 *
 * @param <T> the value kept for each player
 */
public final class PlayerState<T> extends CoreState<T> {

    private final SavedData.Factory<StateSave<Map<UUID, T>>> factory;

    PlayerState(final Builder<T> builder) {
        super(builder);
        this.factory = StateSave.factory(id().toString(), Codec.unboundedMap(UUIDUtil.STRING_CODEC, codec()),
                layout(), HashMap::new, HashMap::new);
    }

    /** How its file is made new and read back, to read it from a world's saved data other than the running one. */
    public SavedData.Factory<StateSave<Map<UUID, T>>> factory() {
        return this.factory;
    }

    public T get(final MinecraftServer server, final UUID player) {
        return file(server).value().getOrDefault(player, defaultValue());
    }

    /** Keeps {@code value} for {@code player}; a value equal to the one kept changes nothing and sends nothing. */
    public void set(final MinecraftServer server, final UUID player, final T value) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(value, "value");
        final StateSave<Map<UUID, T>> file = file(server);
        final T kept = file.value().getOrDefault(player, defaultValue());
        if (value.equals(kept)) {
            return;
        }
        if (value.equals(defaultValue())) {
            file.value().remove(player);
        } else {
            file.value().put(player, value);
        }
        file.changed();
        changed(player);
    }

    /** Keeps what {@code change} makes of {@code player}'s value, and gives it back. */
    public T update(final MinecraftServer server, final UUID player, final UnaryOperator<T> change) {
        final T next = change.apply(get(server, player));
        set(server, player, next);
        return next;
    }

    /** Every player's value that is not the default, by player. */
    public Map<UUID, T> all(final MinecraftServer server) {
        return Map.copyOf(file(server).value());
    }

    @Override
    void sendTo(final ServerPlayer player) {
        send(player, get(player.server, player.getUUID()));
    }

    @Override
    void sendChange(final MinecraftServer server, final Object key) {
        final ServerPlayer player = server.getPlayerList().getPlayer((UUID) key);
        if (player != null) {
            send(player, get(server, player.getUUID()));
        }
    }

    private StateSave<Map<UUID, T>> file(final MinecraftServer server) {
        return open(server.overworld(), this.factory);
    }
}
