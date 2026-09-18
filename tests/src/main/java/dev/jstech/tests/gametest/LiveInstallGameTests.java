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
        said(helper, shell, cli, "mount /dev/sda /mnt", "/mnt");
        said(helper, shell, cli, "tar xpf stage3-amd64.tar.xz -C /mnt", "");
        /*
         * Unpacking a stage 3 takes time, and nothing enters a system that is still arriving. Asked for it
         * while the unpack is running, the shell says so and says how long is left, which is the one answer
         * that keeps a player from thinking the step was ignored.
         */
        said(helper, shell, cli, "chroot /mnt", "");
        helper.assertFalse(cli.prompt().contains("chroot"),
                "and the shell is still outside it until the unpack ends: " + cli.prompt());
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
        said(helper, shell, cli, "mount /dev/sda /mnt", "/mnt");
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
