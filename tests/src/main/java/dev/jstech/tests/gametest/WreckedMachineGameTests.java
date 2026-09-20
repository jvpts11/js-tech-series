/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.os.FilesystemKind;
import dev.jstech.computers.os.boot.SystemIntegrity;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestWorldBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Wrecking your own computer, and what it costs you.
 *
 * <p>Nothing here is protected: a player who deletes the system has deleted it. What that means is decided by
 * what is missing, and it is decided at the next start, because a machine that is already running has the
 * system in memory and does not care, exactly as a real one would not.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class WreckedMachineGameTests {

    private WreckedMachineGameTests() {
    }

    private static final String ARENA = "empty";
    private static final int SETTLE = 6;

    private static final ResourceLocation FRAMES_XP =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "frames_xp");
    private static final ResourceLocation DEBIAN =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "debian");

    /**
     * A running machine with that system on its disk.
     *
     * <p>Installed twice on purpose when the fixture already put one there: the second is the repair path,
     * and it is what writes the loader onto a disk whose system was put there before there were loaders.
     */
    private static PersonalComputerBlockEntity machine(final GameTestHelper helper,
                                                       final ResourceLocation system) {
        final PersonalComputerBlockEntity computer = TestWorldBuilder.forGameTest(helper)
                .placeRunningPersonalComputer(new BlockPos(2, 2, 2));
        computer.installOs(system);
        computer.installOs(system);
        return computer;
    }

    /** What is installed writes its loader to the disk, or there would be nothing to delete. */
    @GameTest(template = ARENA)
    public static void installing_writesTheFileThatStartsTheSystem(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = machine(helper, FRAMES_XP);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final String loader = SystemIntegrity.loaderOf(computer.installedOs());
                    helper.assertTrue(DiskFilesystem.exists(computer.systemDisk(), loader),
                            "the loader is on the disk at " + loader);
                    helper.assertTrue(SystemIntegrity.check(computer).whole(),
                            "and the machine reads its own disk as whole");
                })
                .thenSucceed();
    }

    /**
     * The loader alone deleted: the system is found and will not start, and the machine says which file it
     * wanted, in the words its own family used.
     */
    @GameTest(template = ARENA)
    public static void deletingTheLoader_leavesASystemThatWillNotStart(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = machine(helper, FRAMES_XP);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final String loader = SystemIntegrity.loaderOf(computer.installedOs());
                    helper.assertTrue(DiskFilesystem.delete(computer.systemDisk(), loader),
                            "nothing is protected, so it really goes");

                    final SystemIntegrity.Result health = SystemIntegrity.check(computer);
                    helper.assertTrue(health.state() == SystemIntegrity.State.NO_LOADER,
                            "the system is found and will not start; got " + health);
                    helper.assertTrue(health.complaint().equals("kickmgr is missing"),
                            "in this family's own words; got " + health.complaint());
                    helper.assertTrue(computer.hasOs(),
                            "the disk still says a system is installed, which is why it is found at all");
                })
                .thenSucceed();
    }

    /** The whole folder gone: nothing to find at all, which is the same as an empty disk. */
    @GameTest(template = ARENA)
    public static void deletingTheSystemFolder_leavesNothingToFind(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = machine(helper, FRAMES_XP);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final String loader = SystemIntegrity.loaderOf(computer.installedOs());
                    DiskFilesystem.delete(computer.systemDisk(), loader);
                    DiskFilesystem.rmdir(computer.systemDisk(),
                            SystemIntegrity.folderOf(computer.installedOs()), FilesystemKind.HIERARCHICAL);

                    helper.assertTrue(SystemIntegrity.check(computer).state() == SystemIntegrity.State.NO_SYSTEM,
                            "there is no system on this disk at all; got " + SystemIntegrity.check(computer));
                })
                .thenSucceed();
    }

    /** A Linux says it in its own words, over its own file, which is the whole point of asking the system. */
    @GameTest(template = ARENA)
    public static void aLinux_complainsTheWayALinuxDoes(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = machine(helper, DEBIAN);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    DiskFilesystem.delete(computer.systemDisk(),
                            SystemIntegrity.loaderOf(computer.installedOs()));
                    final SystemIntegrity.Result health = SystemIntegrity.check(computer);
                    helper.assertTrue(health.complaint().contains("kernel panic"),
                            "a Linux panics; got " + health.complaint());
                })
                .thenSucceed();
    }

    /** Installing over it writes the loader back, which is how a wrecked machine is repaired. */
    @GameTest(template = ARENA)
    public static void installingOverIt_putsTheSystemBack(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = machine(helper, FRAMES_XP);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    DiskFilesystem.write(computer.systemDisk(), "Users/Public/Desktop/notes.txt",
                            dev.jstech.computers.os.fs.FileType.TXT, "the player's own", Long.MAX_VALUE,
                            FilesystemKind.HIERARCHICAL);
                    DiskFilesystem.delete(computer.systemDisk(),
                            SystemIntegrity.loaderOf(computer.installedOs()));

                    computer.installOs(FRAMES_XP);

                    helper.assertTrue(SystemIntegrity.check(computer).whole(),
                            "installing over it puts the system back");
                    helper.assertTrue(DiskFilesystem.exists(computer.systemDisk(),
                                    "Users/Public/Desktop/notes.txt"),
                            "and the player's own files are still there");
                })
                .thenSucceed();
    }
}
