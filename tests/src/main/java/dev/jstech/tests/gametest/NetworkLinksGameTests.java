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
import dev.jstech.computers.operation.payload.NetworkManagerPayload;
import dev.jstech.computers.operation.payload.NetworkNodeInfo;
import dev.jstech.computers.operation.payload.NodeLink;
import dev.jstech.computers.operation.payload.network.NetworkPayloads;
import dev.jstech.core.network.DataLine;
import dev.jstech.core.network.DataLink;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.ServerStacks;
import dev.jstech.tests.testkit.TestCables;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * What the Network Manager is told of its nodes' links: the cable each is plugged into, the Optical Network Card and
 * the optical router the fibre goes through; the machines that lost their link, with why (a fibre that bends where no
 * optical router turns it, a run longer than its cable reaches); and the totals of the links on the Hardware tab.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class NetworkLinksGameTests {

    private static final String ARENA = "empty";
    private static final String BENCH = "bench";
    private static final int SETTLE = 6;
    private static final DataLink FIBRE = new DataLink(DataLine.BACKBONE, HardwareEra.STANDARD);
    private static final DataLink THIN_COAX = new DataLink(DataLine.ACCESS, HardwareEra.VINTAGE);

    private NetworkLinksGameTests() {
    }

    /** The Mainframe's fibre goes through an optical router: its link says so, with the card and its run. */
    @GameTest(template = ARENA)
    public static void collect_tellsTheFibreAndTheOpticalRouter(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = opticalMainframe(helper);
        TestCables.lay(helper, new BlockPos(2, 2, 2), ComputingModule.FIBRE_CABLE);
        final BlockPos router = new BlockPos(3, 2, 2);
        helper.setBlock(router, ComputingModule.OPTICAL_ROUTER.get());
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final NetworkManagerPayload payload = collect(helper, mainframe);
                    final NodeLink link = payload.nodes().getFirst().link();
                    helper.assertValueEqual(link.dataLink(), FIBRE, "the Mainframe's link is the fibre");
                    helper.assertTrue(link.optical() && link.up(), "with its card, and up");
                    helper.assertTrue(link.runLength() == 1, "a run of one cable; got " + link.runLength());
                    helper.assertTrue(link.router() == helper.absolutePos(router).asLong(),
                            "through the optical router beside it");
                    helper.assertTrue(payload.hardware().opticalUp() == 1 && payload.hardware().opticalDown() == 0,
                            "one optical link, up");
                    helper.assertValueEqual(payload.hardware().backbone(), FIBRE.serializedName(),
                            "the backbone is the fibre");
                })
                .thenSucceed();
    }

    /** A rack whose fibre bends on its way to the Mainframe lost its link, and the bend is where. */
    @GameTest(template = ARENA)
    public static void collect_tellsAFibreThatBends(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = opticalMainframe(helper);
        TestCables.lay(helper, new BlockPos(2, 2, 2), ComputingModule.FIBRE_CABLE);
        final BlockPos bend = new BlockPos(3, 2, 2);
        TestCables.lay(helper, bend, ComputingModule.FIBRE_CABLE);
        TestCables.lay(helper, new BlockPos(3, 2, 3), ComputingModule.FIBRE_CABLE);
        TestCables.lay(helper, new BlockPos(3, 2, 4), ComputingModule.FIBRE_CABLE);
        final BlockPos rackAt = new BlockPos(3, 2, 5);
        helper.setBlock(rackAt, ComputingModule.SERVER_RACK.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        if (!(helper.getBlockEntity(rackAt) instanceof ServerRackBlockEntity rack)) {
            helper.fail("no Server Rack placed");
            return;
        }
        rack.getServers().setStackInSlot(0, ServerStacks.opticalServer());
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final NetworkNodeInfo lost = lost(helper, collect(helper, mainframe));
                    helper.assertTrue(lost.kind() == NetworkNodeInfo.KIND_SERVER, "the rack's server lost its link");
                    helper.assertTrue(lost.link().reason() == NodeLink.REASON_BENDS, "because its fibre bends");
                    helper.assertTrue(lost.link().where() == helper.absolutePos(bend).asLong(),
                            "at the block where it would have to turn");
                    helper.assertTrue(lost.link().optical(), "the rack holds a card all the same");
                })
                .thenSucceed();
    }

    /** A computer beyond a run longer than its cable reaches lost its link, and the run is told. */
    @GameTest(template = BENCH, timeoutTicks = 200)
    public static void collect_tellsARunTooLong(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final MainframeBlockEntity mainframe = world.placeRunningMainframe(new BlockPos(1, 2, 2));
        world.setBlock(new BlockPos(2, 2, 2), ComputingModule.HBW_CABLE);
        world.setBlock(new BlockPos(3, 2, 2), ComputingModule.PERSONAL_ROUTER.get());
        final int run = THIN_COAX.range() + 1;
        for (int i = 0; i < run; i++) {
            world.setBlock(new BlockPos(4 + i, 2, 2), ComputingModule.THIN_COAX_CABLE);
        }
        world.placeRunningPersonalComputer(new BlockPos(4 + run, 2, 2));
        helper.startSequence()
                .thenWaitUntil(() -> {
                    final NetworkNodeInfo lost = lost(helper, collect(helper, mainframe));
                    helper.assertTrue(lost.kind() == NetworkNodeInfo.KIND_PC, "the computer lost its link");
                    helper.assertTrue(lost.link().reason() == NodeLink.REASON_TOO_LONG, "to a run too long");
                    helper.assertValueEqual(lost.link().dataLink(), THIN_COAX, "of thin coax");
                    helper.assertTrue(lost.link().runLength() == run, "of " + run + " cables; got "
                            + lost.link().runLength());
                })
                .thenSucceed();
    }

    /* A running Standard Mainframe at (1, 2, 2) holding an Optical Network Card. */
    private static MainframeBlockEntity opticalMainframe(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = TestWorldBuilder.forGameTest(helper)
                .placeRunningMainframe(new BlockPos(1, 2, 2));
        mainframe.getInventory().setStackInSlot(MainframeBlockEntity.GPU_SLOTS_START,
                new ItemStack(ComputingModule.OPTICAL_NETWORK_CARD.get()));
        return mainframe;
    }

    private static NetworkManagerPayload collect(final GameTestHelper helper, final MainframeBlockEntity mainframe) {
        return NetworkPayloads.collectNetworkManager(helper.getLevel(), mainframe);
    }

    private static NetworkNodeInfo lost(final GameTestHelper helper, final NetworkManagerPayload payload) {
        final List<NetworkNodeInfo> lost = payload.nodes().stream().filter(node -> !node.link().up()).toList();
        if (lost.isEmpty()) {
            helper.fail("no node lost its link; nodes " + payload.nodes().size());
        }
        return lost.getFirst();
    }
}
