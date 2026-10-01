/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.team;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.world.scores.PlayerTeam;
import org.jetbrains.annotations.Nullable;

/**
 * The game's own teams, the ones {@code /team} makes: a player is on the scoreboard team their name is in, and on a
 * team of their own when it is in none. A player who is offline is found by the name the server last knew them by.
 */
public final class ScoreboardTeams implements ITeamSource {

    /** The one source of the game's teams. */
    public static final ScoreboardTeams INSTANCE = new ScoreboardTeams();
    /** What every id of a scoreboard team starts with, so it never meets an id another source gives. */
    private static final String PREFIX = "scoreboard:";

    private ScoreboardTeams() {
    }

    /** The id of a scoreboard team, as {@link #teamOf} gives it for the players on it. */
    public static String idOf(final PlayerTeam team) {
        return PREFIX + team.getName();
    }

    @Override
    public String teamOf(final MinecraftServer server, final UUID player) {
        final String name = nameOf(server, player);
        if (name != null) {
            final PlayerTeam team = server.getScoreboard().getPlayersTeam(name);
            if (team != null) {
                return idOf(team);
            }
        }
        return CoreTeams.solo(player);
    }

    private static @Nullable String nameOf(final MinecraftServer server, final UUID player) {
        final ServerPlayer online = server.getPlayerList().getPlayer(player);
        if (online != null) {
            return online.getScoreboardName();
        }
        final GameProfileCache profiles = server.getProfileCache();
        return profiles == null ? null : profiles.get(player).map(GameProfile::getName).orElse(null);
    }
}
