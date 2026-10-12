/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.HardwareItems;
import dev.jstech.computers.block.FibreReach;
import dev.jstech.computers.block.MainframeBlock;
import dev.jstech.computers.block.OpticalPort;
import dev.jstech.computers.block.ServerRackBlock;
import dev.jstech.computers.block.ServerRackStructure;
import dev.jstech.computers.blockentity.ClusterManagementComputerBlockEntity;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.PartRefusals;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.item.ServerHardwareHandler;
import dev.jstech.core.cable.CableEntry;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.ServerStacks;
import dev.jstech.tests.testkit.TestCables;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The way the fibre of the backbone goes into a machine: a Mainframe or a Cluster Management Computer takes it only
 * while it holds an Optical Network Card, every block of a cabinet with it; a server rack only through a Fibre Optic
 * Interface mounted in it, as far as the interface reaches, and a server never takes the card.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class OpticalCardGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;

    private OpticalCardGameTests() {
    }

    /** A Mainframe takes the fibre only while it holds the card, and the card is told on every block of its cabinet. */
    @GameTest(template = ARENA)
    public static void mainframe_takesTheFibreOnlyWithTheCard(final GameTestHelper helper) {
        final BlockPos at = new BlockPos(4, 2, 4);
        final MainframeBlockEntity mainframe = TestWorldBuilder.forGameTest(helper).placeRunningMainframe(at);
        final BlockState state = helper.getBlockState(at);
        state.getBlock().setPlacedBy(helper.getLevel(), helper.absolutePos(at), state, null, ItemStack.EMPTY);
        final List<BlockPos> cabinet = ((MainframeBlock) state.getBlock())
                .footprint(helper.absolutePos(at), state.getValue(HorizontalDirectionalBlock.FACING));
        final Direction side = freeSide(helper, at, cabinet);
        final BlockPos fibre = at.relative(side);
        TestCables.lay(helper, fibre, ComputingModule.FIBRE_CABLE);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertFalse(crosses(helper, fibre, side.getOpposite()), "no card, and the fibre stays out");
                    mainframe.getInventory().setStackInSlot(MainframeBlockEntity.GPU_SLOTS_START,
                            new ItemStack(ComputingModule.OPTICAL_NETWORK_CARD.get()));
                })
                .thenExecuteAfter(SETTLE, () -> {
                    for (final BlockPos block : cabinet) {
                        helper.assertTrue(helper.getLevel().getBlockState(block).getValue(OpticalPort.OPTICAL),
                                "every block of the cabinet knows of the card, " + block.toShortString() + " too");
                    }
                    helper.assertTrue(crosses(helper, fibre, side.getOpposite()), "with the card the fibre goes in");
                    mainframe.getInventory().setStackInSlot(MainframeBlockEntity.GPU_SLOTS_START, ItemStack.EMPTY);
                })
                .thenExecuteAfter(SETTLE, () -> helper.assertFalse(crosses(helper, fibre, side.getOpposite()),
                        "the card taken out, the fibre is left out again"))
                .thenSucceed();
    }

    /** A server refuses the card through its own slots, as its screen does: a rack takes the fibre another way. */
    @GameTest(template = ARENA)
    public static void server_refusesTheOpticalCard(final GameTestHelper helper) {
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, ServerStacks.defaultServer());
        final ServerHardwareHandler slots = new ServerHardwareHandler(player, InteractionHand.MAIN_HAND);
        final ItemStack card = new ItemStack(ComputingModule.OPTICAL_NETWORK_CARD.get());
        helper.assertFalse(slots.isItemValid(ServerHardwareHandler.GPU_START, card),
                "a server's expansion slot refuses the optical network card");
        helper.assertTrue(slots.refusal(ServerHardwareHandler.GPU_START, card) == PartRefusals.SERVER_CARD,
                "and says which cards a server takes");
        helper.succeed();
    }

    /** A Server Rack takes the fibre while a Fibre Optic Interface is mounted in it, and lets it go when it leaves. */
    @GameTest(template = ARENA)
    public static void rack_takesTheFibreWithAFibreInterface(final GameTestHelper helper) {
        final BlockPos rack = new BlockPos(2, 2, 2);
        helper.setBlock(rack, ComputingModule.SERVER_RACK.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        final BlockPos back = rack.south();
        TestCables.lay(helper, back, ComputingModule.FIBRE_CABLE);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertFalse(crosses(helper, back, Direction.NORTH), "no interface, no fibre");
                    cabinet(helper, rack).getServers().setStackInSlot(0, ServerStacks.defaultServer());
                    cabinet(helper, rack).getServers().setStackInSlot(1,
                            new ItemStack(ComputingModule.FIBRE_OPTIC_INTERFACE.get()));
                })
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(helper.getBlockState(rack).getValue(OpticalPort.FIBRE) == FibreReach.STANDARD,
                            "the cabinet knows how far its interface reaches");
                    helper.assertTrue(crosses(helper, back, Direction.NORTH), "and takes the fibre");
                    cabinet(helper, rack).getServers().setStackInSlot(1, ItemStack.EMPTY);
                })
                .thenExecuteAfter(SETTLE, () -> helper.assertFalse(crosses(helper, back, Direction.NORTH),
                        "the interface taken out, the fibre is left out"))
                .thenSucceed();
    }

    /**
     * In an Advanced cabinet the Standard interface takes the Fibre Optic Cable alone; the Advanced one takes the OM5
     * Fibre Cable as well.
     */
    @GameTest(template = ARENA)
    public static void advancedRack_takesTheOm5OnlyThroughTheAdvancedInterface(final GameTestHelper helper) {
        final BlockPos at = new BlockPos(2, 2, 2);
        placeFormed(helper, at, ComputingModule.ADVANCED_SERVER_RACK.get());
        // The cabinet is two blocks deep behind its front: the first free places behind it are two blocks back.
        final BlockPos fibre = at.relative(Direction.SOUTH, 2);
        final BlockPos om5 = fibre.above();
        TestCables.lay(helper, fibre, ComputingModule.FIBRE_CABLE);
        TestCables.lay(helper, om5, ComputingModule.OM5_CABLE);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> cabinet(helper, at).getServers().setStackInSlot(0,
                        new ItemStack(ComputingModule.FIBRE_OPTIC_INTERFACE.get())))
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(crosses(helper, fibre, ComputingModule.FIBRE_CABLE, Direction.NORTH),
                            "the Standard interface takes the Fibre Optic Cable");
                    helper.assertFalse(crosses(helper, om5, ComputingModule.OM5_CABLE, Direction.NORTH),
                            "but not the OM5 Fibre Cable");
                    cabinet(helper, at).getServers().setStackInSlot(0,
                            new ItemStack(ComputingModule.ADVANCED_FIBRE_OPTIC_INTERFACE.get()));
                })
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(crosses(helper, fibre, ComputingModule.FIBRE_CABLE, Direction.NORTH),
                            "the Advanced interface takes the Fibre Optic Cable");
                    helper.assertTrue(crosses(helper, om5, ComputingModule.OM5_CABLE, Direction.NORTH),
                            "and the OM5 Fibre Cable");
                })
                .thenSucceed();
    }

    /** A fibre interface seats in a Server Rack of its era or a newer one, never in an older or a compute cabinet. */
    @GameTest(template = ARENA)
    public static void fibreInterface_seatsOnlyInAServerRackOfItsEraOrNewer(final GameTestHelper helper) {
        final ServerRackBlockEntity transition = placed(helper, new BlockPos(1, 2, 1),
                ComputingModule.TRANSITION_SERVER_RACK.get());
        final ServerRackBlockEntity standard = placed(helper, new BlockPos(5, 2, 1), ComputingModule.SERVER_RACK.get());
        final ServerRackBlockEntity advanced = placed(helper, new BlockPos(1, 2, 5),
                ComputingModule.ADVANCED_SERVER_RACK.get());
        final ServerRackBlockEntity compute = placed(helper, new BlockPos(5, 2, 5),
                ComputingModule.SUPERCOMPUTER_RACK.get());
        final ItemStack fibre = new ItemStack(ComputingModule.FIBRE_OPTIC_INTERFACE.get());
        final ItemStack om5 = new ItemStack(ComputingModule.ADVANCED_FIBRE_OPTIC_INTERFACE.get());
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertFalse(transition.getServers().isItemValid(0, fibre),
                            "a Transition rack, with no fibre of its own, refuses the Standard interface");
                    helper.assertTrue(transition.rowRefusal(0, fibre) == ServerRackBlock.WRONG_UNIT,
                            "and says the unit belongs elsewhere");
                    helper.assertTrue(standard.getServers().isItemValid(0, fibre), "a Standard rack seats it");
                    helper.assertFalse(standard.getServers().isItemValid(0, om5),
                            "but not the Advanced one, newer than the rack");
                    helper.assertTrue(advanced.getServers().isItemValid(0, fibre)
                            && advanced.getServers().isItemValid(0, om5), "an Advanced rack seats both");
                    helper.assertFalse(compute.getServers().isItemValid(0, fibre),
                            "a Supercomputer Rack, off the backbone, seats none");
                })
                .thenSucceed();
    }

    /**
     * The whole way a player goes, in an Advanced cabinet: the cabinet placed and formed, a server built through its
     * own slots, the server and the interface mounted through the rack's, and the fibre laid behind the cabinet's
     * back, where a player can lay it.
     */
    @GameTest(template = ARENA)
    public static void formedRack_takesTheFibreAtItsBackThroughAnInterfaceThePlayerMounted(
            final GameTestHelper helper) {
        final BlockPos at = new BlockPos(2, 2, 2);
        placeFormed(helper, at, ComputingModule.ADVANCED_SERVER_RACK.get());
        final List<BlockPos> cabinet = ServerRackStructure.allPositions(helper.absolutePos(at), Direction.NORTH);
        final BlockPos fibre = at.relative(Direction.SOUTH, 2);
        TestCables.lay(helper, fibre, ComputingModule.OM5_CABLE);
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ComputingModule.ADVANCED_SERVER.get()));
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    // The server built part by part, as its screen builds it.
                    final ServerHardwareHandler slots = new ServerHardwareHandler(player, InteractionHand.MAIN_HAND);
                    seat(helper, slots, ServerHardwareHandler.MOBO, HardwareItems.MOTHERBOARD_EEB_A_SP3.get());
                    seat(helper, slots, ServerHardwareHandler.CPU_START, HardwareItems.CPU_VELOCION_EPIC_7251.get());
                    seat(helper, slots, ServerHardwareHandler.RAM_START, HardwareItems.RAM_DDR4_16384.get());
                    seat(helper, slots, ServerHardwareHandler.PSU, HardwareItems.PSU_1200P.get());
                    final ServerRackBlockEntity rack = cabinet(helper, at);
                    helper.assertTrue(rack.getServers().insertItem(0,
                            player.getItemInHand(InteractionHand.MAIN_HAND).copy(), false).isEmpty(),
                            "the rack takes the server");
                    helper.assertFalse(crosses(helper, fibre, ComputingModule.OM5_CABLE, Direction.NORTH),
                            "a server alone brings no fibre");
                    helper.assertTrue(rack.getServers().insertItem(1,
                            new ItemStack(ComputingModule.ADVANCED_FIBRE_OPTIC_INTERFACE.get()), false).isEmpty(),
                            "the rack takes the interface in the row below");
                })
                .thenExecuteAfter(SETTLE, () -> {
                    for (final BlockPos block : cabinet) {
                        helper.assertTrue(helper.getLevel().getBlockState(block).getValue(OpticalPort.FIBRE)
                                        == FibreReach.ADVANCED,
                                "every block of the cabinet knows of the interface, " + block.toShortString() + " too");
                    }
                    helper.assertTrue(crosses(helper, fibre, ComputingModule.OM5_CABLE, Direction.NORTH),
                            "and the fibre goes into its back");
                })
                .thenSucceed();
    }

    /** A Cluster Management Computer takes the fibre on its back only with the card. */
    @GameTest(template = ARENA)
    public static void clusterManager_takesTheFibreOnlyWithTheCard(final GameTestHelper helper) {
        final BlockPos at = new BlockPos(2, 2, 2);
        helper.setBlock(at, ComputingModule.CLUSTER_MANAGEMENT_COMPUTER.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        final BlockPos back = at.south();
        TestCables.lay(helper, back, ComputingModule.FIBRE_CABLE);
        if (!(helper.getBlockEntity(at) instanceof ClusterManagementComputerBlockEntity manager)) {
            helper.fail("no Cluster Management Computer placed");
            return;
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertFalse(crosses(helper, back, Direction.NORTH), "no card, no fibre");
                    manager.getHardware().setStackInSlot(ClusterManagementComputerBlockEntity.MOTHERBOARD_SLOT,
                            new ItemStack(HardwareItems.MOTHERBOARD_ATX_STANDARD_LGA1150.get()));
                    manager.getHardware().setStackInSlot(ClusterManagementComputerBlockEntity.CPU_SLOT,
                            new ItemStack(HardwareItems.CPU_INTEGRA_CENTRO_C7_4790K.get()));
                    manager.getHardware().setStackInSlot(ClusterManagementComputerBlockEntity.RAM_SLOTS_START,
                            new ItemStack(ComputingModule.RAM_DDR3_8192.get()));
                    manager.getHardware().setStackInSlot(ClusterManagementComputerBlockEntity.PSU_SLOT,
                            new ItemStack(ComputingModule.PSU_650G.get()));
                    manager.getHardware().setStackInSlot(ClusterManagementComputerBlockEntity.PCIE_SLOTS_START,
                            new ItemStack(ComputingModule.OPTICAL_NETWORK_CARD.get()));
                })
                .thenExecuteAfter(SETTLE, () -> helper.assertTrue(crosses(helper, back, Direction.NORTH),
                        "assembled with the card, it takes the fibre"))
                .thenSucceed();
    }

    /* Puts a part into a server's slot the way its screen does, failing the test when the slot refuses it. */
    private static void seat(final GameTestHelper helper, final ServerHardwareHandler slots, final int slot,
                             final Item part) {
        helper.assertTrue(slots.insertItem(slot, new ItemStack(part), false).isEmpty(),
                "the server's slot " + slot + " takes " + BuiltInRegistries.ITEM.getKey(part));
    }

    /* Places a cabinet facing north and forms it, as a player placing it does. */
    private static void placeFormed(final GameTestHelper helper, final BlockPos at, final Block cabinet) {
        final BlockState state = cabinet.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING,
                Direction.NORTH);
        helper.setBlock(at, state);
        state.getBlock().setPlacedBy(helper.getLevel(), helper.absolutePos(at), state, null, ItemStack.EMPTY);
    }

    private static ServerRackBlockEntity placed(final GameTestHelper helper, final BlockPos at, final Block cabinet) {
        helper.setBlock(at, cabinet.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        return cabinet(helper, at);
    }

    private static boolean crosses(final GameTestHelper helper, final BlockPos cable, final Direction face) {
        return crosses(helper, cable, ComputingModule.FIBRE_CABLE, face);
    }

    private static boolean crosses(final GameTestHelper helper, final BlockPos cable, final CableEntry type,
                                   final Direction face) {
        return TestCables.cable(helper, cable).crosses(type.get(), face);
    }

    private static ServerRackBlockEntity cabinet(final GameTestHelper helper, final BlockPos at) {
        if (!(helper.getBlockEntity(at) instanceof ServerRackBlockEntity rack)) {
            throw new IllegalStateException("no Server Rack at " + at.toShortString());
        }
        return rack;
    }

    /* A side of the block at {@code at} that the cabinet does not stand on, for a cable to come in by. */
    private static Direction freeSide(final GameTestHelper helper, final BlockPos at, final List<BlockPos> cabinet) {
        for (final Direction side : Direction.Plane.HORIZONTAL) {
            if (!cabinet.contains(helper.absolutePos(at.relative(side)))) {
                return side;
            }
        }
        throw new IllegalStateException("the cabinet stands on every side of " + at.toShortString());
    }
}
