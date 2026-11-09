/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.blockentity.ServerRouterBlockEntity;
import dev.jstech.computers.datacenter.SectionAlert;
import dev.jstech.computers.datacenter.SectionDiagnostics;
import dev.jstech.computers.datacenter.SectionReading;
import dev.jstech.computers.eventlog.EventLevel;
import dev.jstech.computers.eventlog.NetworkEventLog;
import dev.jstech.computers.operation.NetworkInsertOperation;
import dev.jstech.computers.storage.ServerStore;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestCables;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A Server Router watches each of its datacenter sections once a second: what the section's servers moved and how long
 * they took to answer, its base and the most it can move, and its alerts, each written into the network's log as it
 * is raised (computers/docs/design/servers-and-racks.md).
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class SectionDiagnosticsGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos MAINFRAME = new BlockPos(1, 2, 3);
    private static final BlockPos CABLE = new BlockPos(2, 2, 3);
    private static final BlockPos ROUTER = new BlockPos(3, 2, 3);
    private static final BlockPos SECTION_CABLE = new BlockPos(4, 2, 3);
    private static final BlockPos RACK = new BlockPos(5, 2, 3);
    private static final int SETTLE = 4;
    private static final long DEMAND = 640L;

    private SectionDiagnosticsGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void section_countsWhatItsServersMovedAndHowLongTheyTook(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = datacenter(helper);
        final ServerRouterBlockEntity router = routerAt(helper);
        final NetworkInsertOperation[] op = new NetworkInsertOperation[1];
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(mainframe.isRunning() && mainframe.networkUuid() != null
                        && !router.sections().isEmpty(), "the section stands on a running network"))
                .thenExecuteAfter(SETTLE, () -> {
                    op[0] = mainframe.submitNetworkInsert(Items.COBBLESTONE, DEMAND, "test");
                    helper.assertTrue(op[0] != null, "the INSERT dispatches");
                })
                .thenWaitUntil(() -> helper.assertTrue(op[0].isDone(), "the INSERT settles"))
                .thenIdle(25)
                .thenExecute(() -> {
                    final SectionDiagnostics diagnostics = router.diagnostics(Direction.EAST);
                    final SectionReading reading = router.reading(Direction.EAST);
                    helper.assertTrue(diagnostics != null && reading != null, "the router read its section");
                    helper.assertTrue(diagnostics.throughput() > 0, "the items written went through the section");
                    helper.assertTrue(diagnostics.latencyTenths() > 0,
                            "its server's answer took some time; got " + diagnostics.latencyTenths());
                    helper.assertTrue(reading.baseTenths() > 0, "the section has a base from its server's drives");
                    helper.assertTrue(reading.maxThroughput() > 0, "and a most it can move");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void section_fullDisksRaiseAnErrorAndTellTheLogOnce(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = datacenter(helper);
        final ServerRouterBlockEntity router = routerAt(helper);
        final ServerRackBlockEntity rack = rackAt(helper);
        final NetworkUuid[] network = new NetworkUuid[1];
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(mainframe.networkUuid() != null
                        && !router.sections().isEmpty(), "the section stands on a running network"))
                .thenExecute(() -> {
                    network[0] = mainframe.networkUuid();
                    final ServerStore store = rack.getServerStorage(0);
                    store.insert(Items.COBBLESTONE, store.free());
                })
                .thenWaitUntil(() -> helper.assertTrue(errorAlerts(router) == 1,
                        "a server's full disks raise an ERROR"))
                .thenIdle(45)
                .thenExecute(() -> {
                    helper.assertTrue(errorAlerts(router) == 1, "the alert stands while the disks are full");
                    final long logged = NetworkEventLog.of(helper.getLevel()).newest(network[0],
                                    helper.getLevel().getGameTime(), any -> true, NetworkEventLog.MOST).stream()
                            .filter(event -> event.level() == EventLevel.ERROR
                                    && event.message().english().contains("100% full"))
                            .count();
                    helper.assertTrue(logged == 1, "the network's log has it once; got " + logged);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void router_underItsEraBudgetRaisesNoWarning(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = datacenter(helper);
        final ServerRouterBlockEntity router = routerAt(helper);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(mainframe.networkUuid() != null
                        && !router.sections().isEmpty(), "the section stands on a running network"))
                .thenIdle(25)
                .thenExecute(() -> helper.assertTrue(alertsWithKey(router, "budget") == 0,
                        "one rack is under the Standard router's 16"))
                .thenSucceed();
    }

    /* A running Mainframe, a Standard Server Router uplinked to it, and a rack with one server east of the router. */
    private static MainframeBlockEntity datacenter(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final MainframeBlockEntity mainframe = world.placeRunningMainframe(MAINFRAME);
        TestCables.lay(helper, CABLE, ComputingModule.HBW_CABLE);
        helper.setBlock(ROUTER, ComputingModule.SERVER_ROUTER.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST)); // its back, the uplink, meets the cable
        TestCables.lay(helper, SECTION_CABLE, ComputingModule.HBW_CABLE);
        helper.setBlock(RACK, ComputingModule.SERVER_RACK.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST)); // its rear meets the section cable
        TestWorldBuilder.mountDefaultServer(rackAt(helper), 0);
        return mainframe;
    }

    private static long errorAlerts(final ServerRouterBlockEntity router) {
        final SectionReading reading = router.reading(Direction.EAST);
        return reading == null ? 0L
                : reading.alerts().stream().filter(alert -> alert.level() == EventLevel.ERROR).count();
    }

    private static long alertsWithKey(final ServerRouterBlockEntity router, final String key) {
        final SectionReading reading = router.reading(Direction.EAST);
        return reading == null ? 0L : reading.alerts().stream().map(SectionAlert::key).filter(key::equals).count();
    }

    private static ServerRouterBlockEntity routerAt(final GameTestHelper helper) {
        if (!(helper.getBlockEntity(ROUTER) instanceof ServerRouterBlockEntity router)) {
            throw new IllegalStateException("no Server Router at " + ROUTER);
        }
        return router;
    }

    private static ServerRackBlockEntity rackAt(final GameTestHelper helper) {
        if (!(helper.getBlockEntity(RACK) instanceof ServerRackBlockEntity rack)) {
            throw new IllegalStateException("no rack at " + RACK);
        }
        return rack;
    }
}
