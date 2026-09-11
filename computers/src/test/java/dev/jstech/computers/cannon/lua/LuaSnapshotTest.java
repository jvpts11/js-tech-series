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

import dev.jstech.computers.cannon.run.IHost;
import dev.jstech.computers.cannon.run.Loaded;
import dev.jstech.computers.cannon.run.Process;
import dev.jstech.computers.cannon.run.Snapshot;
import org.junit.jupiter.api.Test;

/**
 * A Lua program put away and brought back between every few instructions, which is what a world
 * being saved and loaded does to it, must end exactly as one left alone.
 */
class LuaSnapshotTest {

    private static final long ROOM = 256L * 1024;
    private static final int SLICE = 7;
    private static final int PATIENCE = 200_000;

    private static Process straight(final Loaded program) {
        final Process process = LuaExecutionTest.start(program, ROOM);
        for (int i = 0; i < 200 && process.state() == Process.State.RUNNING; i++) {
            process.step(1_000_000);
        }
        return process;
    }

    private static Process throughSaves(final Loaded program) {
        Process process = LuaExecutionTest.start(program, ROOM);
        for (int i = 0; i < PATIENCE && process.state() == Process.State.RUNNING; i++) {
            process.step(SLICE);
            final Snapshot shot = process.save();
            process = Process.restore(program, shot, IHost.still());
        }
        return process;
    }

    private static void bothWays(final String source) {
        final Loaded program = LuaExecutionTest.load(source);
        final Process straight = straight(program);
        final Process saved = throughSaves(program);
        assertEquals(Process.State.FINISHED, straight.state(), straight::message);
        assertEquals(straight.state(), saved.state(), saved::message);
        assertEquals(straight.console(), saved.console());
        assertTrue(!straight.console().isEmpty(), "the program said something");
    }

    @Test
    void save_carriesTablesClosuresAndGlobalsThrough() {
        bothWays("""
                local t = {}
                local function add(k, v) t[k] = v end
                for i = 1, 20 do add("k" .. i, i * i) end
                total = 0
                for k, v in pairs(t) do total = total + v end
                print(total, #t, t.k3)
                """);
    }

    @Test
    void save_carriesACoroutineInTheMiddleOfAYield() {
        bothWays("""
                local gen = coroutine.wrap(function()
                  for i = 1, 5 do coroutine.yield(i * 2) end
                end)
                local s = 0
                for v in gen do s = s + v end
                print(s)
                """);
    }

    @Test
    void save_carriesASortThatCallsBackIntoLua() {
        bothWays("""
                local t = {}
                for i = 1, 30 do t[i] = (i * 7919) % 101 end
                table.sort(t, function(a, b) return a > b end)
                print(table.concat(t, ","))
                """);
    }

    @Test
    void save_bringsBackAChunkTheProgramLoadedWhileRunningInsideIt() {
        bothWays("""
                local f = load("local s = 0 for i = 1, 40 do s = s + i end return s, ...")
                print(f("x"))
                print(f("y"))
                """);
    }

    @Test
    void save_carriesAProtectedCallAndAMetamethod() {
        bothWays("""
                local mt = {__index = function(t, k) return k .. "!" end}
                local t = setmetatable({}, mt)
                local ok, err = pcall(function()
                  local x = t.hello
                  error("after " .. x)
                end)
                print(ok, err)
                print(string.gsub("a b c", "%a", function(c) return c:upper() end))
                """);
    }
}
