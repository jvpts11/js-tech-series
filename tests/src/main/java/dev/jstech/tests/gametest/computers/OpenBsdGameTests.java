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
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.operation.payload.firmware.FirmwarePayloads;
import dev.jstech.computers.operation.payload.firmware.InstallerPayloads;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsDisks;
import dev.jstech.computers.os.PackageManagerKind;
import dev.jstech.computers.os.Platform;
import dev.jstech.computers.os.boot.BootLines;
import dev.jstech.computers.os.boot.BootRunner;
import dev.jstech.computers.os.boot.BootSequence;
import dev.jstech.computers.os.install.InstallerFlow;
import dev.jstech.computers.os.install.InstallerPage;
import dev.jstech.computers.os.install.InstallerStyle;
import dev.jstech.computers.os.install.OpenBsdInstall;
import dev.jstech.computers.os.install.OsInstallJob;
import dev.jstech.computers.os.install.OsInstallRunner;
import dev.jstech.computers.os.media.MediaItem;
import dev.jstech.computers.os.media.MediaKind;
import dev.jstech.computers.os.media.MediaReaderBlockEntity;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.CliCommands;
import dev.jstech.computers.program.cli.CliShell;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.tests.JsTests;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

import static dev.jstech.tests.testkit.computers.TestShell.text;

