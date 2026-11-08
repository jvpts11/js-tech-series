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
import dev.jstech.computers.blockentity.AbstractSmallComputerBlockEntity;
import dev.jstech.computers.blockentity.ClusterManagementComputerBlockEntity;
import dev.jstech.computers.blockentity.ComputerHardwareLayout;
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.hardware.ComputerBuild;
import dev.jstech.tests.JsTests;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * A two-socket workstation board makes a Personal or Cluster Management Computer count two processors and eight
 * memory modules, as real two-socket workstations do; a board of one socket keeps the first row, however many memory
 * slots it has; and the Crafting Computer takes no workstation board.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class TwoSocketBoardGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos WHERE = new BlockPos(2, 2, 2);
    private static final int ROW = 4;

    private TwoSocketBoardGameTests() {
    }

    @GameTest(template = ARENA)
    public static void twoSocketBoard_countsTwoProcessorsAndEightModules(final GameTestHelper helper) {
        final AbstractSmallComputerBlockEntity pc = place(helper, ComputingModule.PERSONAL_COMPUTER.get());
        fill(pc, HardwareItems.MOTHERBOARD_EATX_STANDARD_DUAL_2011.get(), HardwareItems.CPU_INTEGRA_SERVO_2609.get(),
                HardwareItems.RAM_DDR3_4096.get());
        final ComputerBuild build = pc.currentBuild();
        helper.assertTrue(build != null && build.cpus().size() == 2 && build.rams().size() == 2 * ROW,
                "a two-socket board counts both processors and all eight modules: " + describe(build));
        helper.assertTrue(pc.boardCpuSlots() == 2 && pc.boardRamSlots() == 2 * ROW, "and opens their slots");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void oneSocketBoard_keepsTheFirstRowWhateverItsSlots(final GameTestHelper helper) {
        final AbstractSmallComputerBlockEntity pc = place(helper, ComputingModule.PERSONAL_COMPUTER.get());
        // The one-socket workstation board has eight memory slots, but its one processor opens only the first four.
        fill(pc, HardwareItems.MOTHERBOARD_EATX_STANDARD_2011.get(), HardwareItems.CPU_INTEGRA_SERVO_2609.get(),
                HardwareItems.RAM_DDR3_4096.get());
        final ComputerBuild build = pc.currentBuild();
        helper.assertTrue(build != null && build.cpus().size() == 1 && build.rams().size() == ROW,
                "a one-socket board counts one processor and four modules: " + describe(build));
        helper.assertTrue(pc.boardCpuSlots() == 1 && pc.boardRamSlots() == ROW, "and leaves the second row shut");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void legacyClusterManager_countsTheTwoOfItsServerBoard(final GameTestHelper helper) {
        final AbstractSmallComputerBlockEntity cmc = place(helper,
                ComputingModule.LEGACY_CLUSTER_MANAGEMENT_COMPUTER.get());
        fill(cmc, HardwareItems.MOTHERBOARD_EATX_LEGACY_604.get(), HardwareItems.CPU_INTEGRA_SERVO_2800.get(),
                HardwareItems.RAM_DDR_1024.get());
        final ComputerBuild build = cmc.currentBuild();
        helper.assertTrue(build != null && build.cpus().size() == 2 && build.rams().size() == 2 * ROW,
                "a Legacy Cluster Management Computer on two Socket 604s counts both: " + describe(build));
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void secondRow_sitsAfterTheBays(final GameTestHelper helper) {
        final ComputerHardwareLayout pc = place(helper, ComputingModule.PERSONAL_COMPUTER.get()).hardwareLayout();
        helper.assertTrue(pc.cpuSlot(0) == PersonalComputerBlockEntity.CPU_SLOT
                && pc.ramSlot(0) == PersonalComputerBlockEntity.RAM_SLOTS_START
                && pc.cpuSlot(1) == PersonalComputerBlockEntity.SECOND_CPU_SLOT
                && pc.ramSlot(ROW) == PersonalComputerBlockEntity.SECOND_RAM_SLOTS_START
                && pc.totalSlots() == PersonalComputerBlockEntity.HARDWARE_SLOTS,
                "the first row keeps its slots and the second comes after the bays: " + pc);
        helper.assertTrue(ClusterManagementComputerBlockEntity.PARTS.processors() == 2
                && ClusterManagementComputerBlockEntity.PARTS.memoryPerProcessor() == ROW,
                "the Cluster Management Computer's case seats its memory by processor too");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void craftingComputer_takesNoWorkstationBoard(final GameTestHelper helper) {
        final AbstractSmallComputerBlockEntity crafting = place(helper, ComputingModule.CRAFTING_COMPUTER.get());
        helper.assertFalse(crafting.isValidForSlot(crafting.hardwareLayout().motherboardSlot(),
                new ItemStack(HardwareItems.MOTHERBOARD_EATX_STANDARD_DUAL_2011.get())),
                "the Crafting Computer takes no EATX board");
        helper.assertTrue(CraftingComputerBlockEntity.PARTS.processors() == 1, "and seats one processor");
        helper.succeed();
    }

    private static AbstractSmallComputerBlockEntity place(final GameTestHelper helper, final Block block) {
        helper.setBlock(WHERE, block);
        if (!(helper.getBlockEntity(WHERE) instanceof AbstractSmallComputerBlockEntity computer)) {
            throw new IllegalStateException("no small computer at " + WHERE);
        }
        return computer;
    }

    /* The board, a processor in every processor slot, a module in every memory slot, and a supply. */
    private static void fill(final AbstractSmallComputerBlockEntity computer, final Item board, final Item cpu,
                             final Item ram) {
        final ComputerHardwareLayout layout = computer.hardwareLayout();
        final ItemStackHandler hardware = computer.getHardware();
        hardware.setStackInSlot(layout.motherboardSlot(), new ItemStack(board));
        for (int i = 0; i < layout.cpuCount(); i++) {
            hardware.setStackInSlot(layout.cpuSlot(i), new ItemStack(cpu));
        }
        for (int i = 0; i < layout.ramCount(); i++) {
            hardware.setStackInSlot(layout.ramSlot(i), new ItemStack(ram));
        }
        hardware.setStackInSlot(layout.psuSlot(), new ItemStack(HardwareItems.PSU_500B.get()));
    }

    private static String describe(final ComputerBuild build) {
        return build == null ? "no build" : build.cpus().size() + " processors, " + build.rams().size() + " modules";
    }
}
