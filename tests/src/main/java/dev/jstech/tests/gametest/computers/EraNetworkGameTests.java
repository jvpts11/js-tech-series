/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.operation.NetworkStorage;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.computers.EraKit;
import dev.jstech.tests.testkit.computers.EraNetwork;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.Objects;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The network every era's journey starts from, laid in the era's own machines and cables: its Mainframe runs the
 * network, the rack's server puts its drives on it and the Personal Computer joins it, in every era.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class EraNetworkGameTests {

    private static final String BENCH = "bench";
    /** How long the machines may take to start and find one another. */
    private static final int WITHIN = 400;

    private EraNetworkGameTests() {
    }

    @GameTest(template = BENCH, timeoutTicks = WITHIN + 20)
    public static void vintage_networkRunsOnTheErasOwnMachines(final GameTestHelper helper) {
        runs(helper, EraKit.VINTAGE);
    }

    @GameTest(template = BENCH, timeoutTicks = WITHIN + 20)
    public static void legacy_networkRunsOnTheErasOwnMachines(final GameTestHelper helper) {
        runs(helper, EraKit.LEGACY);
    }

    @GameTest(template = BENCH, timeoutTicks = WITHIN + 20)
    public static void transition_networkRunsOnTheErasOwnMachines(final GameTestHelper helper) {
        runs(helper, EraKit.TRANSITION);
    }

    @GameTest(template = BENCH, timeoutTicks = WITHIN + 20)
    public static void standard_networkRunsOnTheErasOwnMachines(final GameTestHelper helper) {
        runs(helper, EraKit.STANDARD);
    }

    @GameTest(template = BENCH, timeoutTicks = WITHIN + 20)
    public static void advanced_networkRunsOnTheErasOwnMachines(final GameTestHelper helper) {
        runs(helper, EraKit.ADVANCED);
    }

    /* Lays the era's network and waits for the Mainframe, the rack's storage and the computer to be one network. */
    private static void runs(final GameTestHelper helper, final EraKit kit) {
        final EraNetwork network = EraNetwork.lay(TestWorldBuilder.forGameTest(helper), kit);
        helper.startSequence()
                .thenWaitUntil(() -> {
                    helper.assertTrue(network.mainframe().isRunning() && network.mainframe().hasOs(),
                            kit + ": the Mainframe runs the network system");
                    final NetworkUuid uuid = network.mainframe().networkUuid();
                    helper.assertTrue(uuid != null, kit + ": the Mainframe has a network");
                    helper.assertTrue(NetworkStorage.of(helper.getLevel(), uuid).capacity() > 0,
                            kit + ": the rack's drives are the network's storage");
                    helper.assertTrue(network.computer().isRunning()
                                    && Objects.equals(network.computer().networkUuid(), uuid),
                            kit + ": the computer runs on the network; it is on "
                                    + network.computer().networkUuid());
                })
                .thenExecute(() -> {
                    final NetworkStorage storage = NetworkStorage.of(helper.getLevel(),
                            network.mainframe().networkUuid());
                    helper.assertTrue(storage.insert(StorageKey.of(Items.COBBLESTONE), 64L) == 64L,
                            kit + ": the network keeps items");
                })
                .thenSucceed();
    }
}
