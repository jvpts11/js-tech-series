/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.machine.FileService;
import dev.jstech.computers.machine.MachinePrograms;
import dev.jstech.computers.sigma.SigmaCompiler;
import dev.jstech.computers.sigma.SourceFile;
import dev.jstech.core.time.WorldCalendar;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The third version's library on a real machine where it reaches the world: the world's calendar, the machine's
 * folders, a file's size and the tick it changed in, and being told when a file changes.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class SigmaToolkitGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 2;
    private static final BlockPos AT = new BlockPos(2, 2, 2);
    private static final String PRELUDE = "using System.*; using System.IO.*; using System.Utils.*; "
            + "using Standard.Script; namespace Programs; ";

    private SigmaToolkitGameTests() {
    }

    @GameTest(template = ARENA)
    public static void calendar_aProgramReadsTheWorldsCalendar(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(AT);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final WorldCalendar.Moment now = WorldCalendar.now(helper.getLevel());
                    final List<String> said = run(helper, pc, """
                            class Reader { static void Main() {
                                Console.PrintLine(Calendar.Day + " " + Calendar.Hour + " " + Calendar.Season);
                            } }
                            """);
                    helper.assertValueEqual(said, List.of(now.day() + " " + now.hour() + " "
                            + now.season().serializedName()), "what the program read");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void files_aProgramMeasuresFilesAndKeepsFolders(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(AT);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final long tick = helper.getLevel().getGameTime();
                    final List<String> said = run(helper, pc, """
                            class Reader { static void Main() {
                                Console.PrintLine(Directory.Make("C:\\\\logs") ? "made" : "refused");
                                Console.PrintLine(Directory.Exists("C:\\\\logs") ? "there" : "gone");
                                File.Write("C:\\\\logs\\\\stock.txt", "iron 64");
                                Console.PrintLine(File.Size("C:\\\\logs\\\\stock.txt") + " "
                                        + File.Size("C:\\\\logs\\\\none.txt"));
                                Console.PrintLine("" + File.Changed("C:\\\\logs\\\\stock.txt"));
                                foreach (string name in Directory.Files("C:\\\\logs")) {
                                    Console.PrintLine("file " + name);
                                }
                                foreach (string name in Directory.Folders("C:\\\\")) {
                                    if (name == "logs") { Console.PrintLine("folder " + name); }
                                }
                                File.Delete("C:\\\\logs\\\\stock.txt");
                                Console.PrintLine(Directory.Delete("C:\\\\logs") ? "deleted" : "kept");
                            } }
                            """);
                    helper.assertValueEqual(said, List.of("made", "there", "7 -1", String.valueOf(tick),
                            "file stock.txt", "folder logs", "deleted"), "what the program did");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void watch_aProgramIsToldWhenAFileItWatchesChanges(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(AT);
        final int[] id = new int[1];
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final SigmaCompiler.Result built = SigmaCompiler.compile(List.of(new SourceFile("Watcher.sgs",
                            PRELUDE + """
                                    class Watcher : Script {
                                        Subscription told;
                                        public override void OnInit() {
                                            told = File.Watch("C:\\\\watched.txt",
                                                    changed => Console.PrintLine("changed " + changed.Path));
                                        }
                                    }
                                    """)));
                    helper.assertTrue(built.ok(), "the watcher compiles: " + String.join("\n", built.lines()));
                    final MachinePrograms.Started started = pc.programs().start("watcher.asm", built.assembly(), 1,
                            pc);
                    helper.assertTrue(started.ok(), "it starts: " + started.message());
                    id[0] = started.id();
                    // The watch is set, and the machine reads the file's tick as it reads a watched item's stock.
                    pc.programs().tick(100000, pc::networkStock);
                    pc.programs().tick(100000, pc::networkStock);
                    helper.assertTrue(pc.programs().byId(id[0]).process().console().isEmpty(), "nothing yet");
                })
                .thenExecuteAfter(SETTLE, () -> {
                    final FileService files = pc.services().files();
                    helper.assertTrue(files != null && files.write("C:\\watched.txt", "now"), "the file is written");
                    for (int i = 0; i < 4; i++) {
                        pc.programs().tick(100000, pc::networkStock);
                    }
                    final List<String> said = pc.programs().byId(id[0]).process().console();
                    helper.assertValueEqual(said, List.of("changed C:\\watched.txt"), "the program is told once");
                })
                .thenSucceed();
    }

    /* Runs {@code source} on {@code computer} to its end and gives back what it printed. */
    private static List<String> run(final GameTestHelper helper, final AbstractComputerBlockEntity computer,
                                    final String source) {
        final SigmaCompiler.Result built = SigmaCompiler.compile(List.of(new SourceFile("Reader.sgs",
                PRELUDE + source)));
        helper.assertTrue(built.ok(), "the program compiles: " + String.join("\n", built.lines()));
        final MachinePrograms.Started started = computer.programs().start("reader.asm", built.assembly(), 1,
                computer);
        helper.assertTrue(started.ok(), "the program starts: " + started.message());
        computer.programs().tick(100000);
        return computer.programs().byId(started.id()).process().console();
    }
}
