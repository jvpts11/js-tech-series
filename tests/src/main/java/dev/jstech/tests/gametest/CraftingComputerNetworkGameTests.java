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
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.core.network.NetworkSystem;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestCables;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

import static dev.jstech.tests.testkit.NetworkFixtures.placeRunningCraftingComputer;
import static dev.jstech.tests.testkit.NetworkFixtures.placeRunningMainframe;

/**
 * GameTests for the Crafting Computer as a node of the data network: assembly, the Crafting Card it needs,
 * and joining the Mainframe network.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class CraftingComputerNetworkGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;

    private CraftingComputerNetworkGameTests() {
    }

    @GameTest(template = ARENA)
    public static void craftingComputer_assemblesAndCanCraft(final GameTestHelper helper) {
        final BlockPos cc = new BlockPos(2, 2, 2);
        final CraftingComputerBlockEntity computer = placeRunningCraftingComputer(helper, cc);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(computer.buildValid(), "ATX build with a Crafting Card should be valid");
                    helper.assertTrue(computer.isRunning(), "Crafting Computer should be running after power-on");
                    helper.assertTrue(computer.craftingCardFactor() > 0.0,
                            "an installed Crafting Card gives a non-zero factor");
                    helper.assertTrue(computer.craftingThroughput() > 0,
                            "a running Crafting Computer reports crafting throughput");
                    helper.assertTrue(computer.canCraft(), "a powered Crafting Computer with a card can craft");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void craftingComputer_withoutCardCannotCraft(final GameTestHelper helper) {
        final BlockPos cc = new BlockPos(2, 2, 2);
        helper.setBlock(cc, ComputingModule.CRAFTING_COMPUTER.get());
        if (!(helper.getBlockEntity(cc) instanceof CraftingComputerBlockEntity computer)) {
            throw new IllegalStateException("no crafting computer at " + cc);
        }
        final ItemStackHandler hw = computer.getHardware();
        hw.setStackInSlot(CraftingComputerBlockEntity.MOTHERBOARD_SLOT,
                new ItemStack(HardwareItems.MOTHERBOARD_ATX_STANDARD_LGA1150.get()));
        hw.setStackInSlot(CraftingComputerBlockEntity.CPU_SLOT,
                new ItemStack(HardwareItems.CPU_INTEGRA_CENTRO_C7_4790K.get()));
        hw.setStackInSlot(CraftingComputerBlockEntity.RAM_SLOTS_START,
                new ItemStack(ComputingModule.RAM_DDR3_8192.get()));
        hw.setStackInSlot(CraftingComputerBlockEntity.PSU_SLOT,
                new ItemStack(ComputingModule.PSU_650G.get()));
        computer.togglePower();
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(computer.isRunning(), "the computer still powers on without a card");
                    helper.assertTrue(computer.craftingCardFactor() == 0.0,
                            "no Crafting Card means a zero crafting factor");
                    helper.assertFalse(computer.canCraft(), "without a card the computer cannot craft");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void craftingComputer_joinsMainframeNetworkAsNode(final GameTestHelper helper) {
        final BlockPos m = new BlockPos(1, 2, 2);
        final BlockPos hbw = new BlockPos(2, 2, 2);
        final BlockPos router = new BlockPos(3, 2, 2);
        final BlockPos eth = new BlockPos(4, 2, 2);
        final BlockPos cc = new BlockPos(5, 2, 2);
        final MainframeBlockEntity mainframe = placeRunningMainframe(helper, m);
        TestCables.lay(helper, hbw, ComputingModule.HBW_CABLE);
        helper.setBlock(router, ComputingModule.PERSONAL_ROUTER.get());
        TestCables.lay(helper, eth, ComputingModule.ETHERNET_CABLE);
        final CraftingComputerBlockEntity computer = placeRunningCraftingComputer(helper, cc);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    helper.assertTrue(computer.networkUuid() != null, "Crafting Computer should be on a network");
                    helper.assertTrue(mainframe.networkUuid() != null, "mainframe should own a network");
                    helper.assertTrue(computer.networkUuid().equals(mainframe.networkUuid()),
                            "Crafting Computer must share the mainframe's network through the router");
                    helper.assertTrue(NetworkSystem.get(helper.getLevel())
                                    .craftingComputersOf(mainframe.networkUuid()).size() == 1,
                            "the Crafting Computer registers as a network node");
                })
                .thenExecute(() -> helper.destroyBlock(cc))
                .thenExecuteAfter(SETTLE, () -> helper.assertTrue(
                        NetworkSystem.get(helper.getLevel())
                                .craftingComputersOf(mainframe.networkUuid()).isEmpty(),
                        "breaking the Crafting Computer unregisters its node"))
                .thenSucceed();
    }
}
