/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.cannon.lua;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.cannon.CannonCompiler;
import dev.jstech.computers.cannon.Diagnostic;
import dev.jstech.computers.cannon.DiagnosticBag;
import dev.jstech.computers.cannon.SourceFile;
import dev.jstech.computers.cannon.asm.AsmProgram;
import dev.jstech.computers.cannon.asm.AsmReader;
import dev.jstech.computers.cannon.run.IHost;
import dev.jstech.computers.cannon.run.Loaded;
import dev.jstech.computers.cannon.run.Process;
import dev.jstech.computers.cannon.run.Snapshot;
import java.util.List;
import org.junit.jupiter.api.Test;

/** A Cannon program calling into a Lua file it includes, reading and writing what the file keeps. */
class LuaIncludeTest {

    private static final long ROOM = 512L * 1024;

    private static final String PROGRAM = """
            include "lib/reactor.lua";
            using System.IO.*;
            using System.Collections.*;
            namespace Plant;
            class Program {
                static void Main() {
                    Console.PrintLine("" + reactor.Heat(40, 2));
                    int level = (int) reactor.Heat(10, 5);
                    Console.PrintLine("level " + (level + 1));
                    Console.PrintLine((string) reactor.Name);
                    reactor.Limit = 99;
                    Console.PrintLine("" + reactor.Check());
                    List<object> readings = (List<object>) reactor.Readings();
                    Console.PrintLine("readings " + readings.Count + " last " + (long) readings.Get(2));
                    Console.PrintLine("" + reactor.Sum(1, 2, 3, 4));
                }
            }
            """;

    private static final String REACTOR = """
            print("reactor loaded")
            Name = "core-1"
            Limit = 50
            function Heat(a, b) return a * b end
            function Check() return Limit > 60 end
            function Readings() return {1, 2, 3} end
            function Sum(...)
              local total = 0
              for _, v in ipairs({...}) do total = total + v end
              return total
            end
            """;

    private static CannonCompiler.Result compile(final String program, final String lua) {
        return CannonCompiler.compile(List.of(new SourceFile("Program.can", program),
                new SourceFile("lib/reactor.lua", lua)));
    }

    private static Loaded load(final CannonCompiler.Result built) {
        assertTrue(built.ok(), () -> String.join("\n", built.lines()));
        final DiagnosticBag bag = new DiagnosticBag("Program.asm");
        final AsmProgram program = new AsmReader(built.assembly(), bag).read();
        assertFalse(bag.hasErrors(), () -> String.join("\n", bag.sorted().stream().map(Diagnostic::format).toList()));
        return Loaded.of(program);
    }

    private static Process start(final Loaded program) {
        final Process process = new Process(program, ROOM, IHost.still());
        process.beginStatic(program.entryPoint(), "Main");
        return process;
    }

    private static final List<String> EXPECTED = List.of("reactor loaded", "80", "level 51", "core-1", "true",
            "readings 3 last 3", "10");

    @Test
    void include_letsCannonCallFunctionsAndReachGlobalsOfALuaFile() {
        final Process process = start(load(compile(PROGRAM, REACTOR)));
        for (int i = 0; i < 100 && process.state() == Process.State.RUNNING; i++) {
            process.step(1_000_000);
        }
        assertEquals(Process.State.FINISHED, process.state(), process::message);
        assertEquals(EXPECTED, process.console());
    }

    @Test
    void include_isCarriedThroughASaveInTheMiddle() {
        final Loaded program = load(compile(PROGRAM, REACTOR));
        Process process = start(program);
        for (int i = 0; i < 100_000 && process.state() == Process.State.RUNNING; i++) {
            process.step(5);
            final Snapshot shot = process.save();
            process = Process.restore(program, shot, IHost.still());
        }
        assertEquals(Process.State.FINISHED, process.state(), process::message);
        assertEquals(EXPECTED, process.console());
    }

    @Test
    void include_ofAFileNotCompiledWithTheProgramSaysWhichOne() {
        final CannonCompiler.Result built = CannonCompiler.compile(List.of(new SourceFile("Program.can", PROGRAM)));
        assertFalse(built.ok());
        assertEquals("C3043", built.diagnostics().getFirst().code());
        assertTrue(built.diagnostics().getFirst().message().contains("lib/reactor.lua"));
    }

    @Test
    void include_reportsAMistakeInTheLuaFileUnderItsOwnName() {
        final CannonCompiler.Result built = compile(PROGRAM, "function Heat(a, b) return a * end");
        assertFalse(built.ok());
        assertEquals("lib/reactor.lua", built.diagnostics().getFirst().file());
    }

    @Test
    void include_refusesAFunctionTheFileDoesNotHave() {
        final CannonCompiler.Result built = compile("""
                include "reactor.lua";
                namespace Plant;
                class Program { static void Main() { reactor.Missing(); } }
                """, REACTOR);
        assertFalse(built.ok());
        assertEquals("C3004", built.diagnostics().getFirst().code());
    }

    @Test
    void include_mustComeBeforeTheNamespace() {
        final CannonCompiler.Result built = compile("""
                namespace Plant;
                include "reactor.lua";
                class Program { static void Main() { } }
                """, REACTOR);
        assertFalse(built.ok());
        assertEquals("C2013", built.diagnostics().getFirst().code());
    }

    @Test
    void numbers_goIntoAnObjectAndComeBackOutWithACast() {
        final CannonCompiler.Result built = CannonCompiler.compile(List.of(new SourceFile("Program.can", """
                using System.IO.*;
                namespace Plant;
                class Program {
                    static void Main() {
                        object held = 41;
                        int back = (int) held + 1;
                        object real = 2.5;
                        Console.PrintLine(back + " " + ((double) real * 2) + " " + (held is int));
                    }
                }
                """)));
        final Process process = start(load(built));
        process.step(1_000_000);
        assertEquals(List.of("42 5.0 true"), process.console(), process::message);
    }
}
