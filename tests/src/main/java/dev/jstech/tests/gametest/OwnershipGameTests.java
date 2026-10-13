/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import com.mojang.authlib.GameProfile;
import dev.jstech.core.gametest.GameTestPlayers;
import dev.jstech.core.integration.ftbteams.FtbTeamsIntegration;
import dev.jstech.core.team.Access;
import dev.jstech.core.team.CoreTeams;
import dev.jstech.core.team.ITeamSource;
import dev.jstech.core.team.Ownership;
import dev.jstech.core.team.ScoreboardTeams;
import dev.jstech.tests.JsTests;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Core's owners and teams: the teams come from FTB Teams when it is installed and from the scoreboard when it is
 * not, and a thing's owner lets in whom its access says: nobody else, the owner's team, or everyone.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class OwnershipGameTests {

    private static final String ARENA = "empty";
    private static final String CREW = "jstests:crew";

    private OwnershipGameTests() {
    }

    @GameTest(template = ARENA)
    public static void teams_comeFromFtbTeamsOnlyWhenItIsInstalled(final GameTestHelper helper) {
        final boolean expected = "present".equals(System.getProperty("jsc.gametests.ftbteams"));
        helper.assertTrue(FtbTeamsIntegration.isLoaded() == expected,
                "FTB Teams is loaded exactly when the run asked for it");
        final boolean fromScoreboard = CoreTeams.source() == ScoreboardTeams.INSTANCE;
        helper.assertTrue(fromScoreboard != FtbTeamsIntegration.isLoaded(),
                "the teams come from FTB Teams when it is there and from the scoreboard otherwise; source "
                        + CoreTeams.source().getClass().getSimpleName());
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void ownership_letsInWhomItsAccessSays(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        /*
         * Fake players: a mock one would log in, and the mods in the run send a player who logs in what its connection
         * never agreed to take.
         */
        final ServerPlayer owner = FakePlayerFactory.get(helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "jstests-owner"));
        final ServerPlayer visitor = FakePlayerFactory.get(helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "jstests-visitor"));
        final Ownership mine = Ownership.of(owner.getUUID());
        helper.assertTrue(mine.mayUse(owner), "the owner may always use it");
        helper.assertTrue(!mine.mayUse(visitor), "nobody else may use a private thing");
        helper.assertTrue(mine.withAccess(Access.PUBLIC).mayUse(visitor), "anybody may use a public one");
        helper.assertTrue(!mine.withAccess(Access.TEAM).mayUse(visitor), "a player on their own is on no team");
        // Both on one crew, for as long as the check takes: the same test body runs to its end before any other.
        final ITeamSource before = CoreTeams.source();
        CoreTeams.use((onServer, player) -> CREW);
        try {
            helper.assertTrue(mine.withAccess(Access.TEAM).mayUse(visitor),
                    "a player on the owner's team may use what the team shares");
            helper.assertTrue(!mine.mayUse(visitor), "but not what the owner keeps private");
        } finally {
            CoreTeams.use(before);
        }
        // Nobody of that id ever joined this server, so the id is the only name it knows.
        helper.assertTrue(mine.denial(server).english().contains(owner.getUUID().toString()),
                "a player turned away is told whose it is");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void scoreboardTeams_putAPlayerTheServerNeverMetOnATeamOfTheirOwn(final GameTestHelper helper) {
        final UUID stranger = UUID.randomUUID();
        helper.assertTrue(ScoreboardTeams.INSTANCE.teamOf(helper.getLevel().getServer(), stranger)
                .equals(CoreTeams.solo(stranger)), "a player with no name the server knows is on their own");
        final PlayerTeam team = new PlayerTeam(helper.getLevel().getServer().getScoreboard(), "jstests_crew");
        helper.assertTrue(ScoreboardTeams.idOf(team).equals("scoreboard:jstests_crew"),
                "a scoreboard team's id is its name, marked as the scoreboard's");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void scoreboardTeams_letTheOwnersTeamIn(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final ServerPlayer owner = GameTestPlayers.join(helper, "jstests-crew-owner");
        final ServerPlayer mate = GameTestPlayers.join(helper, "jstests-crew-mate");
        final Scoreboard scoreboard = server.getScoreboard();
        final PlayerTeam team = scoreboard.addPlayerTeam("jstests_crew_" + owner.getUUID().toString().substring(0, 8));
        try {
            final Ownership shared = Ownership.of(owner.getUUID()).withAccess(Access.TEAM);
            helper.assertTrue(!ScoreboardTeams.INSTANCE.teamOf(server, mate.getUUID())
                            .equals(ScoreboardTeams.INSTANCE.teamOf(server, owner.getUUID())),
                    "two players on no team are each on their own");
            scoreboard.addPlayerToTeam(owner.getScoreboardName(), team);
            scoreboard.addPlayerToTeam(mate.getScoreboardName(), team);
            helper.assertTrue(ScoreboardTeams.INSTANCE.teamOf(server, mate.getUUID())
                            .equals(ScoreboardTeams.idOf(team)),
                    "a player online on a scoreboard team is on it, found by their name");
            // Forced to the scoreboard source so the check runs whichever team mod the run has loaded.
            final ITeamSource before = CoreTeams.source();
            CoreTeams.use(ScoreboardTeams.INSTANCE);
            try {
                helper.assertTrue(shared.mayUse(mate), "and may use what the owner shares with the team");
            } finally {
                CoreTeams.use(before);
            }
        } finally {
            scoreboard.removePlayerTeam(team);
            GameTestPlayers.leave(owner);
            GameTestPlayers.leave(mate);
        }
        helper.succeed();
    }
}
