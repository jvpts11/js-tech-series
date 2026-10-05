/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.integration.ftbteams;

import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import dev.jstech.core.team.CoreTeams;
import dev.jstech.core.team.ITeamSource;
import dev.jstech.core.team.ScoreboardTeams;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;

/**
 * FTB Teams as the Core's source of teams: a player in a party or a server team is on that team, by its id, and a
 * player on their own is on their own team. Before FTB Teams has read its teams, while the server starts, the
 * scoreboard answers.
 */
final class FtbTeamSource implements ITeamSource {

    /** What every id of an FTB team starts with, so it never meets an id another source gives. */
    private static final String PREFIX = "ftbteams:";

    @Override
    public String teamOf(final MinecraftServer server, final UUID player) {
        if (!FTBTeamsAPI.api().isManagerLoaded()) {
            return ScoreboardTeams.INSTANCE.teamOf(server, player);
        }
        final Optional<Team> team = FTBTeamsAPI.api().getManager().getTeamForPlayerID(player);
        // A player's own team in FTB Teams is a team of one, the same as the Core's own.
        return team.filter(found -> !found.isPlayerTeam()).map(found -> PREFIX + found.getId())
                .orElseGet(() -> CoreTeams.solo(player));
    }
}
