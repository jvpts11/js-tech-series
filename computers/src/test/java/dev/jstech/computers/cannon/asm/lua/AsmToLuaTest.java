/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.cannon.asm.lua;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.cannon.CannonCompiler;
import dev.jstech.computers.cannon.Diagnostic;
import dev.jstech.computers.cannon.DiagnosticBag;
import dev.jstech.computers.cannon.SourceFile;
import dev.jstech.computers.cannon.asm.AsmProgram;
import dev.jstech.computers.cannon.asm.AsmReader;
import dev.jstech.computers.cannon.lua.LuaCompiler;
import dev.jstech.computers.cannon.run.IHost;
import dev.jstech.computers.cannon.run.Loaded;
import dev.jstech.computers.cannon.run.Process;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The two translators are each other's opposite, and that is what proves them without a Lua machine
 * anywhere: a program is run here as it was written, and then run again after being turned into Lua and
 * read back by our own Lua front end. Whatever it says, it has to say the same both times.
 */
class AsmToLuaTest {

    private static final long ROOM = 1024L * 1024;
    private static final int PLENTY = 2_000_000;
    private static final int ROUNDS = 400;

    private static final String PRELUDE = "using System.*; using System.IO.*; using System.Collections.*; "
            + "using System.Utils.*; using System.Execution.*; namespace Tests; ";

    /** Compiles a Cannon program and loads it, ready to run. */
    private static Loaded cannon(final String source) {
        final CannonCompiler.Result built =
                CannonCompiler.compile(List.of(new SourceFile("Program.can", PRELUDE + source)));
        assertTrue(built.ok(), () -> String.join("\n", built.lines()));
        return read(built.assembly(), "Program.asm");
    }

    private static Loaded read(final String assembly, final String named) {
        final DiagnosticBag bag = new DiagnosticBag(named);
        final AsmProgram program = new AsmReader(assembly, bag).read();
        assertFalse(bag.hasErrors(), () -> String.join("\n",
                bag.sorted().stream().map(Diagnostic::format).toList()) + "\n" + assembly);
        return Loaded.of(program);
    }

    private static List<String> run(final Loaded program, final String entryMethod) {
        final Process process = new Process(program, ROOM, IHost.still());
        process.beginStatic(program.entryPoint(), entryMethod);
        for (int i = 0; i < ROUNDS && process.state() == Process.State.RUNNING; i++) {
            process.step(PLENTY);
        }
        assertEquals(Process.State.FINISHED, process.state(),
                () -> process.message() + "\n" + String.join("\n", process.console()));
        return process.console();
    }

    /** What the program says when it runs here. */
    private static List<String> directly(final String source) {
        return run(cannon(source), "Main");
    }

    /** What it says after being turned into Lua and read back, which has to be the same. */
    private static List<String> throughLua(final String source) {
        final String lua = AsmToLua.of(cannon(source));
        final LuaCompiler.Result built = LuaCompiler.compile(new SourceFile("translated.lua", lua));
        assertTrue(built.ok(), () -> String.join("\n", built.lines()) + "\n\n" + lua);
        return run(read(built.assembly(), "translated.asm"), "Main");
    }

    /** The program says that here, and says exactly the same after the trip through Lua. */
    private static void bothWays(final String source, final String... says) {
        assertEquals(List.of(says), directly(source), "the program says what it was written to say");
        assertEquals(List.of(says), throughLua(source), "the translated program says the same");
    }

    private static String console(final String body) {
        return "class Program { static void Main() {\n" + body + "\n} }";
    }

    @Test
    void translate_carriesArithmeticAndWholeDivision() {
        bothWays(console("""
                int a = 17;
                int b = 5;
                Console.PrintLine("sum " + (a + b));
                Console.PrintLine("difference " + (a - b));
                Console.PrintLine("product " + (a * b));
                Console.PrintLine("quotient " + (a / b));
                Console.PrintLine("rest " + (a % b));
                Console.PrintLine("negative " + (-a / b));
                double x = 17.0;
                Console.PrintLine("real " + (x / 5.0));
                """),
                "sum 22", "difference 12", "product 85", "quotient 3", "rest 2", "negative -3", "real 3.4");
    }

    @Test
    void translate_carriesBranchesAndLoops() {
        bothWays(console("""
                int total = 0;
                for (int i = 1; i <= 10; i = i + 1) {
                    if (i % 2 == 0) {
                        total = total + i;
                    }
                }
                Console.PrintLine("total " + total);
                int count = 0;
                while (count < 3) {
                    Console.PrintLine("round " + count);
                    count = count + 1;
                }
                """),
                "total 30", "round 0", "round 1", "round 2");
    }

    @Test
    void translate_carriesTextAndWhatIsDoneToIt() {
        bothWays(console("""
                string name = "reactor";
                Console.PrintLine(name.ToUpper());
                Console.PrintLine("length " + name.Length);
                Console.PrintLine(name.Substring(0, 4));
                Console.PrintLine("has act " + name.Contains("act"));
                Console.PrintLine("starts " + name.StartsWith("re"));
                """),
                "REACTOR", "length 7", "reac", "has act true", "starts true");
    }

    @Test
    void translate_carriesAnObjectWithFieldsAndMethods() {
        bothWays("""
                class Core {
                    public int Heat;
                    public void Warm(int by) { Heat = Heat + by; }
                    public int Read() { return Heat; }
                }
                class Program {
                    static void Main() {
                        Core core = new Core();
                        core.Warm(5);
                        core.Warm(7);
                        Console.PrintLine("heat " + core.Read());
                    }
                }
                """,
                "heat 12");
    }

