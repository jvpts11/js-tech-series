/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.HardwareItems;
import dev.jstech.computers.advancement.MachineOperators;
import dev.jstech.computers.blockentity.ChunkAnchor;
import dev.jstech.computers.blockentity.ClusterManagementComputerBlockEntity;
import dev.jstech.computers.eventlog.NetworkEvent;
import dev.jstech.computers.eventlog.NetworkEventLog;
import dev.jstech.core.region.ChunkLoaders;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestCables;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * A Network Anchor Card keeps the chunks round its computer loaded while the computer runs, under its operator's name
 * and within their quota, and lets them go when the computer stops or is broken (computers/docs/design/network.md).
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class NetworkAnchorGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos MAINFRAME = new BlockPos(1, 2, 2);
    private static final BlockPos CABLE = new BlockPos(2, 2, 2);
    private static final BlockPos COMPUTER = new BlockPos(3, 2, 2);
    /* A second place the same owner keeps chunks from, to fill their quota. */
    private static final BlockPos ELSEWHERE = new BlockPos(5, 2, 5);

    private NetworkAnchorGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void anchorCard_keepsTheSquareWhileTheComputerRuns(final GameTestHelper helper) {
        final ClusterManagementComputerBlockEntity computer = anchored(helper, UUID.randomUUID(), false);
        final ServerLevel level = helper.getLevel();
        final BlockPos at = helper.absolutePos(COMPUTER);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(ChunkLoaders.loadedFrom(level, at).size() == 9,
                        "the Standard card keeps the 3 by 3 round its computer; it keeps "
                                + ChunkLoaders.loadedFrom(level, at).size()))
                .thenExecute(() -> helper.assertTrue(ChunkLoaders.loadedFrom(level, at)
                        .contains(new ChunkPos(at)), "the computer's own chunk is among them"))
                .thenExecute(computer::togglePower)
                .thenWaitUntil(() -> helper.assertTrue(ChunkLoaders.loadedFrom(level, at).isEmpty(),
                        "the computer switched off lets them go"))
                .thenExecute(() -> ChunkLoaders.releaseAll(level, at))
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void anchorCard_brokenComputerLetsGo(final GameTestHelper helper) {
        anchored(helper, UUID.randomUUID(), false);
        final ServerLevel level = helper.getLevel();
        final BlockPos at = helper.absolutePos(COMPUTER);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertFalse(ChunkLoaders.loadedFrom(level, at).isEmpty(),
                        "the running computer keeps its chunks"))
                .thenExecute(() -> helper.setBlock(COMPUTER, Blocks.AIR))
                .thenExecute(() -> helper.assertTrue(ChunkLoaders.loadedFrom(level, at).isEmpty(),
                        "the computer broken lets them go at once"))
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void anchorCard_withNobodyWorkingItKeepsNothing(final GameTestHelper helper) {
        anchored(helper, null, false);
        final ServerLevel level = helper.getLevel();
        final BlockPos at = helper.absolutePos(COMPUTER);
        helper.startSequence()
                .thenIdle(45)
                .thenExecute(() -> helper.assertTrue(ChunkLoaders.loadedFrom(level, at).isEmpty(),
                        "a computer nobody works charges no quota, so it keeps nothing"))
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void anchorCard_fullQuotaKeepsWhatFitsAndTellsTheLog(final GameTestHelper helper) {
        final UUID owner = UUID.randomUUID();
        final ServerLevel level = helper.getLevel();
        final BlockPos at = helper.absolutePos(COMPUTER);
        final BlockPos elsewhere = helper.absolutePos(ELSEWHERE);
        // The owner keeps all but one chunk of their quota from somewhere else already.
        final ChunkPos centre = new ChunkPos(at);
        int filled = 0;
        for (int dx = -2; dx <= 2 && filled < 24; dx++) {
            for (int dz = -2; dz <= 2 && filled < 24; dz++) {
                ChunkLoaders.load(level, elsewhere, owner, ChunkAnchor.REASON,
                        new ChunkPos(centre.x + dx, centre.z + dz), false);
                filled++;
            }
        }
        final ClusterManagementComputerBlockEntity computer = anchored(helper, owner, true);
        final NetworkUuid[] network = new NetworkUuid[1];
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(computer.networkUuid() != null, "the computer joins"))
                .thenExecute(() -> network[0] = computer.networkUuid())
                .thenWaitUntil(() -> helper.assertTrue(ChunkLoaders.loadedFrom(level, at).size() == 1,
                        "the card keeps only what the quota leaves, its own chunk first; it keeps "
                                + ChunkLoaders.loadedFrom(level, at).size()))
                .thenExecute(() -> helper.assertTrue(ChunkLoaders.loadedFrom(level, at).contains(centre),
                        "the chunk it keeps is the nearest, its own"))
                .thenWaitUntil(() -> helper.assertTrue(logged(level, network[0], "keeps 1 of its 9 chunks") == 1,
                        "the network's log says so, once"))
                .thenIdle(45)
                .thenExecute(() -> helper.assertTrue(logged(level, network[0], "keeps 1 of its 9 chunks") == 1,
                        "and says it only once while the quota stays full"))
                .thenExecute(() -> {
                    ChunkLoaders.releaseAll(level, elsewhere);
                    ChunkLoaders.releaseAll(level, at);
                })
                .thenSucceed();
    }

    /*
     * A Cluster Management Computer with a Standard Network Anchor Card, worked by {@code owner} (nobody when null),
     * switched on; on the network of a running Mainframe when {@code networked}.
     */
    private static ClusterManagementComputerBlockEntity anchored(final GameTestHelper helper, final UUID owner,
                                                                 final boolean networked) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        if (networked) {
            world.placeRunningMainframe(MAINFRAME);
            TestCables.lay(helper, CABLE, ComputingModule.HBW_CABLE);
        }
        helper.setBlock(COMPUTER, ComputingModule.CLUSTER_MANAGEMENT_COMPUTER.get());
        world.faceRearTowardCable(COMPUTER);
        if (!(helper.getBlockEntity(COMPUTER) instanceof ClusterManagementComputerBlockEntity computer)) {
            throw new IllegalStateException("no Cluster Management Computer at " + COMPUTER);
        }
        final ItemStackHandler hardware = computer.getHardware();
        hardware.setStackInSlot(ClusterManagementComputerBlockEntity.MOTHERBOARD_SLOT,
                new ItemStack(HardwareItems.MOTHERBOARD_ATX_STANDARD_LGA1150.get()));
        hardware.setStackInSlot(ClusterManagementComputerBlockEntity.CPU_SLOT,
                new ItemStack(HardwareItems.CPU_INTEGRA_CENTRO_C7_4790K.get()));
        hardware.setStackInSlot(ClusterManagementComputerBlockEntity.RAM_SLOTS_START,
                new ItemStack(ComputingModule.RAM_DDR3_8192.get()));
        hardware.setStackInSlot(ClusterManagementComputerBlockEntity.PSU_SLOT,
                new ItemStack(ComputingModule.PSU_650G.get()));
        hardware.setStackInSlot(ClusterManagementComputerBlockEntity.PCIE_SLOTS_START,
                new ItemStack(ComputingModule.NETWORK_ANCHOR_CARD_STANDARD.get()));
        if (owner != null) {
            MachineOperators.note(computer, owner);
        }
        computer.togglePower();
        return computer;
    }

    /* How many lines of the network's log say {@code words}. */
    private static long logged(final ServerLevel level, final NetworkUuid network, final String words) {
        if (network == null) {
            return 0L;
        }
        final List<NetworkEvent> events = NetworkEventLog.of(level).newest(network, level.getGameTime(), any -> true,
                NetworkEventLog.MOST);
        return events.stream().filter(event -> event.message().english().contains(words)).count();
    }
}
