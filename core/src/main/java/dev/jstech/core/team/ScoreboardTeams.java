/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.team;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;

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
        return CoreTeams.nameOf(server, player).map(name -> server.getScoreboard().getPlayersTeam(name))
                .map(ScoreboardTeams::idOf).orElseGet(() -> CoreTeams.solo(player));
    }

    /* The players whose names are in the scoreboard team, each by the id the server knows the name by. */
    @Override
    public List<UUID> membersOf(final MinecraftServer server, final String team) {
        final PlayerTeam found = team.startsWith(PREFIX) ? server.getScoreboard().getPlayerTeam(team.substring(
                PREFIX.length())) : null;
        if (found == null) {
            return CoreTeams.soloPlayer(team).map(List::of).orElse(List.of());
        }
        final List<UUID> out = new ArrayList<>();
        for (final String name : found.getPlayers()) {
            final ServerPlayer online = server.getPlayerList().getPlayerByName(name);
            if (online != null) {
                out.add(online.getUUID());
            } else if (server.getProfileCache() != null) {
                server.getProfileCache().get(name).map(GameProfile::getId).ifPresent(out::add);
            }
        }
        return out;
    }

    /* A scoreboard team's display name, as /team names it. */
    @Override
    public Optional<String> teamName(final MinecraftServer server, final String team) {
        return Optional.ofNullable(team.startsWith(PREFIX) ? server.getScoreboard().getPlayerTeam(team.substring(
                PREFIX.length())) : null).map(found -> found.getDisplayName().getString());
    }
}
