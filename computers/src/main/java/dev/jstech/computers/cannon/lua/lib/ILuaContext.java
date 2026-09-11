/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.cannon.lua.lib;

import dev.jstech.computers.cannon.run.Halt;
import dev.jstech.computers.cannon.run.Values;
import java.util.List;

/**
 * What the standard library needs of the runtime it runs on.
 *
 * <p>Everything a library function makes goes on the process's heap through here, so a table or a
 * piece of text made by {@code string.rep} is counted and saved like one the program made itself.
 * The library never calls a Lua function itself: it hands back a {@link LuaCall} and the runtime
 * makes the call and carries on with the continuation named in it.
 */
public interface ILuaContext {

    /** A new, empty table on the heap. */
    Values.Table table(int line);

    /** A piece of text on the heap. */
    String text(String value, int line);

    /** A run of values on the heap: what a call with several results, or the arguments, travel as. */
    Values.Arr values(List<Object> items, int line);

    /** The values a result stands for: one for a single value, all of them for a run. */
    List<Object> expand(Object result);

    /** Tells the heap a table has grown or shrunk. */
    void resized(Values.Table table, int line);

    /** The metatable that answers for the value, or null: a table's own, or the one all text shares. */
    Values.Table metatableOf(Object value);

    /** A function of the library, as a value the program can hold and call. */
    Values.DelegateValue function(String name, Object target, int line);

    /** An object that carries a continuation's state between the call and the callback. */
    Values.Obj state(String next, String failed, int line);

    /** The halt that raising this value is, with no position added. */
    Halt raise(Object value, int line);

    /** The halt that raising this message is, with the position of the running code in front. */
    Halt error(String message, int line);

    /** Where the running code is, as {@code file:line:}, or {@code ""} when that is not known. */
    String where(int level, int line);

    /** Writes a whole line to the process's console. */
    void print(String line);

    /** Writes text to the console without ending the line. */
    void write(String text);

    /** The tick the machine is on. */
    long tick();

    /** The time of day on the machine, in ticks of the day. */
    long dayTime();

    /** The day the machine is on. */
    long day();

    /** How many instructions the process has run, which is what {@code os.clock} measures. */
    long spent();

    /** The runtime's random numbers, seeded by {@code math.randomseed}. */
    java.util.Random random();

    /** Reseeds the random numbers. */
    void seed(long seed);

    /** How much of the heap the process holds, in bytes, for {@code collectgarbage("count")}. */
    long heldBytes();

    /** Frees what the program can no longer reach and says how many bytes that was. */
    long collect();

    // what ComputerCraft's libraries need

    /** The program's screen, made the first time it is asked for. */
    LuaTerminal terminal();

    /** Puts an event at the back of the program's queue: its name, then what it carries. */
    void queueEvent(List<Object> values);

    /** Starts a timer that queues {@code timer} after so many seconds, and gives its number. */
    long startTimer(double seconds);

    boolean cancelTimer(long id);

    /** Sets an alarm that queues {@code alarm} when the world's clock reaches that hour of the day. */
    long setAlarm(double hour);

    boolean cancelAlarm(long id);

    /** The machine's disks. */
    ILuaFiles files();

    /**
     * Compiles text into a function, as {@code load} does: the function, or a run of nil and the
     * complaint. {@code environment} is where its globals go, or null for the program's own.
     */
    Object loadChunk(String text, String name, Object environment, int line);

    /** The folder the program was started in, as a ComputerCraft path; empty for the root. */
    String directory();

    void setDirectory(String path);

    /** The name the program was started from. */
    String programName();

    /** The machine's name, which is ComputerCraft's computer label. */
    String computerName();

    /** A number for the machine that stays the same, which is ComputerCraft's computer id. */
    long computerId();

    /** Makes a coroutine of a function, as {@code coroutine.create} does. */
    Object createCoroutine(Object function, int line);

    /** Whether a coroutine has finished, one way or another. */
    boolean coroutineDead(Object coroutine);

    /** Ends the program where it stands, as {@code os.shutdown} and {@code shell.exit} do. */
    void exit();

    /** A global of the program, as the library reads one. */
    Object global(String name);

    /** A table the library keeps for itself under that name, saved with the program. */
    Values.Table store(String name, int line);

    /** Says the program is, or is no longer, in the middle of reading a line from the keyboard. */
    void reading(boolean value);
}
