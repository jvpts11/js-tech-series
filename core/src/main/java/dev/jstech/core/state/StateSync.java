/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.state;

import dev.jstech.core.team.CoreTeams;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * What of the synced states each player is sent, and when. A value that changes is sent once, at the end of the tick,
 * however many times it changed in it. A player who joins is sent every value that is theirs to see; one who arrives
 * in another dimension, that dimension's values; one who changes team, the new team's values. A team change is
 * noticed by looking at each player's team once a second, since teams can change in ways nothing announces.
 */
final class StateSync {

    /** The values changed since the end of the last tick, each once. */
    private static final Set<Change> PENDING = ConcurrentHashMap.newKeySet();
    /** The team each player online was last sent the values of. */
    private static final Map<UUID, String> TEAMS = new ConcurrentHashMap<>();
    /** How often each player's team is looked at, in ticks. */
    private static final int TEAM_CHECK_EVERY = 20;

    private StateSync() {
    }

    static void changed(final CoreState<?> state, final Object key) {
        PENDING.add(new Change(state, key));
    }

    /** Sends what changed this tick, and the team values of each player who changed team. */
    static void endOfTick(final MinecraftServer server) {
        if (!PENDING.isEmpty()) {
            final List<Change> changes = new ArrayList<>(PENDING);
            PENDING.removeAll(changes);
            for (final Change change : changes) {
                change.state().sendChange(server, change.key());
            }
        }
        if (server.getTickCount() % TEAM_CHECK_EVERY == 0) {
            for (final ServerPlayer player : server.getPlayerList().getPlayers()) {
                final String team = CoreTeams.teamOf(server, player.getUUID());
                if (!team.equals(TEAMS.put(player.getUUID(), team))) {
                    sendAll(player, TeamState.class);
                }
            }
        }
    }

    static void joined(final ServerPlayer player) {
        TEAMS.put(player.getUUID(), CoreTeams.teamOf(player.server, player.getUUID()));
        sendAll(player, CoreState.class);
    }

    static void arrived(final ServerPlayer player) {
        sendAll(player, DimensionState.class);
    }

    static void left(final UUID player) {
        TEAMS.remove(player);
    }

    static void clear() {
        PENDING.clear();
        TEAMS.clear();
    }

    /* Every registered synced state of the kind {@code kind} sends {@code player} what is theirs to see. */
    private static void sendAll(final ServerPlayer player, final Class<?> kind) {
        for (final CoreState<?> state : CoreStates.all()) {
            if (state.synced() && kind.isInstance(state)) {
                state.sendTo(player);
            }
        }
    }

    /**
     * A value that changed.
     *
     * @param state the state
     * @param key   which of its values: the server's, a dimension's, a player's or a team's
     */
    private record Change(CoreState<?> state, Object key) {

        Change {
            Objects.requireNonNull(state, "state");
            Objects.requireNonNull(key, "key");
        }
    }
}
