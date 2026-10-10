/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.testkit;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.block.MainframeBlock;
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.program.IqlEngine;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.core.persistence.NetworkRegistry;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.core.uuid.NetworkUuidState;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * The placing, forming and inspecting helpers the data network GameTests share: running Mainframes and
 * Personal Computers, seeded racks and storage networks, monitors, and the network registry lookups.
 */
public final class NetworkFixtures {

    private NetworkFixtures() {
    }

    public static int countIn(final ItemStackHandler handler, final net.minecraft.world.item.Item item) {
        int total = 0;
        for (int i = 0; i < handler.getSlots(); i++) {
            if (handler.getStackInSlot(i).getItem() == item) {
                total += handler.getStackInSlot(i).getCount();
            }
        }
        return total;
    }

    /**
     * Places the given Personal Computer block, verifies its block entity and menu construction, then
     * checks the very test the server runs each tick to decide whether to keep the GUI open:
     * {@link net.minecraft.world.inventory.AbstractContainerMenu#stillValid}. A menu whose
     * {@code stillValid} returns false is closed by the server on the next tick, which is exactly the
     * "the GUI never opens" symptom for the player.
     */
    public static void assertPcMenuOpens(final GameTestHelper helper,
                                          final net.minecraft.world.level.block.Block block,
                                          final net.minecraft.world.item.Item blockItem) {
        final BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, block);

        /*
         * The block entity must be created for the era blocks too, or useWithoutItem's instanceof
         * check fails silently and no menu ever opens.
         */
        if (!(helper.getBlockEntity(pos) instanceof PersonalComputerBlockEntity be)) {
            helper.fail("no PersonalComputerBlockEntity at " + pos + " for block " + block + " ("
                    + blockItem + ")");
            return;
        }

        /*
         * A plain mock player (no networking, so opening menus broadcasts nothing) standing on the
         * block, well inside the interaction reach stillValid also checks.
         */
        final net.minecraft.world.entity.player.Player player =
                helper.makeMockPlayer(net.minecraft.world.level.GameType.CREATIVE);
        final BlockPos absolute = helper.absolutePos(pos);
        player.setPos(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5);

        // Menu construction must succeed for an era PC: it reads the shared block entity.
        final dev.jstech.computers.menu.PersonalComputerMenu menu =
                new dev.jstech.computers.menu.PersonalComputerMenu(
                        1, player.getInventory(), be);
        helper.assertFalse(menu.slots.isEmpty(), "the PC menu must build its slots for " + block);

        // The block's own name must resolve without throwing (useWithoutItem uses it as the title).
        helper.assertTrue(block.getName() != null, "block name must resolve for " + block);

