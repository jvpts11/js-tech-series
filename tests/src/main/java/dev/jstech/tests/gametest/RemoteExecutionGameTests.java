/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.cannon.CannonCompiler;
import dev.jstech.computers.cannon.SourceFile;
import dev.jstech.computers.cannon.machine.MachinePrograms;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.operation.NetworkStorage;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.language.ILanguageProcess;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A program reaching the other computers on its network: starting a program there and reading what it
 * left, running a line at its prompt, being refused by a machine that says no, and asking the network
 * in its own language.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class RemoteExecutionGameTests {

    private RemoteExecutionGameTests() {
    }

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;

    /** Two personal computers off one router, and the Mainframe with a rack for stock. */
    private record Fleet(MainframeBlockEntity mainframe, PersonalComputerBlockEntity lab,
                         PersonalComputerBlockEntity desk) {
    }

    private static Fleet wire(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final MainframeBlockEntity mainframe = world.placeRunningMainframe(new BlockPos(1, 2, 2));
        world.setBlock(new BlockPos(2, 2, 2), ComputingModule.HBW_CABLE.get());
        world.placeSeededRack(new BlockPos(2, 2, 1));
        world.setBlock(new BlockPos(3, 2, 2), ComputingModule.PERSONAL_ROUTER.get());
        world.setBlock(new BlockPos(4, 2, 2), ComputingModule.ETHERNET_CABLE.get());
        final PersonalComputerBlockEntity lab = world.placeRunningPersonalComputer(new BlockPos(5, 2, 2));
        world.setBlock(new BlockPos(4, 2, 3), ComputingModule.ETHERNET_CABLE.get());
        final PersonalComputerBlockEntity desk = world.placeRunningPersonalComputer(new BlockPos(5, 2, 3));
        lab.console().setComputerName("lab");
        desk.console().setComputerName("desk");
        return new Fleet(mainframe, lab, desk);
    }

    private static String listing(final String source) {
        final CannonCompiler.Result built = CannonCompiler.compile(List.of(new SourceFile("Program.can", source)));
        if (!built.ok()) {
            throw new IllegalStateException(String.join("\n", built.lines()));
        }
        return built.assembly();
    }

    /** Starts a program and keeps it at the prompt, so what it printed stays readable after it returns. */
    private static ILanguageProcess held(final GameTestHelper helper, final PersonalComputerBlockEntity machine,
                                         final String name, final String source) {
        final MachinePrograms programs = machine.cannon();
        final MachinePrograms.Started started = programs.start(name, listing(source), 1, machine);
        helper.assertTrue(started.ok(), name + " starts: " + started.message());
        programs.hold(started.id());
        return programs.byId(started.id()).process();
    }

    private static final String TOOL = """
            using System.*;
            using System.IO.*;
            using System.Execution.*;
            namespace Programs;
            class Tool {
                static void Main() {
                    Console.PrintLine("tool " + Program.Args.Get(0));
                    Program.Exit(6);
                }
            }
            """;

    private static final String REMOTE = """
            using System.*;
            using System.IO.*;
            using System.Collections.*;
            using System.Network.*;
            using System.Execution.*;
            namespace Programs;
            class Remote {
                static void Main() {
                    RemoteComputer desk = Network.Computer("desk");
                    Console.PrintLine("online " + desk.Online + " " + desk.Type);
                    List<string> args = new List<string>();
                    args.Add("x");
                    Process p = desk.Start("C:\\\\tool.asm", args);
                    Console.PrintLine("started " + p.Name + " on " + p.Host);
                    p.Wait();
                    foreach (string line in p.Output()) { Console.PrintLine("desk said " + line); }
                    Console.PrintLine("code " + p.ExitCode);
                    Console.PrintLine("shell " + (desk.Shell("config").Count > 0));
                    Console.PrintLine("fleet " + (Network.Computers().Count >= 2));
                }
            }
            """;

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void programs_startAProgramOnAnotherComputerAndReadWhatItSaid(final GameTestHelper helper) {
        final Fleet fleet = wire(helper);
        final ILanguageProcess[] remote = new ILanguageProcess[1];
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 4, () -> {
                    final ServerCliComputer desk = new ServerCliComputer(fleet.desk(), helper.getLevel());
                    helper.assertTrue(desk.writeFile("C:\\tool.asm", listing(TOOL)).ok(), "the tool is on desk");
                    remote[0] = held(helper, fleet.lab(), "remote.asm", REMOTE);
                })
                .thenExecuteAfter(40, () -> {
                    final List<String> said = remote[0].console();
                    helper.assertTrue(said.equals(List.of("online true Personal Computer", "started tool.asm on desk",
                                    "desk said tool x", "code 6", "shell true", "fleet true")),
                            "lab started the tool on desk, waited and read it; got " + said
                                    + " (" + remote[0].message() + ")");
                })
                .thenSucceed();
    }

    private static final String PATIENT = """
            using System.*;
            using System.IO.*;
            using System.Collections.*;
            using System.Network.*;
            using System.Execution.*;
            using System.Threading.*;
            namespace Programs;
            class Patient {
                static void Main() {
                    List<string> args = new List<string>();
                    args.Add("y");
                    Process p = Network.Computer("desk").Start("C:\\\\tool.asm", args);
                    p.Wait();
                    Thread.Sleep(10);
                    Console.PrintLine("code " + p.ExitCode);
                }
            }
            """;

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void programs_keepWhatTheyLeftForAParentOnAnotherComputerUntilItIsGone(final GameTestHelper helper) {
        final Fleet fleet = wire(helper);
        final ILanguageProcess[] patient = new ILanguageProcess[1];
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 4, () -> {
                    final ServerCliComputer desk = new ServerCliComputer(fleet.desk(), helper.getLevel());
                    helper.assertTrue(desk.writeFile("C:\\tool.asm", listing(TOOL)).ok(), "the tool is on desk");
                    patient[0] = held(helper, fleet.lab(), "patient.asm", PATIENT);
                })
                .thenExecuteAfter(60, () -> {
                    /*
                     * The parent reads the exit code ten ticks after the tool ended, long after desk would
                     * have cleared away a finished program nobody on desk was waiting for. Once the parent
                     * has returned and its terminal has let it go, desk has nobody left to keep the tool for.
                     */
                    helper.assertTrue(patient[0].console().contains("code 6"), "the exit code outlived the wait; got "
                            + patient[0].console() + " (" + patient[0].message() + ")");
                    helper.assertTrue(!hasTool(fleet.desk()), "once the parent is gone, desk lets the tool go");
                })
                .thenSucceed();
    }

    private static boolean hasTool(final PersonalComputerBlockEntity machine) {
        return machine.cannon().all().stream().anyMatch(one -> "tool.asm".equals(one.file()));
    }

    private static final String REFUSED = """
            using System.*;
            using System.IO.*;
            using System.Network.*;
            using System.Execution.*;
            namespace Programs;
            class Refused {
                static void Main() {
                    Process p = Network.Computer("desk").Start("C:\\\\tool.asm");
                    Console.PrintLine("never");
                }
            }
            """;

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void programs_areRefusedByAComputerThatSaysNo(final GameTestHelper helper) {
        final Fleet fleet = wire(helper);
        final ILanguageProcess[] refused = new ILanguageProcess[1];
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 4, () -> {
                    final ServerCliComputer desk = new ServerCliComputer(fleet.desk(), helper.getLevel());
                    helper.assertTrue(desk.writeFile("C:\\tool.asm", listing(TOOL)).ok(), "the tool is on desk");
                    helper.assertTrue(desk.setConfig("remote", "off").ok(), "desk says no");
                    refused[0] = held(helper, fleet.lab(), "refused.asm", REFUSED);
                })
                .thenExecuteAfter(20, () -> {
                    helper.assertTrue(refused[0].state() == ILanguageProcess.State.HALTED,
                            "the program stops on the refusal; state " + refused[0].state());
                    helper.assertTrue(refused[0].message().contains("does not take programs"),
                            "and says why; got " + refused[0].message());
                    helper.assertTrue(!refused[0].console().contains("never"), "nothing ran past it");
                })
                .thenSucceed();
    }

    private static final String ASK = """
            using System.*;
            using System.IO.*;
            using System.Collections.*;
            using System.Network.*;
            namespace Programs;
            class Ask {
                static void Main() {
                    foreach (Map<string, object> row in Iql.Query("QUERY items")) {
                        Console.PrintLine(row.Get("name") + " " + row.Get("quantity"));
                    }
                    IqlResult bad = Iql.Run("garbage tokens");
                    Console.PrintLine("bad " + bad.Ok);
                }
            }
            """;

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void programs_askTheNetworkInIql(final GameTestHelper helper) {
        final Fleet fleet = wire(helper);
        final ILanguageProcess[] ask = new ILanguageProcess[1];
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 4, () -> NetworkStorage.of(helper.getLevel(), fleet.mainframe().networkUuid())
                        .insert(StorageKey.of(Items.COBBLESTONE), 200))
                .thenExecuteAfter(SETTLE + 4, () -> ask[0] = held(helper, fleet.lab(), "ask.asm", ASK))
                .thenExecuteAfter(20, () -> {
                    final List<String> said = ask[0].console();
                    helper.assertTrue(said.stream().anyMatch(line ->
                                    line.toLowerCase(java.util.Locale.ROOT).contains("cobblestone") && line.contains("200")),
                            "the query lists the stock; got " + said + " (" + ask[0].message() + ")");
                    helper.assertTrue(said.contains("bad false"), "a refused statement says so; got " + said);
                })
                .thenSucceed();
    }
}
