/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.sigma.SigmaCompiler;
import dev.jstech.computers.sigma.SourceFile;
import dev.jstech.computers.vm.listing.AsmProgram;
import dev.jstech.computers.vm.listing.AsmReader;
import dev.jstech.computers.vm.listing.ListingProblem;
import dev.jstech.computers.vm.system.CallCost;
import dev.jstech.computers.vm.system.MemberId;
import dev.jstech.computers.vm.system.SigmaCosts;
import dev.jstech.computers.vm.system.SystemApi;
import dev.jstech.core.text.Text;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CallDispatchTest {

    private static final String SOURCE = "using System.*; namespace Tests; class Monitor : IScript {\n"
            + "    public void OnInit() { }\n"
            + "    public void OnTick() { }\n"
            + "    public void OnDestroy() { }\n"
            + "    public int Add(int a, int b) { return a + b; }\n"
            + "    public bool Find(out int value) { value = 42; return true; }\n"
            + "}\n";

    private static final ProgramImage PROGRAM = load(SOURCE);

    private static final long ROOM = 64L * 1024;
    private static final int PLENTY = 1_000_000;

    /**
     * A machine whose whole world is a handful of files. It answers the calls on them with functions it binds, which
     * say nothing of what the calls cost: whatever a program is charged for them comes from what the system declares.
     */
    private static final class Drive implements IHost {

        private final Map<String, String> files = new LinkedHashMap<>();
        /** Which program the drive was last told asked. */
        private String askedBy;

        @Override
        public long tick() {
            return 0;
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
        public IWorldFunction bind(final MemberId id) {
            if ("Iql".equals(id.owner()) && "Run".equals(id.name())) {
                // A statement whose rows are held in the record it answers, one row for every letter it was given.
                return (call, target, arguments, line) -> {
                    call.rows(String.valueOf(arguments[0]).length());
                    final Values.Obj result = new Values.Obj("IqlResult");
                    result.set("Ok", true);
                    return result;
                };
            }
            if (!"File".equals(id.owner())) {
                return null;
            }
            return switch (id.name()) {
                case "Exists" -> (call, target, arguments, line) -> {
                    this.askedBy = call.caller();
                    return this.files.containsKey(String.valueOf(arguments[0]));
                };
                case "Read" -> (call, target, arguments, line) -> {
                    final String held = this.files.get(String.valueOf(arguments[0]));
                    if (held == null) {
                        throw new Halt(Halt.Reason.NO_SUCH_MEMBER, line,
                                Text.literal(arguments[0] + ": file not found"));
                    }
                    call.moved(held.getBytes(StandardCharsets.UTF_8).length);
                    return held;
                };
                case "TryRead" -> (call, target, arguments, line) -> {
                    final String held = this.files.get(String.valueOf(arguments[0]));
                    arguments[1] = held;
                    return held != null;
                };
                case "Write" -> (call, target, arguments, line) -> {
                    this.files.put(String.valueOf(arguments[0]), String.valueOf(arguments[1]));
                    return true;
                };
                default -> null;
            };
        }
    }

    private static ProgramImage load(final String source) {
        final SigmaCompiler.Result built = SigmaCompiler.compile(List.of(new SourceFile("Monitor.sgs", source)));
        assertTrue(built.ok(), () -> String.join("\n", built.lines()));
        final AsmReader reader = new AsmReader(built.assembly());
        final AsmProgram listing = reader.read();
        assertFalse(reader.hasProblems(), () -> String.join("\n",
                reader.problems().stream().map(ListingProblem::format).toList()));
        return ProgramImage.of(listing);
    }

    /** Runs a script whose tick is those lines through on that machine, with the machine's drives in reach. */
    private static Process run(final IHost host, final String tick) {
        final ProgramImage program = load("using System.*; using System.IO.*; using System.Network.*; namespace Tests; "
                + "class Monitor : IScript {\n"
                + "    public void OnInit() { }\n"
                + "    public void OnTick() {\n" + tick + "\n    }\n"
                + "    public void OnDestroy() { }\n}\n");
        final Process process = new Process(program, ROOM, host);
        process.begin(process.create(program.entryPoint()), "OnTick");
        process.step(PLENTY);
        return process;
    }

    private static MethodImage method(final String name, final List<String> parameters) {
        final MethodImage found = PROGRAM.method(PROGRAM.entryPoint(), name, parameters);
        assertNotNull(found, name);
        return found;
    }

    private static Process process() {
        return new Process(PROGRAM, ROOM, IHost.still());
    }

    private static CallDispatch dispatch(final Process process) {
        return new CallDispatch(process, process.heap0(), PROGRAM);
    }

    @Test
    void take_skipsThePlacesACallFillsInAndKeepsTheOrder() {
        final Frame written = new Frame(method("OnTick", List.of()), null);
        written.push(1);
        written.push(2);
        final Frame loaded = new Frame(method("OnTick", List.of()), null);
        loaded.push(1);
        loaded.push(2);

        assertEquals(Arrays.asList(1, null, 2), CallDispatch.take(written, List.of("int", "out int", "int")));
        assertEquals(Arrays.asList(1, null, 2), CallDispatch.take(loaded, new boolean[]{false, true, false}));
    }

    @Test
    void enter_startsTheMethodOnTheRunningThreadWithItsArguments() {
        final Process process = process();
        final CallDispatch calls = dispatch(process);
        final MethodImage add = method("Add", List.of("int", "int"));

        calls.enter(add, null, List.of(3, 4), 1);

        final Frame top = process.mainThread().frames.peek();
        assertSame(add, top.method);
        assertEquals(3, top.slots[0]);
        assertEquals(4, top.slots[1]);
    }

    @Test
    void leave_handsTheAnswerAndWhatWasFilledInToTheCaller() {
        final Process process = process();
        final CallDispatch calls = dispatch(process);
        final Frame caller = new Frame(method("OnTick", List.of()), null);
        process.mainThread().frames.push(caller);
        calls.enter(method("Find", List.of("out int")), null, Arrays.asList((Object) null), 1);
        final Frame called = process.mainThread().frames.peek();
        called.slots[0] = 42;

        calls.leave(called, true);

        assertSame(caller, process.mainThread().frames.peek());
        assertEquals(42, caller.pop());
        assertEquals(true, caller.pop());
    }

    @Test
    void enter_haltsOnceAThreadsCallsGoAsDeepAsTheyMay() {
        final Process process = process();
        final CallDispatch calls = dispatch(process);
        final MethodImage tick = method("OnTick", List.of());
        for (int i = 0; i < CallDispatch.DEEPEST; i++) {
            calls.enter(tick, null, List.of(), 1);
        }

        final Halt halt = assertThrows(Halt.class, () -> calls.enter(tick, null, List.of(), 7));

        assertEquals(Halt.Reason.STACK_DEPTH, halt.reason());
        assertEquals(CallDispatch.DEEPEST, process.mainThread().frames.size());
    }

    @Test
    void call_handsACallToTheWorldToWhatTheHostBoundAndGivesBackItsAnswer() {
        final Drive drive = new Drive();

        final Process process = run(drive, """
                        File.Write("log.txt", "first line");
                        Console.PrintLine(File.Read("log.txt"));
                """);

        assertEquals(Process.State.FINISHED, process.state(), String.valueOf(process.message()));
        assertEquals(List.of("first line"), process.console());
        assertEquals("first line", drive.files.get("log.txt"));
    }

    @Test
    void call_putsWhatACallToTheWorldFillsInBesideItsAnswer() {
        final Drive drive = new Drive();
        drive.files.put("stock.csv", "iron,64");

        final Process process = run(drive, """
                        if (File.TryRead("stock.csv", out string held)) { Console.PrintLine("got " + held); }
                        if (!File.TryRead("gone.csv", out string missing)) { Console.PrintLine("no file"); }
                """);

        assertEquals(Process.State.FINISHED, process.state(), String.valueOf(process.message()));
        assertEquals(List.of("got iron,64", "no file"), process.console());
    }

    @Test
    void call_chargesACallToTheWorldWhatTheSystemDeclaresForIt() {
        final Drive drive = new Drive();
        drive.files.put("a", "x");

        final long look = run(drive, "        File.Exists(\"a\");").spent();
        final long read = run(drive, "        File.Read(\"a\");").spent();

        // The drive says nothing of prices, so the two runs can only differ by what the two calls are declared to cost.
        final int declared = SystemApi.member("File", "Read", List.of("string")).cost().at(0, 1)
                - SystemApi.member("File", "Exists", List.of("string")).cost().at(0, 0);
        assertEquals(declared, read - look);
    }

    @Test
    void call_chargesACallToTheWorldForTheBytesItSaysItMoved() {
        final Drive drive = new Drive();
        drive.files.put("small", "x");
        drive.files.put("large", "x".repeat(CallCost.BLOCK_BYTES + 1));

        final long small = run(drive, "        File.Read(\"small\");").spent();
        final long large = run(drive, "        File.Read(\"large\");").spent();

        assertEquals(SigmaCosts.READ_PER_BLOCK, large - small, "a second block read costs a block's price more");
    }

    @Test
    void call_chargesACallToTheWorldItsDeclaredPriceOnTopOfItsInstruction() {
        final Drive drive = new Drive();
        drive.files.put("a", "x");

        final long pure = run(drive, "        Convert.ToInt(\"1\");").spent();
        final long look = run(drive, "        File.Exists(\"a\");").spent();

        // Both lines are the same three instructions, and a function of the language costs nothing beyond its own.
        assertEquals(SystemApi.member("File", "Exists", List.of("string")).cost().at(0, 0), look - pure);
    }

    @Test
    void call_makesWhatTheWorldHandsBackTheProgramsToHold() {
        final Drive drive = new Drive();
        drive.files.put("stock.csv", "iron,64");

        final Process process = run(drive, """
                        string held = File.Read("stock.csv");
                        Console.PrintLine("holding " + held.Length);
                """);

        assertEquals(Process.State.FINISHED, process.state(), String.valueOf(process.message()));
        // The text came from outside, but it weighs on this program's heap like anything else it holds.
        assertTrue(process.heap().used() >= 16 + 2L * "iron,64".length(),
                "the file's text is counted; used " + process.heap().used());
    }

    @Test
    void call_chargesACallToTheWorldForTheRowsItSaysItBroughtBackInARecord() {
        final Drive drive = new Drive();

        final long one = run(drive, "        Iql.Run(\"a\");").spent();
        final long three = run(drive, "        Iql.Run(\"abc\");").spent();

        final int perRow = SystemApi.member("Iql", "Run", List.of("string")).cost().perRow();
        assertEquals(2L * perRow, three - one, "two more rows cost two rows' price more");
    }

    @Test
    void call_tellsWhatAnswersACallToTheWorldWhichProgramAsked() {
        final Drive drive = new Drive();

        run(drive, "        File.Exists(\"a\");");

        assertEquals("Tests.Monitor", drive.askedBy, "the class the program was started from");
    }

    @Test
    void call_letsWhatAnswersACallToTheWorldStopTheProgram() {
        final Process process = run(new Drive(), "        string s = File.Read(\"gone\");");

        assertEquals(Process.State.HALTED, process.state());
        assertTrue(process.message().english().contains("file not found"), String.valueOf(process.message()));
    }

    @Test
    void call_leavesACallToTheWorldNoHostBindsToTheHostsOtherDoor() {
        final Process process = run(IHost.still(), "        File.Write(\"log\", \"x\");");

        assertEquals(Process.State.HALTED, process.state());
        assertTrue(process.message().english().contains("File"), String.valueOf(process.message()));
    }
}
