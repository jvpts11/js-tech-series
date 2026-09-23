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
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.CliCommands;
import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.cli.CliShell;
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
        helper.assertTrue(menu.title().equals("Welcome to FreeBSD"), "headed the way it heads it: " + menu.title());
        helper.assertTrue(menu.entries().get(0).label().equals("Boot") && menu.defaultIndex() == 0,
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
                    helper.assertTrue("player@freebsd:~ $".equals(cli.prompt()),
                            "sh stands at the prompt its home directory sets up; got " + cli.prompt());
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
                    helper.assertTrue(text(shell.run("apt install iqlengine", cli)).contains("command not found"),
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

    private static boolean has(final BootSequence sequence, final String label) {
        for (final BootSequence.Line line : sequence.lines()) {
            if (line.label().equals(label)) {
                return true;
            }
        }
        return false;
    }

    private static boolean any(final BootSequence sequence, final String text) {
        for (final BootSequence.Line line : sequence.lines()) {
            if (line.label().contains(text) || line.value().contains(text)) {
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
        if (!mainframe.installOs(FREEBSD)) {
            throw new IllegalStateException("failed to install FreeBSD on the test Mainframe");
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
