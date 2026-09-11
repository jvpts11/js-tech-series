/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.cannon.lua;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.cannon.run.Halt;
import dev.jstech.computers.cannon.run.IHost;
import dev.jstech.computers.cannon.run.Loaded;
import dev.jstech.computers.cannon.run.Process;
import dev.jstech.computers.cannon.run.Snapshot;
import dev.jstech.computers.cannon.run.Values;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

/**
 * What a ComputerCraft program finds around it: events, timers, parallel, the screen, the disks, the
 * shell, require and settings, run on a machine whose clock moves a tick between each step.
 */
class LuaComputerCraftTest {

    private static final long ROOM = 1024L * 1024;
    private static final int PLENTY = 100_000;
    private static final int TICKS = 400;

    /** A machine with a clock that moves, a name, a number and a disk kept in memory. */
    private static final class Machine implements IHost {

        private long tick;
        private final Map<String, String> files = new TreeMap<>();
        private final TreeSet<String> folders = new TreeSet<>();

        @Override
        public long tick() {
            return this.tick;
        }

        @Override
        public long dayTime() {
            return this.tick % 24_000L;
        }

        @Override
        public long day() {
            return this.tick / 24_000L;
        }

        @Override
        public boolean provides(final String owner) {
            return "File".equals(owner) || "Computer".equals(owner);
        }

        @Override
        public Reply call(final String owner, final String member, final List<Object> arguments, final String caller,
                          final int line) {
            if ("Computer".equals(owner)) {
                return Reply.of("Id".equals(member) ? (Object) 7L : "Reactor Room", 1);
            }
            final String path = arguments.isEmpty() ? "" : String.valueOf(arguments.getFirst());
            return Reply.of(switch (member) {
                case "Entries" -> this.entries(path);
                case "Stat" -> this.stat(path);
                case "Text" -> this.files.get(path);
                case "Put" -> {
                    this.files.put(path, String.valueOf(arguments.get(1)));
                    yield "";
                }
                case "MakeDir" -> {
                    for (String at = path; !at.isEmpty(); at = parentOf(at)) {
                        this.folders.add(at);
                    }
                    yield "";
                }
                case "Remove" -> {
                    this.files.remove(path);
                    this.folders.remove(path);
                    yield "";
                }
                case "Free" -> 1_000_000L;
                case "Capacity" -> 2_000_000L;
                default -> throw new Halt(Halt.Reason.NO_SUCH_MEMBER, line, "File has no " + member);
            }, 1);
        }

        private Values.ListValue entries(final String path) {
            if (!path.isEmpty() && !this.folders.contains(path)) {
                return null;
            }
            final Values.ListValue rows = new Values.ListValue();
            for (final String folder : this.folders) {
                if (parentOf(folder).equals(path)) {
                    rows.items().add(row(nameOf(folder), true, 0));
                }
            }
            for (final Map.Entry<String, String> file : this.files.entrySet()) {
                if (parentOf(file.getKey()).equals(path)) {
                    rows.items().add(row(nameOf(file.getKey()), false, file.getValue().length()));
                }
            }
            return rows;
        }

        private Values.ListValue stat(final String path) {
            if (path.isEmpty() || this.folders.contains(path)) {
                return row(nameOf(path), true, 0);
            }
            final String text = this.files.get(path);
            return text == null ? null : row(nameOf(path), false, text.length());
        }

        private static Values.ListValue row(final String name, final boolean folder, final long size) {
            final Values.ListValue row = new Values.ListValue();
            row.items().addAll(List.of(name, folder, size, false, 0L));
            return row;
        }

        private static String parentOf(final String path) {
            final int slash = path.lastIndexOf('/');
            return slash < 0 ? "" : path.substring(0, slash);
        }

        private static String nameOf(final String path) {
            return path.substring(path.lastIndexOf('/') + 1);
        }
    }

    private static Process start(final Loaded program, final Machine machine) {
        final Process process = new Process(program, ROOM, machine);
        process.beginStatic(program.entryPoint(), "Main");
        return process;
    }

