/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.storage.ServerStore;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.connect.Connection;
import dev.jstech.core.network.NetworkSystem;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestCables;
import dev.jstech.tests.testkit.TestWorldBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static dev.jstech.tests.testkit.NetworkFixtures.placeRunningMainframe;
import static dev.jstech.tests.testkit.NetworkFixtures.seededRack;
import static dev.jstech.tests.testkit.NetworkFixtures.storageNetwork;

/**
 * GameTests for the network index: cataloguing servers and locks, incremental analysis, vacuuming ghost entries,
 * dropping a type or everything, and the index statistics.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class NetworkIndexGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;

    private NetworkIndexGameTests() {
    }

    @GameTest(template = ARENA)
    public static void networkIndex_catalogsServersAndLocks(final GameTestHelper helper) {
        final BlockPos m = new BlockPos(1, 2, 2);
        final BlockPos rack = new BlockPos(2, 2, 3); // behind the cable (rear-only connection)
        final MainframeBlockEntity mainframe = placeRunningMainframe(helper, m);
        TestCables.lay(helper, new BlockPos(2, 2, 2), ComputingModule.HBW_CABLE);
        helper.setBlock(rack, ComputingModule.SERVER_RACK.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING,
                        Direction.SOUTH)); // rear faces the cable to the north
        if (!(helper.getBlockEntity(rack) instanceof ServerRackBlockEntity rackBe)) {
            helper.fail("no server rack");
            return;
        }
        TestWorldBuilder.mountDefaultServer(rackBe, 0);

        helper.startSequence()
                .thenExecuteAfter(SETTLE + 6, () -> rackBe.getServerStorage(0).insert(Items.COBBLESTONE, 40))
                .thenExecuteAfter(4, () -> {
                    final long stored = rackBe.getServerStorage(0).count(Items.COBBLESTONE);
                    helper.assertTrue(stored > 0L, "the server should hold cobblestone; got " + stored);
                    final var index = mainframe.networkIndex();
                    helper.assertTrue(index.available(Items.COBBLESTONE) == stored,
                            "index must catalog the stored amount; got " + index.available(Items.COBBLESTONE)
                                    + " vs " + stored);

                    final java.util.UUID op = new java.util.UUID(0L, 7L);
                    final long want = stored / 2L;
                    final var plan = index.lock(op, Items.COBBLESTONE, want);
                    helper.assertTrue(plan.allocated() == want, "lock should reserve " + want);
                    helper.assertTrue(index.available(Items.COBBLESTONE) == stored - want,
                            "locked items must drop availability");

                    index.unlock(op);
                    helper.assertTrue(index.available(Items.COBBLESTONE) == stored,
                            "unlock must restore availability");
                    helper.assertTrue(!index.isLocked(op), "no lock should remain after unlock");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void analyzeIncremental_tracksDirectStoreWrites(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = storageNetwork(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 4, () ->
                        seededRack(helper).getServerStorage(0).insert(Items.COBBLESTONE, 30))
                .thenExecuteAfter(2, () -> {
                    helper.assertTrue(mainframe.networkIndex().available(Items.COBBLESTONE) == 30,
                            "the catalog sees the direct insert; got "
                                    + mainframe.networkIndex().available(Items.COBBLESTONE));
                    seededRack(helper).getServerStorage(0).extract(Items.COBBLESTONE, 10);
                })
                .thenExecuteAfter(2, () -> helper.assertTrue(
                        mainframe.networkIndex().available(Items.COBBLESTONE) == 20,
                        "the catalog sees the direct extract; got "
                                + mainframe.networkIndex().available(Items.COBBLESTONE)))
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void vacuum_freesGhostEntries(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = storageNetwork(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 4, () ->
                        seededRack(helper).getServerStorage(0).insert(Items.COBBLESTONE, 50))
                .thenExecuteAfter(2, () -> {
                    final NetworkUuid net = mainframe.networkUuid();
                    helper.assertTrue(mainframe.networkIndex().available(Items.COBBLESTONE) == 50,
                            "catalog populated before the ghost");
                    final var system = NetworkSystem.get(helper.getLevel());
                    final var node = system.serversOf(net).get(0).nodeUuid();
                    // Unregister the server: its catalog rows are now ghosts (same tick, no re-scan yet).
                    system.unregisterServer(net, node);
                    final int freed = mainframe.networkIndex().vacuum(helper.getLevel(), net);
                    helper.assertTrue(freed >= 1, "vacuum frees the ghost rows; freed " + freed);
                    helper.assertTrue(mainframe.networkIndex().available(Items.COBBLESTONE) == 0,
                            "the ghost no longer answers queries");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void drop_typeDestroysOnlyTargetType(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = storageNetwork(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 4, () -> {
                    final ServerStore store = seededRack(helper).getServerStorage(0);
                    store.insert(Items.COBBLESTONE, 50);
                    store.insert(Items.DIRT, 30);
                })
                .thenExecuteAfter(2, () -> {
                    final long destroyed = mainframe.networkIndex().dropType(helper.getLevel(),
                            mainframe.networkUuid(), StorageKey.of(Items.COBBLESTONE), null);
                    helper.assertTrue(destroyed == 50, "dropType destroys all 50 cobblestone; got " + destroyed);
                    final ServerStore store = seededRack(helper).getServerStorage(0);
                    helper.assertTrue(store.count(Items.COBBLESTONE) == 0, "the dropped type is gone");
                    helper.assertTrue(store.count(Items.DIRT) == 30, "every other type is untouched");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void drop_allWipesNetworkAndIndexReflectsIt(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = storageNetwork(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 4, () -> {
                    final ServerStore store = seededRack(helper).getServerStorage(0);
                    store.insert(Items.COBBLESTONE, 50);
                    store.insert(Items.DIRT, 30);
                })
                .thenExecuteAfter(2, () -> {
                    helper.assertTrue(mainframe.networkIndex().catalogSize() == 2,
                            "two types catalogued before the wipe; got " + mainframe.networkIndex().catalogSize());
                    final long destroyed = mainframe.networkIndex().dropAll(helper.getLevel(), mainframe.networkUuid());
                    helper.assertTrue(destroyed == 80, "dropAll destroys all 80 units; got " + destroyed);
                    helper.assertTrue(seededRack(helper).getServerStorage(0).used() == 0, "the server is emptied");
                })
                // The per-tick ANALYZE drops the now-empty rows from the catalog.
                .thenExecuteAfter(2, () -> helper.assertTrue(mainframe.networkIndex().catalogSize() == 0,
                        "the index reflects the wiped network; got " + mainframe.networkIndex().catalogSize()))
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void maintenance_indexStatsReflectNetwork(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = storageNetwork(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 4, () -> {
                    final ServerStore store = seededRack(helper).getServerStorage(0);
                    store.insert(Items.COBBLESTONE, 50);
                    store.insert(Items.DIRT, 30);
                })
                .thenExecuteAfter(2, () -> {
                    final var index = mainframe.networkIndex();
                    helper.assertTrue(index.catalogSize() == 2, "2 types; got " + index.catalogSize());
                    helper.assertTrue(index.indexedServerCount() == 1, "1 server; got " + index.indexedServerCount());
                    helper.assertTrue(index.activeLockCount() == 0, "no locks idle; got " + index.activeLockCount());
                    helper.assertTrue(mainframe.indexedTypes() == 2 && mainframe.indexedServers() == 1,
                            "the host exposes the same stats to the terminal");
                })
                .thenSucceed();
    }
}
