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
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.eventlog.EventLevel;
import dev.jstech.computers.eventlog.NetworkEvent;
import dev.jstech.computers.eventlog.NetworkEventLog;
import dev.jstech.computers.hardware.PsuCondition;
import dev.jstech.computers.hardware.PsuConditions;
import dev.jstech.computers.hardware.PsuSpec;
import dev.jstech.computers.item.ServerItem;
import dev.jstech.computers.operation.payload.FirmwareStatePayload;
import dev.jstech.computers.operation.payload.firmware.FirmwarePayloads;
import dev.jstech.computers.os.boot.DiskChecks;
import dev.jstech.computers.os.crash.CrashCause;
import dev.jstech.computers.os.crash.CrashPage;
import dev.jstech.computers.os.devices.DeviceMap;
import dev.jstech.computers.os.devices.DeviceMaps;
import dev.jstech.computers.os.devices.DeviceRows;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.computers.TestComputerEnergy;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Power supplies wear while their machines run and in the end fail: the wear written onto the supply, a degraded one
 * warned of in the network's log, the Device Manager and the self-test, a failed one taking its machine dark until it
 * is replaced, and the redundant supply carrying on with one unit gone.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class PsuWearGameTests {

    private static final String ARENA = "empty";
    private static final int PATIENT = 400;
    /* A minute of running and a little more, for the wear a running machine works out once a minute. */
    private static final int OVER_A_MINUTE = 1_600;
    /* Just under failing, and just under degraded: a few ticks of running cross either. */
    private static final float ALMOST_FAILED = 0.9999999f;
    private static final float ALMOST_DEGRADED = 0.7999999f;
    private static final BlockPos PC = new BlockPos(2, 2, 2);

    private PsuWearGameTests() {
    }

    @BeforeBatch(batch = TestComputerEnergy.NO_WEAR_BATCH)
    public static void suppliesLastForEver(final ServerLevel level) {
        TestComputerEnergy.wearing(false);
    }

    @AfterBatch(batch = TestComputerEnergy.NO_WEAR_BATCH)
    public static void suppliesWearAgain(final ServerLevel level) {
        TestComputerEnergy.wearing(true);
    }

    /** A running computer's supply wears, and draws its own life the first time it does. */
    @GameTest(template = ARENA, timeoutTicks = PATIENT)
    public static void psu_wearsWhileItsMachineRuns(final GameTestHelper helper) {
        final CraftingComputerBlockEntity cc = TestWorldBuilder.forGameTest(helper).buildCraftingNetwork().cc();
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(cc.isRunning(), "the Crafting Computer runs"))
                .thenExecuteAfter(40, cc::togglePower)
                .thenExecuteAfter(2, () -> {
                    final PsuCondition condition = PsuConditions.of(cc.psuStack());
                    helper.assertTrue(condition.wearOf(0) > 0f, "the supply wore; got " + condition);
                    helper.assertTrue(condition.life() >= 0.8f && condition.life() <= 1.2f,
                            "and drew its own life; got " + condition.life());
                })
                .thenSucceed();
    }

    /** Crossing four fifths worn, the supply is degraded: the network's log warns, and the Device Manager marks it. */
    @GameTest(template = ARENA, timeoutTicks = PATIENT)
    public static void psu_degradedIsWarnedOfAndMarked(final GameTestHelper helper) {
        final CraftingComputerBlockEntity cc = TestWorldBuilder.forGameTest(helper).buildCraftingNetwork().cc();
        final NetworkUuid[] network = new NetworkUuid[1];
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(cc.isRunning() && cc.networkUuid() != null,
                        "the Crafting Computer runs on its network"))
                .thenExecute(() -> {
                    network[0] = cc.networkUuid();
                    PsuConditions.set(cc.psuStack(), new PsuCondition(1f, List.of(ALMOST_DEGRADED)));
                })
                .thenExecuteAfter(20, cc::togglePower)
                .thenExecuteAfter(2, () -> {
                    final PsuSpec spec = PsuConditions.spec(cc.psuStack());
                    helper.assertTrue(PsuConditions.of(cc.psuStack()).degraded(spec), "the supply is degraded");
                    helper.assertTrue(logged(helper, network[0], EventLevel.WARN, "Power supply degraded"),
                            "the network it ran on warns of it, though it stopped");
                    final DeviceMap map = DeviceMaps.of(helper.getLevel(), cc);
                    helper.assertTrue(map.power().size() == 1 && map.power().getFirst().attention()
                            && map.needingAttention() == 1, "the Device Manager marks it; got " + map.power());
                    helper.assertTrue(DeviceRows.rows(map, DeviceRows.View.BY_TYPE, Set.of()).stream()
                                    .anyMatch(row -> row.state() == DeviceRows.State.ATTENTION),
                            "under Power supplies, as a device that needs attention");
                    final FirmwareStatePayload.Psu posted = FirmwarePayloads.buildFirmwareState(helper.getLevel(), cc,
                            cc.getBlockPos()).psu();
                    helper.assertTrue(posted != null && posted.condition().degraded(posted.spec()),
                            "the self-test is told of it; got " + posted);
                })
                .thenSucceed();
    }

    /** A supply that fails takes its machine dark at once, and the machine does not start until it is replaced. */
    @GameTest(template = ARENA, timeoutTicks = OVER_A_MINUTE)
    public static void psu_failsAndItsMachineGoesDarkUntilReplaced(final GameTestHelper helper) {
        final CraftingComputerBlockEntity cc = TestWorldBuilder.forGameTest(helper).buildCraftingNetwork().cc();
        final NetworkUuid[] network = new NetworkUuid[1];
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(cc.isRunning() && cc.networkUuid() != null,
                        "the Crafting Computer runs on its network"))
                .thenExecute(() -> {
                    network[0] = cc.networkUuid();
                    PsuConditions.set(cc.psuStack(), new PsuCondition(1f, List.of(ALMOST_FAILED)));
                })
                .thenWaitUntil(() -> helper.assertFalse(cc.isRunning(), "within a minute the supply fails"))
                .thenExecute(() -> {
                    helper.assertTrue(PsuConditions.failed(cc.psuStack()), "the supply has failed");
                    helper.assertFalse(cc.buildValid(), "a failed supply makes no computer");
                    helper.assertTrue(logged(helper, network[0], EventLevel.ERROR, "Power supply failed"),
                            "the network it ran on says so");
                    cc.togglePower();
                    helper.assertFalse(cc.isRunning(), "its button brings nothing up");
                    cc.getHardware().setStackInSlot(CraftingComputerBlockEntity.PSU_SLOT,
                            new ItemStack(ComputingModule.PSU_650G.get()));
                    helper.assertTrue(cc.buildValid(), "a new supply makes it a computer again");
                })
                .thenSucceed();
    }

    /**
     * A supply failing under a system that is up brings it down on its red page, which names the supply; the machine is
     * dark after it, a sudden shutdown, its disk left to be checked.
     */
    @GameTest(template = ARENA, timeoutTicks = OVER_A_MINUTE + 800)
    public static void psu_failingBringsTheSystemDownOnItsRedPage(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(PC);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(pc.isRunning() && !pc.needsPost() && !pc.booting()
                        && !pc.atBootMenu(), "the computer comes up"))
                .thenExecute(() -> PsuConditions.set(pc.psuStack(), new PsuCondition(1f, List.of(ALMOST_FAILED))))
                .thenWaitUntil(() -> helper.assertFalse(pc.isRunning(), "within a minute the supply fails"))
                .thenExecute(() -> {
                    final CrashPage page = pc.crashPage();
                    helper.assertTrue(page != null && page.cause() == CrashCause.POWER_SUPPLY && page.red()
                            && page.failedPart().equals(pc.psuStack().getHoverName().getString()),
                            "on the red page of a failing supply, naming it; got " + page);
                    helper.assertTrue(pc.crashEndsDark() && pc.stoppedSuddenly(), "which ends dark, suddenly");
                    helper.assertTrue(DiskChecks.unclean(pc.diskInSlot(0)), "its disk left to be checked");
                })
                .thenSucceed();
    }

    /** The redundant supply runs on with one unit failed, the other carrying the load, and fails with both. */
    @GameTest(template = ARENA, timeoutTicks = PATIENT)
    public static void psu_redundantCarriesOnWithOneUnitAndFailsWithBoth(final GameTestHelper helper) {
        final ServerRackBlockEntity rack = TestWorldBuilder.forGameTest(helper).buildCraftingNetwork().rack();
        final NetworkUuid[] network = new NetworkUuid[1];
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(rack.networkUuid() != null && rack.bayPowerOn(0),
                        "the rack's server runs on its network"))
                .thenExecute(() -> {
                    network[0] = rack.networkUuid();
                    mountRedundant(rack, new PsuCondition(1f, List.of(1f, 0.5f)));
                })
                .thenExecuteAfter(4, () -> {
                    final ItemStack psu = ServerItem.psu(rack.getServers().getStackInSlot(0));
                    final PsuSpec spec = PsuConditions.spec(psu);
                    helper.assertTrue(spec.redundant() && !PsuConditions.failed(psu)
                            && PsuConditions.of(psu).degraded(spec), "one unit gone, it is degraded, not failed");
                    helper.assertTrue(rack.bayPowerOn(0), "and the server runs on");
                    mountRedundant(rack, new PsuCondition(1f, List.of(1f, ALMOST_FAILED)));
                })
                .thenExecuteAfter(4, () -> rack.toggleBayPower(0))
                .thenExecuteAfter(2, () -> {
                    helper.assertTrue(PsuConditions.failed(ServerItem.psu(rack.getServers().getStackInSlot(0))),
                            "with both units gone the supply has failed");
                    helper.assertTrue(logged(helper, network[0], EventLevel.ERROR, "Power supply failed"),
                            "the network it ran on says so under the server");
                    rack.toggleBayPower(0);
                    helper.assertFalse(rack.bayPowerOn(0), "the bay's switch brings nothing up");
                })
                .thenSucceed();
    }

    /** With the server's wear off, a running supply stays as it was and is never degraded nor failed. */
    @GameTest(template = ARENA, timeoutTicks = PATIENT, batch = TestComputerEnergy.NO_WEAR_BATCH)
    public static void psu_lastsForEverWithTheWearOff(final GameTestHelper helper) {
        final CraftingComputerBlockEntity cc = TestWorldBuilder.forGameTest(helper).buildCraftingNetwork().cc();
        final PsuCondition worn = new PsuCondition(1f, List.of(1f));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(cc.isRunning(), "the Crafting Computer runs"))
                .thenExecute(() -> PsuConditions.set(cc.psuStack(), worn))
                .thenExecuteAfter(20, cc::togglePower)
                .thenExecuteAfter(2, () -> {
                    helper.assertFalse(PsuConditions.failed(cc.psuStack()), "a worn-out supply counts as sound");
                    cc.togglePower();
                    helper.assertTrue(cc.isRunning(), "and the machine runs on it");
                })
                .thenSucceed();
    }

    /* Mounts the rack's server again with the redundant supply in {@code condition}, as a player would. */
    private static void mountRedundant(final ServerRackBlockEntity rack, final PsuCondition condition) {
        final ItemStack server = rack.getServers().getStackInSlot(0).copy();
        final ItemStack psu = new ItemStack(HardwareItems.PSU_3000P.get());
        PsuConditions.set(psu, condition);
        ServerItem.replacePsu(server, psu);
        rack.getServers().setStackInSlot(0, server);
    }

    private static boolean logged(final GameTestHelper helper, final NetworkUuid network, final EventLevel level,
                                  final String startsWith) {
        final List<NetworkEvent> log = NetworkEventLog.of(helper.getLevel()).newest(network,
                helper.getLevel().getGameTime(), any -> true, NetworkEventLog.MOST);
        return log.stream().anyMatch(event -> event.level() == level
                && event.message().english().startsWith(startsWith));
    }
}
