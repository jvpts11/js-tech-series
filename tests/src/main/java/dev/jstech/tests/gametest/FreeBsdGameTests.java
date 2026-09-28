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
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.gui.term.TermBuffer;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.operation.payload.ThisPcPayload;
import dev.jstech.computers.operation.payload.desktop.ThisPcPayloads;
import dev.jstech.computers.operation.payload.firmware.FirmwarePayloads;
import dev.jstech.computers.operation.payload.firmware.InstallerPayloads;
import dev.jstech.computers.os.FilesystemKind;
import dev.jstech.computers.os.FirmwareKind;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsGating;
import dev.jstech.computers.os.PackageManagerKind;
import dev.jstech.computers.os.Platform;
import dev.jstech.computers.os.ShellFamily;
import dev.jstech.computers.os.boot.BootController;
import dev.jstech.computers.os.boot.BootLines;
import dev.jstech.computers.os.boot.BootManager;
import dev.jstech.computers.os.boot.BootMenu;
import dev.jstech.computers.os.boot.BootSequence;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.ProgramFilesProjection;
import dev.jstech.computers.os.install.InstallerFlow;
import dev.jstech.computers.os.install.InstallerPage;
import dev.jstech.computers.os.install.InstallerStyle;
import dev.jstech.computers.os.install.Installers;
import dev.jstech.computers.os.install.OsInstallJob;
import dev.jstech.computers.os.install.OsInstallRunner;
import dev.jstech.computers.os.media.MediaItem;
import dev.jstech.computers.os.media.MediaKind;
import dev.jstech.computers.os.media.MediaReaderBlockEntity;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.CliCommands;
import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.cli.CliShell;
import dev.jstech.computers.program.cli.CliStyle;
import dev.jstech.core.tier.HardwareEra;
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
 * FreeBSD is a system of its own and not a Linux under another name.
 *
 * <p>The two are met at the same kind of prompt, which is exactly why everything that tells them apart has to be
 * held to: the words the kernel comes up in, the architecture it names, the loader it counts down at, the
 * manager that installs for it, and where what is installed goes.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class FreeBsdGameTests {

    private static final String ARENA = "empty";

    private static final BlockPos WHERE = new BlockPos(2, 2, 2);

    /** Ticks for a machine's attachment to find what is beside it. */
    private static final int SETTLE = 4;

    /** Room for the slowest self-test any machine here runs, so the wait for the loader is never cut short. */
    private static final int PATIENT = 1_200;

    private static final ResourceLocation FREEBSD =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "freebsd");

    private static final ResourceLocation UBUNTU =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "ubuntu");

    private FreeBsdGameTests() {
    }

    /** It asks for a machine of the Legacy generation, and is a terminal system with its own manager on it. */
    @GameTest(template = ARENA)
    public static void freebsd_installsFromTheLegacyGenerationOn(final GameTestHelper helper) {
        final PersonalComputerBlockEntity older = vintage(helper, new BlockPos(4, 2, 2));
        final PersonalComputerBlockEntity computer = legacy(helper, WHERE);
        if (older == null || computer == null) {
            return;
        }
        helper.assertTrue(computer.installOs(FREEBSD), "a Legacy machine takes it");
        final OsDef system = computer.installedOs();
        helper.assertTrue(system != null && system.platform() == Platform.FREEBSD,
                "it is its own family, not a Linux: " + (system == null ? "none" : system.platform()));
        helper.assertFalse(OsGating.canInstall(system.minEra(), older.installedEra()),
                "a machine of the first age is too early for it");
        helper.assertTrue(OsGating.canInstall(system.minEra(), computer.installedEra()),
                "and the Legacy generation is where it begins");
        helper.assertTrue(system.packageManager() == PackageManagerKind.PKG, "with pkg as its manager");
        helper.assertTrue(BootController.targetForComputer(computer) == BootController.BootTarget.TERMINAL_ONLY,
                "and it comes up at a terminal until a desktop is installed on it");
        helper.succeed();
    }

    /** The kernel names itself, the maker's architecture and the disks that are in, and never says Linux. */
    @GameTest(template = ARENA)
    public static void boot_readsTheMachineOutInItsOwnWords(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = legacy(helper, WHERE);
        if (computer == null) {
            return;
        }
        helper.assertTrue(computer.installOs(FREEBSD), "FreeBSD installs on the machine");
        final BootSequence up = BootLines.forMachine(computer, helper.getLevel());
        helper.assertTrue(has(up, "FreeBSD 14.1-RELEASE GENERIC IA-32"),
                "a 32-bit machine's kernel names the Integra Architecture: " + labels(up));
        helper.assertTrue(any(up, "ada0: <"), "the first disk is ada0: " + labels(up));
        helper.assertTrue(has(up, "real memory  = " + computer.ramTotalMb() + " MB"),
                "and the memory is the memory that is in it: " + labels(up));
        helper.assertFalse(any(up, "Linux") || any(up, "x86_64") || any(up, "i686"),
                "nothing in it is a Linux's word: " + labels(up));
        helper.assertFalse(any(up, "Starting Network"), "and with no cable there is no network to start");

        final BootSequence down = BootLines.shutdownFor(computer, false);
        helper.assertTrue(last(down).equals("The operating system has halted."),
                "it stops on the sentence it has always stopped on: " + last(down));
        helper.assertTrue(last(BootLines.shutdownFor(computer, true)).equals("Rebooting..."),
                "and says another when it is coming straight back");
        helper.succeed();
    }

    /** The loader offers the boot and starting over, and the firmware only where the firmware is reached so. */
    @GameTest(template = ARENA)
    public static void loader_listsOnlyWhatTheMachineCanDo(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = legacy(helper, WHERE);
        if (computer == null) {
            return;
        }
        helper.assertTrue(computer.installOs(FREEBSD), "FreeBSD installs on the machine");
        final BootMenu menu = BootLines.menuFor(computer, 200);
        helper.assertTrue(menu.manager() == BootManager.LOADER, "FreeBSD brings its own loader: " + menu.manager());
        helper.assertTrue(menu.title().english().equals("Welcome to FreeBSD"),
                "headed the way it heads it: " + menu.title());
        helper.assertTrue(menu.entries().get(0).label().english().equals("Boot") && menu.defaultIndex() == 0,
                "the boot comes first and is what Enter does: " + menu.entries());
        helper.assertTrue(FREEBSD.toString().equals(menu.entries().get(0).osId()),
                "and it boots the system it belongs to: " + menu.entries().get(0).osId());
        /*
         * The firmware's preference is "whichever disk has a system" until somebody sets one, and that travels as
         * the same number the way into the setup does. An entry that carried it would open the setup on Enter.
         */
        helper.assertTrue(menu.entries().get(0).slot() >= 0 && !menu.entries().get(0).isFirmware(),
                "the boot names the disk the system is really on: " + menu.entries().get(0));
        helper.assertTrue(menu.entries().get(1).isRestart(), "starting over comes second: " + menu.entries());
        final HardwareEra era = computer.installedEra() != null ? computer.installedEra() : HardwareEra.STANDARD;
        final boolean reached = FirmwareKind.forEra(era) == FirmwareKind.UEFI;
        helper.assertTrue(menu.entries().size() == (reached ? 3 : 2),
                "and nothing is listed that does nothing: " + menu.entries());
        helper.assertTrue(menu.entries().stream().anyMatch(BootMenu.Entry::isFirmware) == reached,
                "the firmware is offered only where it is reached this way");
        helper.succeed();
    }

    /** Choosing to start over from the loader leaves the menu and owes the self-test again. */
    @GameTest(template = ARENA, timeoutTicks = PATIENT)
    public static void loader_startingOverRunsTheSelfTestAgain(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = legacy(helper, WHERE);
        if (computer == null) {
            return;
        }
        helper.assertTrue(computer.installOs(FREEBSD), "FreeBSD installs on the machine");
        computer.setPowered(true);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(computer.atBootMenu(),
                        "the machine stops at the loader once its self-test is over"))
                .thenExecute(() -> {
                    helper.assertFalse(computer.needsPost(), "with the self-test behind it");
                    computer.restartFromBootMenu();
                    helper.assertFalse(computer.atBootMenu(), "starting over leaves the loader");
                    helper.assertTrue(computer.needsPost(), "and the self-test is owed again");
                })
                .thenSucceed();
    }

    /** {@code uname} and the prompt are FreeBSD's, down to the letters asked for together. */
    @GameTest(template = ARENA)
    public static void uname_namesTheKernelAndTheMakersArchitecture(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = mainframe(helper, WHERE);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final ServerCliComputer cli = new ServerCliComputer(mainframe, helper.getLevel());
                    helper.assertTrue(cli.shellFamily() == ShellFamily.POSIX, "its kernel gives the shell Unix verbs");
                    helper.assertTrue("root@freebsd:~ #".equals(cli.prompt()),
                            "sh stands at root's prompt, as a machine fresh from its installer does; got "
                                    + cli.prompt());
                    helper.assertTrue(cli.promptLine().style() == CliStyle.RED,
                            "with root and the machine's name in red; got " + cli.promptLine().spans());
                    final CliShell shell = CliCommands.newShell(cli.shellFamily(), 52);
                    helper.assertTrue("FreeBSD".equals(text(shell.run("uname", cli)).trim()),
                            "asked nothing, it names the kernel; got " + text(shell.run("uname", cli)));
                    helper.assertTrue("FreeBSD 14.1-RELEASE".equals(text(shell.run("uname -sr", cli)).trim()),
                            "letters asked for together are answered together; got "
                                    + text(shell.run("uname -sr", cli)));
                    final String everything = text(shell.run("uname -a", cli));
                    helper.assertTrue(everything.contains("FreeBSD freebsd 14.1-RELEASE")
                                    && everything.contains("GENERIC vel64"),
                            "everything names the host, the release and Velocion's architecture; got " + everything);
                    helper.assertFalse(everything.contains("Linux") || everything.contains("x86_64"),
                            "and none of it is a Linux's word; got " + everything);
                    helper.assertTrue(text(shell.run("uname -x", cli)).contains("invalid option"),
                            "a letter it never had is refused");
                })
                .thenSucceed();
    }

    /** {@code pkg} installs from the Mirror in its own words, and the other families' managers are not there. */
    @GameTest(template = ARENA)
    public static void pkg_installsFromTheMirrorInItsOwnWords(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = mainframe(helper, WHERE);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final ServerCliComputer cli = new ServerCliComputer(mainframe, helper.getLevel());
                    final CliShell shell = CliCommands.newShell(cli.shellFamily(), 52);
                    final String unreachable = text(shell.run("pkg install iqlengine", cli));
                    helper.assertTrue(unreachable.contains("pkg: ") && unreachable.contains("mirror"),
                            "with no Mirror pkg says so, naming itself as it does; got " + unreachable);
                    helper.assertTrue(text(shell.run("mirror install", cli)).contains("Mirror installed"),
                            "the Mirror installs on the Mainframe");
                    final String installed = text(shell.run("pkg install iqlengine", cli));
                    helper.assertTrue(installed.contains("Updating Mirror repository catalogue...")
                                    && installed.contains("done"),
                            "it looks at the repository first and then installs; got " + installed);
                    helper.assertTrue(mainframe.isIqlEngineInstalled(), "and the Engine is on");
                    helper.assertTrue(text(shell.run("pkg install iqlengine", cli)).contains("already installed"),
                            "a second time there is nothing to do, said its way");
                    helper.assertTrue(text(shell.run("pkg info", cli)).contains("iqlengine"),
                            "what is installed is what 'pkg info' lists");
                    helper.assertTrue(text(shell.run("pkg update", cli)).contains("All repositories are up to date."),
                            "'pkg update' ends the way it ends");
                    helper.assertTrue(text(shell.run("apt install iqlengine", cli)).contains("apt: not found"),
                            "and apt does not exist here");
                })
                .thenSucceed();
    }

    /** screenfetch, installed with pkg, reports the machine in FreeBSD's words under FreeBSD's own mark. */
    @GameTest(template = ARENA)
    public static void screenfetch_showsTheSystemAsFreeBsd(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = mainframe(helper, WHERE);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final ServerCliComputer cli = new ServerCliComputer(mainframe, helper.getLevel());
                    // As wide as a monitor's terminal: screenfetch cuts what passes the edge rather than wrap it.
                    final CliShell shell = CliCommands.shellFor(cli, TermBuffer.MONITOR_COLUMNS);
                    helper.assertTrue(text(shell.run("screenfetch", cli)).contains("not found"),
                            "screenfetch is a package, absent on a fresh install");
                    mainframe.installMirror();
                    final String fetched = text(shell.run("pkg install screenfetch", cli));
                    helper.assertTrue(fetched.contains("New packages to be INSTALLED:")
                                    && fetched.contains("[1/1] Fetching screenfetch-"),
                            "pkg says what it will install and fetches it; got " + fetched);
                    TestWorldBuilder.finishSetup(mainframe, helper.getLevel(), helper.absolutePos(WHERE));
                    final String out = text(shell.run("screenfetch", cli));
                    helper.assertTrue(out.contains("player@freebsd"), "the header is user@host; got " + out);
                    helper.assertTrue(out.contains("OS: FreeBSD vel64"), "the OS line names Velocion's architecture");
                    helper.assertTrue(out.contains("Kernel: FreeBSD 14.1-RELEASE vel64"), "and so does the kernel's");
                    helper.assertTrue(out.contains("(pkg)"), "the packages are counted by pkg");
                    helper.assertTrue(out.contains("Shell: sh"), "the shell is sh");
                    helper.assertTrue(out.contains("DE: none (ttyv0)"), "and a machine with no desktop is at ttyv0");
                    helper.assertTrue(out.contains(".---.....----."), "under the mark with the two horns; got " + out);
                    helper.assertFalse(out.contains("x86_64") || out.contains("Linux"),
                            "with none of a Linux's words in it; got " + out);
                })
                .thenSucceed();
    }

    /** The root of a FreeBSD disk is a Unix root, with a desktop on it or without: nothing of Frames' is there. */
    @GameTest(template = ARENA)
    public static void root_holdsAUnixTreeAndNothingOfFrames(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = mainframe(helper, WHERE);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final ServerCliComputer cli = new ServerCliComputer(mainframe, helper.getLevel());
                    final CliShell shell = CliCommands.shellFor(cli, 52);
                    mainframe.console().install(
                            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "kde_plasma").toString());
                    shell.run("cd /", cli);
                    final String root = text(shell.run("ls", cli));
                    helper.assertTrue(root.contains("home/") && root.contains("etc/") && root.contains("usr/"),
                            "the Unix tree is there; got " + root);
                    helper.assertFalse(root.contains("Program Files") || root.contains("Frames")
                                    || root.contains("Users"),
                            "and nothing of Frames' is; got " + root);
                })
                .thenSucceed();
    }

    /** What is installed goes under /usr/local, apart from the base system, and rc.conf names this machine. */
    @GameTest(template = ARENA)
    public static void installedPackages_liveUnderUsrLocal(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = mainframe(helper, WHERE);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final ServerCliComputer cli = new ServerCliComputer(mainframe, helper.getLevel());
                    final CliShell shell = CliCommands.newShell(cli.shellFamily(), 52);
                    mainframe.console().install(
                            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "screenfetch").toString());
                    final String local = text(shell.run("ls /usr/local/bin", cli));
                    helper.assertTrue(local.contains("screenfetch"),
                            "a package's binary is under /usr/local; got " + local);
                    helper.assertFalse(text(shell.run("ls /usr/bin", cli)).contains("screenfetch"),
                            "and not where a Linux would have put it");
                    final String readme = text(shell.run("cat /usr/local/share/screenfetch/readme", cli));
                    helper.assertTrue(readme.contains("screenfetch"), "with its readme beside it; got " + readme);
                    final String conf = text(shell.run("cat /etc/rc.conf", cli));
                    helper.assertTrue(conf.contains("hostname=\"freebsd\""),
                            "and rc.conf names the machine it is on; got " + conf);
                })
                .thenSucceed();
    }

    /**
     * bsdinstall's own answers become the machine's own state: sshd is off unless ticked, cron is on, and a
     * ticked ports component lays an empty tree ready for the first {@code portsnap fetch}.
     */
    @GameTest(template = ARENA)
    public static void bsdinstall_setsSshdOffAndCronOnAndLaysAnEmptyPortsTree(final GameTestHelper helper) {
        final BlockPos pos = new BlockPos(2, 2, 2);
        final MainframeBlockEntity mainframe = mainframeWithBsdInstallMedia(helper, pos);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 4, () -> {
                    mainframe.setNeedsPost(false);
                    helper.assertTrue(FirmwarePayloads.beginInstall(helper.getLevel(), mainframe, -1L, -1) == null,
                            "bsdinstall opens");
                    final InstallerFlow flow = mainframe.installer();
                    helper.assertTrue(flow != null && flow.style() == InstallerStyle.BSD_INSTALL,
                            "the machine is in bsdinstall's own style");
                    helper.assertTrue(flow.portsSelected(), "the ports component is ticked by default");
                    helper.assertFalse(flow.sshdEnabled(), "sshd is off by default, unlike every other system");
                    helper.assertTrue(flow.cronEnabled(), "cron is on by default");
                    helper.assertTrue(!flow.disks().isEmpty() && "ada0".equals(flow.disks().get(0).label()),
                            "the disk is named the way bsdinstall names one: " + flow.disks());
                    for (int i = 0; i < flow.style().stages().size() && !flow.started(); i++) {
                        flow.next();
                    }
                    final OsInstallJob job = mainframe.installing();
                    helper.assertTrue(job != null, "the machine took the copy on");
                    for (int i = 0; i <= job.ticksTotal(); i++) {
                        OsInstallRunner.tick(mainframe, helper.getLevel(), pos);
                    }
                    helper.assertTrue(flow.page() == InstallerPage.SERVICES,
                            "the extraction stops on the page that still asks something, off any Mirror");
                    helper.assertTrue(mainframe.installedOsId() == null,
                            "and the system is not written while that page is unanswered");
                    flow.next();
                    for (int i = 0; i <= 10; i++) {
                        OsInstallRunner.tick(mainframe, helper.getLevel(), pos);
                    }
                    helper.assertTrue(FREEBSD.equals(mainframe.installedOsId()), "FreeBSD is on the disk");
                    helper.assertFalse(mainframe.console().settings().remoteAllowed(),
                            "the machine kept sshd off");
                    helper.assertTrue(mainframe.console().settings().cronEnabled(), "and cron on");
                    final String conf = ProgramFilesProjection.text(mainframe, "etc/rc.conf").orElse("");
                    helper.assertTrue(conf.contains("sshd_enable=\"NO\"") && conf.contains("cron_enable=\"YES\""),
                            "rc.conf reads back the same two switches; got " + conf);
                    final ItemStack disk = mainframe.diskInSlot(0);
                    helper.assertTrue(
                            DiskFilesystem.listDirs(disk, "usr", FilesystemKind.HIERARCHICAL).contains("usr/ports"),
                            "the ports tree's folder is there, empty, ready for the first fetch");
                })
                .thenSucceed();
    }

    /** Ticking sshd on and unticking ports on the services and components pages is carried all the way through. */
    @GameTest(template = ARENA)
    public static void bsdinstall_carriesTheServicesAndComponentsPagesAnswersThrough(final GameTestHelper helper) {
        final BlockPos pos = new BlockPos(2, 2, 2);
        final MainframeBlockEntity mainframe = mainframeWithBsdInstallMedia(helper, pos);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 4, () -> {
                    mainframe.setNeedsPost(false);
                    helper.assertTrue(FirmwarePayloads.beginInstall(helper.getLevel(), mainframe, -1L, -1) == null,
                            "bsdinstall opens");
                    final InstallerFlow flow = mainframe.installer();
                    helper.assertTrue(flow != null, "the machine opened an installer");
                    flow.setPortsSelected(false);
                    flow.setSshdEnabled(true);
                    flow.setCronEnabled(false);
                    /*
                     * The components page's own checkbox resizes the job the same way the disk and Mirror
                     * pages do, since it changes how much the copy has to cover; done here by hand because this
                     * answer is given straight to the flow rather than through the payload that pairs the two.
                     */
                    final OsInstallJob quoted = mainframe.installing();
                    helper.assertTrue(quoted != null, "bsdinstall quoted a job the moment it opened");
                    mainframe.setInstalling(new OsInstallJob(quoted.osId(), flow.targetSlot(), quoted.readerPos(),
                            flow.ticksTotal(), flow.ticksTotal()));
                    for (int i = 0; i < flow.style().stages().size() && !flow.started(); i++) {
                        flow.next();
                    }
                    final OsInstallJob job = mainframe.installing();
                    helper.assertTrue(job != null, "the copy started");
                    for (int i = 0; i <= job.ticksTotal(); i++) {
                        OsInstallRunner.tick(mainframe, helper.getLevel(), pos);
                    }
                    helper.assertTrue(flow.page() == InstallerPage.SERVICES,
                            "the extraction stops on the page that still asks something");
                    flow.next();
                    for (int i = 0; i <= 10; i++) {
                        OsInstallRunner.tick(mainframe, helper.getLevel(), pos);
                    }
                    helper.assertTrue(mainframe.console().settings().remoteAllowed(), "sshd was ticked on");
                    helper.assertFalse(mainframe.console().settings().cronEnabled(), "cron was unticked");
                    final ItemStack disk = mainframe.diskInSlot(0);
                    helper.assertFalse(
                            DiskFilesystem.listDirs(disk, "usr", FilesystemKind.HIERARCHICAL).contains("usr/ports"),
                            "with ports unticked, no ports tree is laid down");
                })
                .thenSucceed();
    }

    /**
     * With a Mirror in use, the extraction still has to stop on Services and then Desktop before the system is
     * written, and the ports component comes down as the full tree the Mirror serves rather than an empty folder.
     */
    @GameTest(template = ARENA)
    public static void bsdinstall_reachesServicesAndDesktopBeforeWritingTheSystem(final GameTestHelper helper) {
        final BlockPos pos = new BlockPos(2, 2, 2);
        final MainframeBlockEntity mainframe = mainframeWithBsdInstallMedia(helper, pos);
        mainframe.installMirror();
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 4, () -> {
                    mainframe.setNeedsPost(false);
                    helper.assertTrue(FirmwarePayloads.beginInstall(helper.getLevel(), mainframe, -1L, -1) == null,
                            "bsdinstall opens");
                    final InstallerFlow flow = mainframe.installer();
                    helper.assertTrue(flow != null && flow.mirrorAnswers(), "the network's own Mirror answers");
                    for (int i = 0; i < flow.style().stages().size() && !flow.started(); i++) {
                        flow.next();
                    }
                    final OsInstallJob job = mainframe.installing();
                    helper.assertTrue(job != null, "the copy started");
                    for (int i = 0; i <= job.ticksTotal() && flow.page() != InstallerPage.SERVICES; i++) {
                        OsInstallRunner.tick(mainframe, helper.getLevel(), pos);
                    }
                    helper.assertTrue(flow.page() == InstallerPage.SERVICES,
                            "the extraction stops on Services rather than finishing behind it: " + flow.page());
                    helper.assertTrue(mainframe.installedOsId() == null,
                            "the system is not written while a page that asks is still ahead");
                    flow.setCronEnabled(true);
                    flow.setSshdEnabled(true);
                    flow.next();
                    helper.assertTrue(flow.page() == InstallerPage.DESKTOP,
                            "a Mirror in use puts the desktop page next: " + flow.page());
                    helper.assertTrue(mainframe.installedOsId() == null,
                            "and the system is still not written while the desktop page is unanswered");
                    helper.assertTrue(!flow.desktops().isEmpty(), "the Mirror offers at least one desktop");
                    final InstallerFlow.Desktop chosen = flow.desktops().get(0);
                    final int beforeTotal = mainframe.installing().ticksTotal();
                    // The same apply-and-resize a real ACTION_DESKTOP answer runs, not a bare flow mutation.
                    InstallerPayloads.chooseDesktopAndResize(mainframe, flow, 0);
                    final OsInstallJob resized = mainframe.installing();
                    helper.assertTrue(resized != null && resized.ticksTotal() == beforeTotal + chosen.ticks(),
                            "choosing the desktop grows the job by exactly its own ticks; got "
                                    + (resized == null ? "none" : resized.ticksTotal()) + " for "
                                    + (beforeTotal + chosen.ticks()));
                    flow.next();
                    helper.assertTrue(flow.page() == InstallerPage.COPY,
                            "the desktop's fetch runs on the second copy stage, after the desktop page: "
                                    + flow.page());
                    helper.assertTrue(mainframe.installedOsId() == null,
                            "and the system is still not written before that fetch ticks out");
                    for (int i = 0; i <= resized.ticksTotal() && mainframe.installedOsId() == null; i++) {
                        OsInstallRunner.tick(mainframe, helper.getLevel(), pos);
                    }
                    helper.assertTrue(FREEBSD.equals(mainframe.installedOsId()),
                            "the system is written once the fetch and both pages are answered");
                    helper.assertTrue(mainframe.console().settings().cronEnabled(), "cron was answered on Services");
                    helper.assertTrue(mainframe.console().settings().remoteAllowed(), "sshd was answered on Services");
                    helper.assertTrue(mainframe.console().isInstalled(chosen.id()),
                            "the desktop chosen from the Mirror is installed with the system");
                    final ItemStack disk = mainframe.diskInSlot(0);
                    helper.assertTrue(DiskFilesystem.exists(disk, "usr/ports/INDEX-14"),
                            "with the Mirror in use, the ports tree is laid full rather than empty");
                })
                .thenSucceed();
    }

    /** A running Mainframe with an empty disk and, beside it, a CD drive holding FreeBSD's guided installer. */
    private static MainframeBlockEntity mainframeWithBsdInstallMedia(final GameTestHelper helper, final BlockPos pos) {
        helper.setBlock(pos, ComputingModule.MAINFRAME.get());
        if (!(helper.getBlockEntity(pos) instanceof MainframeBlockEntity mainframe)) {
            throw new IllegalStateException("no Mainframe at " + pos);
        }
        final ItemStackHandler inv = mainframe.getInventory();
        inv.setStackInSlot(MainframeBlockEntity.MOTHERBOARD_SLOT,
                new ItemStack(ComputingModule.MOTHERBOARD_MTX_P.get()));
        inv.setStackInSlot(MainframeBlockEntity.CPU_SLOTS_START, new ItemStack(ComputingModule.CPU_SERVO_2620.get()));
        inv.setStackInSlot(MainframeBlockEntity.RAM_SLOTS_START, new ItemStack(ComputingModule.RAM_DDR3_8192.get()));
        inv.setStackInSlot(MainframeBlockEntity.PSU_SLOT, new ItemStack(ComputingModule.PSU_650G.get()));
        inv.setStackInSlot(MainframeBlockEntity.GPU_SLOTS_START, new ItemStack(ComputingModule.GPU_HD_7970.get()));
        inv.setStackInSlot(MainframeBlockEntity.DISK_SLOTS_START,
                new ItemStack(ComputingModule.disk(StorageTier.HDD, DiskSize.GB_500)));
        mainframe.togglePower();

        final BlockPos readerPos = pos.east();
        helper.setBlock(readerPos, ComputingModule.CD_DRIVE.get());
        if (!(helper.getBlockEntity(readerPos) instanceof MediaReaderBlockEntity reader)) {
            throw new IllegalStateException("no reader at " + readerPos);
        }
        final ItemStack disc = new ItemStack(ComputingModule.CD_ROM.get());
        MediaItem.setKind(disc, MediaKind.OS_INSTALL);
        MediaItem.setPayload(disc, FREEBSD);
        reader.mediaSlot().setStackInSlot(0, disc);
        return mainframe;
    }

    /**
     * This PC's About-style page (Info Center on KDE, About on GNOME, System Info on Cinnamon) reads FreeBSD in
     * its own words: the release in the "Operating System" line, Velocion's word for the architecture, the
     * kernel's own build name, the desktop by its plain name with no version, and the machine's own hardware.
     */
    @GameTest(template = ARENA)
    public static void thisPc_aboutPageReadsFreeBsdInItsOwnWords(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = mainframe(helper, WHERE);
        final ResourceLocation kdePlasma = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "kde_plasma");
        mainframe.console().install(kdePlasma.toString());
        mainframe.setBootedDesktopId(kdePlasma);
        final ThisPcPayload.AboutFacts about = ThisPcPayloads.aboutFactsOf(mainframe);
        helper.assertTrue(about.operatingSystem().english().equals("FreeBSD 14.1-RELEASE"),
                "the system by name and release; got " + about.operatingSystem().english());
        helper.assertTrue(about.architecture().english().equals("vel64"),
                "Velocion's word for a 64-bit processor; got " + about.architecture().english());
        helper.assertTrue(about.kernel().english().equals("14.1-RELEASE GENERIC"),
                "the kernel names its own build, as FreeBSD's uname does; got " + about.kernel().english());
        helper.assertTrue(about.desktop().english().equals("KDE Plasma"),
                "the desktop by its plain name, with no version on it; got " + about.desktop().english());
        helper.assertTrue(about.hostName().english().equals(Installers.hostName(mainframe)),
                "the machine's own host name; got " + about.hostName().english());
        helper.assertTrue(about.freeBsd(), "the platform is named FreeBSD's own");
        helper.assertTrue(about.ramMb() == mainframe.ramTotalMb() && about.diskMb() > 0,
                "the machine's real memory and its system disk's real size, not item-equivalents");
        helper.succeed();
    }

    /**
     * A Linux is named by its distribution alone on the About page: the kernel keeps its own row, so the two
     * never repeat the same fact, and its architecture and processor bits are read the Linux way.
     */
    @GameTest(template = ARENA)
    public static void thisPc_aboutPageNamesALinuxByItsDistributionAlone(final GameTestHelper helper) {
        final MainframeBlockEntity ubuntu = mainframeWithOs(helper, WHERE, UBUNTU);
        final ResourceLocation gnome = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "gnome");
        ubuntu.console().install(gnome.toString());
        ubuntu.setBootedDesktopId(gnome);
        final ThisPcPayload.AboutFacts about = ThisPcPayloads.aboutFactsOf(ubuntu);
        helper.assertTrue(about.operatingSystem().english().equals("Ubuntu"),
                "the distribution alone, not the kernel line a second time; got " + about.operatingSystem().english());
        helper.assertTrue(about.kernel().english().equals("6.8-jsc"),
                "a Linux's kernel has no build name of its own to add; got " + about.kernel().english());
        helper.assertTrue(about.architecture().english().equals("x86_64"),
                "a Linux names a 64-bit processor its own way; got " + about.architecture().english());
        helper.assertFalse(about.freeBsd(), "the platform is not named FreeBSD's own");
        helper.assertTrue(about.bits64(), "the seated processor is 64-bit");
        helper.succeed();
    }

    /** A Legacy machine reads IA-32, the narrower word FreeBSD and UNIX give a 32-bit processor. */
    @GameTest(template = ARENA)
    public static void thisPc_aboutPageOnALegacyMachineReadsIa32(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = legacy(helper, WHERE);
        if (computer == null) {
            return;
        }
        helper.assertTrue(computer.installOs(FREEBSD), "FreeBSD installs on a Legacy machine");
        final ResourceLocation kdePlasma = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "kde_plasma");
        computer.console().install(kdePlasma.toString());
        computer.setBootedDesktopId(kdePlasma);
        final ThisPcPayload.AboutFacts about = ThisPcPayloads.aboutFactsOf(computer);
        helper.assertTrue(about.architecture().english().equals("IA-32"),
                "a 32-bit Legacy processor is IA-32, not the maker's 64-bit word; got "
                        + about.architecture().english());
        helper.assertFalse(about.bits64(), "the seated processor is not 64-bit");
        helper.succeed();
    }

    /** A system with no About-style page of its own (every one but FreeBSD and a Linux) answers with nothing. */
    @GameTest(template = ARENA)
    public static void thisPc_aboutPageIsEmptyOnASystemWithNoneOfItsOwn(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = legacy(helper, new BlockPos(6, 2, 2));
        if (computer == null) {
            return;
        }
        helper.assertTrue(computer.installOs(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "frames_xp")),
                "Frames XP installs on the machine");
        helper.assertTrue(ThisPcPayloads.aboutFactsOf(computer).equals(ThisPcPayload.AboutFacts.EMPTY),
                "Frames has its own explorer-style This PC, and no About page beside it");
        helper.succeed();
    }

    private static boolean has(final BootSequence sequence, final String label) {
        for (final BootSequence.Line line : sequence.lines()) {
            if (line.label().english().equals(label)) {
                return true;
            }
        }
        return false;
    }

    private static boolean any(final BootSequence sequence, final String text) {
        for (final BootSequence.Line line : sequence.lines()) {
            if (line.label().english().contains(text) || line.value().english().contains(text)) {
                return true;
            }
        }
        return false;
    }

    private static String last(final BootSequence sequence) {
        return sequence.lines().isEmpty() ? ""
                : sequence.lines().get(sequence.lines().size() - 1).label().english();
    }

    private static String labels(final BootSequence sequence) {
        final StringBuilder out = new StringBuilder();
        for (final BootSequence.Line line : sequence.lines()) {
            out.append(line.label().english()).append(" | ");
        }
        return out.toString();
    }

    /** All of a shell response's lines joined with newlines. */
    private static String text(final CliShell.Response response) {
        final StringBuilder out = new StringBuilder();
        for (final CliLine line : response.lines()) {
            out.append(line.text()).append('\n');
        }
        return out.toString();
    }

    /** A powered Mainframe with a full build and a disk, with FreeBSD installed on it. */
    private static MainframeBlockEntity mainframe(final GameTestHelper helper, final BlockPos pos) {
        return mainframeWithOs(helper, pos, FREEBSD);
    }

    /** A powered Mainframe with a full, 64-bit build and a disk, with {@code osId} installed on it. */
    private static MainframeBlockEntity mainframeWithOs(final GameTestHelper helper, final BlockPos pos,
                                                         final ResourceLocation osId) {
        helper.setBlock(pos, ComputingModule.MAINFRAME.get());
        if (!(helper.getBlockEntity(pos) instanceof MainframeBlockEntity mainframe)) {
            throw new IllegalStateException("no MainframeBlockEntity at " + pos);
        }
        final ItemStackHandler inv = mainframe.getInventory();
        inv.setStackInSlot(MainframeBlockEntity.MOTHERBOARD_SLOT,
                new ItemStack(ComputingModule.MOTHERBOARD_MTX_P.get()));
        inv.setStackInSlot(MainframeBlockEntity.CPU_SLOTS_START, new ItemStack(ComputingModule.CPU_SERVO_2620.get()));
        inv.setStackInSlot(MainframeBlockEntity.RAM_SLOTS_START, new ItemStack(ComputingModule.RAM_DDR3_8192.get()));
        inv.setStackInSlot(MainframeBlockEntity.PSU_SLOT, new ItemStack(ComputingModule.PSU_650G.get()));
        inv.setStackInSlot(MainframeBlockEntity.DISK_SLOTS_START,
                new ItemStack(ComputingModule.disk(StorageTier.HDD, DiskSize.GB_500)));
        mainframe.togglePower();
        if (!mainframe.installOs(osId)) {
            throw new IllegalStateException("failed to install " + osId + " on the test Mainframe");
        }
        return mainframe;
    }

    /** A Legacy machine: the 32-bit generation, the earliest FreeBSD installs on. */
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
        hardware.setStackInSlot(PersonalComputerBlockEntity.PSU_SLOT, new ItemStack(HardwareItems.PSU_500B.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.DISK_SLOTS_START,
                new ItemStack(ComputingModule.disk(StorageTier.HDD, DiskSize.GB_500)));
        return computer;
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
