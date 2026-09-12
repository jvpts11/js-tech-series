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
import dev.jstech.computers.cannon.run.Values;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

/**
 * A program's own files mean the same thing on either kind of computer.
 *
 * <p>{@code File} is the one part of the language whose meaning MOVED when a program is translated: here
 * it is this machine's disk, and on a ComputerCraft computer it is that computer's disk, reached through
 * their own {@code fs}. Two roads to the same idea is exactly where a difference hides, so the same
 * program is run both ways against the same disk and has to say the same thing: once on this runtime
 * with the calls it was written with, and once translated, read back by our own Lua front end, and
 * answered through {@code fs}.
 */
class FileConformanceTest {

    private static final long ROOM = 1024L * 1024;
    private static final int PLENTY = 2_000_000;
    private static final int ROUNDS = 400;

    private static final String PRELUDE = "using System.*; using System.IO.*; using System.Collections.*; "
            + "using System.Utils.*; using System.Execution.*; namespace Tests; ";

    /**
     * A disk, answering both the calls a program makes and the ones their {@code fs} makes of it.
     *
     * <p>One store behind both, because that is the whole point: the two roads must arrive at the same
     * place, and a test with a disk for each would prove nothing about either.
     */
    private static final class Disk implements IHost {

        private final Map<String, String> files = new TreeMap<>();
        private final TreeSet<String> folders = new TreeSet<>();

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
        public boolean provides(final String owner) {
            return "File".equals(owner);
        }

        @Override
        public Reply call(final String owner, final String member, final List<Object> arguments,
                          final String caller, final int line) {
            final String path = arguments.isEmpty() ? "" : String.valueOf(arguments.getFirst());
            final String text = arguments.size() > 1 ? String.valueOf(arguments.get(1)) : "";
            return switch (member) {
                // What a program of ours asks of its disk.
                case "Exists" -> Reply.of(this.files.containsKey(path) || this.folders.contains(path), 1);
                case "Read" -> {
                    // Reading what is not there stops the program; the try form is the one that asks.
                    if (!this.files.containsKey(path)) {
                        throw new dev.jstech.computers.cannon.run.Halt(
                                dev.jstech.computers.cannon.run.Halt.Reason.NO_SUCH_MEMBER, line,
                                "there is no file called " + path);
                    }
                    yield Reply.of(this.files.get(path), 1);
                }
                case "TryRead" -> new Reply(this.files.containsKey(path),
                        List.of(this.files.getOrDefault(path, "")), 1);
                case "Write" -> {
                    this.files.put(path, text);
                    yield Reply.of(Boolean.TRUE, 1);
                }
                case "Append" -> {
                    this.files.merge(path, text, String::concat);
                    yield Reply.of(Boolean.TRUE, 1);
                }
                case "Delete" -> Reply.of(this.files.remove(path) != null, 1);
                case "MkDir" -> Reply.of(this.folders.add(path), 1);
                case "List" -> Reply.of(this.namesIn(path), 1);
                // The same disk, as their fs asks about it.
                case "Entries" -> Reply.of(this.entries(path), 1);
                case "Stat" -> Reply.of(this.stat(path), 1);
                case "Text" -> Reply.of(this.files.get(path), 1);
                case "Put" -> {
                    this.files.put(path, text);
                    yield Reply.of("", 1);
                }
                case "MakeDir" -> {
                    this.folders.add(path);
                    yield Reply.of("", 1);
                }
                case "Remove" -> {
                    this.files.remove(path);
                    this.folders.remove(path);
                    yield Reply.of("", 1);
                }
                case "Free" -> Reply.of(1_000_000L, 1);
                case "Capacity" -> Reply.of(2_000_000L, 1);
                default -> Reply.of(null, 1);
            };
        }

