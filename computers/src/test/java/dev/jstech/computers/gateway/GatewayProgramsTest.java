/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * What happens to one of our programs on its way to a computer of theirs.
 *
 * <p>The rule this holds to is that translating is not a choice: a program goes over as what that
 * computer runs, because that is what going there means. What is already in their language crosses
 * untouched, and what is in neither is refused rather than sent as something nobody can run.
 */
class GatewayProgramsTest {

    private static final String CANNON = """
            using System.*;
            using System.IO.*;
            namespace Plant;
            class Reactor {
                static void Main() {
                    Console.PrintLine("holding at " + 900);
                }
            }
            """;

    @Test
    void translated_sendsTheirOwnLanguageUntouched() throws GatewayRefusedException {
        final String theirs = "print('hello')";
        assertSame(theirs, GatewayPrograms.translated("hello.lua", theirs),
                "a program already in their language is not touched on the way");
    }

    @Test
    void translated_turnsOneOfOursIntoWhatThatComputerRuns() throws GatewayRefusedException {
        final String made = GatewayPrograms.translated("reactor.can", CANNON);
        assertTrue(made.startsWith("local P"), "it crosses as a translated program");
        assertTrue(made.contains("holding at"), "carrying what it says");
        assertFalse(made.contains("Console.PrintLine"), "and not as what it was written in");
    }

    @Test
    void translated_compilesTheSameSourceOnlyOnce() throws GatewayRefusedException {
        GatewayPrograms.forget();
        final String first = GatewayPrograms.translated("reactor.can", CANNON);
        assertSame(first, GatewayPrograms.translated("reactor.can", CANNON),
                "asked for again, it is the same answer and not the same work");
        assertSame(first, GatewayPrograms.translated("elsewhere.can", CANNON),
                "what matters is what the file says, not what it is called");
        final String edited = GatewayPrograms.translated("reactor.can",
                CANNON.replace("900", "1200"));
        assertTrue(edited.contains("1200"), "and an edited program is translated again");
    }

    @Test
    void translated_refusesWhatIsNotAProgramAndWhatDoesNotCompile() {
        assertThrows(GatewayRefusedException.class,
                () -> GatewayPrograms.translated("notes.txt", "hello"));
        final GatewayRefusedException broken = assertThrows(GatewayRefusedException.class,
                () -> GatewayPrograms.translated("bad.can", "class Program { static void Main() { x"));
        assertTrue(broken.getMessage().contains("does not compile"), broken.getMessage());
    }

    @Test
    void carries_knowsWhatCanCrossAtAll() {
        assertTrue(GatewayPrograms.carries("reactor.can"));
        assertTrue(GatewayPrograms.carries("reactor.asm"));
        assertTrue(GatewayPrograms.carries("REACTOR.LUA"));
        assertFalse(GatewayPrograms.carries("reactor.txt"));
        assertFalse(GatewayPrograms.carries("reactor"));
    }

    @Test
    void translated_readsTheAssemblyItselfWhenThatIsWhatItIs() throws GatewayRefusedException {
        final String assembly = """
                .asm 1
                .start Plant.Reactor console

                .class Plant.Reactor

                .method static void Main() slots 0
                    ldstr   "from the assembly"
                    call    Console.PrintLine(string) -> void
                    ret
                """;
        final String made = GatewayPrograms.translated("reactor.asm", assembly);
        assertTrue(made.contains("from the assembly"), made);
        assertEquals(made, GatewayPrograms.translated("reactor.asm", assembly));
    }
}
