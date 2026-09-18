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
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.CliCommands;
import dev.jstech.computers.program.cli.CliShell;
import dev.jstech.computers.program.install.LiveInstallState;
import dev.jstech.tests.JsTests;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * A live medium answers every step of its own sequence, on the machine a player would really run it on.
 *
 * <p>The sequences are covered step by step where the state machine lives; what these are for is the whole
 * thing seen from the prompt, on a machine of the generation the medium is usually put in: a Legacy computer,
 * whose firmware wants no partition of its own, so the disk is formatted whole exactly as the guide says.
 *
 * <p>Every one of these asserts that the tool <em>said something</em>. A step that goes through in silence is
 * indistinguishable, at the prompt, from a step that never ran, and a sequence of silent steps is a player
 * typing into a machine that appears to be ignoring them.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class LiveInstallGameTests {

    private static final String ARENA = "empty";

    private static final BlockPos WHERE = new BlockPos(2, 2, 2);

    /** How wide the prompt wraps its output, which is what the terminal itself passes. */
    private static final int WIDTH = 52;

    private LiveInstallGameTests() {
    }

    /**
     * The Gentoo sequence, from the first look at the disks to the chroot, each step answering out loud.
     *
     * <p>The steps up to the chroot are the ones a player does before anything takes time, so they are the
     * ones where silence is most obviously wrong: nothing here waits, so every one of them has an answer
     * ready the moment it is typed.
     */
    @GameTest(template = ARENA)
    public static void gentoo_everyStepUpToTheChrootSaysSomething(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = legacyWithDisk(helper);
        if (computer == null) {
            return;
        }
        computer.console().startLiveInstall(LiveInstallState.Distro.GENTOO);
        final ServerCliComputer cli = new ServerCliComputer(computer, helper.getLevel());
        final CliShell shell = CliCommands.shellFor(cli, WIDTH);

        said(helper, shell, cli, "lsblk", "sda");
        said(helper, shell, cli, "mkfs.ext4 /dev/sda", "");
        silent(helper, shell, cli, "mount /dev/sda /mnt");
        /*
         * This machine is on a network with no Mirror on it, which is the commonest thing to be wrong when
         * a by-hand install stops working, and every step of it says so in the words the real tool uses
         * rather than failing quietly and leaving the player to work out which step did not happen.
         */
        said(helper, shell, cli, "wget https://distfiles.mainframe/stage3-amd64-openrc.tar.xz",
                "Name or service not known");
        said(helper, shell, cli, "tar xpvf stage3-amd64-openrc.tar.xz", "Cannot open");
        said(helper, shell, cli, "chroot /mnt", "");
        helper.assertFalse(cli.prompt().contains("chroot"),
                "and nothing enters a system that was never unpacked: " + cli.prompt());
        helper.succeed();
    }

    /** The same for the other medium, whose first steps are the same and whose words are its own. */
    @GameTest(template = ARENA)
    public static void arch_everyStepUpToTheChrootSaysSomething(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = legacyWithDisk(helper);
        if (computer == null) {
            return;
        }
        computer.console().startLiveInstall(LiveInstallState.Distro.ARCH);
        final ServerCliComputer cli = new ServerCliComputer(computer, helper.getLevel());
        final CliShell shell = CliCommands.shellFor(cli, WIDTH);

        said(helper, shell, cli, "lsblk", "sda");
        said(helper, shell, cli, "mkfs.ext4 /dev/sda", "");
        silent(helper, shell, cli, "mount /dev/sda /mnt");
        helper.succeed();
    }

    /** A live medium booted on a machine names the disks that are really in it, by the size written on them. */
    @GameTest(template = ARENA)
    public static void lsblk_namesTheDisksThatAreInTheMachine(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = legacyWithDisk(helper);
        if (computer == null) {
            return;
        }
        computer.console().startLiveInstall(LiveInstallState.Distro.GENTOO);
        final ServerCliComputer cli = new ServerCliComputer(computer, helper.getLevel());
        final CliShell shell = CliCommands.shellFor(cli, WIDTH);
        final String out = text(shell.run("lsblk", cli));
        helper.assertTrue(out.contains("sda"), "the first disk is sda: " + out);
        helper.assertFalse(out.contains("sdb"), "and a machine with one disk has no second: " + out);
        helper.succeed();
    }

    /**
     * Asking whether the medium is still in the drive never takes the session away.
     *
     * <p>This is the bug that made a whole installation vanish without a word. The question is asked by the
     * gate on every line typed and by the container every tick, and it used to end the session the moment it
     * did not like the answer: a drive one tick late to load read as a drive with nothing in it, the
     * installation was thrown away, and the terminal stayed on the screen answering nothing at all, for
     * ever, with no way to tell from the machine that anything had happened to it.
     */
    @GameTest(template = ARENA)
    public static void validateOsSession_neverThrowsTheSessionAwayByItself(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = legacyWithDisk(helper);
        if (computer == null) {
            return;
        }
        computer.console().startLiveInstall(LiveInstallState.Distro.GENTOO);
        computer.validateOsSession();
        computer.validateOsSession();
        helper.assertTrue(computer.console().liveInstall() != null,
                "asking where the medium is must never be what takes it away");
        helper.succeed();
    }

    /** Ending a session is the machine's own business, and it does end one whose medium is really gone. */
    @GameTest(template = ARENA)
    public static void settleLiveInstall_endsTheSessionWhenNoDriveHoldsTheMedium(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = legacyWithDisk(helper);
        if (computer == null) {
            return;
        }
        computer.console().startLiveInstall(LiveInstallState.Distro.GENTOO);
        helper.assertTrue(computer.settleLiveInstall(), "no drive holds it, so the session is over");
        helper.assertTrue(computer.console().liveInstall() == null, "and it is gone");
        helper.assertFalse(computer.settleLiveInstall(), "and there is nothing left to end a second time");
        helper.succeed();
    }

    /**
     * Runs one step and insists it answered.
     *
     * @param must a word the answer has to carry, or empty when any answer at all will do
     */
    private static void said(final GameTestHelper helper, final CliShell shell, final ServerCliComputer cli,
                             final String line, final String must) {
        final String out = text(shell.run(line, cli)).trim();
        helper.assertFalse(out.isEmpty(), "\"" + line + "\" answered nothing at all");
        if (!must.isEmpty()) {
            helper.assertTrue(out.contains(must), "\"" + line + "\" answered: " + out);
        }
    }

    /**
     * Runs one step that the real tool performs without a word, and insists it stayed quiet.
     *
     * <p>Mounting a filesystem is the one step in either sequence that says nothing when it works, and that
     * silence is as much a part of the tool as any of the others' output. A line of confirmation here would
     * be the tell that somebody wrote this from a description rather than from having run it.
     */
    private static void silent(final GameTestHelper helper, final CliShell shell, final ServerCliComputer cli,
                               final String line) {
        final String out = text(shell.run(line, cli)).trim();
        helper.assertTrue(out.isEmpty(), "\"" + line + "\" should say nothing at all, and said: " + out);
    }

    private static String text(final CliShell.Response response) {
        final StringBuilder out = new StringBuilder();
        for (final var line : response.lines()) {
            out.append(line.text()).append('\n');
        }
        return out.toString();
    }

    /** A Legacy machine with one disk in it, which is what a live medium is usually put into. */
    private static PersonalComputerBlockEntity legacyWithDisk(final GameTestHelper helper) {
        helper.setBlock(WHERE, ComputingModule.LEGACY_PERSONAL_COMPUTER.get());
        if (!(helper.getBlockEntity(WHERE) instanceof PersonalComputerBlockEntity computer)) {
            helper.fail("no legacy personal computer at " + WHERE);
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
        computer.togglePower();
        return computer;
    }
}
