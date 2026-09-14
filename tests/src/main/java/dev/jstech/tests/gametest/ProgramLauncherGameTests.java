/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.machine.ProgramLauncher;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.vm.program.IProgramParent;
import dev.jstech.computers.vm.program.ProgramPriority;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Every way of starting a program from a file goes through one launcher: the prompt, a program starting another, a
 * program on another machine, a Gateway and the desktop. These put the launcher itself through a start and each way
 * it refuses, with the file handed straight to it, so what they check is the order and the answers and not a disk.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ProgramLauncherGameTests {

    private static final String ARENA = "empty";

    private static final String HELLO = """
            using System.*;
            using System.IO.*;
            namespace Programs;
            class Hello {
                static void Main() {
                    Console.PrintLine("hello");
                }
            }
            """;

    private ProgramLauncherGameTests() {
    }

    private static PersonalComputerBlockEntity computer(final GameTestHelper helper) {
        return TestWorldBuilder.at(helper.getLevel(), helper.absolutePos(BlockPos.ZERO))
                .placeRunningPersonalComputer(new BlockPos(2, 2, 2));
    }

    /** Launches the file at {@code path}, handing the launcher {@code file} whatever path it asks for. */
    private static ProgramLauncher.Launch launch(final PersonalComputerBlockEntity computer, final String path,
                                                 final ICliComputer.FsResult file) {
        return ProgramLauncher.launch(computer, path, asked -> file, List.of(), IProgramParent.NONE,
                ProgramPriority.MEDIUM, 0);
    }

    @GameTest(template = ARENA)
    public static void launch_startsAProgramFromTheTextTheReaderGives(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = computer(helper);

        final ProgramLauncher.Launch launch =
                launch(computer, "C:\\progs\\hello.sgs", ICliComputer.FsResult.ok(HELLO));

        helper.assertTrue(launch.ok(), "it starts: " + launch.message());
        helper.assertTrue("hello.sgs".equals(launch.name()),
                "it is named by its file, whichever slash the path is written with; got " + launch.name());
        helper.assertTrue(computer.programs().byId(launch.id()) != null, "the machine lists it");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void launch_refusesAKindNothingRunsWithoutReadingIt(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = computer(helper);
        final boolean[] read = new boolean[1];

        final ProgramLauncher.Launch launch = ProgramLauncher.launch(computer, "thing.zz", asked -> {
            read[0] = true;
            return ICliComputer.FsResult.ok("");
        }, List.of(), IProgramParent.NONE, ProgramPriority.MEDIUM, 0);

        helper.assertTrue(launch.refusal() == ProgramLauncher.Refusal.NO_RUNNER,
                "a kind nothing installed runs is refused; got " + launch.refusal());
        helper.assertFalse(read[0], "and the file is never read");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void launch_refusesAFileTheReaderCannotGive(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = computer(helper);

        final ProgramLauncher.Launch launch = launch(computer, "gone.sgs", ICliComputer.FsResult.fail("no such file"));

        helper.assertTrue(launch.refusal() == ProgramLauncher.Refusal.UNREADABLE, "refused; got " + launch.refusal());
        helper.assertTrue("no such file".equals(launch.message()),
                "with the reader's own words; got " + launch.message());
        helper.assertTrue(computer.programs().isEmpty(), "and nothing is running");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void launch_refusesTextTheLanguageCannotRun(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = computer(helper);

        final ProgramLauncher.Launch launch =
                launch(computer, "broken.asm", ICliComputer.FsResult.ok("this is not an assembly"));

        helper.assertTrue(launch.refusal() == ProgramLauncher.Refusal.NOT_STARTED, "refused; got " + launch.refusal());
        helper.assertTrue(!launch.message().isBlank(), "with the language's reason");
        helper.assertTrue(computer.programs().isEmpty(), "and nothing is running");
        helper.succeed();
    }
}