    /* Runs the program a tick at a time until it ends or the ticks run out. */
    private static Process run(final Process process, final Machine machine) {
        for (int i = 0; i < TICKS && !ended(process); i++) {
            process.step(PLENTY);
            machine.tick++;
        }
        return process;
    }

    private static boolean ended(final Process process) {
        return process.state() == Process.State.FINISHED || process.state() == Process.State.HALTED;
    }

    private static List<String> output(final String source, final Machine machine) {
        final Process process = run(start(LuaExecutionTest.load(source), machine), machine);
        assertEquals(Process.State.FINISHED, process.state(),
                () -> process.message() + "\n" + String.join("\n", process.console()));
        return process.console();
    }

    private static List<String> output(final String source) {
        return output(source, new Machine());
    }

    @Test
    void pullEvent_takesWhatTheProgramQueuedInOrder() {
        assertEquals(List.of("a\t1", "b\ttwo"), output("""
                os.queueEvent("a", 1)
                os.queueEvent("b", "two")
                print(os.pullEvent())
                print(os.pullEvent())
                """));
    }

    @Test
    void pullEvent_withAFilterLetsTheOtherEventsGo() {
        assertEquals(List.of("y", "z"), output("""
                os.queueEvent("x")
                os.queueEvent("y")
                print(os.pullEvent("y"))
                os.queueEvent("z")
                print(os.pullEvent())
                """));
    }

    @Test
    void pullEvent_waitsWithoutSpendingWhileNothingHasCome() {
        final Machine machine = new Machine();
        final Process process = start(LuaExecutionTest.load("print(os.pullEvent('never'))"), machine);
        process.step(PLENTY);
        assertEquals(Process.State.PARKED, process.state());
        assertEquals(0, process.step(PLENTY), "a waiting program spends nothing");
    }

    @Test
    void terminate_endsTheProgramUnlessItPullsRaw() {
        final Machine machine = new Machine();
        final Process ended = run(start(LuaExecutionTest.load("os.queueEvent('terminate') os.pullEvent() print('after')"),
                machine), machine);
        assertEquals(Process.State.HALTED, ended.state());
        assertTrue(ended.message().contains("Terminated"), ended::message);
        assertEquals(List.of("terminate"), output("os.queueEvent('terminate') print(os.pullEventRaw())"));
    }

    @Test
    void startTimer_queuesATimerCarryingItsNumber() {
        assertEquals(List.of("true"), output("""
                local id = os.startTimer(0.5)
                local _, got = os.pullEvent("timer")
                print(got == id)
                """));
    }

    @Test
    void cancelTimer_keepsItsEventFromComing() {
        assertEquals(List.of("true"), output("""
                local a = os.startTimer(0.1)
                local b = os.startTimer(0.2)
                os.cancelTimer(a)
                local _, got = os.pullEvent("timer")
                print(got == b)
                """));
    }

    @Test
    void sleep_waitsTheTicksItWasAskedFor() {
        final Machine machine = new Machine();
        final List<String> said = output("""
                local before = os.clock()
                sleep(1)
                print(os.clock() - before >= 1)
                """, machine);
        assertEquals(List.of("true"), said);
        assertTrue(machine.tick >= 20, "a second is twenty ticks");
    }

    @Test
    void waitForAny_endsWhenTheFirstFunctionReturns() {
        assertEquals(List.of("2\tfast"), output("""
                local order = {}
                local winner = parallel.waitForAny(
                  function() sleep(0.5) table.insert(order, "slow") end,
                  function() sleep(0.1) table.insert(order, "fast") end)
                print(winner, table.concat(order, ","))
                """));
    }

    @Test
    void waitForAll_handsEachFunctionTheEventsItAskedFor() {
        assertEquals(List.of("b2,a1"), output("""
                os.queueEvent("b", 2)
                os.queueEvent("a", 1)
                local got = {}
                parallel.waitForAll(
                  function() local _, v = os.pullEvent("a") got[#got + 1] = "a" .. v end,
                  function() local _, v = os.pullEvent("b") got[#got + 1] = "b" .. v end)
                print(table.concat(got, ","))
                """));
    }

