/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.team;

import java.util.List;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;

/**
 * Where the Core learns which team a player is on. The game's own scoreboard teams answer by default; a mod that
 * brings teams of its own (a party or faction mod) hands the Core a source of its own through {@link CoreTeams#use}.
 */
@FunctionalInterface
public interface ITeamSource {

    /**
     * The team {@code player} is on, as an id that stays the same across restarts, whether or not the player is
     * online. A player on no team is on a team of their own, {@link CoreTeams#solo(UUID)}.
     */
    String teamOf(MinecraftServer server, UUID player);

    /**
     * The players on {@code team}, as far as the source can say; none by default, for a source that cannot list a
     * team's players. A team of one player is that player.
     */
    default List<UUID> membersOf(final MinecraftServer server, final String team) {
        return List.of();
    }
}