    @Test
    void translate_carriesArrays() {
        bothWays(console("""
                int[] readings = new int[4];
                for (int i = 0; i < 4; i = i + 1) {
                    readings[i] = i * i;
                }
                int total = 0;
                foreach (int one in readings) {
                    total = total + one;
                }
                Console.PrintLine("total " + total);
                """),
                "total 14");
    }

    @Test
    void translate_carriesListsAndMaps() {
        bothWays(console("""
                List<int> readings = new List<int>();
                readings.Add(3);
                readings.Add(9);
                Console.PrintLine("count " + readings.Count);
                Console.PrintLine("first " + readings[0]);
                int total = 0;
                foreach (int one in readings) {
                    total = total + one;
                }
                Console.PrintLine("total " + total);
                Map<string, int> heat = new Map<string, int>();
                heat["core"] = 40;
                heat["pump"] = 12;
                Console.PrintLine("kinds " + heat.Count);
                Console.PrintLine("core " + heat["core"]);
                """),
                "count 2", "first 3", "total 12", "kinds 2", "core 40");
    }

    @Test
    void translate_carriesTheWayAMethodIsChosenByTheObject() {
        bothWays("""
                class Part {
                    public string Name() { return "part"; }
                }
                class Pump : Part {
                    public string Name() { return "pump"; }
                }
                class Program {
                    static void Main() {
                        Part one = new Part();
                        Part two = new Pump();
                        Console.PrintLine(one.Name());
                        Console.PrintLine(two.Name());
                        Console.PrintLine("a pump " + (two is Pump));
                        Pump back = (Pump) two;
                        Console.PrintLine("back " + back.Name());
                    }
                }
                """,
                "part", "pump", "a pump true", "back pump");
    }

    @Test
    void translate_carriesEnumsAndSwitches() {
        bothWays("""
                enum Level { Low, High }
                class Program {
                    static void Main() {
                        Level at = Level.High;
                        if (at == Level.High) {
                            Console.PrintLine("high");
                        } else {
                            Console.PrintLine("low");
                        }
                        Console.PrintLine("named " + at);
                    }
                }
                """,
                "high", "named 1");
    }

    @Test
    void translate_carriesAMethodHandedOverWithoutBrackets() {
        bothWays("""
                class Program {
                    static int Twice(int x) { return x * 2; }
                    static void Main() {
                        Func<int, int> doubled = Twice;
                        Console.PrintLine("twice 7 " + doubled(7));
                    }
                }
                """,
                "twice 7 14");
    }

    @Test
    void translate_carriesAMethodThatCallsItself() {
        bothWays("""
                class Program {
                    static int Factorial(int of) {
                        if (of <= 1) { return 1; }
                        return of * Factorial(of - 1);
                    }
                    static void Main() {
                        Console.PrintLine("factorial 6 " + Factorial(6));
                    }
                }
                """,
                "factorial 6 720");
    }

    @Test
    void translate_carriesWhatTheLanguageDoesWithNumbers() {
        bothWays(console("""
                Console.PrintLine("abs " + Math.Abs(-8));
                Console.PrintLine("min " + Math.Min(4, 9));
                Console.PrintLine("max " + Math.Max(4, 9));
                Console.PrintLine("floor " + Math.Floor(3.7));
                Console.PrintLine("sqrt " + Math.Sqrt(16.0));
                Console.PrintLine("as int " + Convert.ToInt("42"));
                Console.PrintLine("as text " + Convert.ToString(17));
                """),
                "abs 8", "min 4", "max 9", "floor 3.0", "sqrt 4.0", "as int 42", "as text 17");
    }

    @Test
    void translate_carriesTheShorthandThatReadsAndWritesTheSamePlace() {
        bothWays("""
                class Core {
                    public int Heat;
                }
                class Program {
                    static void Main() {
                        Core core = new Core();
                        core.Heat += 5;
                        core.Heat++;
                        Console.PrintLine("heat " + core.Heat);
                        int[] places = new int[2];
                        places[0] += 3;
                        places[0]++;
                        Console.PrintLine("place " + places[0]);
                        List<int> kept = new List<int>();
                        kept.Add(10);
                        kept[0] += 2;
                        kept[0]++;
                        Console.PrintLine("kept " + kept[0]);
                        Map<string, int> heat = new Map<string, int>();
                        heat["core"] = 1;
                        heat["core"] += 4;
                        Console.PrintLine("core " + heat["core"]);
                        object held = core;
                        Core back = held as Core;
                        Console.PrintLine("back " + (back != null));
                        Program other = held as Program;
                        Console.PrintLine("other " + (other == null));
                    }
                }
                """,
                "heat 6", "place 4", "kept 13", "core 5", "back true", "other true");
    }

    @Test
    void translate_carriesAProgramThatStopsItself() {
        /* Not the usual class name here: a class called Program would stand in front of the built-in one. */
        bothWays("""
                class Runner {
                    static void Main() {
                        Console.PrintLine("before");
                        Program.Exit(0);
                        Console.PrintLine("after");
                    }
                }
                """,
                "before");
    }

    @Test
    void translate_carriesStaticFieldsAndTheirStartingValues() {
        bothWays("""
                class Counter {
                    public static int Made = 3;
                    public static int Next() { Made = Made + 1; return Made; }
                }
                class Program {
                    static void Main() {
                        Console.PrintLine("first " + Counter.Next());
                        Console.PrintLine("second " + Counter.Next());
                        Console.PrintLine("held " + Counter.Made);
                    }
                }
                """,
                "first 4", "second 5", "held 5");
    }
}