/**
 * OpenBSD, secure by default (computers/docs/design/operating-systems.md): its own kernel's words, ksh at the host's
 * prompt, rcctl over its daemons with xenodm keeping the desktop off until it is switched on, pkg_add's three tools,
 * syspatch, pf and doas, and the sets its installer may leave out, which keep their room and take their commands.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class OpenBsdGameTests {

    private static final String ARENA = "empty";

    private static final BlockPos WHERE = new BlockPos(2, 2, 2);

    /** Ticks for a machine's attachment to find what is beside it. */
    private static final int SETTLE = 4;

    private static final ResourceLocation OPENBSD = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "openbsd");

    private static final String KDE = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "kde_plasma").toString();

    private OpenBsdGameTests() {
    }

    /** It is a Legacy system of its own family, at the terminal, with pkg_add as its manager. */
    @GameTest(template = ARENA)
    public static void openbsd_isItsOwnLegacySystem(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = legacy(helper, WHERE);
        if (computer == null) {
            return;
        }
        helper.assertTrue(computer.installOs(OPENBSD), "a Legacy machine takes it");
        final OsDef system = computer.installedOs();
        helper.assertTrue(system != null && system.platform() == Platform.OPENBSD, "it is its own family");
        helper.assertTrue(system.packageManager() == PackageManagerKind.PKG_ADD, "with pkg_add as its manager");
        helper.assertTrue(system.installerStyle() == InstallerStyle.OPENBSD, "and its own installer");
        helper.succeed();
    }

    /** The kernel names its release and build, mounts its root on the first disk, and the system halts in its words. */
    @GameTest(template = ARENA)
    public static void boot_readsTheMachineOutInItsOwnWords(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = legacy(helper, WHERE);
        if (computer == null) {
            return;
        }
        helper.assertTrue(computer.installOs(OPENBSD), "OpenBSD installs on the machine");
        final BootSequence up = BootLines.forMachine(computer, helper.getLevel());
        helper.assertTrue(any(up, "OpenBSD 7.5 (GENERIC) #79"), "the kernel names its build: " + labels(up));
        helper.assertTrue(any(up, "root on ") && any(up, "swap on "), "and mounts its root: " + labels(up));
        helper.assertFalse(any(up, "Linux") || any(up, "FreeBSD"), "nothing in it is another system's word");
        helper.succeed();
    }

    /** ksh stands at the host's name and a hash, and says what it did not find in its own way. */
    @GameTest(template = ARENA)
    public static void ksh_standsAtTheHostAndAHash(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = mainframe(helper, WHERE);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final ServerCliComputer cli = new ServerCliComputer(mainframe, helper.getLevel());
                    helper.assertTrue(cli.prompt().endsWith("#"), "root's prompt ends at a hash; got " + cli.prompt());
                    final CliShell shell = CliCommands.shellFor(cli, 80);
                    final String missing = text(shell.run("apt install vim", cli));
                    helper.assertTrue(missing.contains("ksh: apt: not found"),
                            "another family's manager is a word ksh does not know; got " + missing);
                    helper.assertTrue("OpenBSD".equals(text(shell.run("uname", cli)).trim()),
                            "uname names the kernel; got " + text(shell.run("uname", cli)));
                })
                .thenSucceed();
    }

    /** rcctl lists the base system's daemons, switches sshd, and keeps pf on whatever it is asked. */
    @GameTest(template = ARENA)
    public static void rcctl_switchesTheDaemonsAndKeepsTheBaseOnes(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = mainframe(helper, WHERE);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final ServerCliComputer cli = new ServerCliComputer(mainframe, helper.getLevel());
                    final CliShell shell = CliCommands.shellFor(cli, 80);
                    final String on = text(shell.run("rcctl ls on", cli));
                    helper.assertTrue(on.contains("cron") && on.contains("pf") && on.contains("pflogd")
                            && on.contains("syslogd"), "the base system's daemons are on; got " + on);
                    shell.run("rcctl disable sshd", cli);
                    helper.assertFalse(mainframe.console().settings().remoteAllowed(),
                            "disabling sshd is this machine's own remote switch");
                    helper.assertFalse(text(shell.run("rcctl ls on", cli)).contains("sshd"), "and it is off now");
                    helper.assertTrue(text(shell.run("rcctl disable pf", cli)).contains("always runs"),
                            "pf is the base system's and stays on");
                    helper.assertTrue(text(shell.run("rcctl start xenodm", cli)).contains("does not exist"),
                            "with no desktop there is no xenodm");
                    helper.assertTrue(text(shell.run("rcctl start sshd", cli)).contains("need -f"),
                            "a daemon switched off starts only when forced");
                })
                .thenSucceed();
    }

    /** A desktop installed still comes up at the terminal until rcctl enable xenodm, as OpenBSD ships. */
    @GameTest(template = ARENA)
    public static void xenodm_keepsTheDesktopOffUntilItIsSwitchedOn(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = mainframe(helper, WHERE);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    mainframe.console().install(KDE);
                    helper.assertTrue(mainframe.installedDesktopId() != null, "a desktop is installed");
                    helper.assertTrue(BootRunner.desktopAtBoot(mainframe) == null,
                            "and the machine still comes up at its terminal");
                    final ServerCliComputer cli = new ServerCliComputer(mainframe, helper.getLevel());
                    final CliShell shell = CliCommands.shellFor(cli, 80);
                    helper.assertTrue(text(shell.run("rcctl ls off", cli)).contains("xenodm"),
                            "xenodm came with the desktop, switched off");
                    shell.run("rcctl enable xenodm", cli);
                    helper.assertTrue(mainframe.installedDesktopId().equals(BootRunner.desktopAtBoot(mainframe)),
                            "switched on, the desktop comes up with the machine");
                    helper.assertTrue(text(shell.run("rcctl start xenodm", cli)).contains("xenodm(ok)"),
                            "and starting it brings the desktop up now");
                    helper.assertTrue(mainframe.bootedDesktopId() != null, "the desktop is up");
                })
                .thenSucceed();
    }

    /** syspatch lists the release's patches, installs them from the Mirror, lists them installed and takes one off. */
    @GameTest(template = ARENA)
    public static void syspatch_installsTheReleasesPatches(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = mainframe(helper, WHERE);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final ServerCliComputer cli = new ServerCliComputer(mainframe, helper.getLevel());
                    final CliShell shell = CliCommands.shellFor(cli, 80);
                    helper.assertTrue(text(shell.run("syspatch -c", cli)).contains("no route to the Mirror"),
                            "with no Mirror there is nothing to fetch from");
                    mainframe.installMirror();
                    final String waiting = text(shell.run("syspatch -c", cli));
                    helper.assertTrue(waiting.contains("001_ksh") && waiting.contains("002_sshd"),
                            "two patches wait for the release; got " + waiting);
                    final String installed = text(shell.run("syspatch", cli));
                    helper.assertTrue(installed.contains("Installing patch 001_ksh")
                                    && installed.contains("Relinking to create unique kernel... done."),
                            "they are installed and the kernel linked anew; got " + installed);
                    helper.assertTrue(text(shell.run("syspatch -c", cli)).isBlank(), "nothing waits now");
                    helper.assertTrue(text(shell.run("syspatch -l", cli)).contains("002_sshd"), "both are listed");
                    helper.assertTrue(text(shell.run("syspatch -r", cli)).contains("Reverting patch 002_sshd"),
                            "and the last comes back off");
                    helper.assertTrue(mainframe.console().settings().patchesInstalled() == 1, "leaving one");
                })
                .thenSucceed();
    }

    /** pkg_add opens with the release's quirks, and pkg_info lists the Mirror's catalogue. */
    @GameTest(template = ARENA)
    public static void pkgAdd_installsFromTheMirrorInItsOwnWords(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = mainframe(helper, WHERE);
        mainframe.installMirror();
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final ServerCliComputer cli = new ServerCliComputer(mainframe, helper.getLevel());
                    final CliShell shell = CliCommands.shellFor(cli, 80);
                    final String fetched = text(shell.run("pkg_add screenfetch", cli));
                    helper.assertTrue(fetched.contains("quirks-7.14 signed on") && fetched.contains("screenfetch-")
                            && fetched.contains(": ok"), "pkg_add checks its quirks and adds it; got " + fetched);
                    final String search = text(shell.run("pkg_info -Q screen", cli));
                    helper.assertTrue(search.contains("screenfetch"), "pkg_info -Q finds it; got " + search);
                })
                .thenSucceed();
    }

    /** doas runs what it is given as root, says permit for the one rule, and knows no user that is not there. */
    @GameTest(template = ARENA)
    public static void doas_runsTheCommandItIsGiven(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = mainframe(helper, WHERE);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final ServerCliComputer cli = new ServerCliComputer(mainframe, helper.getLevel());
                    final CliShell shell = CliCommands.shellFor(cli, 80);
                    helper.assertTrue("OpenBSD".equals(text(shell.run("doas uname", cli)).trim()),
                            "doas runs uname; got " + text(shell.run("doas uname", cli)));
                    helper.assertTrue("permit".equals(text(shell.run("doas -C /etc/doas.conf pkg_add", cli)).trim()),
                            "the one rule permits it");
                    helper.assertTrue(text(shell.run("doas -u bob uname", cli)).contains("unknown user"),
                            "and there is no bob");
                    helper.assertTrue(text(shell.run("cat /etc/doas.conf", cli)).contains("permit persist :wheel"),
                            "the rule is the file's");
                    helper.assertTrue(text(shell.run("cat /etc/pf.conf", cli)).contains("set skip on lo"),
                            "pf's rules are the system's own");
                    helper.assertTrue(text(shell.run("pfctl -si", cli)).contains("Status: Enabled for"),
                            "and pf is enabled");
                })
                .thenSucceed();
    }

    /** The man and comp sets left out take their commands with them, and every set left out keeps its room. */
    @GameTest(template = ARENA)
    public static void sets_leftOutTakeTheirCommandsAndKeepTheirRoom(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = mainframe(helper, WHERE);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final ServerCliComputer cli = new ServerCliComputer(mainframe, helper.getLevel());
                    final CliShell shell = CliCommands.shellFor(cli, 80);
                    helper.assertFalse(text(shell.run("man ls", cli)).contains("not found"), "the manual is there");
                    mainframe.console().settings().setSetsLeftOut(InstallerFlow.NO_MAN | InstallerFlow.NO_COMP);
                    helper.assertTrue(text(shell.run("man ls", cli)).contains("ksh: man: not found"),
                            "without the man set there is no man");
                    helper.assertTrue(text(shell.run("make", cli)).contains("ksh: make: not found"),
                            "and without the comp set nothing to build with");
                    final ItemStack disk = mainframe.diskInSlot(0);
                    final OsDef system = mainframe.installedOs();
                    // Measured on the Legacy's small items, where a set's room is more than a rounding.
                    final long whole = OsDisks.footprintItemsOn(disk, system, HardwareEra.LEGACY);
                    final int kept = OpenBsdInstall.savedMb(InstallerFlow.NO_COMP, system.footprintMb());
                    OsDisks.keepOff(disk, OPENBSD, kept);
                    helper.assertTrue(OsDisks.systemsOn(disk).savedMbOf(OPENBSD) == kept, "the disk records it");
                    helper.assertTrue(OsDisks.footprintItemsOn(disk, system, HardwareEra.LEGACY) < whole,
                            "a set left out keeps its room off the disk");
                })
                .thenSucceed();
    }

    /** The installer's answers become the system's: sshd off, xenodm on, and the sets left out off the disk. */
    @GameTest(template = ARENA)
    public static void installer_carriesItsAnswersIntoTheSystem(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = mainframeWithInstallMedia(helper, WHERE);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 4, () -> {
                    mainframe.setNeedsPost(false);
                    helper.assertTrue(FirmwarePayloads.beginInstall(helper.getLevel(), mainframe, -1L, -1) == null,
                            "the installer opens");
                    final InstallerFlow flow = mainframe.installer();
                    helper.assertTrue(flow != null && flow.style() == InstallerStyle.OPENBSD, "OpenBSD's own");
                    flow.setSshdEnabled(false);
                    // Through the answer's own path, which re-quotes the copy to the sets that are left.
                    InstallerPayloads.answerOptionsAndResize(mainframe, flow,
                            InstallerFlow.XENODM | InstallerFlow.NO_MAN);
                    for (int i = 0; i < flow.style().stages().size() && !flow.started(); i++) {
                        flow.next();
                    }
                    final OsInstallJob job = mainframe.installing();
                    helper.assertTrue(job != null, "the copy started");
                    for (int i = 0; i <= 2 * job.ticksTotal() && mainframe.installedOsId() == null; i++) {
                        OsInstallRunner.tick(mainframe, helper.getLevel(), WHERE);
                        if (asks(flow.page())) {
                            flow.next();
                        }
                    }
                    helper.assertTrue(OPENBSD.equals(mainframe.installedOsId()), "OpenBSD is on the disk; the installer"
                            + " is on " + flow.page() + " with " + (mainframe.installing() == null ? "no copy"
                            : mainframe.installing().ticksLeft() + " ticks left"));
                    helper.assertFalse(mainframe.console().settings().remoteAllowed(), "sshd was answered no");
                    helper.assertTrue(mainframe.console().settings().xenodmEnabled(), "xenodm was answered yes");
                    helper.assertTrue(mainframe.console().settings().setsLeftOut() == InstallerFlow.NO_MAN,
                            "the man set was left out");
                    final int kept = OpenBsdInstall.savedMb(InstallerFlow.NO_MAN,
                            mainframe.installedOs().footprintMb());
                    helper.assertTrue(OsDisks.systemsOn(mainframe.diskInSlot(0)).savedMbOf(OPENBSD) == kept,
                            "and its room is kept off the disk");
                })
                .thenSucceed();
    }

    /* The pages of OpenBSD's installer that wait for an answer, which the copy stops at. */
    private static boolean asks(final InstallerPage page) {
        return page == InstallerPage.NAME || page == InstallerPage.SERVICES || page == InstallerPage.DISK
                || page == InstallerPage.MIRROR || page == InstallerPage.COMPONENTS;
    }

    private static boolean any(final BootSequence sequence, final String text) {
        for (final BootSequence.Line line : sequence.lines()) {
            if (line.label().english().contains(text) || line.value().english().contains(text)) {
                return true;
            }
        }
        return false;
    }

    private static String labels(final BootSequence sequence) {
        final StringBuilder out = new StringBuilder();
        for (final BootSequence.Line line : sequence.lines()) {
            out.append(line.label().english()).append(" | ");
        }
        return out.toString();
    }

    /** A powered Mainframe with a full, 64-bit build and a disk, with OpenBSD installed on it. */
    private static MainframeBlockEntity mainframe(final GameTestHelper helper, final BlockPos pos) {
        final MainframeBlockEntity mainframe = bareMainframe(helper, pos);
        if (!mainframe.installOs(OPENBSD)) {
            throw new IllegalStateException("failed to install OpenBSD on the test Mainframe");
        }
        return mainframe;
    }

    /** A running Mainframe with an empty disk and, beside it, a CD drive holding OpenBSD's installer. */
    private static MainframeBlockEntity mainframeWithInstallMedia(final GameTestHelper helper, final BlockPos pos) {
        final MainframeBlockEntity mainframe = bareMainframe(helper, pos);
        final BlockPos readerPos = pos.east();
        helper.setBlock(readerPos, ComputingModule.CD_DRIVE.get());
        if (!(helper.getBlockEntity(readerPos) instanceof MediaReaderBlockEntity reader)) {
            throw new IllegalStateException("no reader at " + readerPos);
        }
        final ItemStack disc = new ItemStack(ComputingModule.CD_ROM.get());
        MediaItem.setKind(disc, MediaKind.OS_INSTALL);
        MediaItem.setPayload(disc, OPENBSD);
        reader.mediaSlot().setStackInSlot(0, disc);
        return mainframe;
    }

    private static MainframeBlockEntity bareMainframe(final GameTestHelper helper, final BlockPos pos) {
        helper.setBlock(pos, ComputingModule.MAINFRAME.get());
        if (!(helper.getBlockEntity(pos) instanceof MainframeBlockEntity mainframe)) {
            throw new IllegalStateException("no Mainframe at " + pos);
        }
        final ItemStackHandler inv = mainframe.getInventory();
        inv.setStackInSlot(MainframeBlockEntity.MOTHERBOARD_SLOT,
                new ItemStack(ComputingModule.MOTHERBOARD_MTX_S_2011.get()));
        inv.setStackInSlot(MainframeBlockEntity.CPU_SLOTS_START, new ItemStack(ComputingModule.CPU_SERVO_2620.get()));
        inv.setStackInSlot(MainframeBlockEntity.RAM_SLOTS_START, new ItemStack(ComputingModule.RAM_DDR3_8192.get()));
        inv.setStackInSlot(MainframeBlockEntity.PSU_SLOT, new ItemStack(ComputingModule.PSU_650G.get()));
        inv.setStackInSlot(MainframeBlockEntity.DISK_SLOTS_START,
                new ItemStack(ComputingModule.disk(StorageTier.HDD, DiskSize.GB_500)));
        mainframe.togglePower();
        return mainframe;
    }

    /** A Legacy machine: the 32-bit generation, the earliest OpenBSD installs on. */
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
                new ItemStack(HardwareItems.CPU_INTEGRA_PENTIX_4_560.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.RAM_SLOTS_START,
                new ItemStack(HardwareItems.RAM_DDR_1024.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.PSU_SLOT, new ItemStack(HardwareItems.PSU_500B.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.DISK_SLOTS_START,
                new ItemStack(ComputingModule.disk(StorageTier.HDD, DiskSize.GB_500)));
        return computer;
    }
}
