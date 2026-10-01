/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.core.team.CoreTeams;
import dev.jstech.core.team.ScoreboardTeams;
import dev.jstech.tests.testkit.TestStates;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

/**
 * The synced states reaching a player's game: the server's value, the value of the dimension they are in, their own
 * value and no other player's, and their team's, which follows them when they join a scoreboard team.
 */
public final class CoreStateClientTests {

    /** The scoreboard team the test puts the player on. */
    private static final String CREW = "jst_crew";

    private CoreStateClientTests() {
    }

    @ClientTest(timeoutTicks = 400)
    public static void states_reachThePlayerWhoseValuesTheyAre(final ClientTestContext ctx) {
        final UUID stranger = UUID.randomUUID();
        ctx.thenServer(0, level -> {
                    final MinecraftServer server = level.getServer();
                    final ServerPlayer player = ctx.serverPlayer();
                    TestStates.COUNTER.set(server, 1234);
                    TestStates.WEATHER.set(player.serverLevel(), "fog");
                    TestStates.SCORE.set(server, player.getUUID(), 77);
                    TestStates.SCORE.set(server, stranger, 99);
                })
                .thenWaitUntil(() -> TestStates.COUNTER.client() == 1234 && "fog".equals(TestStates.WEATHER.client())
                        && TestStates.SCORE.client() == 77, 60, "the server's, the dimension's and the player's own "
                        + "values on the player's game", () -> "counter " + TestStates.COUNTER.client() + ", weather "
                        + TestStates.WEATHER.client() + ", score " + TestStates.SCORE.client())
                .thenAssert(10, () -> TestStates.SCORE.client() == 77, "another player's score never reaches this game")
                .thenServer(0, level -> {
                    final MinecraftServer server = level.getServer();
                    TestStates.COUNTER.set(server, 0);
                    TestStates.WEATHER.set(ctx.serverPlayer().serverLevel(), "");
                    TestStates.SCORE.set(server, ctx.serverPlayer().getUUID(), 0);
                    TestStates.SCORE.set(server, stranger, 0);
                })
                .thenWaitUntil(() -> TestStates.COUNTER.client() == 0 && TestStates.SCORE.client() == 0, 60,
                        "the values back at their defaults on the player's game");
    }

    @ClientTest(timeoutTicks = 400)
    public static void teamState_followsThePlayerOntoAScoreboardTeam(final ClientTestContext ctx) {
        ctx.thenServer(0, level -> {
                    final MinecraftServer server = level.getServer();
                    TestStates.FUNDS.set(server, CoreTeams.solo(ctx.serverPlayer().getUUID()), 5);
                    TestStates.FUNDS.set(server, ScoreboardTeams.idOf(crew(server)), 900);
                })
                .thenWaitUntil(() -> TestStates.FUNDS.client() == 5, 60, "the funds of the player's team of their own")
                .thenServer(0, level -> level.getServer().getScoreboard()
                        .addPlayerToTeam(ctx.serverPlayer().getScoreboardName(), crew(level.getServer())))
                .thenWaitUntil(() -> TestStates.FUNDS.client() == 900, 60,
                        "the crew's funds, once the player is on the crew")
                .thenServer(0, level -> {
                    final MinecraftServer server = level.getServer();
                    final Scoreboard board = server.getScoreboard();
                    TestStates.FUNDS.set(server, ScoreboardTeams.idOf(crew(server)), 0);
                    TestStates.FUNDS.set(server, CoreTeams.solo(ctx.serverPlayer().getUUID()), 0);
                    board.removePlayerTeam(crew(server));
                });
    }

    private static PlayerTeam crew(final MinecraftServer server) {
        final Scoreboard board = server.getScoreboard();
        final PlayerTeam team = board.getPlayerTeam(CREW);
        return team != null ? team : board.addPlayerTeam(CREW);
    }
}