    @Test
    void parallel_raisesWhatOneOfItsFunctionsRaised() {
        assertEquals(List.of("false\tboom"), output("""
                print(pcall(parallel.waitForAny, function() error("boom", 0) end, function() sleep(5) end))
                """));
    }

    @Test
    void read_takesWhatIsTypedUpToEnter() {
        final Machine machine = new Machine();
        final Process process = start(LuaExecutionTest.load("""
                write("name? ")
                local name = read()
                print("hi " .. name)
                """), machine);
        process.step(PLENTY);
        assertTrue(process.waitingForInput(), "reading is waiting for the keyboard");
        process.offerInput("joao");
        run(process, machine);
        assertEquals(Process.State.FINISHED, process.state(), process::message);
        assertEquals("hi joao", process.console().getLast());
        assertTrue(process.terminal().row(0).startsWith("name? joao"), process.terminal().row(0));
    }

    @Test
    void term_drawsOnTheGridAndPrintWrapsAtItsEdge() {
        final Machine machine = new Machine();
        final Process process = run(start(LuaExecutionTest.load("""
                term.clear()
                term.setCursorPos(1, 1)
                term.write("hello")
                term.setCursorPos(1, 3)
                print(string.rep("x", 60))
                print(term.getSize())
                """), machine), machine);
        assertEquals(Process.State.FINISHED, process.state(), process::message);
        assertTrue(process.terminal().row(0).startsWith("hello"));
        assertEquals("x".repeat(51), process.terminal().row(2));
        assertTrue(process.terminal().row(3).startsWith("xxxxxxxxx "));
        assertEquals("51\t19", process.console().getLast());
    }

    @Test
    void save_bringsTheScreenAndTheWaitBack() {
        final Machine machine = new Machine();
        final Loaded program = LuaExecutionTest.load("""
                term.setCursorPos(1, 2)
                term.write("kept")
                local _, v = os.pullEvent("go")
                print("got " .. v)
                """);
        Process process = start(program, machine);
        process.step(PLENTY);
        assertEquals(Process.State.PARKED, process.state());
        final Snapshot shot = process.save();
        process = Process.restore(program, shot, machine);
        assertTrue(process.terminal().row(1).startsWith("kept"), process.terminal().row(1));
        process.queueEvent(new ArrayList<>(List.of("go", 5L)));
        run(process, machine);
        assertEquals(Process.State.FINISHED, process.state(), process::message);
        assertEquals("got 5", process.console().getLast());
    }

    @Test
    void fs_writesReadsAndListsTheMachinesFiles() {
        final Machine machine = new Machine();
        assertEquals(List.of("true\ttrue\t7", "one", "two", "log.txt", "true"), output("""
                fs.makeDir("data")
                local h = fs.open("data/log.txt", "w")
                h.writeLine("one")
                h.write("two")
                h.close()
                print(fs.exists("data/log.txt"), fs.isDir("data"), fs.getSize("data/log.txt"))
                local r = fs.open("data/log.txt", "r")
                print(r.readLine())
                print(r.readAll())
                r.close()
                print(table.concat(fs.list("data"), ","))
                print(fs.getFreeSpace("data") > 0)
                """, machine));
        assertEquals("one\ntwo", machine.files.get("data/log.txt"));
    }

    @Test
    void textutils_readsBackWhatItWrote() {
        assertEquals(List.of("1\t2\ty", "[1,2,3]", "true"), output("""
                local t = textutils.unserialize(textutils.serialize({1, 2, x = "y"}))
                print(t[1], t[2], t.x)
                print(textutils.serializeJSON({1, 2, 3}))
                print(textutils.unserializeJSON('{"a":[true]}').a[1])
                """));
    }

