/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * Teams, owners and permissions: which team a player is on, asked in one place, from the game's scoreboard teams or
 * from a mod that brings teams of its own; whose a thing is, and who else may use, open, set up or break it; and the
 * permissions the series asks the server's permission system about.
 *
 * <p>A mod asks {@link CoreTeams} which team a player is on, {@code teamOf(server, player)}, and never the source
 * itself; a player on no team is on a team of their own. The answer comes from one {@link ITeamSource}: the game's own
 * teams ({@link ScoreboardTeams}) until a mod hands over another with {@code CoreTeams.use}, once, while the game
 * loads. A thing somebody owns implements {@link IOwned} and answers its {@link Ownership}: the owner, the
 * {@link Access} it is shared with (nobody, the owner's team, or everyone) and the players the owner trusts by name,
 * each to a {@link Trust} that holds the levels under it. {@code mayUse}, {@code mayOpen}, {@code maySetUp} and
 * {@code mayBreak} answer for a player, the team asked each time, and a block whose block entity is owned, or an owned
 * entity, is kept from whoever its ownership does not let. An ownership has a codec and a stream codec, so it is saved
 * and sent like any value. {@link CorePermissions} declares each permission with the operator level that decides it
 * when no permission mod is installed, and a mod declares its own the same way; an operator, or whoever the server
 * grants it to, passes by an owner.
 *
 * <p>How owners, access and teams work is in {@code core/docs/OWNERSHIP.md}, and the permissions in
 * {@code core/docs/PROTECTION.md}. Examples that compile are J's Example's machines, each owned by whoever places it
 * ({@code example/src/main/java/dev/jstech/example/machine}), and the Core's tests of owners and teams
 * ({@code tests/src/main/java/dev/jstech/tests/gametest/core/OwnershipGameTests.java}).
 */
@ApiStatus.Experimental
package dev.jstech.core.team;

import org.jetbrains.annotations.ApiStatus;
