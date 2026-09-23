/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.os.MachineMemory;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.os.SourceAdvantage;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.program.ComputerConsoleState;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TerminalAt;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * FreeBSD's ports, on a machine ticking in a world: the tree fetched from the Mirror and laid out on the disk as
 * files, and a port built from it on the machine, which installs a program made for that machine.
 *
 * <p>screenfetch is what gets built: a small port, on the test machine a few seconds of compiling.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class PortsGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos WHERE = new BlockPos(2, 2, 2);

    /** Ticks for a machine's attachment to find what is beside it. */
    private static final int SETTLE = 8;

    /** Well past fetching the tree, laying it out, and fetching and building a small port. */
    private static final int A_WHOLE_BUILD = 1_600;

    private static final String SCREENFETCH = "jsc:screenfetch";
    private static final String VIM = "jsc:vim";
    private static final String PORT = "usr/ports/sysutils/screenfetch";

    private PortsGameTests() {
    }

    /**
     * portsnap brings the tree from the Mirror and lays it out as files that take room, and make install clean in a
     * port's folder builds the program there, installs it as built here and takes the build away after it.
     */
    @GameTest(template = ARENA, timeoutTicks = A_WHOLE_BUILD)
    public static void ports_areFetchedLaidOutAndBuiltIntoAProgramMadeHere(final GameTestHelper helper) {
        final MainframeBlockEntity machine = freebsd(helper, WHERE);
        final TerminalAt term = new TerminalAt(machine, helper.getLevel());
        final long[] before = new long[1];
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(term.type("portsnap fetch extract").contains("none found"),
                            "with no Mirror there is nowhere to fetch the tree from");
                    machine.installMirror();
                    helper.assertTrue(term.type("portsnap extract").contains("No snapshot available"),
                            "and nothing to lay out before a snapshot has been fetched");
                    before[0] = DiskFilesystem.filesWeight(machine.systemDisk());
                    helper.assertTrue(term.type("portsnap fetch extract")
                                    .contains("Looking up the Mirror for the ports tree... found."),
                            "portsnap finds the Mirror");
                    helper.assertTrue(term.busy(), "and holds the terminal while it fetches and lays out");
                })
                .thenWaitUntil(() -> helper.assertFalse(term.busy(), "portsnap is still at work"))
                .thenExecute(() -> {
                    final ItemStack disk = machine.systemDisk();
                    helper.assertTrue(DiskFilesystem.exists(disk, "usr/ports/INDEX-14"), "the index is on the disk");
                    final String makefile = DiskFilesystem.read(disk, PORT + "/Makefile").orElse("");
                    helper.assertTrue(makefile.contains("PORTNAME=\tscreenfetch")
                                    && makefile.contains("CATEGORIES=\tsysutils")
                                    && makefile.contains(".include <bsd.port.mk>"),
                            "each port is filed where the real tree files it, with its Makefile; got " + makefile);
                    helper.assertTrue(DiskFilesystem.filesWeight(disk) > before[0],
                            "and the tree takes room on the disk");
                    helper.assertTrue(term.type("make install clean").contains("don't know how to make install"),
                            "away from a port there is nothing to make");
                    term.type("cd /usr/ports/sysutils/screenfetch");
                    helper.assertTrue("root@freebsd:/usr/ports/sysutils/screenfetch #".equals(term.prompt()),
                            "the port's folder is where the shell stands; got " + term.prompt());
                    helper.assertTrue(term.type("make install clean").contains("License GPLv3 accepted by the user"),
                            "the port opens with its licence");
                    helper.assertTrue(term.busy(), "and holds the terminal while it builds");
                    helper.assertFalse(machine.console().isInstalled(SCREENFETCH), "with nothing installed yet");
                })
                .thenWaitUntil(() -> helper.assertFalse(term.busy(), "the port is still building"))
                .thenExecute(() -> {
                    helper.assertTrue(machine.console().isInstalled(SCREENFETCH), "the program is there at the end");
                    helper.assertTrue(machine.console().builtFromSource(SCREENFETCH), "recorded as built here");
                    helper.assertFalse(DiskFilesystem.exists(machine.systemDisk(),
                                    PORT + "/work/.build_done.screenfetch._usr_local"),
                            "and clean took the build away");
                    helper.assertTrue(term.type("make install").contains("is already installed"),
                            "a second install is refused in the ports' words");
                    helper.assertTrue(term.type("screenfetch").contains("player@freebsd"), "and the program runs");
                })
                .thenSucceed();
    }

    /** A build left without clean is found by the next install, which installs it without building it again. */
    @GameTest(template = ARENA, timeoutTicks = A_WHOLE_BUILD)
    public static void make_aBuildLeftUncleanedIsInstalledWithoutBuildingAgain(final GameTestHelper helper) {
        final MainframeBlockEntity machine = freebsd(helper, WHERE);
        final TerminalAt term = new TerminalAt(machine, helper.getLevel());
        final String cookie = PORT + "/work/.build_done.screenfetch._usr_local";
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    machine.installMirror();
                    term.type("portsnap auto");
                })
                .thenWaitUntil(() -> helper.assertFalse(term.busy(), "portsnap is still at work"))
                .thenExecute(() -> {
                    helper.assertTrue(DiskFilesystem.exists(machine.systemDisk(), PORT + "/Makefile"),
                            "auto lays out a tree the machine does not have yet");
                    term.type("cd /usr/ports/sysutils/screenfetch");
                    term.type("make");
                })
                .thenWaitUntil(() -> helper.assertFalse(term.busy(), "the port is still building"))
                .thenExecute(() -> {
                    helper.assertTrue(DiskFilesystem.exists(machine.systemDisk(), cookie),
                            "a build leaves its mark in the work folder");
                    helper.assertFalse(machine.console().isInstalled(SCREENFETCH), "and installs nothing");
                    final String install = term.type("make install");
                    helper.assertTrue(install.contains("Staging for screenfetch-3.9.1")
                                    && !install.contains("License"),
                            "the next install goes straight to staging; got " + install);
                })
                .thenWaitUntil(() -> helper.assertFalse(term.busy(), "the port is still installing"))
                .thenExecute(() -> {
                    helper.assertTrue(machine.console().isInstalled(SCREENFETCH), "and installs it");
                    helper.assertTrue(DiskFilesystem.exists(machine.systemDisk(), cookie),
                            "leaving the build where it was, not having been told to clean");
                    term.type("make clean");
                })
                .thenWaitUntil(() -> helper.assertFalse(term.busy(), "make clean is still at work"))
                .thenExecute(() -> {
                    helper.assertFalse(DiskFilesystem.exists(machine.systemDisk(), cookie), "until clean takes it");
                    helper.assertTrue(term.type("make deinstall").contains("Deinstalling for screenfetch"),
                            "and deinstall takes the program off, in the ports' words");
                    helper.assertTrue(term.type("make fly").contains("don't know how to make fly"),
                            "a target make does not have is refused");
                })
                .thenSucceed();
    }

    /** update needs a tree to bring up to date, and brings one up to date without laying it out afresh. */
    @GameTest(template = ARENA, timeoutTicks = A_WHOLE_BUILD)
    public static void portsnap_updateNeedsATreeAndKeepsIt(final GameTestHelper helper) {
        final MainframeBlockEntity machine = freebsd(helper, WHERE);
        final TerminalAt term = new TerminalAt(machine, helper.getLevel());
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    machine.installMirror();
                    term.type("portsnap fetch");
                })
                .thenWaitUntil(() -> helper.assertFalse(term.busy(), "portsnap is still fetching"))
                .thenExecute(() -> {
                    helper.assertFalse(DiskFilesystem.exists(machine.systemDisk(), "usr/ports/INDEX-14"),
                            "a fetch alone lays nothing out");
                    helper.assertTrue(term.type("portsnap update").contains("was not created by portsnap"),
                            "and there is no tree to update");
                    term.type("portsnap extract");
                })
                .thenWaitUntil(() -> helper.assertFalse(term.busy(), "portsnap is still laying out"))
                .thenExecute(() -> term.type("portsnap fetch update"))
                .thenWaitUntil(() -> helper.assertFalse(term.busy(), "portsnap is still updating"))
                .thenExecute(() -> {
                    helper.assertTrue(DiskFilesystem.exists(machine.systemDisk(), PORT + "/Makefile"),
                            "the tree is still there after an update");
                    helper.assertTrue(term.type("portsnap sideways").contains("usage: portsnap"),
                            "a command it does not have is answered with its usage");
                })
                .thenSucceed();
    }

    /** A program built on the machine holds a little less memory there than the package would have. */
    @GameTest(template = ARENA)
    public static void builtHere_holdsLessMemoryThanThePackage(final GameTestHelper helper) {
        final MainframeBlockEntity machine = freebsd(helper, WHERE);
        final OsDef os = machine.installedOs();
        final ProgramSpec kde = OsRegistry.getProgram(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID,
                "kde_plasma"));
        helper.assertTrue(os != null && kde != null, "the test machine runs a system and KDE is registered");
        final int asPackage = kde.ramMbOn(os);
        helper.assertTrue(kde.ramMbOn(os, true) == SourceAdvantage.of(asPackage) && kde.ramMbOn(os, true) < asPackage,
                "built here it holds a tenth less; package " + asPackage + ", built " + kde.ramMbOn(os, true));
        final int window = MachineMemory.windowRamMb("Network Management Studio", os, null, spec -> false);
        final int builtWindow = MachineMemory.windowRamMb("Network Management Studio", os, null, spec -> true);
        helper.assertTrue(builtWindow < window, "and so does its window; " + window + " against " + builtWindow);
        helper.succeed();
    }

    /** What was built here goes with the machine through a save, and is forgotten with the program. */
    @GameTest(template = ARENA)
    public static void console_keepsWhatWasBuiltHereAndForgetsItWithTheProgram(final GameTestHelper helper) {
        final ComputerConsoleState state = new ComputerConsoleState();
        state.markBuiltFromSource("jsc:vim");
        helper.assertFalse(state.builtFromSource("jsc:vim"), "a program not installed is not recorded");
        state.install("jsc:vim");
        state.markBuiltFromSource("jsc:vim");
        final CompoundTag saved = new CompoundTag();
        state.save(saved);
        final ComputerConsoleState loaded = new ComputerConsoleState();
        loaded.load(saved);
        helper.assertTrue(loaded.builtFromSource("jsc:vim"), "it comes back from a save");
        loaded.uninstall("jsc:vim");
        helper.assertFalse(loaded.builtFromSource("jsc:vim"), "and goes with the program");
        helper.succeed();
    }

    /**
     * Formatting the system disk takes what was built on it with it: the same program installed again afterwards as a
     * package is a package, and asks what a package asks.
     */
    @GameTest(template = ARENA)
    public static void format_takesTheBuildsWithTheSystem(final GameTestHelper helper) {
        final MainframeBlockEntity machine = freebsd(helper, WHERE);
        final ComputerConsoleState console = machine.console();
        console.install(VIM);
        console.markBuiltFromSource(VIM);
        helper.assertTrue(console.builtFromSource(VIM), "vim is recorded as built here");
        helper.assertTrue(machine.formatDisk(0), "the system disk formats");
        helper.assertFalse(console.isInstalled(VIM), "the format took the program");
        helper.assertFalse(console.builtFromSource(VIM), "and the build with it");
        helper.assertTrue(machine.installOs(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "freebsd")),
                "a system goes back on the disk");
        console.install(VIM);
        helper.assertFalse(console.builtFromSource(VIM), "vim installed again as a package is a package");
        helper.succeed();
    }

    /** A program installed afresh arrives as a package, whatever an older copy of it was. */
    @GameTest(template = ARENA)
    public static void install_aFreshCopyIsAPackageWhateverTheOldOneWas(final GameTestHelper helper) {
        final ComputerConsoleState state = new ComputerConsoleState();
        state.install(VIM);
        state.markBuiltFromSource(VIM);
        helper.assertFalse(state.install(VIM), "installing what is there changes nothing");
        helper.assertTrue(state.builtFromSource(VIM), "and keeps the build it has");
        final CompoundTag stale = new CompoundTag();
        state.save(stale);
        stale.remove("Installed");
        final ComputerConsoleState reloaded = new ComputerConsoleState();
        reloaded.load(stale);
        reloaded.install(VIM);
        helper.assertFalse(reloaded.builtFromSource(VIM),
                "a mark left over from a copy that is gone does not carry to the new one");
        helper.succeed();
    }

    /** A powered Mainframe with a full build and a disk, with FreeBSD installed on it. */
    private static MainframeBlockEntity freebsd(final GameTestHelper helper, final BlockPos pos) {
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
        if (!mainframe.installOs(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "freebsd"))) {
            throw new IllegalStateException("failed to install FreeBSD on the test Mainframe");
        }
        return mainframe;
    }
}