    @Test
    void require_loadsAModuleOnceFromBesideTheProgram() {
        final Machine machine = new Machine();
        machine.folders.add("lib");
        machine.files.put("lib/greet.lua", "print('loading') return { hi = function(n) return 'hi ' .. n end }");
        assertEquals(List.of("loading", "hi joao\ttrue"), output("""
                local g = require("lib.greet")
                local again = require("lib.greet")
                print(g.hi("joao"), g == again)
                """, machine));
    }

    @Test
    void require_namesEveryPlaceItLookedWhenNothingIsThere() {
        final List<String> said = output("print(pcall(require, 'nope'))");
        assertTrue(said.getFirst().startsWith("false"), said.getFirst());
        assertTrue(said.getFirst().contains("module 'nope' not found"), said.getFirst());
    }

    @Test
    void shellRun_runsAProgramWithItsArguments() {
        final Machine machine = new Machine();
        machine.files.put("hello.lua", "print('args', ...) print(arg[0])");
        assertEquals(List.of("args\ta\tb", "hello.lua", "true", "false"), output("""
                print(shell.run("hello", "a b"))
                print(shell.run("missing"))
                """, machine).stream().filter(line -> !line.equals("No such program")).toList());
    }

    @Test
    void settings_keepTheirTypeAndSaveToTheSettingsFile() {
        final Machine machine = new Machine();
        final List<String> said = output("""
                settings.define("reactor.speed", { default = 3, type = "number" })
                print(settings.get("reactor.speed"))
                settings.set("reactor.speed", 5)
                print(settings.get("reactor.speed"))
                print((pcall(settings.set, "reactor.speed", "fast")))
                print(settings.save())
                """, machine);
        assertEquals(List.of("3", "5", "false", "true"), said);
        assertTrue(machine.files.get(".settings").contains("reactor.speed"), machine.files.get(".settings"));
    }

    @Test
    void osRun_runsAFileInAnEnvironmentOfItsOwn() {
        final Machine machine = new Machine();
        machine.files.put("job.lua", "count = (count or 0) + 1 print('job', count, ...)");
        // The environment is the program's to keep: what the file left in it is there the second time.
        assertEquals(List.of("job\t1\tx", "true", "job\t2", "true", "nil"), output("""
                local env = {}
                print(os.run(env, "job.lua", "x"))
                print(os.run(env, "job.lua"))
                print(count)
                """, machine));
    }

    @Test
    void osRun_saysSoWhenTheFileIsNotThere() {
        assertEquals(List.of("File not found", "false"), output("print(os.run({}, 'nowhere.lua'))"));
    }

    @Test
    void loadfile_givesBackTheFunctionOrTheComplaint() {
        final Machine machine = new Machine();
        machine.files.put("good.lua", "return 21 * 2");
        assertEquals(List.of("42", "nil\tFile not found"), output("""
                print(loadfile("good.lua")())
                print(loadfile("bad.lua"))
                """, machine));
    }

    @Test
    void shellSetDir_movesTheProgramAndRefusesWhatIsNotAFolder() {
        final Machine machine = new Machine();
        machine.folders.add("data");
        machine.files.put("data/note.txt", "hi");
        assertEquals(List.of("data", "data/note.txt", "false"), output("""
                shell.setDir("data")
                print(shell.dir())
                print(shell.resolve("note.txt"))
                print((pcall(shell.setDir, "note.txt")))
                """, machine));
    }

    @Test
    void rednetReceive_givesNothingWhenItsTimeIsUp() {
        assertEquals(List.of("nil", "false"), output("""
                print(rednet.receive(0.5))
                print(rednet.isOpen())
                """));
    }

    @Test
    void os_knowsTheComputersNameAndNumber() {
        assertEquals(List.of("Reactor Room\t7\ttrue"), output("""
                print(os.getComputerLabel(), os.getComputerID(), _HOST:find("lrt") ~= nil)
                """));
    }

    @Test
    void devices_areThoseOfAComputerWithNothingAttached() {
        assertEquals(List.of("0\tfalse\tnil", "false"), output("""
                print(#peripheral.getNames(), peripheral.isPresent("left"), peripheral.wrap("left"))
                print((pcall(rednet.open, "top")))
                """));
    }
}
