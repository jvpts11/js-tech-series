/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.integration.computercraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.cannon.SourceFile;
import dev.jstech.computers.cannon.lua.LuaCompiler;
import org.junit.jupiter.api.Test;

/**
 * The agent is the one program of ours that is never written by hand in the other side's language, and
 * this is what keeps it honest: it is compiled from its own source here, translated by the same
 * translator every other program goes through, and read back by our own front end. A translator that
 * breaks takes the agent with it, and this says so before a computer anywhere tries to run it.
 */
class JscRomTest {

    @Test
    void agent_compilesFromItsOwnSourceAndIsReadableLua() {
        JscRom.forget();
        final String lua = JscRom.agent();
        assertFalse(lua.isEmpty(), "the agent's source is in the jar and compiles");
        final LuaCompiler.Result read = LuaCompiler.compile(new SourceFile("jsc.lua", lua));
        assertTrue(read.ok(), () -> String.join("\n", read.lines()) + "\n\n" + lua);
    }

    @Test
    void agent_saysWhatItDoesAndNothingMore() {
        final String lua = JscRom.agent();
        assertTrue(lua.contains("_serve("), "it serves what the Gateway asks");
        assertTrue(lua.contains("_shell("), "it can run a line at its own prompt");
        assertTrue(lua.contains("_fsread("), "it reads its own computer's files");
        assertTrue(lua.contains("jsc_gateway"), "it looks for the Gateway itself");
        // The prelude always knows how to ask our network; what matters is that the agent never does.
        assertFalse(lua.contains("_ask(\""), "it never asks our network anything");
    }

    @Test
    void agent_isMadeOnceAndKept() {
        JscRom.forget();
        final String first = JscRom.agent();
        assertEquals(first, JscRom.agent(), "the same text, without being made again");
    }
}
