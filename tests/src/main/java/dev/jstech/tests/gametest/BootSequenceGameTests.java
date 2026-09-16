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
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.os.boot.BootLines;
import dev.jstech.computers.os.boot.BootMenu;
import dev.jstech.computers.os.boot.BootSequence;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestWorldBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * What a system says while it comes up is read off the machine it is coming up on.
 *
 * <p>A line that was written in advance would say the same thing on every computer, which is the one thing a
 * starting machine must never do: the drive letters are the drives that are in, and a step about the network
 * reports what answered, or says that nothing did.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class BootSequenceGameTests {

    private static final String ARENA = "empty";

    private static final BlockPos WHERE = new BlockPos(2, 2, 2);

    /** Ticks for a machine's attachment to find the cable beside it. */
    private static final int SETTLE = 4;

    private static final ResourceLocation MC_DOS =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "mc_dos");

    private static final ResourceLocation DEBIAN =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "debian");

    private BootSequenceGameTests() {
    }

    @GameTest(template = ARENA)
    public static void dos_readsTheDrivesThatAreInTheMachine(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = vintageWithDos(helper);
        if (computer == null) {
            return;
        }
        final BootSequence sequence = BootLines.forMachine(computer);
        helper.assertTrue(sequence.title().startsWith("Starting MC-DOS"),
                "the system names itself as it starts: " + sequence.title());
        helper.assertTrue(has(sequence, "C:"), "the first drive gets the first letter: " + labels(sequence));
        helper.assertFalse(has(sequence, "D:"), "and a machine with one drive has no second letter");
        helper.assertTrue(has(sequence, "HIMEM"), "the memory above the line is counted: " + labels(sequence));
        helper.succeed();
    }

    /** No cable, no network step: a machine does not report a link it does not have. */
    @GameTest(template = ARENA)
    public static void dos_withNoCable_saysNothingOfANetwork(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = vintageWithDos(helper);
        if (computer == null) {
            return;
        }
        helper.assertFalse(has(BootLines.forMachine(computer), "NET"),
                "a machine with no cable reaching it has no network line to show");
        helper.succeed();
    }

    /**
     * A kernel names the architecture the processor really understands, so the same distribution reads
     * differently on a machine of another age.
     */
    @GameTest(template = ARENA)
    public static void linux_namesTheArchitectureTheProcessorUnderstands(final GameTestHelper helper) {
        final PersonalComputerBlockEntity older = legacy(helper);
        if (older == null) {
            return;
        }
        helper.assertTrue(older.installOs(DEBIAN), "Debian installs on the machine");
        final BootSequence sequence = BootLines.forMachine(older);
        helper.assertTrue(has(sequence, "Linux version 6.8-jsc (i686)"),
                "a 16-bit machine's kernel is not x86_64: " + labels(sequence));
        helper.assertTrue(has(sequence, "Memory: " + older.ramTotalMb() + " MB available"),
                "and the memory is the memory that is in it: " + labels(sequence));
        helper.succeed();
    }

    /**
     * A machine on a cable says its network is up, whatever is or is not running on it yet.
     *
     * <p>The line is a claim about the network, so it used to be wrong to put the question to the shell: the
     * machine is on a network the moment a cable reaches one, and it is saying so on its way up, which is
     * before anything on it is running to be asked.
     */
    @GameTest(template = ARENA)
    public static void linux_saysTheNetworkIsUpWhenACableReachesOne(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.at(helper.getLevel(), helper.absolutePos(BlockPos.ZERO));
        /*
         * The whole chain, because a computer never meets a Mainframe directly: the Mainframe sits on the HBW
         * backbone and a Personal Router is what bridges an Ethernet machine onto it.
         */
        world.placeRunningMainframe(new BlockPos(1, 2, 2));
        world.setBlock(new BlockPos(2, 2, 2), ComputingModule.HBW_CABLE.get());
        world.setBlock(new BlockPos(3, 2, 2), ComputingModule.PERSONAL_ROUTER.get());
        world.setBlock(new BlockPos(4, 2, 2), ComputingModule.ETHERNET_CABLE.get());
        final PersonalComputerBlockEntity onACable = legacy(helper, new BlockPos(5, 2, 2));
        if (onACable == null) {
            return;
        }
        world.faceRearTowardCable(new BlockPos(5, 2, 2));
        helper.assertTrue(onACable.installOs(DEBIAN), "Debian installs on the cabled machine");
        onACable.togglePower();

        final PersonalComputerBlockEntity onItsOwn = legacy(helper, new BlockPos(5, 2, 5));
        if (onItsOwn == null) {
            return;
        }
        helper.assertTrue(onItsOwn.installOs(DEBIAN), "and on the one standing on its own");
        onItsOwn.togglePower();
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(onACable.networkAttached(), "the cable reaches a network");
                    helper.assertTrue(has(BootLines.forMachine(onACable), "Reached target Network is Online."),
                            "so the machine says so coming up: " + labels(BootLines.forMachine(onACable)));
                    helper.assertFalse(has(BootLines.forMachine(onItsOwn), "Reached target Network is Online."),
                            "and the one with no cable claims nothing of the sort");
                })
                .thenSucceed();
    }

    /**
     * The Frames family says nothing while it loads, which is its character and not a gap.
     *
     * <p>A machine that reports its steps and one that shows a name over a bar are both true to what they are, so
     * the test is that this family comes up behind its maker's name with no account of what it is doing.
     */
    @GameTest(template = ARENA)
    public static void frames_comesUpBehindItsMakersNameAndSaysNothing(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer =
                TestWorldBuilder.at(helper.getLevel(), helper.absolutePos(BlockPos.ZERO))
                        .placeRunningPersonalComputer(WHERE);
        /*
         * An ordinary start, not the first one: the newest edition greets the machine by name the first time it
         * ever comes up, and this is the question about every start after that.
         */
        computer.setSystemWelcome(computer.systemWelcome().met());
        final BootSequence sequence = BootLines.forMachine(computer);
        helper.assertTrue(sequence.title().equals("Midsoft"),
                "the maker's name is what goes up first: " + sequence.title());
        helper.assertTrue(sequence.subtitle().equals("Frames 11"),
                "with the system under it: " + sequence.subtitle());
        helper.assertTrue(sequence.lines().isEmpty(), "and not a word about what it is doing");
        helper.succeed();
    }

    /**
     * The later systems say goodbye; the earliest ones simply stop.
     *
     * <p>A machine of the first age switched off where it stood, and giving it a farewell screen would be the
     * kind of politeness those machines never had.
     */
    @GameTest(template = ARENA)
    public static void shutdown_belongsToTheSystemsThatHadOne(final GameTestHelper helper) {
        final PersonalComputerBlockEntity frames =
                TestWorldBuilder.at(helper.getLevel(), helper.absolutePos(BlockPos.ZERO))
                        .placeRunningPersonalComputer(WHERE);
        helper.assertFalse(BootLines.shutdownFor(frames).isEmpty(),
                "a machine of this age closes its programs and says so");

        final PersonalComputerBlockEntity older = vintageWithDos(helper, new BlockPos(4, 2, 2));
        if (older == null) {
            return;
        }
        helper.assertTrue(BootLines.shutdownFor(older).isEmpty(),
                "and one of the first age goes dark where it stands");
        helper.succeed();
    }

    /**
     * The boot manager lists the disks that really carry a system, and only the systems that bring one show it.
     */
    @GameTest(template = ARENA)
    public static void bootMenu_listsWhatTheMachineCouldBoot(final GameTestHelper helper) {
        final PersonalComputerBlockEntity linux = legacy(helper);
        if (linux == null) {
            return;
        }
        helper.assertTrue(linux.installOs(DEBIAN), "Debian installs on the machine");
        final BootMenu menu = BootLines.menuFor(linux, 100);
        helper.assertFalse(menu.isEmpty(), "a Linux machine stops at its boot manager");
        helper.assertTrue(menu.entries().size() == 1, "with one entry for its one disk: " + menu.entries());
        helper.assertTrue(menu.entries().get(0).label().equals("Debian"),
                "named after the system on it: " + menu.entries().get(0).label());

        final PersonalComputerBlockEntity frames =
                TestWorldBuilder.at(helper.getLevel(), helper.absolutePos(BlockPos.ZERO))
                        .placeRunningPersonalComputer(new BlockPos(4, 2, 2));
        helper.assertTrue(BootLines.menuFor(frames, 100).isEmpty(),
                "and a system that brings no boot manager shows none");
        helper.succeed();
    }

    /** A machine with no system has nothing to come up, and so nothing to say. */
    @GameTest(template = ARENA)
    public static void aMachineWithNoSystem_hasNoSequence(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = vintage(helper);
        if (computer == null) {
            return;
        }
        helper.assertTrue(BootLines.forMachine(computer).isEmpty(), "nothing installed, nothing to show");
        helper.succeed();
    }

    private static boolean has(final BootSequence sequence, final String label) {
        for (final BootSequence.Line line : sequence.lines()) {
            if (line.label().equals(label)) {
                return true;
            }
        }
        return false;
    }

    private static String labels(final BootSequence sequence) {
        final StringBuilder out = new StringBuilder();
        for (final BootSequence.Line line : sequence.lines()) {
            out.append(line.label()).append(' ');
        }
        return out.toString();
    }

    private static PersonalComputerBlockEntity vintageWithDos(final GameTestHelper helper) {
        return vintageWithDos(helper, WHERE);
    }

    private static PersonalComputerBlockEntity vintageWithDos(final GameTestHelper helper, final BlockPos at) {
        final PersonalComputerBlockEntity computer = vintage(helper, at);
        if (computer == null) {
            return null;
        }
        if (!computer.installOs(MC_DOS)) {
            helper.fail("MC-DOS would not install on the vintage machine");
            return null;
        }
        return computer;
    }

    /** A Legacy machine: the 32-bit generation, whose kernel calls itself i686. */
    private static PersonalComputerBlockEntity legacy(final GameTestHelper helper) {
        return legacy(helper, WHERE);
    }

    private static PersonalComputerBlockEntity legacy(final GameTestHelper helper, final BlockPos at) {
        helper.setBlock(at, ComputingModule.LEGACY_PERSONAL_COMPUTER.get());
        if (!(helper.getBlockEntity(at) instanceof PersonalComputerBlockEntity computer)) {
            helper.fail("no legacy personal computer at " + at);
            return null;
        }
        final ItemStackHandler hardware = computer.getHardware();
        hardware.setStackInSlot(PersonalComputerBlockEntity.MOTHERBOARD_SLOT,
                new ItemStack(HardwareItems.MOTHERBOARD_ATX_LEGACY_LGA775.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.CPU_SLOT,
                new ItemStack(HardwareItems.CPU_INTEGRA_DUO_E4300.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.RAM_SLOTS_START,
                new ItemStack(HardwareItems.RAM_DDR2_2048.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.PSU_SLOT,
                new ItemStack(HardwareItems.PSU_500B.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.DISK_SLOTS_START,
                new ItemStack(ComputingModule.disk(StorageTier.HDD, DiskSize.GB_500)));
        return computer;
    }

    private static PersonalComputerBlockEntity vintage(final GameTestHelper helper) {
        return vintage(helper, WHERE);
    }

    private static PersonalComputerBlockEntity vintage(final GameTestHelper helper, final BlockPos at) {
        helper.setBlock(at, ComputingModule.VINTAGE_PERSONAL_COMPUTER.get());
        if (!(helper.getBlockEntity(at) instanceof PersonalComputerBlockEntity computer)) {
            helper.fail("no vintage personal computer at " + at);
            return null;
        }
        final ItemStackHandler hardware = computer.getHardware();
        hardware.setStackInSlot(PersonalComputerBlockEntity.MOTHERBOARD_SLOT,
                new ItemStack(HardwareItems.MOTHERBOARD_BABYAT_VINTAGE.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.CPU_SLOT,
                new ItemStack(HardwareItems.CPU_INTEGRA_486SX.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.RAM_SLOTS_START,
                new ItemStack(HardwareItems.RAM_SIMM_4.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.PSU_SLOT,
                new ItemStack(HardwareItems.PSU_300B.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.DISK_SLOTS_START,
                new ItemStack(ComputingModule.disk(StorageTier.HDD, DiskSize.GB_500)));
        return computer;
    }
}
