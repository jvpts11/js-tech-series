/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.integration.ftbteams;

import dev.jstech.core.team.CoreTeams;
import net.neoforged.fml.ModList;

/**
 * Soft integration with FTB Teams: when the mod is present, the Core's teams are its teams, so the machines a player
 * shares, the chunks a team keeps loaded and the states kept per team follow the parties players make there. Nothing
 * here touches an FTB Teams class unless {@link #isLoaded()} is true; without it the game's scoreboard teams answer.
 */
public final class FtbTeamsIntegration {

    public static final String MOD_ID = "ftbteams";

    private FtbTeamsIntegration() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    /** Hands the Core FTB Teams as its source of teams; a no-op when FTB Teams is absent. */
    public static void bootstrap() {
        if (isLoaded()) {
            useFtbTeams();
        }
    }

    // Kept in its own method so the source class (and the FTB Teams API behind it) is only loaded here.
    private static void useFtbTeams() {
        CoreTeams.use(new FtbTeamSource());
    }
}
