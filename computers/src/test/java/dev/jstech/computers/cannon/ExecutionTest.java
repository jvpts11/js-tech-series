/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.cannon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.cannon.asm.AsmProgram;
import dev.jstech.computers.cannon.asm.AsmReader;
import dev.jstech.computers.cannon.run.IHost;
import dev.jstech.computers.cannon.run.Loaded;
import dev.jstech.computers.cannon.run.Process;
import dev.jstech.computers.cannon.run.Values;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * A program speaking of itself and of the other programs on its machine: what it was started with,
 * how it ends, the ones it starts, and the lines they send each other. The machine here is a stand-in
 * that remembers what it was asked, so the program's side is what is under test.
 */
class ExecutionTest {

    private static final long ROOM = 64L * 1024;
    private static final int PLENTY = 1_000_000;

    /** What every file starts with, on one line so the sources keep their line numbers. */
    private static final String PRELUDE = "using System.*; using System.IO.*; using System.Collections.*; "
            + "using System.Utils.*; using System.Machine.*; using System.Network.*; using System.Operations.*; "
            + "using System.Execution.*; using System.Threading.*; namespace Tests; ";

    /** A machine with programs on it, as far as the program under test can tell. */
    private static final class Machine implements IHost {
        final Map<Integer, Boolean> running = new HashMap<>();
        final Map<Integer, Integer> codes = new HashMap<>();
        final List<String> sent = new ArrayList<>();
        final List<String> started = new ArrayList<>();
        int next = 10;
        long now;

        @Override
        public long tick() {
            return this.now;
        }

        @Override
        public long dayTime() {
            return 0;
        }

        @Override
        public long day() {
            return 0;
        }

        @Override
        public boolean provides(final String owner) {
            return "Program".equals(owner);
        }

        @Override
        public Reply call(final String owner, final String member, final List<Object> arguments,
                          final String caller, final int callerId, final int line) {
            final int id = arguments.isEmpty() || !(arguments.getFirst() instanceof Integer number) ? 0 : number;
            return switch (member) {
                case "Start" -> {
                    final int made = this.next++;
                    this.running.put(made, true);
                    this.started.add(callerId + ">" + arguments.getFirst()
                            + (arguments.size() > 1 && arguments.get(1) instanceof Values.ListValue list
                                    ? list.items().toString() : ""));
                    final Values.Obj token = new Values.Obj("Process");
                    token.set("Id", made);
                    token.set("Name", String.valueOf(arguments.getFirst()));
                    yield Reply.of(token, 200);
                }
                case "Running" -> Reply.of(this.running.getOrDefault(id, false), 5);
                case "ExitCode" -> Reply.of(this.codes.getOrDefault(id, 0), 5);
                case "Output" -> {
                    final Values.ListValue lines = new Values.ListValue();
                    lines.items().add("out " + id);
                    yield Reply.of(lines, 51);
                }
                case "Kill" -> {
                    this.running.put(id, false);
                    yield Reply.of(true, 10);
                }
                case "Send" -> {
                    this.sent.add(callerId + ">" + id + ":" + arguments.get(1));
                    yield Reply.of(true, 10);
                }
                default -> throw new IllegalArgumentException(member);
            };
        }
    }

    private static Loaded load(final String members, final String tick) {
        final String source = "class Monitor : IScript {\n" + members
                + "    public void OnInit() { }\n"
                + "    public void OnTick() {\n" + tick + "\n    }\n"
                + "    public void OnDestroy() { }\n}\n";
        final CannonCompiler.Result built =
                CannonCompiler.compile(List.of(new SourceFile("Monitor.can", PRELUDE + source)));
        assertTrue(built.ok(), () -> String.join("\n", built.lines()));
        final DiagnosticBag bag = new DiagnosticBag("Monitor.asm");
        final AsmProgram program = new AsmReader(built.assembly(), bag).read();
        assertFalse(bag.hasErrors(), () -> String.join("\n",
                bag.sorted().stream().map(Diagnostic::format).toList()));
        return Loaded.of(program);
    }

    /** A script ready for its first tick, with the tick queued and nothing run yet. */
    private static Process start(final Loaded program, final IHost host) {
        final Process process = new Process(program, ROOM, host);
        final Values.Obj self = process.create(program.entryPoint());
        assertNotNull(self);
        process.begin(self, "OnTick");
        return process;
    }

    @Test
    void args_areWhatTheProgramWasStartedWith() {
        final Process process = start(load("", """
                Console.PrintLine(Program.Args.Count + " " + Program.Args.Get(1));
                """), IHost.still());
        process.setArgs(List.of("first", "second"));
        process.step(PLENTY);
        assertEquals(List.of("2 second"), process.console(), () -> String.valueOf(process.message()));
    }

    @Test
    void current_isTheProgramAsTheMachineListsIt() {
        final Process process = start(load("", """
                Program.SetName("tool");
                Console.PrintLine(Program.Current.Id + " " + Program.Current.Running);
                """), IHost.still());
        process.identify(7);
        process.step(PLENTY);
        assertEquals(List.of("7 true"), process.console(), () -> String.valueOf(process.message()));
    }

