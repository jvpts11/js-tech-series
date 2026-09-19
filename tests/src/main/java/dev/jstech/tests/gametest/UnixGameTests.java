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
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsGating;
import dev.jstech.computers.os.PackageManagerKind;
import dev.jstech.computers.os.Platform;
import dev.jstech.computers.os.ShellFamily;
import dev.jstech.computers.os.boot.BootController;
import dev.jstech.computers.os.boot.BootLines;
import dev.jstech.computers.os.boot.BootSequence;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.CliCommands;
import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.cli.CliShell;
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
 * UNIX System V is the oldest of the systems met at a Unix prompt, and keeps its own habits.
 *
 * <p>It runs where the others cannot, on a machine of the first age, and what tells it from a Linux is older than
 * a Linux: people live under /usr, drives are mounted under /mnt, the kernel is a file at the root, and it says
 * very little on its way up or down.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class UnixGameTests {

    private static final String ARENA = "empty";

    private static final BlockPos WHERE = new BlockPos(2, 2, 2);

    private static final int SETTLE = 4;

    private static final ResourceLocation UNIX = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "unix");

    private UnixGameTests() {
    }

    /** It is a family of its own, runs from the first age, and takes its programs from media alone. */
    @GameTest(template = ARENA)
    public static void unix_runsOnAMachineOfTheFirstAge(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = vintage(helper, WHERE);
        if (computer == null) {
            return;
        }
        helper.assertTrue(computer.installOs(UNIX), "a vintage machine takes it");
        final OsDef system = computer.installedOs();
        helper.assertTrue(system != null && system.platform() == Platform.UNIX,
                "it is its own family: " + (system == null ? "none" : system.platform()));
        helper.assertTrue(OsGating.canInstall(system.minEra(), computer.installedEra()),
                "the first age is where it begins");
        helper.assertTrue(system.packageManager() == PackageManagerKind.NONE,
                "it has no manager that installs from a network");
        helper.assertTrue(BootController.targetForComputer(computer) == BootController.BootTarget.TERMINAL_ONLY,
                "and it comes up at a terminal");
        helper.assertTrue(BootLines.menuFor(computer, 200).isEmpty(), "with no boot menu, which it never had");
        helper.succeed();
    }

    /** It signs itself, counts this machine's memory in bytes, checks its root and says it is ready. */
    @GameTest(template = ARENA)
    public static void boot_countsTheMemoryThatIsInTheMachine(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = vintage(helper, WHERE);
        if (computer == null) {
            return;
        }
        helper.assertTrue(computer.installOs(UNIX), "UNIX installs on the machine");
        final BootSequence up = BootLines.forMachine(computer, helper.getLevel());
        final long total = computer.ramTotalMb() * 1_024_000L;
        helper.assertTrue(has(up, "UNIX System V Release 3.2"), "it signs itself: " + labels(up));
        helper.assertTrue(has(up, "Copyright (c) 1984, 1986, 1987 Bellwether Labs"), "with its maker: " + labels(up));
        helper.assertTrue(has(up, "Total real memory     = " + total),
                "the memory is this machine's, in bytes: " + labels(up));
        helper.assertTrue(has(up, "Available memory      = " + (total - 524_288L)),
                "less what the system keeps for itself: " + labels(up));
        helper.assertTrue(last(up).equals("The system is ready."), "and it ends by saying so: " + last(up));

        helper.assertTrue(last(BootLines.shutdownFor(computer, false)).equals("The system is down."),
                "it stops on the sentence it has always stopped on");
        helper.assertTrue(has(BootLines.shutdownFor(computer, true), "INIT: New run level: 6"),
                "and a restart is run level 6, not 0");
        helper.succeed();
    }

    /** People live under /usr, the kernel is a file at the root, and uname answers as System V does. */
    @GameTest(template = ARENA)
    public static void shell_keepsSystemVsOwnHabits(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = vintage(helper, WHERE);
        if (computer == null) {
            return;
        }
        helper.assertTrue(computer.installOs(UNIX), "UNIX installs on the machine");
        computer.setPowered(true);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final ServerCliComputer cli = new ServerCliComputer(computer, helper.getLevel());
                    helper.assertTrue(cli.shellFamily() == ShellFamily.POSIX, "its kernel gives the shell Unix verbs");
                    helper.assertTrue("player@unix:~ $".equals(cli.prompt()), "sh stands at home; got " + cli.prompt());
                    final CliShell shell = CliCommands.shellFor(cli, 52);
                    helper.assertTrue("/usr/player".equals(text(shell.run("pwd", cli)).trim()),
                            "home is under /usr; got " + text(shell.run("pwd", cli)));
                    final String root = text(shell.run("ls /", cli));
                    helper.assertTrue(root.contains("usr/") && root.contains("mnt/") && root.contains("unix"),
                            "the root has /usr, /mnt and the kernel in it; got " + root);
                    helper.assertFalse(root.contains("home/") || root.contains("media/"),
                            "and neither /home nor /media, which came later; got " + root);
                    helper.assertTrue(text(shell.run("cat /etc/inittab", cli)).contains("initdefault"),
                            "init's table is where it has always been");
                    shell.run("cd /etc", cli);
                    helper.assertTrue("player@unix:/etc $".equals(cli.prompt()),
                            "the prompt follows; got " + cli.prompt());
                    shell.run("cd ~", cli);
                    helper.assertTrue("player@unix:~ $".equals(cli.prompt()),
                            "and ~ is its own home; got " + cli.prompt());
                    final String everything = text(shell.run("uname -a", cli)).trim();
                    helper.assertTrue(everything.startsWith("UNIX unix 3.2 2 IA-"),
                            "uname gives the system, the node, the release, the version and the machine; got "
                                    + everything);
                    helper.assertTrue(text(shell.run("apt install vim", cli)).contains("not found")
                                    && text(shell.run("pkg install vim", cli)).contains("not found"),
                            "and no manager of another family is here");
                })
                .thenSucceed();
    }

    private static boolean has(final BootSequence sequence, final String label) {
        for (final BootSequence.Line line : sequence.lines()) {
            if (line.label().equals(label)) {
                return true;
            }
        }
        return false;
    }

    private static String last(final BootSequence sequence) {
        return sequence.lines().isEmpty() ? "" : sequence.lines().get(sequence.lines().size() - 1).label();
    }

    private static String labels(final BootSequence sequence) {
        final StringBuilder out = new StringBuilder();
        for (final BootSequence.Line line : sequence.lines()) {
            out.append(line.label()).append(" | ");
        }
        return out.toString();
    }

    private static String text(final CliShell.Response response) {
        final StringBuilder out = new StringBuilder();
        for (final CliLine line : response.lines()) {
            out.append(line.text()).append('\n');
        }
        return out.toString();
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
        hardware.setStackInSlot(PersonalComputerBlockEntity.PSU_SLOT, new ItemStack(HardwareItems.PSU_300B.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.DISK_SLOTS_START,
                new ItemStack(ComputingModule.disk(StorageTier.HDD, DiskSize.GB_500)));
        return computer;
    }
}
