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
 * A whole line, the way a shell reads one: commands handing their lines to the next, a file feeding the first,
 * a file catching the last, and a word with a star in it opened out into the names it matches.
 *
 * <p>On a real machine with real disks, because half of what this does is read and write files, and because
 * the two families read a line differently: a Unix shell opens out the star itself and a DOS one hands it to
 * the command, which is how they really were.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ShellPipelineGameTests {

    private ShellPipelineGameTests() {
    }

    private static final String ARENA = "empty";
    private static final int SETTLE = 6;
    private static final int WIDTH = 80;

    private static final ResourceLocation DEBIAN =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "debian");

    /** Something with lines worth picking through, written where the prompt stands. */
    private static final String NOTES = """
            oak log 640
            spruce log 128
            cobblestone 2048
            oak planks 96""";

    private static PersonalComputerBlockEntity machine(final GameTestHelper helper,
                                                       final ResourceLocation system) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final PersonalComputerBlockEntity computer = world.placeRunningPersonalComputer(new BlockPos(2, 2, 2));
        computer.installOs(system);
        return computer;
    }

    /**
     * Puts a file of several lines on the machine's disk.
     *
     * <p>Written to the disk rather than typed at the prompt, because a prompt writes the one line it was
     * given and what these are about is what happens to several.
     */
    private static void file(final GameTestHelper helper, final PersonalComputerBlockEntity computer,
                             final String name, final String text) {
        final String where = new ServerCliComputer(computer, helper.getLevel())
                .currentLocation().storagePath();
        final String path = where.isEmpty() ? name : where + "/" + name;
        DiskFilesystem.write(computer.systemDisk(), path, FileType.TXT, text, Long.MAX_VALUE,
                FilesystemKind.HIERARCHICAL);
    }

    @GameTest(template = ARENA)
    public static void aPipeline_handsEachCommandWhatTheOneBeforeItPrinted(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = machine(helper, DEBIAN);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    file(helper, computer, "notes.txt", NOTES);

                    final List<String> oak = shell(helper, computer, "cat notes.txt | grep oak");
                    helper.assertTrue(says(oak, "oak log") && says(oak, "oak planks") && !says(oak, "cobblestone"),
                            "grep was handed what cat printed; got " + oak);

                    final List<String> counted = shell(helper, computer, "cat notes.txt | grep oak | wc -l");
                    helper.assertTrue(says(counted, "2"), "and wc was handed what grep printed; got " + counted);

                    final List<String> ordered = shell(helper, computer, "cat notes.txt | sort | head -n 1");
                    helper.assertTrue(says(ordered, "cobblestone"),
                            "three of them in a row read as one line of thought; got " + ordered);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void anArrow_putsWhatWasPrintedIntoAFileAndBack(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = machine(helper, DEBIAN);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    file(helper, computer, "notes.txt", NOTES);

                    shell(helper, computer, "cat notes.txt | grep oak > oak.txt");
                    final List<String> back = shell(helper, computer, "cat oak.txt");
                    helper.assertTrue(says(back, "oak log") && !says(back, "cobblestone"),
                            "the arrow put what was printed into the file; got " + back);

                    shell(helper, computer, "cat notes.txt | grep cobble >> oak.txt");
                    final List<String> both = shell(helper, computer, "cat oak.txt");
                    helper.assertTrue(says(both, "oak log") && says(both, "cobblestone"),
                            "and the doubled arrow added to it rather than replacing it; got " + both);

                    final List<String> fed = shell(helper, computer, "sort < oak.txt");
                    helper.assertTrue(says(fed, "cobblestone"), "a file can feed the first command; got " + fed);
                })
                .thenSucceed();
    }

    /**
     * A star is the shell's on a Unix system and the command's on a DOS one, which is what each family really
     * did. Here it is asked of the same machine's own names.
     */
    @GameTest(template = ARENA)
    public static void aStar_isOpenedOutByTheShellThatAlwaysOpenedItOut(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = machine(helper, DEBIAN);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    file(helper, computer, "one.txt", "first");
                    file(helper, computer, "two.txt", "second");
                    file(helper, computer, "three.sgs", "third");

                    final List<String> texts = shell(helper, computer, "cat *.txt");
                    helper.assertTrue(says(texts, "first") && says(texts, "second") && !says(texts, "third"),
                            "the shell handed cat the names the star matched; got " + texts
                                    + " and the folder holds " + shell(helper, computer, "ls"));

                    final List<String> none = shell(helper, computer, "cat *.zip");
                    helper.assertTrue(says(none, "*.zip") || says(none, "no such") || says(none, "not found"),
                            "a star that matches nothing is left as it was typed; got " + none);
                })
                .thenSucceed();
    }

    /** What a name stands for is put in before the command sees it, each family reading its own mark. */
    @GameTest(template = ARENA)
    public static void aName_standsForSomethingBeforeTheCommandSeesIt(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = machine(helper, DEBIAN);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final List<String> said = shell(helper, computer, "echo $HOSTNAME is $USER");
                    helper.assertTrue(says(said, "player") && !says(said, "$"),
                            "the shell put in what the names stand for; got " + said);

                    final List<String> nothing = shell(helper, computer, "echo [$NOWHERE]");
                    helper.assertTrue(says(nothing, "[]"),
                            "and a name nothing stands for comes out as nothing; got " + nothing);
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