        /** What is in that folder, by name, the way a program of ours is given it. */
        private Values.ListValue namesIn(final String folder) {
            final Values.ListValue out = new Values.ListValue();
            for (final String name : this.inside(folder)) {
                out.items().add(name);
            }
            return out;
        }

        /** The same, the way their fs is given it: a row of fields for each. */
        private Values.ListValue entries(final String folder) {
            final Values.ListValue out = new Values.ListValue();
            for (final String name : this.inside(folder)) {
                final String full = folder.isEmpty() ? name : folder + "/" + name;
                out.items().add(this.row(name, this.folders.contains(full)));
            }
            return out;
        }

        private Values.ListValue stat(final String path) {
            if (this.folders.contains(path)) {
                return this.row(nameOf(path), true);
            }
            return this.files.containsKey(path) ? this.row(nameOf(path), false) : null;
        }

        private Values.ListValue row(final String name, final boolean folder) {
            final Values.ListValue fields = new Values.ListValue();
            fields.items().add(name);
            fields.items().add(folder);
            fields.items().add((long) (folder ? 0 : this.files.getOrDefault(name, "").length()));
            fields.items().add(Boolean.FALSE);
            fields.items().add(0L);
            return fields;
        }

        private List<String> inside(final String folder) {
            final TreeSet<String> names = new TreeSet<>();
            for (final String path : this.files.keySet()) {
                add(names, folder, path);
            }
            for (final String path : this.folders) {
                add(names, folder, path);
            }
            return new ArrayList<>(names);
        }

        private static void add(final TreeSet<String> names, final String folder, final String path) {
            if (folder.isEmpty()) {
                if (!path.contains("/")) {
                    names.add(path);
                }
                return;
            }
            if (path.startsWith(folder + "/")) {
                final String rest = path.substring(folder.length() + 1);
                if (!rest.contains("/")) {
                    names.add(rest);
                }
            }
        }

        private static String nameOf(final String path) {
            final int slash = path.lastIndexOf('/');
            return slash < 0 ? path : path.substring(slash + 1);
        }
    }

    private static Loaded read(final String assembly, final String named) {
        final DiagnosticBag bag = new DiagnosticBag(named);
        final AsmProgram program = new AsmReader(assembly, bag).read();
        assertFalse(bag.hasErrors(), () -> String.join("\n",
                bag.sorted().stream().map(Diagnostic::format).toList()) + "\n" + assembly);
        return Loaded.of(program);
    }

    private static Loaded cannon(final String source) {
        final CannonCompiler.Result built =
                CannonCompiler.compile(List.of(new SourceFile("Program.can", PRELUDE + source)));
        assertTrue(built.ok(), () -> String.join("\n", built.lines()));
        return read(built.assembly(), "Program.asm");
    }

    private static List<String> run(final Loaded program, final IHost host) {
        final Process process = new Process(program, ROOM, host);
        process.beginStatic(program.entryPoint(), "Main");
        for (int i = 0; i < ROUNDS && process.state() == Process.State.RUNNING; i++) {
            process.step(PLENTY);
        }
        assertEquals(Process.State.FINISHED, process.state(),
                () -> process.message() + "\n" + String.join("\n", process.console()));
        return process.console();
    }

    /** The same program, run as it was written and again translated, against a disk of its own each time. */
    private static void bothWays(final String body, final String... says) {
        final String source = "class Program { static void Main() {\n" + body + "\n} }";
        assertEquals(List.of(says), run(cannon(source), new Disk()),
                "the program says what it was written to say");

        final String lua = AsmToLua.of(cannon(source));
        final LuaCompiler.Result built = LuaCompiler.compile(new SourceFile("translated.lua", lua));
        assertTrue(built.ok(), () -> String.join("\n", built.lines()) + "\n\n" + lua);
        assertEquals(List.of(says), run(read(built.assembly(), "translated.asm"), new Disk()),
                "and the translated program says the same about its own disk");
    }

