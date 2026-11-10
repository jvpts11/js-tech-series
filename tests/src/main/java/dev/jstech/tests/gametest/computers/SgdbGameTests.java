/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.machine.MachinePrograms;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.CliCommands;
import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.cli.CliShell;
import dev.jstech.computers.program.cli.SigmaCommands;
import dev.jstech.computers.program.tty.ITtyProcess;
import dev.jstech.computers.program.tty.ITtySink;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * sgdb on a running machine: a program built with sgsc --debug runs under it, stops at a breakpoint, shows its
 * variables and its calls, steps a line at a time, and runs on to its end, spending as it runs.
 *
 * <p>The debugger is driven here as a terminal drives it, a typed line and a tick at a time, with the lines it prints
 * kept, since nobody is at the machine to see them.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class SgdbGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos WHERE = new BlockPos(2, 2, 2);
    /** Long enough for a machine to be running and for a program to reach where it stops. */
    private static final int SETTLE = 8;
    private static final int WIDTH = 80;
    private static final String PLANT = String.join("\n",
            "using System.IO.*; namespace Plant;",
            "class Plant {",
            "    static int Twice(int n) {",
            "        int doubled = n * 2;",
            "        return doubled;",
            "    }",
            "    static void Main() {",
            "        long logs = 41;",
            "        Console.PrintLine(\"Logs: \" + logs);",
            "        if (logs < 64) {",
            "            int more = Twice(21);",
            "            Console.PrintLine(\"\" + more);",
            "        }",
            "    }",
            "}");

    private SgdbGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void sgdb_stopsStepsShowsAndRunsTheProgramToItsEnd(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = machine(helper);
        final List<ITtyProcess> held = new ArrayList<>();
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final ITtyProcess sgdb = sgdb(helper, computer);
                    held.add(sgdb);
                    helper.assertTrue(answer(sgdb, "break 10").contains("Breakpoint 1 at Plant.sgs, line 10."),
                            "a breakpoint before the program runs");
                    answer(sgdb, "run");
                })
                .thenExecuteAfter(SETTLE, () -> {
                    final String stopped = advance(held.getFirst(), helper);
                    helper.assertTrue(stopped.contains("Logs: 41"), "what it printed comes first; got " + stopped);
                    helper.assertTrue(stopped.contains("Breakpoint 1, Plant.Main () at Plant.sgs:10")
                            && stopped.contains("if (logs < 64) {"), "then where it stopped, with the line; got "
                            + stopped);
                    helper.assertTrue(answer(held.getFirst(), "print logs").contains("logs = 41 (long)"),
                            "a variable is shown with its type");
                    final CompoundTag saved = new CompoundTag();
                    computer.programs().save(saved);
                    final MachinePrograms read = new MachinePrograms();
                    read.load(saved, computer);
                    helper.assertTrue(plants(computer.programs()) == 1 && plants(read) == 0,
                            "the program is running, and is not written down with the world");
                    answer(held.getFirst(), "next");
                })
                .thenExecuteAfter(SETTLE, () -> {
                    final String next = advance(held.getFirst(), helper);
                    helper.assertTrue(next.contains("int more = Twice(21);") && !next.contains("Plant.Main ("),
                            "next goes to the next line of the same call, which it does not name; got " + next);
                    answer(held.getFirst(), "step");
                })
                .thenExecuteAfter(SETTLE, () -> {
                    final String into = advance(held.getFirst(), helper);
                    helper.assertTrue(into.contains("Plant.Twice (n=21) at Plant.sgs:4"),
                            "step goes into the call and names it with what it was handed; got " + into);
                    final String trace = answer(held.getFirst(), "backtrace");
                    helper.assertTrue(trace.contains("#0  Plant.Twice (n=21) at Plant.sgs:4")
                            && trace.contains("#1  Plant.Main () at Plant.sgs:11"), "the calls it is in; got " + trace);
                    final String credits = answer(held.getFirst(), "info credits");
                    helper.assertTrue(credits.contains("credits spent: each step pays for what it runs"),
                            "and what it has spent; got " + credits);
                    answer(held.getFirst(), "continue");
                })
                .thenExecuteAfter(SETTLE, () -> {
                    final String end = advance(held.getFirst(), helper);
                    helper.assertTrue(end.contains("42") && end.contains("[Plant.asm exited with code 0]"),
                            "it runs on to its end; got " + end);
                    answer(held.getFirst(), "quit");
                    helper.assertTrue(held.getFirst().over(), "and quit gives the terminal back");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void quit_inTheMiddleEndsTheProgramItWasDebugging(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = machine(helper);
        final List<ITtyProcess> held = new ArrayList<>();
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    held.add(sgdb(helper, computer));
                    answer(held.getFirst(), "break 4");
                    answer(held.getFirst(), "run");
                })
                .thenExecuteAfter(SETTLE, () -> {
                    final String stopped = advance(held.getFirst(), helper);
                    helper.assertTrue(plants(computer.programs()) == 1, "stopped, it is still a program; " + stopped);
                    answer(held.getFirst(), "quit");
                    helper.assertTrue(plants(computer.programs()) == 0, "and quitting sgdb ends it at once");
                })
                .thenSucceed();
    }

    /** A running Linux machine with the compiler and the runtime, and the program's source in its home folder. */
    private static PersonalComputerBlockEntity machine(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = TestWorldBuilder.forGameTest(helper)
                .placeRunningPersonalComputer(WHERE, ResourceLocation.fromNamespaceAndPath(JsComputers.MODID,
                        "ubuntu"));
        computer.console().install(SigmaCommands.COMPILER);
        computer.console().install(SigmaCommands.RUNTIME);
        return computer;
    }

    /* Builds the program for the debugger at the machine's prompt and starts sgdb on it. */
    private static ITtyProcess sgdb(final GameTestHelper helper, final PersonalComputerBlockEntity computer) {
        final ServerCliComputer cli = new ServerCliComputer(computer, helper.getLevel());
        cli.writeFile("Plant.sgs", PLANT);
        final CliShell shell = CliCommands.shellFor(cli, WIDTH);
        final String built = text(shell.run("sgsc --debug Plant.sgs", cli).lines());
        helper.assertTrue(built.contains("with its lines for the debugger"), "built for it; got " + built);
        final ITtyProcess sgdb = shell.run("sgdb Plant.asm", cli).started();
        helper.assertTrue(sgdb != null, "sgdb holds the terminal");
        return sgdb;
    }

    /* How many of the machine's programs run the listing debugged here. */
    private static long plants(final MachinePrograms programs) {
        return programs.view().stream().filter(one -> one.file().endsWith("Plant.asm")).count();
    }

    private static String answer(final ITtyProcess tool, final String line) {
        final Lines out = new Lines();
        tool.answer(line, 0, out);
        return out.text();
    }

    private static String advance(final ITtyProcess tool, final GameTestHelper helper) {
        final Lines out = new Lines();
        tool.advance(helper.getLevel().getGameTime(), out);
        return out.text();
    }

    private static String text(final List<CliLine> lines) {
        final StringBuilder text = new StringBuilder();
        for (final CliLine line : lines) {
            text.append(line.text()).append('\n');
        }
        return text.toString();
    }

    /** What the debugger printed, kept in order. */
    private static final class Lines implements ITtySink {

        private final List<CliLine> lines = new ArrayList<>();

        @Override
        public void line(final CliLine line) {
            this.lines.add(line);
        }

        @Override
        public void redraw(final CliLine line) {
            this.lines.add(line);
        }

        String text() {
            return SgdbGameTests.text(this.lines);
        }
    }
}