        /*
         * The decisive check: the server validates the open menu against the block at the position
         * every tick. If stillValid is false the menu is closed at once, so the player never sees it.
         */
        helper.assertTrue(menu.stillValid(player),
                "the Personal Computer menu must stay valid for " + block
                        + "; a menu that is not stillValid is closed immediately, so the GUI never opens");
        helper.succeed();
    }

    /** A statement run through the network's door, the way a machine of the network runs one. */
    public static IqlEngine.Outcome runIql(final MainframeBlockEntity mainframe, final ICliComputer machine,
                                            final String statement) {
        return mainframe.networkOperations().query(IqlEngine.viewOf(machine), statement, 64);
    }

    public static MainframeBlockEntity placeRunningMainframe(final GameTestHelper helper, final BlockPos relative) {
        return TestWorldBuilder.forGameTest(helper).placeRunningMainframe(relative);
    }

    public static MainframeBlockEntity formRunningMainframe(final GameTestHelper helper,
                                                            final BlockPos controller, final Direction facing) {
        helper.setBlock(controller, ComputingModule.MAINFRAME.get().defaultBlockState()
                .setValue(MainframeBlock.FACING, facing));
        ((MainframeBlock) ComputingModule.MAINFRAME.get()).setPlacedBy(
                helper.getLevel(), helper.absolutePos(controller),
                helper.getBlockState(controller), null, ItemStack.EMPTY);
        final MainframeBlockEntity be = mainframeAt(helper, controller);
        installValidBuild(be);
        be.togglePower();
        return be;
    }

    public static MainframeBlockEntity mainframeAt(final GameTestHelper helper, final BlockPos relative) {
        if (helper.getBlockEntity(relative) instanceof MainframeBlockEntity be) {
            return be;
        }
        throw new IllegalStateException("no mainframe at " + relative);
    }

    public static MainframeBlockEntity storageNetwork(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = placeRunningMainframe(helper, new BlockPos(1, 2, 2));
        TestCables.lay(helper, new BlockPos(2, 2, 2), ComputingModule.HBW_CABLE);
        helper.setBlock(new BlockPos(3, 2, 2), ComputingModule.SERVER_RACK.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING,
                        Direction.EAST)); // cables attach through the rear (west side here)
        seedServer(helper, new BlockPos(3, 2, 2));
        return mainframe;
    }

    public static ServerRackBlockEntity seededRack(final GameTestHelper helper) {
        if (helper.getBlockEntity(new BlockPos(3, 2, 2)) instanceof ServerRackBlockEntity rack) {
            return rack;
        }
        throw new IllegalStateException("no rack at (3,2,2)");
    }

    public static ItemStackHandler fullHandler() {
        final ItemStackHandler handler = new ItemStackHandler(1);
        handler.setStackInSlot(0, new ItemStack(Items.STICK, 64));
        return handler;
    }

    public static dev.jstech.computers.storage.ExternalDataPort port(
            final ItemStackHandler handler) {
        return new dev.jstech.computers.storage.ExternalDataPort(handler, null);
    }

    public static void seedServer(final GameTestHelper helper, final BlockPos rack) {
        TestWorldBuilder.forGameTest(helper).seedServer(rack);
    }

    /**
     * Turns a just-placed computer so its rear (its only data port) meets an adjacent horizontal
     * cable. Computers now connect through the back face alone, so a test that drops one beside a
     * cable must orient it; this keeps the fixtures declaring "computer next to cable" working.
     */
    public static void faceRearTowardCable(final GameTestHelper helper, final BlockPos pos) {
        TestWorldBuilder.forGameTest(helper).faceRearTowardCable(pos);
    }

    public static PersonalComputerBlockEntity placeRunningPC(final GameTestHelper helper, final BlockPos relative) {
        return TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(relative);
    }

    /* A Standard monitor whose front looks {@code facing}, its port on the back. */
    public static BlockState monitorFacing(final Direction facing) {
        return ComputingModule.MONITOR.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, facing);
    }

    public static CraftingComputerBlockEntity placeRunningCraftingComputer(
            final GameTestHelper helper, final BlockPos relative) {
        return TestWorldBuilder.forGameTest(helper).placeRunningCraftingComputer(relative);
    }

    public static void installValidBuild(final MainframeBlockEntity be) {
        TestWorldBuilder.installMainframeBuild(be);
    }

    public static int droppedItems(final GameTestHelper helper, final BlockPos around) {
        final net.minecraft.world.phys.AABB box =
                new net.minecraft.world.phys.AABB(helper.absolutePos(around)).inflate(6.0);
        return helper.getLevel().getEntitiesOfClass(
                net.minecraft.world.entity.item.ItemEntity.class, box).size();
    }

    public static Optional<NetworkUuid> networkOf(final GameTestHelper helper, final BlockPos relative) {
        return TestCables.network(helper, relative);
    }

    public static boolean sameNetwork(final GameTestHelper helper, final BlockPos a, final BlockPos b) {
        return TestCables.joined(helper, a, b);
    }

    public static NetworkUuidState registryState(final GameTestHelper helper, final NetworkUuid uuid) {
        return NetworkRegistry.networkState(helper.getLevel(), uuid);
    }
}