    @Test
    void files_readAndWriteTheSameEitherWay() {
        bothWays("""
                File.Write("notes.txt", "one");
                Console.PrintLine("wrote " + File.Exists("notes.txt"));
                Console.PrintLine("said " + File.Read("notes.txt"));
                File.Write("notes.txt", "two");
                Console.PrintLine("again " + File.Read("notes.txt"));
                """,
                "wrote true", "said one", "again two");
    }

    @Test
    void files_appendAddsToWhatIsThere() {
        bothWays("""
                File.Write("log.txt", "a");
                File.Append("log.txt", "b");
                Console.PrintLine("log " + File.Read("log.txt"));
                """,
                "log ab");
    }

    @Test
    void files_existsAnswersForAFolderAsWellAsAFile() {
        bothWays("""
                Console.PrintLine("before " + File.Exists("work"));
                File.MkDir("work");
                Console.PrintLine("after " + File.Exists("work"));
                Console.PrintLine("nothing " + File.Exists("nowhere.txt"));
                """,
                "before false", "after true", "nothing false");
    }

    @Test
    void files_deleteTakesItAway() {
        bothWays("""
                File.Write("gone.txt", "here");
                Console.PrintLine("there " + File.Exists("gone.txt"));
                File.Delete("gone.txt");
                Console.PrintLine("gone " + File.Exists("gone.txt"));
                """,
                "there true", "gone false");
    }

    @Test
    void files_listNamesWhatIsInAFolder() {
        bothWays("""
                File.MkDir("work");
                File.Write("work/one.txt", "1");
                File.Write("work/two.txt", "2");
                List<string> named = File.List("work");
                Console.PrintLine("count " + named.Count);
                foreach (string one in named) {
                    Console.PrintLine("in " + one);
                }
                Console.PrintLine("empty " + File.List("nowhere").Count);
                """,
                "count 2", "in one.txt", "in two.txt", "empty 0");
    }

    /*
     * Asking for what may not be there is the try form, and it answers twice over: whether it found
     * anything, and what it found. Both halves have to cross the translation, which is the one call in
     * the language that fills a place in rather than only giving something back.
     */
    @Test
    void files_tryReadSaysWhetherItFoundItAndWhatItFound() {
        bothWays("""
                string held = "";
                Console.PrintLine("missing " + File.TryRead("nowhere.txt", out held));
                Console.PrintLine("held [" + held + "]");
                File.Write("here.txt", "something");
                Console.PrintLine("found " + File.TryRead("here.txt", out held));
                Console.PrintLine("held [" + held + "]");
                """,
                "missing false", "held []", "found true", "held [something]");
    }

    /* Reading what is not there stops a program on either kind of computer, rather than reading nothing. */
    @Test
    void files_readingWhatIsNotThereStopsTheProgramEitherWay() {
        final String source = "class Program { static void Main() {\n"
                + "Console.PrintLine(\"before\");\n"
                + "Console.PrintLine(File.Read(\"missing.txt\"));\n"
                + "} }";
        final Process here = new Process(cannon(source), ROOM, new Disk());
        here.beginStatic(cannon(source).entryPoint(), "Main");
        here.step(PLENTY);
        assertEquals(Process.State.HALTED, here.state(), "it stops here");

        final String lua = AsmToLua.of(cannon(source));
        final LuaCompiler.Result built = LuaCompiler.compile(new SourceFile("translated.lua", lua));
        assertTrue(built.ok(), () -> String.join("\n", built.lines()));
        final Loaded translated = read(built.assembly(), "translated.asm");
        final Process there = new Process(translated, ROOM, new Disk());
        there.beginStatic(translated.entryPoint(), "Main");
        there.step(PLENTY);
        assertEquals(Process.State.HALTED, there.state(), "and it stops there");
        assertEquals("before", there.console().getFirst(),
                "both having said the same up to the point where they stopped");
        assertTrue(there.message().contains("missing.txt"), there.message());
    }
}
