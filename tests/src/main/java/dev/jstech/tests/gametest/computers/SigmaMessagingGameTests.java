/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.machine.MachinePrograms;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.CliCommands;
import dev.jstech.computers.program.cli.SigmaCommands;
import dev.jstech.computers.sigma.SigmaCompiler;
import dev.jstech.computers.sigma.SourceFile;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * What a program says on its network through System.Network: an entry it writes to the event log is heard by a program
 * listening on another computer, and so is a value it sends on a channel that program hears, and on no other.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class SigmaMessagingGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 8;
    /** The namespaces a program brings in, on one line so its source keeps its line numbers. */
    private static final String PRELUDE = "using System.*; using System.IO.*; using System.Network.*; "
            + "namespace Programs; ";
    private static final String LISTENER = String.join("\n",
            "using System.*; using System.IO.*; using System.Network.*; namespace Tests;",
            "class Listener : IScript {",
            "    public void OnInit() {",
            "        Network.OnEvent(e => Console.PrintLine(\"event \" + e.Level + \" \" + e.Text));",
            "        Channel.Listen(\"door\", m => Console.PrintLine(\"heard \" + m.Channel + \" \" + m.Value",
            "                + \" from \" + m.From));",
            "    }",
            "    public void OnTick() { }",
            "    public void OnDestroy() { }",
            "}");

    private SigmaMessagingGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void aProgram_hearsTheLogEntryAndTheChannelAnotherComputerWrites(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final MainframeBlockEntity mainframe = world.placeRunningMainframe(new BlockPos(1, 2, 2));
        world.setBlock(new BlockPos(2, 2, 2), ComputingModule.HBW_CABLE);
        world.setBlock(new BlockPos(3, 2, 2), ComputingModule.PERSONAL_ROUTER.get());
        world.setBlock(new BlockPos(4, 2, 2), ComputingModule.ETHERNET_CABLE);
        final PersonalComputerBlockEntity computer = world.placeRunningPersonalComputer(new BlockPos(5, 2, 2));
        computer.console().install(SigmaCommands.COMPILER);
        computer.console().install(SigmaCommands.RUNTIME);
        final List<Integer> started = new ArrayList<>();
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(computer.networkUuid() != null
                                    && computer.networkUuid().equals(mainframe.networkUuid()),
                            "the computer is on the network");
                    final ServerCliComputer cli = new ServerCliComputer(computer, helper.getLevel());
                    cli.writeFile("Listener.sgs", LISTENER);
                    CliCommands.shellFor(cli, 80).run("sgsc Listener.sgs", cli);
                    helper.assertTrue(cli.startSigma("Listener.asm", 0).ok(), "the listener starts");
                    computer.programs().view().stream().filter(one -> one.file().endsWith("Listener.asm"))
                            .forEach(one -> started.add(one.id()));
                })
                .thenExecuteAfter(SETTLE, () -> helper.assertValueEqual(run(helper, mainframe, """
                        class Sender {
                            static void Main() {
                                EventLog.Write("WARN", "valve stuck");
                                Channel.Send("door", "open");
                                Channel.Send("window", "shut");
                                Console.PrintLine("sent");
                            }
                        }
                        """), List.of("sent"), "what the sender said"))
                .thenExecuteAfter(SETTLE, () -> {
                    final List<String> said = computer.programs().byId(started.getFirst()).process().console();
                    helper.assertTrue(said.contains("event warn valve stuck"), "it heard the log; got " + said);
                    helper.assertTrue(said.stream().anyMatch(one -> one.startsWith("heard door open from ")),
                            "it heard the channel it listens to; got " + said);
                    helper.assertTrue(said.stream().noneMatch(one -> one.contains("shut")),
                            "and not the one it does not; got " + said);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void aProgram_onNoNetworkIsToldSoAndAWrongLevelIsRefused(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer =
                TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(new BlockPos(2, 2, 2));
        helper.startSequence()
                .thenExecuteAfter(2, () -> helper.assertValueEqual(run(helper, computer, """
                        class Sender {
                            static void Main() {
                                try {
                                    EventLog.Write("info", "hello");
                                } catch (NetworkException failed) {
                                    Console.PrintLine("no network to log on");
                                }
                                try {
                                    Channel.Send("door", "open");
                                } catch (NetworkException failed) {
                                    Console.PrintLine("no network to send on");
                                }
                                try {
                                    EventLog.Write("loud", "hello");
                                } catch (ArgumentOutOfRangeException failed) {
                                    Console.PrintLine("no such level");
                                }
                            }
                        }
                        """), List.of("no network to log on", "no network to send on", "no such level"),
                        "what the program was told"))
                .thenSucceed();
    }

    /* Runs {@code source} on {@code computer} to its end and gives back what it printed. */
    private static List<String> run(final GameTestHelper helper, final AbstractComputerBlockEntity computer,
                                    final String source) {
        final SigmaCompiler.Result built = SigmaCompiler.compile(List.of(new SourceFile("Script.sgs",
                PRELUDE + source)));
        helper.assertTrue(built.ok(), "the script compiles: " + String.join("\n", built.lines()));
        final MachinePrograms.Started started = computer.programs().start("sender.asm", built.assembly(), 1,
                computer);
        helper.assertTrue(started.ok(), "the program starts: " + started.message());
        computer.programs().tick(100000);
        return computer.programs().byId(started.id()).process().console();
    }
}
