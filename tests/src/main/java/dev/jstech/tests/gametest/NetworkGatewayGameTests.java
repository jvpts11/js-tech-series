/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dan200.computercraft.api.network.wired.WiredElement;
import dan200.computercraft.api.network.wired.WiredElementCapability;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.PeripheralCapability;
import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.block.NetworkGatewayBlock;
import dev.jstech.computers.blockentity.NetworkGatewayBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.gateway.GatewayLog;
import dev.jstech.computers.gateway.GatewayPermissions;
import dev.jstech.computers.integration.computercraft.ComputerCraftIntegration;
import dev.jstech.core.operation.OperationPriority;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestWorldBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Network Gateway block: it links to a computer only through the socket on its back, by a cable or
 * by standing against it, takes a default name when it does, keeps its name, permissions and log across
 * a save, offers its buffer on the faces without a socket, and is a ComputerCraft peripheral on its
 * front face and nowhere else.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class NetworkGatewayGameTests {

    private NetworkGatewayGameTests() {
    }

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;
    private static final BlockPos COMPUTER = new BlockPos(1, 2, 2);

    private static BlockState gateway(final Direction facing) {
        return ComputingModule.NETWORK_GATEWAY.get().defaultBlockState().setValue(NetworkGatewayBlock.FACING, facing);
    }

    private static NetworkGatewayBlockEntity gatewayAt(final GameTestHelper helper, final BlockPos pos) {
        if (helper.getBlockEntity(pos) instanceof NetworkGatewayBlockEntity be) {
            return be;
        }
        throw new IllegalStateException("no Network Gateway at " + pos);
    }

    @GameTest(template = ARENA)
    public static void gateway_linksOnlyThroughItsBackSocket(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final PersonalComputerBlockEntity pc = world.placeRunningPersonalComputer(COMPUTER);
        pc.console().setComputerName("desk");
        final BlockPos at = new BlockPos(2, 2, 2);
        // Facing west, the front (the ComputerCraft side) touches the computer: that is not a link.
        helper.setBlock(at, gateway(Direction.WEST));
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 4, () -> helper.assertTrue(!gatewayAt(helper, at).online(),
                        "a computer against the front does not link"))
                // Turned round, the back socket touches the computer, and the link comes up.
                .thenExecute(() -> helper.setBlock(at, gateway(Direction.EAST)))
                .thenExecuteAfter(SETTLE + 4, () -> {
                    final NetworkGatewayBlockEntity g = gatewayAt(helper, at);
                    helper.assertTrue(g.online(), "the back socket against the computer links");
                    helper.assertTrue("adjacent".equals(g.linkKind()), "with no cable between; got " + g.linkKind());
                    helper.assertTrue("gateway-1".equals(g.name()), "and takes the first default name; got " + g.name());
                    helper.assertTrue("desk".equals(g.hostName()), "on the host it linked to; got " + g.hostName());
                    helper.assertTrue(pc.linkedEndpoints().contains(helper.absolutePos(at).asLong()),
                            "the computer counts it as a peripheral");
                    helper.assertTrue(helper.getBlockState(at).getValue(NetworkGatewayBlock.LIT), "the lights come on");
                    helper.assertTrue(g.log().entries().stream().anyMatch(e -> e.what().equals("link")),
                            "the link is the first thing in its log");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void gateway_reachesItsHostThroughACableOnTheBack(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        world.placeRunningPersonalComputer(COMPUTER);
        helper.setBlock(new BlockPos(2, 2, 2), ComputingModule.PERIPHERAL_CABLE.get());
        helper.setBlock(new BlockPos(3, 2, 2), ComputingModule.PERIPHERAL_CABLE.get());
        final BlockPos at = new BlockPos(4, 2, 2);
        helper.setBlock(at, gateway(Direction.EAST));
        // A second Gateway on the same cable run, so the second default name is proven too.
        helper.setBlock(new BlockPos(3, 2, 3), ComputingModule.PERIPHERAL_CABLE.get());
        final BlockPos second = new BlockPos(3, 2, 4);
        helper.setBlock(second, gateway(Direction.SOUTH));
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 6, () -> {
                    final NetworkGatewayBlockEntity g = gatewayAt(helper, at);
                    helper.assertTrue(g.online(), "the cable on the back socket links");
                    helper.assertTrue("cable, 2 blocks".equals(g.linkKind()), "and its length is known; got " + g.linkKind());
                    final NetworkGatewayBlockEntity other = gatewayAt(helper, second);
                    helper.assertTrue(other.online(), "the second Gateway links too");
                    helper.assertTrue(!other.name().equals(g.name()), "under its own default name; got " + other.name());
                })
                // Cutting the cable behind the Gateway drops the link.
                .thenExecute(() -> helper.setBlock(new BlockPos(3, 2, 2), net.minecraft.world.level.block.Blocks.AIR))
                .thenExecuteAfter(SETTLE + 4, () -> helper.assertTrue(!gatewayAt(helper, at).online(),
                        "the link drops with the cable"))
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void gateway_keepsItsNameAndPermissionsAcrossASave(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        world.placeRunningPersonalComputer(COMPUTER);
        final BlockPos at = new BlockPos(2, 2, 2);
        helper.setBlock(at, gateway(Direction.EAST));
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 4, () -> {
                    final NetworkGatewayBlockEntity g = gatewayAt(helper, at);
                    helper.assertTrue(g.rename("CC Bridge!", "desk").contains("cc-bridge"), "the name is cleaned");
                    helper.assertTrue("cc-bridge".equals(g.name()), "and kept; got " + g.name());
                    g.setPermissions(GatewayPermissions.DEFAULT.withRead(false)
                            .withCeiling(OperationPriority.HIGH).withCallCap(16), "desk", "set everything");
                    g.buffer().setStackInSlot(2, new ItemStack(Items.COBBLESTONE, 7));
                    final CompoundTag saved = g.saveWithoutMetadata(helper.getLevel().registryAccess());
                    final NetworkGatewayBlockEntity fresh = new NetworkGatewayBlockEntity(at, helper.getBlockState(at));
                    fresh.loadWithComponents(saved, helper.getLevel().registryAccess());
                    helper.assertTrue("cc-bridge".equals(fresh.name()), "the name survives a save; got " + fresh.name());
                    final GatewayPermissions p = fresh.permissions();
                    helper.assertTrue(!p.read() && p.operations()
                                    && p.ceiling() == OperationPriority.HIGH && p.callCap() == 16,
                            "so do the permissions; got " + p);
                    helper.assertTrue(fresh.log().size() == g.log().size() && fresh.log().entries().get(0).what()
                            .equals("set everything"), "and the log, newest first");
                    helper.assertTrue(fresh.buffer().getStackInSlot(2).getCount() == 7, "and the buffer");
                    helper.assertTrue(fresh.log().entries().get(0).tone() == GatewayLog.Tone.OK, "with its tones");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void gateway_offersItsBufferOnTheFacesWithoutASocket(final GameTestHelper helper) {
        final BlockPos at = new BlockPos(2, 2, 2);
        helper.setBlock(at, gateway(Direction.EAST));
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final BlockPos abs = helper.absolutePos(at);
                    helper.assertTrue(helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, abs, Direction.UP) != null,
                            "the top reaches the buffer");
                    helper.assertTrue(helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, abs, Direction.NORTH) != null,
                            "so does a side");
                    helper.assertTrue(helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, abs, Direction.EAST) == null,
                            "the front carries only ComputerCraft's cable");
                    helper.assertTrue(helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, abs, Direction.WEST) == null,
                            "the back carries only ours");
                    final var top = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, abs, Direction.UP);
                    helper.assertTrue(top.insertItem(0, new ItemStack(Items.IRON_INGOT, 3), false).isEmpty(),
                            "a stack goes in through the top");
                    helper.assertTrue(gatewayAt(helper, at).bufferUsed() == 1, "and shows in the buffer");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void gateway_isAComputerCraftPeripheralOnItsFrontFace(final GameTestHelper helper) {
        helper.assertTrue(ComputerCraftIntegration.isLoaded(), "CC: Tweaked is loaded in the dev runs");
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        world.placeRunningPersonalComputer(COMPUTER);
        final BlockPos at = new BlockPos(2, 2, 2);
        helper.setBlock(at, gateway(Direction.EAST));
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 4, () -> {
                    final BlockPos abs = helper.absolutePos(at);
                    final IPeripheral front = helper.getLevel().getCapability(PeripheralCapability.get(), abs, Direction.EAST);
                    helper.assertTrue(front != null && "jsc_gateway".equals(front.getType()),
                            "the front face is a jsc_gateway peripheral; got " + (front == null ? "nothing" : front.getType()));
                    helper.assertTrue(helper.getLevel().getCapability(PeripheralCapability.get(), abs, Direction.WEST) == null,
                            "the back face is not");
                    helper.assertTrue(helper.getLevel().getCapability(PeripheralCapability.get(), abs, Direction.UP) == null,
                            "nor the top");
                    final WiredElement node = helper.getLevel().getCapability(WiredElementCapability.get(), abs, Direction.EAST);
                    helper.assertTrue(node != null, "the front joins ComputerCraft's wired network");
                    helper.assertTrue(node.getNode() != null, "with a node of its own");
                    final NetworkGatewayBlockEntity g = gatewayAt(helper, at);
                    helper.assertTrue(g.bridge() != null, "the block entity holds its ComputerCraft side");
                    helper.assertTrue("jsc_gateway_gateway_1".equals(g.peripheralName()),
                            "published under its name; got " + g.peripheralName());
                    helper.assertTrue(node.getSenderID().equals(g.peripheralName()), "which the node also answers to");
                })
                .thenSucceed();
    }
}
