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
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.CliCommands;
import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * MC-NET keeping files, which it never did.
 *
 * <p>Its kernel declared no filesystem at all while a dozen programs were declared for it, the editors and
 * both compilers among them: a machine with nowhere to read or write, asked to run things that do nothing
 * else. It now keeps a flat store, files at the root and no folders, which is what a network appliance of
 * that age had and what tells it apart from the personal computer of the same year.
 *
 * <p>The first thing that store holds is the file that starts the system, so MC-NET can be wrecked like
 * every other system here. With no folder to lose it has one way of breaking where the others have two.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class McNetGameTests {

    private McNetGameTests() {
    }

    private static final String ARENA = "empty";
    private static final int SETTLE = 6;
    private static final int WIDTH = 80;

    private static final ResourceLocation MC_NET =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "mc_net");

    /** A running machine with MC-NET on its disk. */
    private static PersonalComputerBlockEntity machine(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = TestWorldBuilder.forGameTest(helper)
                .placeRunningPersonalComputer(new BlockPos(2, 2, 2));
        computer.installOs(MC_NET);
        computer.installOs(MC_NET);
        return computer;
    }

    /** The disk keeps files, which is what a dozen programs declared for this machine always needed. */
    @GameTest(template = ARENA)
    public static void theDisk_keepsFiles(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = machine(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final ServerCliComputer shell = new ServerCliComputer(computer, helper.getLevel());
                    helper.assertTrue(shell.hasFiles(),
                            "a machine a text editor is declared for has somewhere to put a file");

                    DiskFilesystem.write(computer.systemDisk(), "notes.txt", FileType.TXT, "kept",
                            Long.MAX_VALUE, FilesystemKind.FLAT);
                    helper.assertTrue(DiskFilesystem.exists(computer.systemDisk(), "notes.txt"),
                            "and what is written to it is there afterwards");
                    helper.assertTrue(DiskFilesystem.list(computer.systemDisk(), "", FilesystemKind.FLAT)
                                    .stream().anyMatch(e -> e.path().equals("notes.txt")),
                            "and the disk lists it");
                })
                .thenSucceed();
    }

    /** Flat means flat: there are no folders to make, and no name may pretend to be in one. */
    @GameTest(template = ARENA)
    public static void theDisk_hasNoFoldersAtAll(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = machine(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(!DiskFilesystem.mkdir(computer.systemDisk(), "progs",
                                    FilesystemKind.FLAT),
                            "a flat disk refuses a folder");
                    helper.assertTrue(DiskFilesystem.listDirs(computer.systemDisk(), "",
                            FilesystemKind.FLAT).isEmpty(), "and has none to list");
                })
                .thenSucceed();
    }

    /**
     * MC-NET stops speaking DOS: its own words answer, and the DOS family's do not.
     *
     * <p>Held to account from the prompt rather than from the registry, because what matters is that a
     * player typing the old verb is told it is not here and a player typing the new one is answered.
     */
    @GameTest(template = ARENA)
    public static void thePrompt_speaksItsOwnWordsAndNotTheDosFamilys(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = machine(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    DiskFilesystem.write(computer.systemDisk(), "notes.txt", FileType.TXT, "a line",
                            Long.MAX_VALUE, FilesystemKind.FLAT);

                    final List<String> listed = shell(helper, computer, "listfiles");
                    helper.assertTrue(says(listed, "notes.txt"),
                            "listfiles answers and lists the disk; got " + listed);

                    final List<String> seen = shell(helper, computer, "seefile notes.txt");
                    helper.assertTrue(says(seen, "a line"), "seefile puts it on the glass; got " + seen);

                    for (final String gone : new String[] {"dir", "type notes.txt", "cd", "mkdir progs",
                            "tree"}) {
                        helper.assertTrue(says(shell(helper, computer, gone), "command not found"),
                                "the DOS word '" + gone + "' is not this machine's");
                    }
                })
                .thenSucceed();
    }

    /** The prompt names the machine, because a flat disk has nowhere to be standing. */
    @GameTest(template = ARENA)
    public static void thePrompt_namesTheMachineAndNotAPlace(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = machine(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final ServerCliComputer shell = new ServerCliComputer(computer, helper.getLevel());
                    helper.assertTrue(shell.prompt().equals("SYSTEM:>"),
                            "no drive letter and no path; got " + shell.prompt());
                })
                .thenSucceed();
    }

    /** Installing writes the file that starts the system, at the root, because there is nowhere else. */
    @GameTest(template = ARENA)
    public static void installing_writesTheFileThatStartsItAtTheRoot(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = machine(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final String loader = SystemIntegrity.loaderOf(computer.installedOs());
                    helper.assertTrue(loader.equals("netstart.sys"),
                            "MC-NET's loader is its own; got " + loader);
                    helper.assertTrue(!loader.contains("/"),
                            "and it is at the root, since a flat disk has no folder to hold it");
                    helper.assertTrue(DiskFilesystem.exists(computer.systemDisk(), loader),
                            "installing put it there");
                    helper.assertTrue(SystemIntegrity.check(computer).whole(),
                            "so the machine reads its own disk as whole");
                })
                .thenSucceed();
    }

    /** Deleting it leaves a system that is found and will not start, in Nouvell's own words. */
    @GameTest(template = ARENA)
    public static void deletingTheLoader_leavesASystemThatWillNotStart(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = machine(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(DiskFilesystem.delete(computer.systemDisk(), "netstart.sys"),
                            "nothing is protected here either, so it really goes");

                    final SystemIntegrity.Result health = SystemIntegrity.check(computer);
                    helper.assertTrue(health.state() == SystemIntegrity.State.NO_LOADER,
                            "the system is found and will not start; got " + health);
                    helper.assertTrue(health.complaint().equals("netstart.sys is missing"),
                            "in this system's own words; got " + health.complaint());
                    helper.assertTrue(computer.hasOs(),
                            "and the disk still says a system is installed, which is why it is found");
                })
                .thenSucceed();
    }

    /**
     * With no folders there is no folder to lose, so this system has one way of breaking, not two.
     *
     * <p>Held to account because the check asks after a system folder for every other system, and asking
     * after one that cannot exist would call a machine empty the moment it was installed.
     */
    @GameTest(template = ARENA)
    public static void aFlatSystem_hasNoWayOfLosingItsFolder(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = machine(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(SystemIntegrity.folderOf(computer.installedOs()).isEmpty(),
                            "there is no system folder on a flat disk");
                    helper.assertTrue(SystemIntegrity.check(computer).whole(),
                            "and a freshly installed machine is whole, not empty");

                    DiskFilesystem.delete(computer.systemDisk(), "netstart.sys");
                    helper.assertTrue(SystemIntegrity.check(computer).state()
                                    == SystemIntegrity.State.NO_LOADER,
                            "the one way it breaks is the file that starts it");
                })
                .thenSucceed();
    }

    /** Installing over a wrecked one writes the file back and leaves the player's own files alone. */
    @GameTest(template = ARENA)
    public static void installingOverIt_putsTheSystemBack(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = machine(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    DiskFilesystem.write(computer.systemDisk(), "iron.craft", FileType.CRAFT, "the player's",
                            Long.MAX_VALUE, FilesystemKind.FLAT);
                    DiskFilesystem.delete(computer.systemDisk(), "netstart.sys");

                    computer.installOs(MC_NET);

                    helper.assertTrue(SystemIntegrity.check(computer).whole(),
                            "installing over it puts the system back");
                    helper.assertTrue(DiskFilesystem.exists(computer.systemDisk(), "iron.craft"),
                            "and the player's own files are still there");
                })
                .thenSucceed();
    }

    private static List<String> shell(final GameTestHelper helper, final PersonalComputerBlockEntity on,
                                      final String command) {
        final ServerCliComputer computer = new ServerCliComputer(on, helper.getLevel());
        final List<String> out = new ArrayList<>();
        for (final CliLine line : CliCommands.shellFor(computer, WIDTH).run(command, computer).lines()) {
            out.add(line.text());
        }
        return out;
    }

    private static boolean says(final List<String> lines, final String text) {
        return lines.stream().anyMatch(line -> line.contains(text));
    }
}
