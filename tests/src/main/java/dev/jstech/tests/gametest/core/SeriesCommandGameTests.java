/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.jstech.core.jstechseries.command.SeriesCommands;
import dev.jstech.core.map.MapMarkers;
import dev.jstech.core.progression.PlayerProgress;
import dev.jstech.core.region.ChunkLoaders;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.jstechseries.tier.SeriesAxes;
import dev.jstech.testkit.gametest.GameTestPlayers;
import dev.jstech.tests.JsTests;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The series' commands under {@code /jstech}, run as a player of the test's own would run them: where they stand
 * along an axis and putting them further, the chunks their machines keep loaded and letting go of them, the server's
 * count of them and every place keeping them on an operator's map, and a player
 * without the right level never reaching them.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class SeriesCommandGameTests {

    private static final String ARENA = "empty";
    private static final TextKey REASON = TextKey.of("jstests.command.chunks.reason", "A test keeps it loaded");

    private SeriesCommandGameTests() {
    }

    @GameTest(template = ARENA)
    public static void commands_areDeclaredUnderTheSeriesRoot(final GameTestHelper helper) {
        helper.assertTrue(SeriesCommands.names().containsAll(List.of("progress", "chunks", "media")),
                "the Core's commands are declared; got " + SeriesCommands.names());
        helper.assertTrue(helper.getLevel().getServer().getCommands().getDispatcher().getRoot()
                .getChild(SeriesCommands.ROOT) != null, "and handed to the game under /" + SeriesCommands.ROOT);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void progress_putsAPlayerAtTheStepNamed(final GameTestHelper helper) throws CommandSyntaxException {
        final ServerPlayer player = GameTestPlayers.join(helper, "commanded");
        try {
            final int moved = run(helper, player, SeriesCommands.GAME_MASTERS,
                    "jstech progress @s jscore:hardware_era transition");
            helper.assertTrue(moved == 1, "one player was moved; the command said " + moved);
            helper.assertTrue(PlayerProgress.reached(player.server, player.getUUID(), SeriesAxes.HARDWARE_ERA)
                    == HardwareEra.TRANSITION, "the player stands at Transition");
            helper.assertTrue(run(helper, player, SeriesCommands.GAME_MASTERS,
                    "jstech progress @s jscore:hardware_era") == 1, "and the command tells where they stand");
            helper.assertTrue(fails(helper, player, SeriesCommands.GAME_MASTERS,
                    "jstech progress @s jscore:hardware_era steam_age"), "a step the axis has not is refused");
            helper.assertTrue(fails(helper, player, SeriesCommands.GAME_MASTERS,
                    "jstech progress @s jstests:no_axis first"), "and an axis nobody registered");
            helper.assertTrue(fails(helper, player, SeriesCommands.EVERYONE,
                    "jstech progress @s jscore:hardware_era standard"), "a player who is no operator never reaches it");
        } finally {
            GameTestPlayers.leave(player);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void chunks_tellsAndReleasesWhatAPlayerKeepsLoaded(final GameTestHelper helper)
            throws CommandSyntaxException {
        final ServerPlayer player = GameTestPlayers.join(helper, "loader");
        final BlockPos source = helper.absolutePos(new BlockPos(1, 2, 1));
        try {
            ChunkLoaders.load(helper.getLevel(), source, player.getUUID(), REASON, new ChunkPos(source), false);
            helper.assertTrue(run(helper, player, SeriesCommands.GAME_MASTERS, "jstech chunks @s") == 1,
                    "the command counts the one chunk the player keeps loaded");
            helper.assertTrue(run(helper, player, SeriesCommands.GAME_MASTERS, "jstech chunks @s release") == 1,
                    "and lets go of it");
            helper.assertTrue(ChunkLoaders.loadedBy(player.server, player.getUUID()) == 0, "nothing stays loaded");
        } finally {
            ChunkLoaders.releaseAll(helper.getLevel(), source);
            GameTestPlayers.leave(player);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void chunks_countsTheServerAndMapsEveryPlaceKeepingChunks(final GameTestHelper helper)
            throws CommandSyntaxException {
        final ServerPlayer player = GameTestPlayers.join(helper, "surveyor");
        final BlockPos source = helper.absolutePos(new BlockPos(2, 2, 2));
        final ChunkPos chunk = new ChunkPos(source);
        try {
            ChunkLoaders.load(helper.getLevel(), source, player.getUUID(), REASON, chunk, false);
            helper.assertTrue(run(helper, player, SeriesCommands.GAME_MASTERS, "jstech chunks") >= 1,
                    "the server's count holds the chunk kept");
            helper.assertTrue(run(helper, player, SeriesCommands.GAME_MASTERS, "jstech chunks map") >= 1,
                    "the map shows at least the one place keeping chunks");
            helper.assertTrue(markersOver(helper, player, chunk) == 2, "the operator sees it twice: as its owner, and"
                    + " among every place");
            helper.assertTrue(MapMarkers.of(helper.getLevel().getServer(), player.getUUID()).stream()
                    .filter(marker -> marker.area().contains(chunk))
                    .allMatch(marker -> marker.label().getString().contains(REASON.english())),
                    "and both say why the chunk is kept");
            helper.assertTrue(run(helper, player, SeriesCommands.GAME_MASTERS, "jstech chunks map hide") >= 1,
                    "hiding takes every place off");
            helper.assertTrue(markersOver(helper, player, chunk) == 1, "and leaves the owner's own marker");
            ChunkLoaders.releaseAll(helper.getLevel(), source);
            helper.assertTrue(markersOver(helper, player, chunk) == 0, "which goes once the chunk is let go");
        } finally {
            ChunkLoaders.releaseAll(helper.getLevel(), source);
            GameTestPlayers.leave(player);
        }
        helper.succeed();
    }

    /* How many of the markers {@code player} sees cover {@code chunk}. */
    private static long markersOver(final GameTestHelper helper, final ServerPlayer player, final ChunkPos chunk) {
        return MapMarkers.of(helper.getLevel().getServer(), player.getUUID()).stream()
                .filter(marker -> marker.area().contains(chunk)).count();
    }

    /* Runs the command as {@code player} at that permission level, its answers kept off the log. */
    private static int run(final GameTestHelper helper, final ServerPlayer player, final int permission,
                           final String command) throws CommandSyntaxException {
        final CommandSourceStack source = helper.getLevel().getServer().createCommandSourceStack()
                .withEntity(player).withPermission(permission).withSuppressedOutput();
        return helper.getLevel().getServer().getCommands().getDispatcher().execute(command, source);
    }

    private static boolean fails(final GameTestHelper helper, final ServerPlayer player, final int permission,
                                 final String command) {
        try {
            run(helper, player, permission, command);
            return false;
        } catch (final CommandSyntaxException refused) {
            return true;
        }
    }
}
