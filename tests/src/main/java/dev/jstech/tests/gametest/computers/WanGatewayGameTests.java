/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.block.WanGatewayBlock;
import dev.jstech.computers.block.WanGatewayPartBlock;
import dev.jstech.computers.block.WanGatewayStructure;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.PartRefusals;
import dev.jstech.computers.blockentity.WanGatewayBlockEntity;
import dev.jstech.core.cable.Cables;
import dev.jstech.core.jstechseries.network.NetworkSystem;
import dev.jstech.core.text.TextKey;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestCables;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static dev.jstech.tests.testkit.computers.NetworkFixtures.formRunningGateway;
import static dev.jstech.tests.testkit.computers.NetworkFixtures.placeRunningMainframe;

/**
 * The WAN Gateway Computer (computers/docs/design/network.md): a 4 x 2 x 2 big block with the four ports of the
 * long-distance line on its back, its WAN Interface Cards only in it and four at most, a port's line up while it runs
 * with a cable its card takes plugged in, and its own network read off the backbone as a Personal Computer reads one.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class WanGatewayGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;
    private static final BlockPos ORIGIN = new BlockPos(4, 2, 4);
    private static final Direction FACING = Direction.NORTH;

    private WanGatewayGameTests() {
    }

    @GameTest(template = ARENA)
    public static void gateway_formsSixteenBlocksWithFourPortsOnItsBack(final GameTestHelper helper) {
        formRunningGateway(helper, ORIGIN, FACING);
        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    int blocks = 0;
                    int ports = 0;
                    for (final BlockPos at : WanGatewayStructure.allPositions(ORIGIN, FACING)) {
                        final BlockState state = helper.getBlockState(at);
                        if (state.getBlock() instanceof WanGatewayBlock) {
                            blocks++;
                        } else if (state.getBlock() instanceof WanGatewayPartBlock) {
                            blocks++;
                            final int port = state.getValue(WanGatewayPartBlock.PORT);
                            if (port > 0) {
                                ports++;
                                helper.assertTrue(port == WanGatewayStructure.portAt(ORIGIN, FACING, at)
                                                && at.getY() == ORIGIN.getY(),
                                        "port " + port + " stands on the bottom back row, at " + at);
                            }
                        }
                    }
                    helper.assertTrue(blocks == WanGatewayStructure.BLOCK_COUNT, "the cabinet is 16 blocks; "
                            + blocks);
                    helper.assertTrue(ports == WanGatewayStructure.PORTS, "it has four ports; " + ports);
                    // Seen from behind a north-facing gateway, the west end is on the left: port 1 is there.
                    final BlockPos first = WanGatewayStructure.portBlock(ORIGIN, FACING, 1);
                    helper.assertTrue(first.getX() == ORIGIN.getX() - 2 && first.getZ() == ORIGIN.getZ() + 1,
                            "port 1 is the westmost block of the back row; " + first);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void gateway_brokenAnywhereDropsItselfAndItsParts(final GameTestHelper helper) {
        formRunningGateway(helper, ORIGIN, FACING);
        helper.startSequence()
                .thenExecuteAfter(2, () -> helper.setBlock(WanGatewayStructure.portBlock(ORIGIN, FACING, 4),
                        Blocks.AIR))
                .thenExecuteAfter(2, () -> {
                    for (final BlockPos at : WanGatewayStructure.allPositions(ORIGIN, FACING)) {
                        final BlockState state = helper.getBlockState(at);
                        helper.assertFalse(state.getBlock() instanceof WanGatewayBlock
                                || state.getBlock() instanceof WanGatewayPartBlock, "the whole cabinet goes, " + at);
                    }
                    helper.assertTrue(dropped(helper, ComputingModule.WAN_GATEWAY.get().asItem()),
                            "it drops the gateway of its era");
                    helper.assertTrue(dropped(helper, ComputingModule.MOTHERBOARD_MTX_S_2011.get()),
                            "and spills the board it held");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void wanCard_goesOnlyInAGatewayAndFourAtMost(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = placeRunningMainframe(helper, new BlockPos(10, 2, 2));
        final WanGatewayBlockEntity gateway = formRunningGateway(helper, ORIGIN, FACING);
        final ItemStack card = new ItemStack(ComputingModule.VLDC_CARD.get());
        final int slot = MainframeBlockEntity.GPU_SLOTS_START;
        helper.assertFalse(mainframe.isValidForSlot(slot, card), "a Mainframe refuses a WAN Interface Card");
        helper.assertTrue(says(mainframe.refusal(slot, card), "only goes in a WAN Gateway Computer"),
                "and says why");
        helper.assertTrue(gateway.isValidForSlot(slot, card), "a gateway takes one");
        for (int i = 0; i < WanGatewayStructure.PORTS; i++) {
            gateway.getInventory().setStackInSlot(slot + i, card.copy());
        }
        final int fifth = slot + WanGatewayStructure.PORTS;
        helper.assertFalse(gateway.isValidForSlot(fifth, card), "a fifth has no port to take");
        helper.assertTrue(gateway.refusal(fifth, card) == PartRefusals.FOUR_WAN_CARDS, "and the slot says so");
        helper.assertTrue(gateway.isValidForSlot(fifth, new ItemStack(ComputingModule.OPTICAL_NETWORK_CARD.get())),
                "another kind of card still goes in the slots left");
        helper.assertTrue(gateway.wanCards().size() == WanGatewayStructure.PORTS, "the four cards work its ports");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void port_takesItsLineOnItsBackOnlyAndNoNewerOne(final GameTestHelper helper) {
        formRunningGateway(helper, ORIGIN, FACING);
        final Direction back = FACING.getOpposite();
        final BlockPos first = WanGatewayStructure.portBlock(ORIGIN, FACING, 1);
        final BlockPos second = WanGatewayStructure.portBlock(ORIGIN, FACING, 2);
        final BlockPos above = WanGatewayStructure.portBlock(ORIGIN, FACING, 3).above();
        final BlockPos side = WanGatewayStructure.portBlock(ORIGIN, FACING, 4);
        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    TestCables.lay(helper, first.relative(back), ComputingModule.LEASED_LINE);
                    TestCables.lay(helper, second.relative(back), ComputingModule.DARK_FIBRE_CABLE);
                    TestCables.lay(helper, above.relative(back), ComputingModule.VLDC_CABLE);
                    TestCables.lay(helper, side.relative(FACING.getClockWise()), ComputingModule.VLDC_CABLE);
                })
                .thenExecuteAfter(2, () -> {
                    helper.assertTrue(reaches(helper, first, back), "a port takes a line older than the gateway");
                    helper.assertFalse(reaches(helper, second, back), "and never a newer one");
                    helper.assertFalse(reaches(helper, above, back), "the upper row has no port");
                    helper.assertFalse(reaches(helper, side, FACING.getClockWise()), "nor has a side face");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void line_isDownWithNothingAnsweringAtTheFarEnd(final GameTestHelper helper) {
        final WanGatewayBlockEntity gateway = formRunningGateway(helper, ORIGIN, FACING);
        final int slot = MainframeBlockEntity.GPU_SLOTS_START;
        gateway.getInventory().setStackInSlot(slot, new ItemStack(ComputingModule.VLDC_CARD.get()));
        gateway.getInventory().setStackInSlot(slot + 1, new ItemStack(ComputingModule.VLDC_CARD.get()));
        // Port 1 has its card's line plugged in and nothing at the other end of it; port 2 has no cable.
        TestCables.lay(helper, WanGatewayStructure.portBlock(ORIGIN, FACING, 1).relative(FACING.getOpposite()),
                ComputingModule.VLDC_CABLE);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(gateway.line(1) == WanGatewayBlockEntity.LINE_DOWN,
                            "a line with no gateway answering at its far end is down");
                    helper.assertTrue(gateway.line(2) == WanGatewayBlockEntity.LINE_DOWN, "no cable, no line");
                    helper.assertTrue(gateway.line(3) == WanGatewayBlockEntity.LINE_NONE, "no card, no port");
                    helper.assertTrue(gateway.portsUp() == 0 && gateway.cardedPorts() == 2, "none up of two");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void gateway_joinsItsNetworkOffTheBackboneAsAComputer(final GameTestHelper helper) {
        final WanGatewayBlockEntity gateway = formRunningGateway(helper, ORIGIN, FACING);
        // The gateway's east end is the block clockwise of the origin; the backbone runs from it to a Mainframe.
        final BlockPos end = ORIGIN.relative(FACING.getClockWise());
        TestCables.lay(helper, end.east(), ComputingModule.HBW_CABLE);
        TestCables.lay(helper, end.east(2), ComputingModule.HBW_CABLE);
        final MainframeBlockEntity mainframe = placeRunningMainframe(helper, end.east(3));
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 4, () -> {
                    helper.assertTrue(mainframe.networkUuid() != null
                            && mainframe.networkUuid().equals(gateway.networkUuid()),
                            "a cable on any block of the cabinet puts it on the Mainframe's network");
                    final List<NetworkSystem.PersonalComputerNode> computers = NetworkSystem.get(helper.getLevel())
                            .personalComputersOf(mainframe.networkUuid());
                    helper.assertTrue(computers.stream().anyMatch(node -> node.nodeUuid().equals(gateway.nodeUuid())),
                            "it is on its network as a computer, not an orchestrator");
                    helper.assertFalse(mainframe.hasNetworkConflict(), "and the network has no conflict");
                    helper.assertTrue(gateway.machineId().startsWith("WAN-"), "the log calls it WAN-...");
                })
                .thenSucceed();
    }

    /* Whether a cable on {@code face} of {@code at} crosses into the block. */
    private static boolean reaches(final GameTestHelper helper, final BlockPos at, final Direction face) {
        return !Cables.reaching(helper.getLevel(), helper.absolutePos(at), face).isEmpty();
    }

    /* Whether an item of {@code item} lies round the cabinet. */
    private static boolean dropped(final GameTestHelper helper, final Item item) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(ORIGIN)).inflate(6))
                .stream().anyMatch(entity -> entity.getItem().is(item));
    }

    private static boolean says(final TextKey refusal, final String words) {
        return refusal != null && refusal.text().english().contains(words);
    }
}
