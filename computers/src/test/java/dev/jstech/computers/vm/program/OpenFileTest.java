/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.sigma.SigmaCompiler;
import dev.jstech.computers.sigma.SourceFile;
import dev.jstech.computers.vm.listing.AsmReader;
import dev.jstech.computers.vm.system.MemberId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** A file a program opened: read a line, a character and a value at a time, written, and back on the disk closed. */
class OpenFileTest {

    private static final long ROOM = 64L * 1024;
    private static final int PLENTY = 1_000_000;
    private static final String PRELUDE = "using System.*; using System.IO.*; namespace Tests; ";

    /** A machine whose disk is a map, opening and closing files the way the machine's own calls do. */
    private static final class Desk implements IHost {

        private final Map<String, String> disk = new HashMap<>();

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
            return switch (id.describe()) {
                case "File.Open(string, string)" -> (call, target, arguments, line) -> {
                    final String path = String.valueOf(arguments[0]);
                    final String mode = OpenFile.mode(String.valueOf(arguments[1]));
                    if (mode == null || (OpenFile.needsFile(mode) && !this.disk.containsKey(path))) {
                        return null;
                    }
                    return OpenFile.opened(path, mode,
                            OpenFile.startsEmpty(mode) ? "" : this.disk.getOrDefault(path, ""));
                };
                case "FILE.Close()" -> (call, target, arguments, line) -> {
                    if (OpenFile.isOpen(target)) {
                        final Values.Obj file = (Values.Obj) target;
                        OpenFile.close(file);
                        if (OpenFile.changed(file)) {
                            this.disk.put(OpenFile.path(file), OpenFile.text(file));
                        }
                    }
                    return null;
                };
                default -> null;
            };
        }
    }

    private static Process run(final Desk desk, final String body) {
        final SigmaCompiler.Result built = SigmaCompiler.compile(List.of(new SourceFile("Tool.sgs",
                PRELUDE + "class Tool { static void Main() { " + body + " } }")));
        assertTrue(built.ok(), () -> String.join("\n", built.lines()));
        final ProgramImage program = ProgramImage.of(new AsmReader(built.assembly()).read());
        final Process process = new Process(program, ROOM, desk);
        process.beginMain(program.entryPoint());
        process.step(PLENTY);
        return process;
    }

    @Test
    void mode_readsTheSixModesOfCAndLeavesOutTheBinaryLetter() {
        assertEquals("r+", OpenFile.mode("r+b"));
        assertEquals("w", OpenFile.mode("wt"));
        assertNull(OpenFile.mode("x"));
        assertNull(OpenFile.mode(""));
    }

    @Test
    void aFileIsWrittenReadAddedToAndLeftOpenAsC() {
        final Desk desk = new Desk();
        final Process process = run(desk, """
                FILE w = fopen("notes.txt", "w");
                fputs("iron 16\\n", w);
                fputs("gold 4\\n", w);
                fclose(w);
                FILE r = fopen("notes.txt", "r");
                string line;
                while (fgets(out line, r)) { Console.PrintLine("[" + line + "]"); }
                Console.PrintLine("" + feof(r));
                rewind(r);
                int c = fgetc(r);
                Console.PrintLine("" + (char) c + ftell(r));
                fclose(r);
                FILE a = fopen("notes.txt", "a");
                fputc('!', a);
                fclose(a);
                Console.PrintLine("" + (fopen("missing.txt", "r") == null));
                FILE left = fopen("left.txt", "w");
                fputs("kept", left);
                """);
        assertEquals(Process.State.FINISHED, process.state(), () -> String.valueOf(process.message()));
        assertEquals(List.of("[iron 16]", "[gold 4]", "true", "i1", "true"), process.console());
        assertEquals("iron 16\ngold 4\n!", desk.disk.get("notes.txt"));
        assertEquals("kept", desk.disk.get("left.txt"), "a file left open is closed when the program ends");
    }

    @Test
    void fprintfAndFscanf_writeWithPrintfsFormatAndReadOneValueACall() {
        final Desk desk = new Desk();
        final Process process = run(desk, """
                FILE w = fopen("stock.txt", "w");
                fprintf(w, "%-6s %3d\\n", "iron", 16);
                fprintf(w, "%d %.1f\\n", 4, 2.5);
                fclose(w);
                FILE r = fopen("stock.txt", "r");
                string name;
                int n;
                int m;
                double x;
                int read = fscanf(r, "%s", out name) + fscanf(r, "%d", out n) + fscanf(r, "%d", out m)
                        + fscanf(r, "%f", out x);
                int none = fscanf(r, "%d", out m);
                Console.PrintLine(name + "|" + n + "|" + m + "|" + x + "|" + read + "|" + none);
                fclose(r);
                """);
        assertEquals(Process.State.FINISHED, process.state(), () -> String.valueOf(process.message()));
        assertEquals("iron    16\n4 2.5\n", desk.disk.get("stock.txt"));
        assertEquals(List.of("iron|16|0|2.5|4|0"), process.console());
    }

    @Test
    void readAndWrite_overwriteWhereTheFileIsUpToAndSeekStaysInside() {
        final Desk desk = new Desk();
        desk.disk.put("level.txt", "0123456789");
        final Process process = run(desk, """
                FILE f = fopen("level.txt", "r+");
                fseek(f, 4);
                fputs("ab", f);
                Console.PrintLine("" + ftell(f));
                fseek(f, 99);
                Console.PrintLine("" + ftell(f) + feof(f));
                fclose(f);
                """);
        assertEquals(List.of("6", "10true"), process.console());
        assertEquals("0123ab6789", desk.disk.get("level.txt"));
    }

    @Test
    void aFileOpenedToReadRefusesToBeWrittenAndAClosedOneRefusesEverything() {
        final Desk desk = new Desk();
        desk.disk.put("a.txt", "x");
        final Process reading = run(desk, "FILE f = fopen(\"a.txt\", \"r\"); fputs(\"y\", f);");
        assertEquals(Process.State.HALTED, reading.state());
        assertTrue(reading.message().english().contains("'a.txt' was opened with \"r\", which does not write"),
                reading.message().english());
        final Process closed = run(desk, "FILE f = fopen(\"a.txt\", \"r\"); fclose(f); int c = fgetc(f);");
        assertTrue(closed.message().english().contains("'a.txt' is closed"), closed.message().english());
        assertEquals("x", desk.disk.get("a.txt"), "and nothing it refused reached the disk");
    }
}
