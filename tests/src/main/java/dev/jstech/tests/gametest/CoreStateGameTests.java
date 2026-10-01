/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import com.mojang.serialization.Codec;
import dev.jstech.core.persistence.SaveFiles;
import dev.jstech.core.persistence.SaveLayout;
import dev.jstech.core.state.CoreState;
import dev.jstech.core.state.CoreStates;
import dev.jstech.core.state.ServerState;
import dev.jstech.core.state.StateSave;
import dev.jstech.core.team.CoreTeams;
import dev.jstech.core.team.ITeamSource;
import dev.jstech.core.team.ScoreboardTeams;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestStates;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.neoforged.neoforge.common.IOUtilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A state of each scope saved with the world and read back from its file as a restart reads it; a team's value found
 * through whichever source says who is on which team; a file a newer mod saved read as it is and copied aside once;
 * and a state used before it is registered, or registered twice, refused.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class CoreStateGameTests {

    private static final String ARENA = "empty";

    private CoreStateGameTests() {
    }

    @GameTest(template = ARENA)
    public static void serverState_isSavedAndReadBack(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final int before = TestStates.COUNTER.get(server);
        TestStates.COUNTER.set(server, 4242);

        final StateSave<Integer> read = reread(server.overworld(), TestStates.COUNTER.factory(),
                TestStates.COUNTER.fileName());

        same(helper, 4242, read.value(), "the counter as its file has it");
        TestStates.COUNTER.set(server, before);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void dimensionState_keepsAValueForEachDimension(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final ServerLevel overworld = server.overworld();
        final ServerLevel nether = Objects.requireNonNull(server.getLevel(Level.NETHER), "the nether");
        TestStates.WEATHER.set(overworld, "rain");
        TestStates.WEATHER.set(nether, "ash");

        same(helper, "rain", TestStates.WEATHER.get(overworld), "the overworld's weather");
        same(helper, "ash", TestStates.WEATHER.get(nether), "the nether's weather");
        same(helper, "ash", reread(nether, TestStates.WEATHER.factory(), TestStates.WEATHER.fileName())
                .value(), "the nether's weather as its own file has it");
        same(helper, "rain", reread(overworld, TestStates.WEATHER.factory(), TestStates.WEATHER.fileName())
                .value(), "the overworld's weather as its own file has it");
        TestStates.WEATHER.set(overworld, "");
        TestStates.WEATHER.set(nether, "");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void playerState_keepsEachPlayersValueWhetherOnlineOrNot(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final UUID first = UUID.randomUUID();
        final UUID second = UUID.randomUUID();
        TestStates.SCORE.set(server, first, 5);
        TestStates.SCORE.set(server, second, 7);

        final Map<UUID, Integer> saved = reread(server.overworld(), TestStates.SCORE.factory(),
                TestStates.SCORE.fileName()).value();
        same(helper, 5, saved.get(first), "the first player's score as the file has it");
        same(helper, 7, saved.get(second), "the second player's score as the file has it");
        same(helper, 0, TestStates.SCORE.get(server, UUID.randomUUID()), "the score of a player never seen");

        TestStates.SCORE.set(server, first, 0);
        helper.assertTrue(!TestStates.SCORE.all(server).containsKey(first), "a score back at the default is dropped");
        TestStates.SCORE.set(server, second, 0);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void teamState_followsTheSourceOfTeams(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final UUID first = UUID.randomUUID();
        final UUID second = UUID.randomUUID();
        final UUID alone = UUID.randomUUID();
        final Set<UUID> crew = Set.of(first, second);
        final ITeamSource source = (on, player) -> crew.contains(player) ? "test:crew" : CoreTeams.solo(player);
        CoreTeams.use(source);
        try {
            TestStates.FUNDS.set(server, CoreTeams.teamOf(server, first), 300);

            same(helper, 300, TestStates.FUNDS.ofPlayer(server, second), "the funds of the second on the crew");
            same(helper, 0, TestStates.FUNDS.ofPlayer(server, alone), "the funds of a player on their own");
            same(helper, CoreTeams.solo(alone), CoreTeams.teamOf(server, alone), "the team of a player alone");
            TestStates.FUNDS.set(server, "test:crew", 0);
        } finally {
            CoreTeams.use(ScoreboardTeams.INSTANCE);
        }
        same(helper, CoreTeams.solo(first), CoreTeams.teamOf(server, first),
                "a player on no scoreboard team, once the game's teams answer again");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void state_keepsACopyOfAFileANewerModSaved(final GameTestHelper helper) {
        final ServerLevel overworld = helper.getLevel().getServer().overworld();
        final ServerState<Integer> state = TestStates.NEWER;
        final Path file = SaveFiles.file(overworld, state.fileName());
        final Path copy = SaveFiles.newerCopy(overworld, state.fileName(), 99);
        try {
            Files.deleteIfExists(copy);
            final CompoundTag data = new CompoundTag();
            data.putInt(SaveLayout.VERSION_KEY, 99);
            data.putInt(SaveLayout.VALUE_KEY, 7);
            final CompoundTag root = new CompoundTag();
            root.put("data", data);
            NbtUtils.addCurrentDataVersion(root);
            Files.createDirectories(file.getParent());
            NbtIo.writeCompressed(root, file);
        } catch (final IOException cannotWrite) {
            helper.fail("the newer file could not be written: " + cannotWrite.getMessage());
            return;
        }

        same(helper, 7, state.get(overworld.getServer()), "the value of the newer file, read as it is");
        helper.assertTrue(Files.isRegularFile(copy), "the newer file, copied aside as " + copy.getFileName());
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void state_isRefusedBeforeItIsRegistered(final GameTestHelper helper) {
        final ServerState<Integer> unregistered = CoreState.builder(id("unregistered"), Codec.INT, 0).server();
        try {
            unregistered.get(helper.getLevel().getServer());
        } catch (final IllegalStateException expected) {
            helper.succeed();
            return;
        }
        helper.fail("a state used before it was registered was not refused");
    }

    @GameTest(template = ARENA)
    public static void states_refuseTwoUnderOneIdOrAFileNameWithAFolder(final GameTestHelper helper) {
        try {
            CoreStates.register(CoreState.builder(TestStates.COUNTER.id(), Codec.INT, 0).server());
            helper.fail("a second state under the counter's id was not refused");
            return;
        } catch (final IllegalStateException expected) {
            // The counter keeps its id.
        }
        try {
            CoreState.builder(id("folder"), Codec.INT, 0).fileName("data/folder");
            helper.fail("a file name with a folder was not refused");
            return;
        } catch (final IllegalArgumentException expected) {
            // The name stays one word.
        }
        same(helper, TestStates.COUNTER, CoreStates.byId(TestStates.COUNTER.id()), "the counter, still registered");
        helper.succeed();
    }

    /*
     * Writes the dimension's saved data to disk and reads the state's file back as a world being opened does. The
     * files are written off the server's thread, so the writing is waited for before the file is read.
     */
    private static <V> StateSave<V> reread(final ServerLevel level, final SavedData.Factory<StateSave<V>> factory,
                                           final String name) {
        level.getDataStorage().save();
        IOUtilities.waitUntilIOWorkerComplete();
        final DimensionDataStorage fresh = new DimensionDataStorage(SaveFiles.dataFolder(level).toFile(),
                level.getServer().getFixerUpper(), level.registryAccess());
        final StateSave<V> read = fresh.get(factory, name);
        if (read == null) {
            throw new IllegalStateException("no file " + name + " in " + SaveFiles.dataFolder(level));
        }
        return read;
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsTests.MODID, path);
    }

    private static void same(final GameTestHelper helper, final Object expected, final Object actual,
                             final String what) {
        helper.assertTrue(Objects.equals(expected, actual), what + ": expected " + expected + ", got " + actual);
    }
}
