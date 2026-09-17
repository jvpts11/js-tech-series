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
import dev.jstech.computers.machine.IMachineRuntime;
import dev.jstech.computers.machine.MachineListing;
import dev.jstech.computers.machine.MachinePrograms;
import dev.jstech.computers.machine.SigmaLanguage;
import dev.jstech.core.language.IProgrammingLanguage;
import dev.jstech.tests.JsTests;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
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

    /** A second machine, for the tests that need two of different ages standing at once. */
    private static final BlockPos ELSEWHERE = new BlockPos(4, 2, 2);

    /** Room enough for a program that does nothing. */
    private static final long ROOM = 1024L * 1024L;

    /** A program that starts and is done, so the test is about where it runs and nothing else. */
    private static final String QUIET = """
            using System.IO.*;
            namespace Programs;
            class Quiet {
                static void Main() { Console.PrintLine("quiet"); }
            }
            """;

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

    /**
     * A program built for the first machines runs on every machine that came after them.
     *
     * <p>This is the half that makes the smaller language worth having. It is written for computers that could
     * not hold the full one, and if what it built ran only there it would be a language for one age rather than
     * the floor of the family: the same listing has to be taken by a machine of each age, exactly as a 386 took
     * what an 8086 ran.
     */
    @GameTest(template = ARENA)
    public static void aProgramBuiltForTheFirstMachines_runsOnEveryMachineAfterThem(final GameTestHelper helper) {
        final PersonalComputerBlockEntity oldest = vintage(helper);
        final PersonalComputerBlockEntity later = legacy(helper, ELSEWHERE);
        if (oldest == null || later == null) {
            return;
        }
        final MachinePrograms.Started onTheOldest = run(oldest, "jsc:x86_16");
        helper.assertTrue(onTheOldest.ok(), "the machine it was built for runs it: " + onTheOldest.message());
        final MachinePrograms.Started onALaterOne = run(later, "jsc:x86_16");
        helper.assertTrue(onALaterOne.ok(),
                "and so does one of the age after: " + onALaterOne.message());
        helper.succeed();
    }

    /**
     * The whole way through: source compiled for the 64-bit machines runs on one and is refused by a 32-bit one.
     *
     * <p>The other tests hand a machine a listing written by hand. This one has the compiler write it, so what the
     * project properties ask for, what the compiler puts on the .arch line and what a machine does with it are held
     * together in one place.
     */
    @GameTest(template = ARENA)
    public static void aProgramBuiltForTheNewerMachines_runsOnOneAndIsRefusedByTheOlder(final GameTestHelper helper) {
        final IProgrammingLanguage.CompileResult built = SigmaLanguage.INSTANCE.compile(
                List.of(new IProgrammingLanguage.SourceText("Quiet.sgs", QUIET)), "jsc:x86_64");
        helper.assertTrue(built.ok(), "the corpus program compiles: " + built.complaints());
        helper.assertTrue(built.binary().contains(".arch jsc:x86_64"),
                "the compiler wrote what it was asked for: " + built.binary());

        final PersonalComputerBlockEntity newer = standard(helper);
        if (newer == null) {
            return;
        }
        final MachinePrograms.Started onNewer = newer.programs().start("quiet.asm", built.binary(), 1, newer);
        helper.assertTrue(onNewer.ok(), "the 64-bit machine runs it: " + onNewer.message());

        final PersonalComputerBlockEntity older = legacy(helper, ELSEWHERE);
        if (older == null) {
            return;
        }
        final MachinePrograms.Started onOlder = older.programs().start("quiet.asm", built.binary(), 1, older);
        helper.assertFalse(onOlder.ok(), "and the 32-bit one will not");
        helper.assertTrue(onOlder.message().contains("built for x86-64; this machine is x86"),
                "saying why: " + onOlder.message());
        helper.succeed();
    }

    /**
     * A program saved on a machine that runs it does not come back on one that does not.
     *
     * <p>Coming back is a second door into running a program, and a machine that would refuse to start something
     * must refuse to find it already running just the same.
     */
    @GameTest(template = ARENA)
    public static void aProgramSavedOnANewerMachine_doesNotComeBackOnAnOlderOne(final GameTestHelper helper) {
        final PersonalComputerBlockEntity newer = standard(helper);
        final PersonalComputerBlockEntity older = legacy(helper, ELSEWHERE);
        if (newer == null || older == null) {
            return;
        }
        final String built = listing("jsc:x86_64");
        final IMachineRuntime running = MachineListing.start(built, ROOM, newer, List.of());
        helper.assertTrue(running != null, "it starts on the machine it was built for");
        final CompoundTag saved = new CompoundTag();
        running.save(saved);

        helper.assertTrue(MachineListing.restore(built, saved, newer) != null,
                "and comes back there");
        helper.assertTrue(MachineListing.restore(built, saved, older) == null,
                "but not on a machine whose processor does not run it");
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
        return vintage(helper, WHERE);
    }

    private static PersonalComputerBlockEntity vintage(final GameTestHelper helper, final BlockPos at) {
        return assemble(helper, at, ComputingModule.VINTAGE_PERSONAL_COMPUTER.get(),
                HardwareItems.MOTHERBOARD_BABYAT_VINTAGE.get(), HardwareItems.CPU_INTEGRA_486SX.get(),
                HardwareItems.RAM_SIMM_4.get(), HardwareItems.PSU_300B.get());
    }

    private static PersonalComputerBlockEntity legacy(final GameTestHelper helper) {
        return legacy(helper, WHERE);
    }

    private static PersonalComputerBlockEntity legacy(final GameTestHelper helper, final BlockPos at) {
        return assemble(helper, at, ComputingModule.LEGACY_PERSONAL_COMPUTER.get(),
                HardwareItems.MOTHERBOARD_ATX_LEGACY_LGA775.get(), HardwareItems.CPU_INTEGRA_DUO_E4300.get(),
                HardwareItems.RAM_DDR2_2048.get(), HardwareItems.PSU_500B.get());
    }

    private static PersonalComputerBlockEntity standard(final GameTestHelper helper) {
        return standard(helper, WHERE);
    }

    private static PersonalComputerBlockEntity standard(final GameTestHelper helper, final BlockPos at) {
        return assemble(helper, at, ComputingModule.PERSONAL_COMPUTER.get(),
                ComputingModule.MOTHERBOARD_ATX_P.get(), ComputingModule.CPU_ASCENT_965.get(),
                ComputingModule.RAM_DDR3_8192.get(), ComputingModule.PSU_650G.get());
    }

    /*
     * The computer is only assembled, not powered on: what is being tested is the processor a listing is held
     * against, and that is read off the parts in the slots.
     */
    private static PersonalComputerBlockEntity assemble(final GameTestHelper helper, final BlockPos at,
                                                        final Block computer, final Item board, final Item cpu,
                                                        final Item ram, final Item psu) {
        helper.setBlock(at, computer);
        if (!(helper.getBlockEntity(at) instanceof PersonalComputerBlockEntity assembled)) {
            helper.fail("no personal computer at " + at);
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
