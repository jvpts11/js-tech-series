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
import dev.jstech.computers.operation.payload.program.DesktopShellPayloads;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.computers.os.fs.FilesystemContents;
import dev.jstech.computers.os.fs.StoredFile;
import dev.jstech.computers.program.install.MakeOpts;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TerminalAt;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * A system that builds what it installs, on a machine ticking in a world.
 *
 * <p>Its package manager takes the terminal for as long as the build lasts, the way the real one does, and
 * the program is on the machine when the build ends and not a tick before. Minesweeper is what gets built:
 * sixteen megabytes on the test machine's two thousand megahertz is eight seconds of compiling on one core.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class SourceBuildGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 8;
    private static final String MINESWEEPER = "jsc:minesweeper";

    /** Well past the fetch and the eight seconds of compiling, with the phases either side of them. */
    private static final int A_WHOLE_BUILD = 1_200;

    private SourceBuildGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = A_WHOLE_BUILD)
    public static void emerge_holdsTheTerminalAndInstallsWhenTheBuildEnds(final GameTestHelper helper) {
        final MainframeBlockEntity machine = gentooMachine(helper, new BlockPos(2, 2, 2));
        final TerminalAt term = new TerminalAt(machine, helper.getLevel());
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    machine.installMirror();
                    helper.assertTrue(term.type("emerge mines").contains("Calculating dependencies"),
                            "the manager starts working out what the package needs");
                    helper.assertTrue(term.busy(), "and holds the terminal while it builds");
                    helper.assertFalse(machine.console().isInstalled(MINESWEEPER), "with nothing installed yet");
                    term.type("emerge mines");
                    helper.assertFalse(machine.console().isInstalled(MINESWEEPER),
                            "and what is typed at it meanwhile goes nowhere");
                })
                .thenWaitUntil(() -> helper.assertFalse(term.busy(), "the build is still running"))
                .thenExecute(() -> {
                    helper.assertTrue(machine.console().isInstalled(MINESWEEPER), "the program is there at the end");
                    helper.assertTrue(term.type("emerge mines").contains("already the newest version"),
                            "and asking for it again says it is there");
                    helper.assertFalse(term.busy(), "without building anything");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = A_WHOLE_BUILD)
    public static void emerge_askedFirstAndToldNo_mergesNothing(final GameTestHelper helper) {
        final MainframeBlockEntity machine = gentooMachine(helper, new BlockPos(2, 2, 2));
        final TerminalAt term = new TerminalAt(machine, helper.getLevel());
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    machine.installMirror();
                    term.type("emerge --ask mines");
                })
                .thenWaitUntil(() -> helper.assertTrue(term.asking().contains("Would you like to merge"),
                        "it lists what it would merge and then asks: " + term.asking()))
                .thenExecute(() -> {
                    helper.assertTrue(term.type("n").contains("Quitting."), "told no, it says it is leaving");
                    helper.assertFalse(term.busy(), "and leaves");
                    helper.assertFalse(machine.console().isInstalled(MINESWEEPER), "having merged nothing");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = A_WHOLE_BUILD)
    public static void emerge_stoppedWithControlC_installsNothing(final GameTestHelper helper) {
        final MainframeBlockEntity machine = gentooMachine(helper, new BlockPos(2, 2, 2));
        final TerminalAt term = new TerminalAt(machine, helper.getLevel());
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    machine.installMirror();
                    term.type("emerge mines");
                })
                .thenExecuteAfter(60, () -> {
                    helper.assertTrue(term.busy(), "the build is under way");
                    term.type(DesktopShellPayloads.INTERRUPT);
                    helper.assertFalse(term.busy(), "Ctrl+C gives the prompt back");
                })
                .thenExecuteAfter(400, () -> helper.assertFalse(machine.console().isInstalled(MINESWEEPER),
                        "and a build that was stopped has installed nothing, however long it is left"))
                .thenSucceed();
    }

    /**
     * The jobs the build options allow are what a compile is shared between, so the same package on the same
     * processor is built sooner on the machine whose options allow more of them.
     */
    @GameTest(template = ARENA, timeoutTicks = A_WHOLE_BUILD)
    public static void emerge_isSoonerDoneWithTheJobsItsBuildOptionsAllow(final GameTestHelper helper) {
        final MainframeBlockEntity oneJob = gentooMachine(helper, new BlockPos(1, 2, 2));
        final MainframeBlockEntity fourJobs = gentooMachine(helper, new BlockPos(3, 2, 2));
        final TerminalAt slow = new TerminalAt(oneJob, helper.getLevel());
        final TerminalAt quick = new TerminalAt(fourJobs, helper.getLevel());
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    oneJob.installMirror();
                    fourJobs.installMirror();
                    final ItemStack disk = fourJobs.systemDisk();
                    final FilesystemContents was =
                            disk.getOrDefault(ComputingModule.FILESYSTEM.get(), FilesystemContents.EMPTY);
                    disk.set(ComputingModule.FILESYSTEM.get(), was.withDir("/etc").withDir("/etc/portage")
                            .with(new StoredFile(MakeOpts.PATH, FileType.CFG, "MAKEOPTS=\"-j4\"")));
                    slow.type("emerge mines");
                    quick.type("emerge mines");
                })
                .thenWaitUntil(() -> helper.assertFalse(quick.busy(), "four jobs are still building"))
                .thenExecute(() -> {
                    helper.assertTrue(fourJobs.console().isInstalled(MINESWEEPER), "four jobs have it built");
                    helper.assertTrue(slow.busy(), "while one job is still at it");
                })
                .thenWaitUntil(() -> helper.assertFalse(slow.busy(), "one job is still building"))
                .thenExecute(() -> helper.assertTrue(oneJob.console().isInstalled(MINESWEEPER),
                        "and gets there in the end"))
                .thenSucceed();
    }

    /**
     * A desktop is asked for the way the package tree really names it, and what is built is everything it is
     * made of: the toolkit, the frameworks and its own pieces, one after another, with the desktop there at the
     * end of the last of them.
     */
    @GameTest(template = ARENA, timeoutTicks = 4_000)
    public static void emerge_aDesktopByItsRealName_buildsWhatItIsMadeOfAndIsThereAtTheEnd(
            final GameTestHelper helper) {
        final MainframeBlockEntity machine = gentooMachine(helper, new BlockPos(2, 2, 2));
        final TerminalAt term = new TerminalAt(machine, helper.getLevel());
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    machine.installMirror();
                    // Every core it has, or the test sits through two minutes of one of them compiling a toolkit.
                    final ItemStack disk = machine.systemDisk();
                    final FilesystemContents was =
                            disk.getOrDefault(ComputingModule.FILESYSTEM.get(), FilesystemContents.EMPTY);
                    disk.set(ComputingModule.FILESYSTEM.get(), was.withDir("/etc").withDir("/etc/portage")
                            .with(new StoredFile(MakeOpts.PATH, FileType.CFG, "MAKEOPTS=\"-j6\"")));
                    helper.assertTrue(term.type("emerge no-such-category/no-such-thing").contains("unable to locate"),
                            "a name the tree does not have is not found");
                    term.type("emerge --ask kde-plasma/plasma-meta");
                    helper.assertTrue(term.busy(), "the desktop's own name in the tree is one it knows");
                })
                .thenWaitUntil(() -> helper.assertTrue(term.asking().contains("Would you like to merge"),
                        "it lists what it would merge and then asks: " + term.asking()))
                .thenExecute(() -> term.type("y"))
                .thenWaitUntil(() -> helper.assertFalse(term.busy(), "the desktop is still building"))
                .thenExecute(() -> helper.assertTrue(machine.console().isInstalled("jsc:kde_plasma"),
                        "and the desktop is on the machine once the last of it is merged"))
                .thenSucceed();
    }

    /** A running machine whose system builds what it installs. */
    private static MainframeBlockEntity gentooMachine(final GameTestHelper helper, final BlockPos where) {
        helper.setBlock(where, ComputingModule.MAINFRAME.get());
        if (!(helper.getBlockEntity(where) instanceof MainframeBlockEntity machine)) {
            throw new IllegalStateException("no mainframe at " + where);
        }
        final ItemStackHandler parts = machine.getInventory();
        parts.setStackInSlot(MainframeBlockEntity.MOTHERBOARD_SLOT,
                new ItemStack(ComputingModule.MOTHERBOARD_MTX_P.get()));
        parts.setStackInSlot(MainframeBlockEntity.CPU_SLOTS_START,
                new ItemStack(ComputingModule.CPU_SERVO_2620.get()));
        parts.setStackInSlot(MainframeBlockEntity.RAM_SLOTS_START,
                new ItemStack(ComputingModule.RAM_DDR3_8192.get()));
        parts.setStackInSlot(MainframeBlockEntity.PSU_SLOT, new ItemStack(ComputingModule.PSU_650G.get()));
        parts.setStackInSlot(MainframeBlockEntity.DISK_SLOTS_START,
                new ItemStack(ComputingModule.disk(StorageTier.HDD, DiskSize.GB_500)));
        machine.togglePower();
        if (!machine.installOs(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "gentoo"))) {
            throw new IllegalStateException("the test machine took no system");
        }
        return machine;
    }
}
