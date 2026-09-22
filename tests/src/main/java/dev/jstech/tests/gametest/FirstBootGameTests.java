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
import dev.jstech.computers.os.OsDisks;
import dev.jstech.computers.os.boot.BootLines;
import dev.jstech.computers.os.boot.BootSequence;
import dev.jstech.computers.os.boot.SystemWelcome;
import dev.jstech.computers.os.boot.WelcomeFacts;
import dev.jstech.computers.program.ComputerConsoleState;
import dev.jstech.tests.JsTests;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Meeting a system for the first time is something the system remembers, not the machine.
 *
 * <p>That is what makes it survive the things it has to survive: a computer taken apart and put back together has
 * still met its system, a disk carried to another machine arrives already met, and a disk erased and installed
 * again is a first meeting all over, because the mark went with what was erased.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class FirstBootGameTests {

    private static final String ARENA = "empty";

    private static final BlockPos WHERE = new BlockPos(2, 2, 2);

    private static final ResourceLocation DEBIAN =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "debian");

    private static final ResourceLocation UBUNTU =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "ubuntu");

    private static final ResourceLocation FRAMES_95 =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "frames_95");

    private static final ResourceLocation FRAMES_11 =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "frames_11");

    private FirstBootGameTests() {
    }

    /** A system that has just been written has met nobody, whatever that disk carried before. */
    @GameTest(template = ARENA)
    public static void welcome_aSystemJustInstalled_hasMetNobody(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = legacy(helper);
        helper.assertTrue(computer.installOs(DEBIAN), "Debian installs on the machine");
        helper.assertFalse(computer.systemWelcome().seen(), "a system just written has met nobody");
        helper.assertTrue(computer.systemWelcome().greets(), "so it greets whoever comes up on it");
        helper.succeed();
    }

    /** Being met is written onto the disk, so it rides with the system wherever that disk goes. */
    @GameTest(template = ARENA)
    public static void welcome_onceMet_isRememberedOnTheDiskItself(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = legacy(helper);
        helper.assertTrue(computer.installOs(DEBIAN), "Debian installs on the machine");
        computer.setSystemWelcome(computer.systemWelcome().met());

        helper.assertTrue(computer.systemWelcome().seen(), "the machine says the system has been met");
        final ItemStack disk = computer.getHardware().getStackInSlot(PersonalComputerBlockEntity.DISK_SLOTS_START);
        helper.assertTrue(OsDisks.welcomeOn(disk).seen(), "and the mark is on the disk, not on the machine");
        helper.succeed();
    }

    /** Erasing takes the mark with the system: installing again is a first meeting again. */
    @GameTest(template = ARENA)
    public static void welcome_erasingTheDisk_makesItAFirstMeetingAgain(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = legacy(helper);
        helper.assertTrue(computer.installOs(DEBIAN), "Debian installs on the machine");
        computer.setSystemWelcome(computer.systemWelcome().met());
        helper.assertTrue(computer.systemWelcome().seen(), "the system has been met");

        helper.assertTrue(computer.formatDisk(0), "the disk is erased");
        helper.assertTrue(computer.installOs(DEBIAN), "and takes the system again");
        helper.assertFalse(computer.systemWelcome().seen(), "which the machine is meeting for the first time");
        helper.succeed();
    }

    /**
     * Erasing the system disk forgets the programs a player installed from the Mirror, which lived on it. They
     * used to be kept, so the desktop went on showing icons for programs whose files were gone.
     */
    @GameTest(template = ARENA)
    public static void erasingTheDisk_forgetsThePlayersOwnPrograms(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = legacy(helper);
        helper.assertTrue(computer.installOs(DEBIAN), "Debian installs on the machine");
        computer.console().addCommunity(new ComputerConsoleState.Community("stockwatch", "1.0.0", "jvpts11",
                "bell", "PROGRAMS/stockwatch/stockwatch.asm"));
        helper.assertTrue(computer.console().communityProgram("stockwatch") != null, "a player's program is in");

        helper.assertTrue(computer.formatDisk(0), "the disk is erased");
        helper.assertTrue(computer.console().community().isEmpty(), "and the machine no longer has it");
        helper.succeed();
    }

    /**
     * Two systems on two disks are met one at a time, since each mark is on its own disk.
     *
     * <p>This is the whole reason the mark does not live on the machine: a computer that dual-boots would
     * otherwise greet the first system and never greet the second.
     */
    @GameTest(template = ARENA)
    public static void welcome_twoSystemsOnTwoDisks_areEachMetOnTheirOwn(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = legacy(helper);
        computer.getHardware().setStackInSlot(PersonalComputerBlockEntity.DISK_SLOTS_START + 1,
                new ItemStack(ComputingModule.disk(StorageTier.HDD, DiskSize.GB_500)));
        helper.assertTrue(computer.installOs(DEBIAN, 0), "Debian goes on the first disk");
        helper.assertTrue(computer.installOs(UBUNTU, 1), "Ubuntu goes on the second");

        computer.setBootDiskSlot(0);
        computer.setSystemWelcome(computer.systemWelcome().met());
        helper.assertTrue(computer.systemWelcome().seen(), "the first system has been met");

        computer.setBootDiskSlot(1);
        helper.assertFalse(computer.systemWelcome().seen(),
                "and the second has not, because the mark is the disk's and not the machine's");
        helper.succeed();
    }

    /** Asking not to be greeted again is the one thing a welcome remembers, and it survives the meeting. */
    @GameTest(template = ARENA)
    public static void welcome_turnedOff_stopsGreetingWithoutForgettingTheMeeting(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = legacy(helper);
        helper.assertTrue(computer.installOs(DEBIAN), "Debian installs on the machine");
        computer.setSystemWelcome(computer.systemWelcome().met().showingAtStartup(false));

        final SystemWelcome welcome = computer.systemWelcome();
        helper.assertTrue(welcome.seen(), "the system has been met");
        helper.assertFalse(welcome.showAtStartup(), "and was asked not to say hello again");
        helper.assertFalse(welcome.greets(), "so it does not");
        helper.succeed();
    }

    /** Only the systems that had a welcome put one up; the ones that come up at a prompt say nothing. */
    @GameTest(template = ARENA)
    public static void greeter_onlyTheSystemsThatHadAWelcome(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = legacy(helper);
        helper.assertTrue(computer.installOs(DEBIAN), "Debian installs on the machine");
        helper.assertFalse(WelcomeFacts.greeter(computer), "a distribution comes up at its terminal");

        helper.assertTrue(computer.formatDisk(0), "the disk is erased");
        helper.assertTrue(computer.installOs(FRAMES_95), "and takes a system of the other family");
        helper.assertTrue(WelcomeFacts.greeter(computer), "which does have a welcome to put up");
        helper.succeed();
    }

    /** What the welcome offers to say about the other systems is read off the other disks. */
    @GameTest(template = ARENA)
    public static void others_areTheSystemsOnTheMachinesOtherDisks(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = legacy(helper);
        computer.getHardware().setStackInSlot(PersonalComputerBlockEntity.DISK_SLOTS_START + 1,
                new ItemStack(ComputingModule.disk(StorageTier.HDD, DiskSize.GB_500)));
        helper.assertTrue(computer.installOs(FRAMES_95, 0), "Frames 95 goes on the first disk");
        helper.assertTrue(computer.installOs(UBUNTU, 1), "Ubuntu goes on the second");
        computer.setBootDiskSlot(0);

        helper.assertTrue(WelcomeFacts.bootedSlot(computer) == 0, "the machine boots the first disk");
        final java.util.List<WelcomeFacts.Other> others = WelcomeFacts.others(computer);
        helper.assertTrue(others.size() == 1, "and knows of one other system: " + others);
        helper.assertTrue("Ubuntu".equals(others.getFirst().system()) && others.getFirst().slot() == 1,
                "named, and on the disk it is really on: " + others.getFirst());
        helper.succeed();
    }

    /**
     * A tip is only worth showing while it is true, so the one about installing software says where software
     * comes from on THIS machine rather than naming a Mirror that is not answering.
     */
    @GameTest(template = ARENA)
    public static void tips_tellTheTruthAboutThisMachinesNetwork(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = legacy(helper);
        helper.assertTrue(computer.installOs(FRAMES_95), "Frames 95 installs on the machine");

        final java.util.List<String> alone = WelcomeFacts.tips(computer, "", false);
        helper.assertTrue(alone.getFirst().contains("once a Mainframe on this network runs the Mirror"),
                "with no Mirror it says where software would come from: " + alone.getFirst());
        for (final String tip : alone) {
            helper.assertFalse(tip.startsWith("Network shows"),
                    "and a machine on no network is told nothing about one: " + tip);
        }

        final java.util.List<String> served = WelcomeFacts.tips(computer, "CORE", true);
        helper.assertTrue(served.getFirst().contains("the Mirror on CORE"),
                "with one answering it names it: " + served.getFirst());
        helper.succeed();
    }

    /**
     * The newest edition greets the machine by name the first time it comes up, and only then.
     *
     * <p>It takes the place of the maker's name inside the same wait, so a first start is no longer than any
     * other; once the system has been met it comes up behind that name like every other start.
     */
    @GameTest(template = ARENA)
    public static void boot_theNewestEdition_greetsByNameOnlyTheFirstTime(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = standard(helper);
        helper.assertTrue(computer.installOs(FRAMES_11), "Frames 11 installs on the machine");
        computer.console().setComputerName("STUDIO-11");

        final BootSequence first = BootLines.forMachine(computer, helper.getLevel());
        helper.assertTrue("Hi.".equals(first.title()), "the first start greets: " + first.title());
        helper.assertTrue(first.subtitle().startsWith("STUDIO-11"),
                "and greets this machine by its own name: " + first.subtitle());

        computer.setSystemWelcome(computer.systemWelcome().met());
        final BootSequence later = BootLines.forMachine(computer, helper.getLevel());
        helper.assertTrue(later.title().isEmpty(),
                "a later start comes up behind the maker's mark and says nothing: " + later.title());
        helper.succeed();
    }

    /** A Standard machine, which is what the newest edition needs. */
    private static PersonalComputerBlockEntity standard(final GameTestHelper helper) {
        helper.setBlock(WHERE, ComputingModule.PERSONAL_COMPUTER.get());
        if (!(helper.getBlockEntity(WHERE) instanceof PersonalComputerBlockEntity computer)) {
            throw new IllegalStateException("no personal computer at " + WHERE);
        }
        final ItemStackHandler hardware = computer.getHardware();
        hardware.setStackInSlot(PersonalComputerBlockEntity.MOTHERBOARD_SLOT,
                new ItemStack(HardwareItems.MOTHERBOARD_ATX_STANDARD_LGA1150.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.CPU_SLOT,
                new ItemStack(HardwareItems.CPU_APEX_7_4790K.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.RAM_SLOTS_START,
                new ItemStack(ComputingModule.RAM_DDR3_8192.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.PSU_SLOT,
                new ItemStack(ComputingModule.PSU_650G.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.DISK_SLOTS_START,
                new ItemStack(ComputingModule.disk(StorageTier.SSD, DiskSize.GB_500)));
        return computer;
    }

    /** A Legacy machine with one empty disk, which is enough for every question here. */
    private static PersonalComputerBlockEntity legacy(final GameTestHelper helper) {
        helper.setBlock(WHERE, ComputingModule.LEGACY_PERSONAL_COMPUTER.get());
        if (!(helper.getBlockEntity(WHERE) instanceof PersonalComputerBlockEntity computer)) {
            throw new IllegalStateException("no legacy personal computer at " + WHERE);
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
}
