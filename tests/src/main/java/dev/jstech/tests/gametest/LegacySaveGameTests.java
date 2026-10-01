/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.advancement.PendingAwards;
import dev.jstech.core.persistence.NetworkRegistry;
import dev.jstech.core.persistence.NetworkRegistryState;
import dev.jstech.core.state.StateSave;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.core.uuid.NetworkUuidState;
import dev.jstech.tests.JsTests;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The files the series saved before its saves had versions, as a world of then holds them, read into the states
 * that keep the same things now: each dimension's networks, in both of the shapes they were saved in, and the awards
 * players earned while away.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class LegacySaveGameTests {

    private static final String ARENA = "empty";

    private LegacySaveGameTests() {
    }

    @GameTest(template = ARENA)
    public static void networkRegistry_readsItsFileFromBeforeVersions(final GameTestHelper helper) {
        final NetworkUuid conflicted = new NetworkUuid(UUID.randomUUID());
        final CompoundTag entry = new CompoundTag();
        entry.putString("uuid", conflicted.value().toString());
        entry.putByte("state", (byte) NetworkUuidState.CONFLICTED.id());
        final ListTag networks = new ListTag();
        networks.add(entry);
        final CompoundTag old = new CompoundTag();
        old.put("networks", networks);

        final NetworkRegistryState read = readOld(helper.getLevel(), old, NetworkRegistry.NETWORKS.factory(),
                NetworkRegistry.NETWORKS.fileName()).value();

        same(helper, NetworkUuidState.CONFLICTED, read.stateOf(conflicted), "the network, still in conflict");
        same(helper, 1, read.size(), "the one network the file held");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void networkRegistry_readsItsOldestFileOfIdsAlone(final GameTestHelper helper) {
        final NetworkUuid first = new NetworkUuid(UUID.randomUUID());
        final NetworkUuid second = new NetworkUuid(UUID.randomUUID());
        final ListTag ids = new ListTag();
        ids.add(StringTag.valueOf(first.value().toString()));
        ids.add(StringTag.valueOf(second.value().toString()));
        final CompoundTag old = new CompoundTag();
        old.put("networks", ids);

        final NetworkRegistryState read = readOld(helper.getLevel(), old, NetworkRegistry.NETWORKS.factory(),
                NetworkRegistry.NETWORKS.fileName()).value();

        same(helper, NetworkUuidState.ACTIVE, read.stateOf(first), "the first network, running");
        same(helper, NetworkUuidState.ACTIVE, read.stateOf(second), "the second network, running");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void pendingAwards_readsItsFileFromBeforeVersions(final GameTestHelper helper) {
        final UUID away = UUID.randomUUID();
        final CompoundTag award = new CompoundTag();
        award.putString("Event", "select_done");
        award.putString("Detail", "");
        final ListTag awards = new ListTag();
        awards.add(award);
        final CompoundTag player = new CompoundTag();
        player.putUUID("Id", away);
        player.put("Awards", awards);
        final ListTag players = new ListTag();
        players.add(player);
        final CompoundTag old = new CompoundTag();
        old.put("Players", players);

        final Map<UUID, List<PendingAwards.Award>> read = readOld(helper.getLevel(), old,
                PendingAwards.WAITING.factory(), PendingAwards.WAITING.fileName()).value();

        same(helper, List.of(new PendingAwards.Award("select_done", "")), read.get(away),
                "what the player earned while away, still waiting");
        helper.succeed();
    }

    /* Writes {@code old} as the saved data file {@code name} of a world of before, and opens it as a world does. */
    private static <V> StateSave<V> readOld(final ServerLevel level, final CompoundTag old,
                                            final SavedData.Factory<StateSave<V>> factory, final String name) {
        final Path folder;
        try {
            folder = Files.createTempDirectory("jstests-legacy");
            final CompoundTag root = new CompoundTag();
            root.put("data", old);
            NbtUtils.addCurrentDataVersion(root);
            NbtIo.writeCompressed(root, folder.resolve(name + ".dat"));
        } catch (final IOException cannotWrite) {
            throw new UncheckedIOException("the old file " + name + " could not be written", cannotWrite);
        }
        final DimensionDataStorage storage = new DimensionDataStorage(folder.toFile(),
                level.getServer().getFixerUpper(), level.registryAccess());
        return Objects.requireNonNull(storage.get(factory, name), "the file " + name);
    }

    private static void same(final GameTestHelper helper, final Object expected, final Object actual,
                             final String what) {
        helper.assertTrue(Objects.equals(expected, actual), what + ": expected " + expected + ", got " + actual);
    }
}
