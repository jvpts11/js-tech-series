/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.state;

import com.mojang.serialization.Codec;
import dev.jstech.core.team.CoreTeams;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.UnaryOperator;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * One value for each team, all kept in one file of the overworld's saved data, by the team's id as {@link CoreTeams}
 * gives it. A player on no team is on a team of their own, so a team state holds a value for them too. A team whose
 * value is the default takes no room in the file. A synced one is sent to the team's players who are online, and to a
 * player again when they change team.
 *
 * @param <T> the value kept for each team
 */
public final class TeamState<T> extends CoreState<T> {

    private final SavedData.Factory<StateSave<Map<String, T>>> factory;

    TeamState(final Builder<T> builder) {
        super(builder);
        this.factory = StateSave.factory(id().toString(), Codec.unboundedMap(Codec.STRING, codec()), layout(),
                HashMap::new, HashMap::new);
    }

    /** How its file is made new and read back, to read it from a world's saved data other than the running one. */
    public SavedData.Factory<StateSave<Map<String, T>>> factory() {
        return this.factory;
    }

    public T get(final MinecraftServer server, final String team) {
        return KeyedValues.get(file(server), team, defaultValue());
    }

    /** The value of the team {@code player} is on. */
    public T ofPlayer(final MinecraftServer server, final UUID player) {
        return get(server, CoreTeams.teamOf(server, player));
    }

    /** Keeps {@code value} for {@code team}; a value equal to the one kept changes nothing and sends nothing. */
    public void set(final MinecraftServer server, final String team, final T value) {
        Objects.requireNonNull(team, "team");
        Objects.requireNonNull(value, "value");
        if (KeyedValues.set(file(server), team, value, defaultValue())) {
            changed(team);
        }
    }

    /** Keeps what {@code change} makes of {@code team}'s value, and gives it back. */
    public T update(final MinecraftServer server, final String team, final UnaryOperator<T> change) {
        final T next = change.apply(get(server, team));
        set(server, team, next);
        return next;
    }

    /** Every team's value that is not the default, by team. */
    public Map<String, T> all(final MinecraftServer server) {
        return KeyedValues.all(file(server));
    }

    @Override
    void sendTo(final ServerPlayer player) {
        send(player, ofPlayer(player.server, player.getUUID()));
    }

    @Override
    void sendChange(final MinecraftServer server, final Object key) {
        final T value = get(server, (String) key);
        for (final ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (key.equals(CoreTeams.teamOf(server, player.getUUID()))) {
                send(player, value);
            }
        }
    }

    private StateSave<Map<String, T>> file(final MinecraftServer server) {
        return open(server.overworld(), this.factory);
    }
}
