/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.team;

import com.mojang.logging.LogUtils;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

/**
 * The one place the Core asks which team a player is on. The answer comes from a single {@link ITeamSource}: the
 * game's scoreboard teams until a mod hands over another, which it does once, while the game loads. A mod that keeps
 * something for a team (a state kept per team, a team's machines) asks here and never the source itself, so it keeps
 * working whichever source is in use.
 */
public final class CoreTeams {

    private static final Logger LOGGER = LogUtils.getLogger();
    /** What the id of a player's team of their own starts with. */
    private static final String SOLO = "player:";

    private static volatile ITeamSource source = ScoreboardTeams.INSTANCE;

    private CoreTeams() {
    }

    /** The source in use. */
    public static ITeamSource source() {
        return source;
    }

    /** Hands the Core another source of teams, from a mod that brings teams of its own. */
    public static void use(final ITeamSource next) {
        final ITeamSource previous = source;
        source = Objects.requireNonNull(next, "next");
        if (previous != ScoreboardTeams.INSTANCE && previous != next) {
            LOGGER.warn("Teams were coming from {} and now come from {}; only one source of teams is used",
                    previous.getClass().getName(), next.getClass().getName());
        }
    }

    /** The team {@code player} is on; a player on no team is on their own, {@link #solo(UUID)}. */
    public static String teamOf(final MinecraftServer server, final UUID player) {
        return source.teamOf(server, player);
    }

    /** The id of {@code player}'s team of their own, the team of a player on no other. */
    public static String solo(final UUID player) {
        return SOLO + player;
    }
}
