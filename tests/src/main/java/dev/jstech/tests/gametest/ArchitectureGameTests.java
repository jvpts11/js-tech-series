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
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.machine.MachinePrograms;
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
 * A program is instructions for a processor, so the processor a machine has decides what it will run.
 *
 * <p>The rule is the real one: the 64-bit machines run what was built for the 32-bit ones and not the other way
 * about, and the earliest machines run only their own. These put a listing in front of a machine of each era and
 * check what happens, because the refusal has to reach the person at the terminal, with the two architectures named.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ArchitectureGameTests {

    private static final String ARENA = "empty";

    private static final BlockPos WHERE = new BlockPos(2, 2, 2);

    private ArchitectureGameTests() {
    }

    @GameTest(template = ARENA)
    public static void legacyMachine_refusesAProgramBuiltForTheNewerArchitecture(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = legacy(helper);
        if (computer == null) {
            return;
        }
        final MachinePrograms.Started started = run(computer, "jsc:x86_64");
        helper.assertFalse(started.ok(), "a 32-bit machine cannot run what was built for the 64-bit one");
        helper.assertTrue(started.message().contains("A4015"), "it says which refusal it is: " + started.message());
        helper.assertTrue(started.message().contains("built for x86-64; this machine is x86"),
                "it names both architectures: " + started.message());
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void legacyMachine_runsAProgramBuiltForItsOwn(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = legacy(helper);
        if (computer == null) {
            return;
        }
        final MachinePrograms.Started started = run(computer, "jsc:x86");
        helper.assertTrue(started.ok(), "a machine runs its own programs: " + started.message());
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void standardMachine_runsAProgramBuiltForTheOlderArchitecture(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = standard(helper);
        if (computer == null) {
            return;
        }
        final MachinePrograms.Started started = run(computer, "jsc:x86");
        helper.assertTrue(started.ok(),
                "a 64-bit machine keeps running what was written for the older ones: " + started.message());
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void standardMachine_refusesAnArchitectureNoModBrought(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = standard(helper);
        if (computer == null) {
            return;
        }
        final MachinePrograms.Started started = run(computer, "other:risc");
        helper.assertFalse(started.ok(), "a machine runs no architecture it has never heard of");
        helper.assertTrue(started.message().contains("built for other:risc"),
                "with nothing registered under that name, the name itself is what it can say: " + started.message());
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void vintageMachine_refusesTheLanguagesOwnListings(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = vintage(helper);
        if (computer == null) {
            return;
        }
        final MachinePrograms.Started started = run(computer, "jsc:x86");
        helper.assertFalse(started.ok(), "the earliest machines run only their own, which is what dates them");
        helper.assertTrue(started.message().contains("this machine is x86-16"),
                "it says what the machine is: " + started.message());
        helper.succeed();
    }

    /** A listing that does nothing, built for that architecture. */
    private static String listing(final String architecture) {
        return ".asm 3\n.arch " + architecture + "\n.start Programs.Quiet console\n\n.class Programs.Quiet\n\n"
                + ".method static void Main() slots 0\n    ret\n";
    }

    private static MachinePrograms.Started run(final PersonalComputerBlockEntity computer, final String architecture) {
        return computer.programs().start("quiet.asm", listing(architecture), 1, computer);
    }

    private static PersonalComputerBlockEntity vintage(final GameTestHelper helper) {
        return assemble(helper, ComputingModule.VINTAGE_PERSONAL_COMPUTER.get(),
                HardwareItems.MOTHERBOARD_BABYAT_VINTAGE.get(), HardwareItems.CPU_INTEGRA_486SX.get(),
                HardwareItems.RAM_SIMM_4.get(), HardwareItems.PSU_300B.get());
    }

    private static PersonalComputerBlockEntity legacy(final GameTestHelper helper) {
        return assemble(helper, ComputingModule.LEGACY_PERSONAL_COMPUTER.get(),
                HardwareItems.MOTHERBOARD_ATX_LEGACY_LGA775.get(), HardwareItems.CPU_INTEGRA_DUO_E4300.get(),
                HardwareItems.RAM_DDR2_2048.get(), HardwareItems.PSU_500B.get());
    }

    private static PersonalComputerBlockEntity standard(final GameTestHelper helper) {
        return assemble(helper, ComputingModule.PERSONAL_COMPUTER.get(),
                ComputingModule.MOTHERBOARD_ATX_P.get(), ComputingModule.CPU_ASCENT_965.get(),
                ComputingModule.RAM_DDR3_8192.get(), ComputingModule.PSU_650G.get());
    }

    /*
     * The computer is only assembled, not powered on: what is being tested is the processor a listing is held
     * against, and that is read off the parts in the slots.
     */
    private static PersonalComputerBlockEntity assemble(final GameTestHelper helper, final Block computer,
                                                        final Item board, final Item cpu, final Item ram,
                                                        final Item psu) {
        helper.setBlock(WHERE, computer);
        if (!(helper.getBlockEntity(WHERE) instanceof PersonalComputerBlockEntity assembled)) {
            helper.fail("no personal computer at " + WHERE);
            return null;
        }
        final ItemStackHandler hardware = assembled.getHardware();
        hardware.setStackInSlot(PersonalComputerBlockEntity.MOTHERBOARD_SLOT, new ItemStack(board));
        hardware.setStackInSlot(PersonalComputerBlockEntity.CPU_SLOT, new ItemStack(cpu));
        hardware.setStackInSlot(PersonalComputerBlockEntity.RAM_SLOTS_START, new ItemStack(ram));
        hardware.setStackInSlot(PersonalComputerBlockEntity.PSU_SLOT, new ItemStack(psu));
        return assembled;
    }
}