    @Test
    void exit_endsTheProgramWithACode() {
        final Process process = start(load("", """
                Thread.Start(() => { while (true) { Thread.Yield(); } });
                Program.Exit(3);
                Console.PrintLine("never");
                """), IHost.still());
        process.step(PLENTY);
        assertEquals(Process.State.FINISHED, process.state(), () -> String.valueOf(process.message()));
        assertTrue(process.exited());
        assertEquals(3, process.exitCode());
        assertTrue(process.console().isEmpty());
        assertEquals(1, process.threads(), "the other thread went with it");
    }

    @Test
    void start_handsBackWhatTheMachineStarted() {
        final Machine machine = new Machine();
        final Process process = start(load("", """
                List<string> args = new List<string>();
                args.Add("x");
                Process p = Program.Start("tool.asm", args);
                Console.PrintLine(p.Id + " " + p.Name + " " + p.Running);
                """), machine);
        process.identify(3);
        process.step(PLENTY);
        assertEquals(List.of("10 tool.asm true"), process.console(), () -> String.valueOf(process.message()));
        assertEquals(List.of("3>tool.asm[x]"), machine.started, "the machine was told who asked and with what");
    }

    @Test
    void wait_parksUntilTheOtherProgramEnds() {
        final Machine machine = new Machine();
        final Process process = start(load("", """
                Process p = Program.Start("tool.asm");
                p.Wait();
                Console.PrintLine("code " + p.ExitCode);
                """), machine);
        process.step(PLENTY);
        assertEquals(Process.State.PARKED, process.state(), () -> String.valueOf(process.message()));
        assertTrue(process.console().isEmpty());
        machine.running.put(10, false);
        machine.codes.put(10, 9);
        process.step(PLENTY);
        assertEquals(List.of("code 9"), process.console());
        assertEquals(Process.State.FINISHED, process.state());
    }

    @Test
    void wait_withATimeGivesUpWhenItRunsOut() {
        final Machine machine = new Machine();
        final Process process = start(load("", """
                Process p = Program.Start("tool.asm");
                bool ended = p.Wait(3);
                Console.PrintLine("ended " + ended + " " + p.Running);
                """), machine);
        process.step(PLENTY);
        assertEquals(Process.State.PARKED, process.state());
        machine.now = 3;
        process.step(PLENTY);
        assertEquals(List.of("ended false true"), process.console(), () -> String.valueOf(process.message()));
    }

    @Test
    void output_andKill_askTheMachine() {
        final Machine machine = new Machine();
        final Process process = start(load("", """
                Process p = Program.Start("tool.asm");
                foreach (string line in p.Output()) { Console.PrintLine("said " + line); }
                p.Kill();
                Console.PrintLine("alive " + p.Running);
                """), machine);
        process.step(PLENTY);
        assertEquals(List.of("said out 10", "alive false"), process.console(), () -> String.valueOf(process.message()));
    }

    @Test
    void onMessage_hearsWhatAnotherProgramSent() {
        final Process process = start(load("", """
                Program.OnMessage(m => Console.PrintLine(m.From + ":" + m.Text + "@" + m.Tick));
                """), IHost.still());
        process.step(PLENTY);
        assertTrue(process.deliverMessage(4, "hi", 20L));
        process.step(PLENTY);
        assertEquals(List.of("4:hi@20"), process.console(), () -> String.valueOf(process.message()));
    }

    @Test
    void send_namesTheSenderToTheMachine() {
        final Machine machine = new Machine();
        final Process process = start(load("", """
                Console.PrintLine("sent " + Process.Send(9, "yo"));
                """), machine);
        process.identify(5);
        process.step(PLENTY);
        assertEquals(List.of("sent true"), process.console(), () -> String.valueOf(process.message()));
        assertEquals(List.of("5>9:yo"), machine.sent);
    }

    @Test
    void save_keepsArgsTheNumberTheExitAndTheHandler() {
        final Loaded program = load("", """
                Program.OnMessage(m => Console.PrintLine("got " + m.Text + " as " + Program.Current.Id
                        + " with " + Program.Args.Get(0)));
                """);
        final Process before = start(program, IHost.still());
        before.setArgs(List.of("z"));
        before.identify(6);
        before.step(PLENTY);
        final Process process = Process.restore(program, before.save(), IHost.still());
        assertEquals(List.of("z"), process.args());
        assertEquals(6, process.machineId());
        assertTrue(process.deliverMessage(1, "back", 0L));
        process.step(PLENTY);
        assertEquals(List.of("got back as 6 with z"), process.console(), () -> String.valueOf(process.message()));

        final Loaded leaving = load("", "        Program.Exit(3);");
        final Process left = Process.restore(leaving, start(leaving, IHost.still()).save(), IHost.still());
        left.step(PLENTY);
        final Process back = Process.restore(leaving, left.save(), IHost.still());
        assertTrue(back.exited());
        assertEquals(3, back.exitCode());
    }
}
