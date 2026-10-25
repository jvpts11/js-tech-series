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
import dev.jstech.computers.blockentity.ComputerHardwareLayout;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.blockentity.SmallComputerBlockEntity;
import dev.jstech.computers.hardware.CaseBays;
import dev.jstech.computers.hardware.CasePorts;
import dev.jstech.computers.hardware.ComputerDef;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.EraStorage;
import dev.jstech.computers.hardware.MachinePorts;
import dev.jstech.computers.hardware.RamGeneration;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.os.HostScope;
import dev.jstech.computers.registry.ComputerRegistry;
import dev.jstech.computers.registry.HardwareRegistry;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.jstechseries.tier.SeriesAxes;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestHardware;
import dev.jstech.tests.TestItems;
import dev.jstech.tests.TestProgression;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Hardware another mod brings, through the same register event this mod's own goes through: the test mod's Quantum
 * era has a processor, a board and memory of a generation of its own, and a small computer of a machine of its own,
 * whose case counts fewer parts than a Personal Computer's. The computer is assembled and switched on; the era's
 * tables answer for it with the Advanced's values; and nothing without an entry goes in any part slot.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class QuantumComputerGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos COMPUTER = new BlockPos(2, 2, 2);
    private static final int SETTLE = 4;

    private QuantumComputerGameTests() {
    }

    @GameTest(template = ARENA)
    public static void quantumHardware_isDeclaredThroughTheRegisterEvent(final GameTestHelper helper) {
        helper.assertTrue(HardwareRegistry.cpu(new ItemStack(TestHardware.QUANTUM_CPU.get()))
                == TestHardware.QUANTUM_CPU_SPEC, "the test mod's item is a processor because its entry says so");
        helper.assertTrue(HardwareRegistry.board(new ItemStack(TestHardware.QUANTUM_BOARD.get()))
                == TestHardware.QUANTUM_BOARD_SPEC, "and its board a motherboard");
        helper.assertTrue(HardwareRegistry.ram(new ItemStack(TestHardware.QUANTUM_RAM.get()))
                == TestHardware.QUANTUM_RAM_SPEC, "and its memory a module");
        helper.assertTrue(RamGeneration.byId("jstests:qram") == TestHardware.QRAM,
                "a memory generation of its own is declared beside this mod's");
        helper.assertTrue(HostScope.byId("jstests:quantum_lab") == TestHardware.QUANTUM_LAB,
                "and a machine of its own");
        final ComputerDef computer = ComputerRegistry.computer(TestHardware.QUANTUM_COMPUTER.get());
        helper.assertTrue(computer != null && computer.era() == TestProgression.QUANTUM
                && computer.machine() == TestHardware.QUANTUM_LAB, "its block is a computer of its era and machine");
        helper.assertTrue(ComputerRegistry.computer(ComputingModule.PERSONAL_COMPUTER.get()).machine()
                == HostScope.PERSONAL_COMPUTER, "this mod's own computers are entries too, declared the same way");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void quantumComputer_readsItsCaseFromItsEntry_assemblesAndSwitchesOn(final GameTestHelper helper) {
        final SmallComputerBlockEntity computer = place(helper);
        final ComputerHardwareLayout layout = computer.hardwareLayout();
        final ItemStackHandler hardware = computer.getHardware();
        helper.assertValueEqual(hardware.getSlots(), 9,
                "a board, a processor, two modules, two cards, a supply, a disk and a bay");
        helper.assertTrue(computer.machine() == TestHardware.QUANTUM_LAB, "the machine its entry names");
        helper.assertFalse(computer.isValidForSlot(layout.motherboardSlot(),
                new ItemStack(HardwareItems.MOTHERBOARD_ATX_STANDARD_LGA1150.get())),
                "a Standard board stays out of a Quantum case, though both are ATX");
        hardware.setStackInSlot(layout.motherboardSlot(), new ItemStack(TestHardware.QUANTUM_BOARD.get()));
        helper.assertTrue(computer.isValidForSlot(layout.cpuStart(), new ItemStack(TestHardware.QUANTUM_CPU.get())),
                "its own processor seats on its own board");
        helper.assertFalse(computer.isValidForSlot(layout.cpuStart(),
                new ItemStack(HardwareItems.CPU_INTEGRA_CENTRO_C7_4790K.get())), "and a processor of another socket");
        helper.assertFalse(computer.isValidForSlot(layout.ramStart(),
                new ItemStack(ComputingModule.RAM_DDR3_8192.get())), "memory of a generation its board does not take");
        helper.assertFalse(computer.isValidForSlot(layout.pcieStart(),
                new ItemStack(ComputingModule.FURNACE_CARD.get())),
                "a personal-use card has no program on a machine that is no Personal Computer");
        hardware.setStackInSlot(layout.cpuStart(), new ItemStack(TestHardware.QUANTUM_CPU.get()));
        hardware.setStackInSlot(layout.ramStart(), new ItemStack(TestHardware.QUANTUM_RAM.get()));
        hardware.setStackInSlot(layout.ramStart() + 1, new ItemStack(TestHardware.QUANTUM_RAM.get()));
        hardware.setStackInSlot(layout.psuSlot(), new ItemStack(ComputingModule.PSU_650G.get()));
        hardware.setStackInSlot(layout.diskStart(), new ItemStack(ComputingModule.disk(StorageTier.SSD,
                DiskSize.GB_500)));
        computer.setPowered(true);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(computer.buildValid(), "its parts make a computer");
                    helper.assertTrue(computer.isRunning(), "and it switches on");
                    helper.assertTrue(computer.displayEra() == TestProgression.QUANTUM, "a machine of its era");
                })
                .thenSucceed();
    }

    /** An era with no tables of its own takes the nearest earlier era's: the Advanced's, for the test era. */
    @GameTest(template = ARENA)
    public static void quantumEra_takesTheAdvancedValuesWhereItSaysNothing(final GameTestHelper helper) {
        final List<String> wrong = new ArrayList<>();
        if (CaseBays.of(TestProgression.QUANTUM) != CaseBays.of(HardwareEra.ADVANCED)) {
            wrong.add("the bays of its cases");
        }
        if (CasePorts.of(TestProgression.QUANTUM) != CasePorts.of(HardwareEra.ADVANCED)) {
            wrong.add("the ports on their fronts");
        }
        if (MachinePorts.devicePorts(TestProgression.QUANTUM) != MachinePorts.devicePorts(HardwareEra.ADVANCED)) {
            wrong.add("the device ports of its boards");
        }
        if (EraStorage.mbPerItem(TestProgression.QUANTUM) != EraStorage.mbPerItem(HardwareEra.ADVANCED)) {
            wrong.add("what an item costs on its disks");
        }
        if (HardwareEra.withParts(TestProgression.QUANTUM) != HardwareEra.ADVANCED) {
            wrong.add("the era whose parts it draws");
        }
        helper.assertTrue(wrong.isEmpty(), "the test era answers as the Advanced for " + wrong);
        helper.succeed();
    }

    /** Progress along the eras is saved by the step's name, so an era inserted before it moves nothing saved. */
    @GameTest(template = ARENA)
    public static void eraProgress_isSavedByName_soAnEraInsertedBeforeItKeepsIt(final GameTestHelper helper) {
        helper.assertTrue(HardwareEra.EXA.level() == HardwareEra.seriesIndex(HardwareEra.EXA) + 1,
                "the test era stands before Exa, one place further than its place in the series");
        helper.assertTrue(SeriesAxes.HARDWARE_ERA.savedAs(HardwareEra.EXA.serializedName()) == HardwareEra.EXA,
                "Exa saved by its name reads back as Exa");
        helper.assertTrue(SeriesAxes.HARDWARE_ERA.savedAs(TestProgression.QUANTUM.serializedName())
                == TestProgression.QUANTUM, "and so does the inserted era");
        helper.succeed();
    }

    /** What has no entry in the hardware registry goes in no part slot, of this mod's computers or another's. */
    @GameTest(template = ARENA)
    public static void anItemWithNoEntry_goesInNoPartSlot(final GameTestHelper helper) {
        final SmallComputerBlockEntity quantum = place(helper);
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper)
                .placeRunningPersonalComputer(new BlockPos(4, 2, 2));
        for (final ItemStack stack : List.of(new ItemStack(Items.DIAMOND), new ItemStack(TestItems.FULL.get()))) {
            for (int slot = 0; slot < quantum.getHardware().getSlots(); slot++) {
                helper.assertFalse(quantum.isValidForSlot(slot, stack), stack + " goes in slot " + slot
                        + " of the Quantum computer");
            }
            for (int slot = 0; slot < pc.getHardware().getSlots(); slot++) {
                helper.assertFalse(pc.isValidForSlot(slot, stack), stack + " goes in slot " + slot + " of the PC");
            }
        }
        helper.succeed();
    }

    private static SmallComputerBlockEntity place(final GameTestHelper helper) {
        helper.setBlock(COMPUTER, TestHardware.QUANTUM_COMPUTER.get());
        return (SmallComputerBlockEntity) helper.getBlockEntity(COMPUTER);
    }
}
