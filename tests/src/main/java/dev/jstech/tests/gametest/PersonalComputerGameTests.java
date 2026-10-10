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
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestCables;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

import static dev.jstech.tests.testkit.NetworkFixtures.assertPcMenuOpens;
import static dev.jstech.tests.testkit.NetworkFixtures.countIn;
import static dev.jstech.tests.testkit.NetworkFixtures.placeRunningMainframe;
import static dev.jstech.tests.testkit.NetworkFixtures.placeRunningPC;
import static dev.jstech.tests.testkit.NetworkFixtures.port;

/**
 * GameTests for the Personal Computer: assembly, passive network membership through a router, private
 * storage, and the menu opening for each era.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class PersonalComputerGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;

    private PersonalComputerGameTests() {
    }

    @GameTest(template = ARENA)
    public static void personalComputer_assemblesAndPowers(final GameTestHelper helper) {
        final BlockPos pc = new BlockPos(2, 2, 2);
        final PersonalComputerBlockEntity computer = placeRunningPC(helper, pc);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(computer.buildValid(), "ATX build should be valid");
                    helper.assertTrue(computer.isRunning(), "PC should be running after power-on");
                    helper.assertTrue(computer.capacity() > 0, "running PC reports capacity");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void personalComputer_joinsMainframeNetworkThroughRouter(final GameTestHelper helper) {
        final BlockPos m = new BlockPos(1, 2, 2);
        final BlockPos hbw = new BlockPos(2, 2, 2);
        final BlockPos router = new BlockPos(3, 2, 2);
        final BlockPos eth = new BlockPos(4, 2, 2);
        final BlockPos pc = new BlockPos(5, 2, 2);
        final MainframeBlockEntity mainframe = placeRunningMainframe(helper, m);
        TestCables.lay(helper, hbw, ComputingModule.HBW_CABLE);
        helper.setBlock(router, ComputingModule.PERSONAL_ROUTER.get());
        TestCables.lay(helper, eth, ComputingModule.ETHERNET_CABLE);
        final PersonalComputerBlockEntity computer = placeRunningPC(helper, pc);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    helper.assertTrue(computer.networkUuid() != null, "PC should be on a network");
                    helper.assertTrue(mainframe.networkUuid() != null, "mainframe should own a network");
                    helper.assertTrue(computer.networkUuid().equals(mainframe.networkUuid()),
                            "PC must share the mainframe's network through the router");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void pcPrivateStorage_isInvisibleUntilPublished(final GameTestHelper helper) {
        final BlockPos m = new BlockPos(1, 2, 2);
        final BlockPos hbw = new BlockPos(2, 2, 2);
        final BlockPos router = new BlockPos(3, 2, 2);
        final BlockPos eth = new BlockPos(4, 2, 2);
        final BlockPos pcPos = new BlockPos(5, 2, 2);
        final MainframeBlockEntity mainframe = placeRunningMainframe(helper, m);
        TestCables.lay(helper, hbw, ComputingModule.HBW_CABLE);
        helper.setBlock(router, ComputingModule.PERSONAL_ROUTER.get());
        TestCables.lay(helper, eth, ComputingModule.ETHERNET_CABLE);
        final PersonalComputerBlockEntity pc = placeRunningPC(helper, pcPos);
        /*
         * Install the smallest disk (2000-item capacity) and seed it with 100 cobblestone, all private
         * by default (0 permille). Public storage is a capacity-fraction budget: a permille of 15 on a
         * 2000-item disk publishes a 30-item budget, so 30 of the 100 become public, 70 stay private.
         */
        pc.getHardware().setStackInSlot(PersonalComputerBlockEntity.DISK_SLOTS_START,
                new ItemStack(ComputingModule.disk(StorageTier.NVME, DiskSize.GB_500)));
        final java.util.function.IntConsumer publish = permille ->
                pc.setDiskPrivacy(0, permille);
        final ItemStackHandler dest = new ItemStackHandler(9);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    helper.assertTrue(pc.networkUuid() != null, "the PC should be on the network");
                    helper.assertTrue(pc.networkUuid().equals(mainframe.networkUuid()),
                            "the PC shares the mainframe network");
                    pc.localStore().insert(StorageKey.of(Items.COBBLESTONE), 100);
                })
                // Default-private: the network must not see any of the PC's storage.
                .thenExecuteAfter(4, () -> {
                    helper.assertTrue(mainframe.networkIndex().available(Items.COBBLESTONE) == 0,
                            "a default-private PC disk must be invisible to the index; got "
                                    + mainframe.networkIndex().available(Items.COBBLESTONE));
                    final var op = mainframe.submitNetworkSelect(Items.COBBLESTONE, 100, port(dest), "test");
                    helper.assertTrue(op == null || op.isDone(),
                            "a SELECT against an all-private PC finds nothing to pull");
                })
                .thenExecuteAfter(4, () -> helper.assertTrue(countIn(dest, Items.COBBLESTONE) == 0,
                        "nothing should have moved while the PC is fully private"))
                /*
                 * Publish a 30-item budget (15 permille of the 2000-item disk): 30 of the 100 become
                 * public; the other 70 stay private.
                 */
                .thenExecute(() -> publish.accept(15))
                .thenExecuteAfter(4, () -> {
                    helper.assertTrue(mainframe.networkIndex().available(Items.COBBLESTONE) == 30,
                            "publishing a 30-item budget exposes 30 of the 100 to the index; got "
                                    + mainframe.networkIndex().available(Items.COBBLESTONE));
                    mainframe.submitNetworkSelect(Items.COBBLESTONE, 100, port(dest), "test");
                })
                // A SELECT for 100 pulls only the 30 public; the 70 private remain on the PC's disk.
                .thenExecuteAfter(30, () -> {
                    helper.assertTrue(countIn(dest, Items.COBBLESTONE) == 30,
                            "only the published 30 are SELECT-able; got " + countIn(dest, Items.COBBLESTONE));
                    helper.assertTrue(pc.localStore().count(StorageKey.of(Items.COBBLESTONE)) == 70,
                            "the private remainder stays on the PC; got "
                                    + pc.localStore().count(StorageKey.of(Items.COBBLESTONE)));
                })
                /*
                 * The public budget is a standing ceiling: 30 of the remaining 70 are public again, so
                 * a repeat pull yields another 30 and never reaches the private floor.
                 */
                .thenExecuteAfter(4, () -> helper.assertTrue(
                        mainframe.networkIndex().available(Items.COBBLESTONE) == 30,
                        "the budget re-exposes 30 of the remaining 70; got "
                                + mainframe.networkIndex().available(Items.COBBLESTONE)))
                // Fully private again: no part of the PC is visible, the whole 70 is protected.
                .thenExecute(() -> publish.accept(0))
                .thenExecuteAfter(4, () -> helper.assertTrue(
                        mainframe.networkIndex().available(Items.COBBLESTONE) == 0,
                        "setting the disk back to private hides all of it again; got "
                                + mainframe.networkIndex().available(Items.COBBLESTONE)))
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void personalComputer_ignoresHbwCable(final GameTestHelper helper) {
        final BlockPos m = new BlockPos(1, 2, 2);
        final BlockPos hbw = new BlockPos(2, 2, 2);
        final BlockPos pc = new BlockPos(3, 2, 2); // PC directly against an HBW cable
        final MainframeBlockEntity mainframe = placeRunningMainframe(helper, m);
        TestCables.lay(helper, hbw, ComputingModule.HBW_CABLE);
        final PersonalComputerBlockEntity computer = placeRunningPC(helper, pc);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    helper.assertTrue(mainframe.networkUuid() != null, "mainframe owns its network");
                    helper.assertTrue(computer.networkUuid() == null,
                            "a PC must not join via an HBW cable (Ethernet only)");
                })
                .thenSucceed();
    }

    /*
     * Era Personal Computers (Vintage / Legacy): the right-click assembly GUI must open exactly
     * like the Standard one. The three blocks share PersonalComputerBlockEntity, the menu and the
     * screen; the era blocks only override era() and codec(). These tests open the menu through the
     * real interaction path and verify the server keeps it open (stillValid), which is what actually
     * decides whether the player sees the GUI.
     */

    @GameTest(template = ARENA)
    public static void standardPc_rightClickOpensAndKeepsMenu(final GameTestHelper helper) {
        assertPcMenuOpens(helper, ComputingModule.PERSONAL_COMPUTER.get(),
                ComputingModule.PERSONAL_COMPUTER.item());
    }

    @GameTest(template = ARENA)
    public static void vintagePc_rightClickOpensAndKeepsMenu(final GameTestHelper helper) {
        assertPcMenuOpens(helper, ComputingModule.VINTAGE_PERSONAL_COMPUTER.get(),
                ComputingModule.VINTAGE_PERSONAL_COMPUTER.item());
    }

    @GameTest(template = ARENA)
    public static void legacyPc_rightClickOpensAndKeepsMenu(final GameTestHelper helper) {
        assertPcMenuOpens(helper, ComputingModule.LEGACY_PERSONAL_COMPUTER.get(),
                ComputingModule.LEGACY_PERSONAL_COMPUTER.item());
    }
}
