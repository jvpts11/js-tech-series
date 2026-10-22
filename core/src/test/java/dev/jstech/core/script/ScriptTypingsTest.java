/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.script;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ScriptTypingsTest {

    @Test
    void declarations_writeEachBindingsStaticCallsWithTheirTypes() {
        final String written = ScriptTypings.declarations(Map.of("JsFixture", Statics.class), List.of());
        assertTrue(written.contains("declare const JsFixture: {"));
        assertTrue(written.contains("teamOf(object: any, uuid: any): string;"));
        assertTrue(written.contains("names(): string[];"));
        assertTrue(written.contains("step(string: string): JavaOptional<number>;"));
        assertTrue(written.contains("levels(): Record<string, number>;"));
        assertFalse(written.contains("notStatic"), "only the binding's static calls");
    }

    @Test
    void declarations_writeEachEventWithItsGettersAndProperties() {
        final String written = ScriptTypings.declarations(Map.of(), List.of(new ScriptTypings.EventGroup(
                "JsFixtureEvents", List.of(new ScriptTypings.Event("gained", Gained.class),
                        new ScriptTypings.Event("lost", Gained.class)))));
        assertTrue(written.contains("declare namespace JsFixtureEvents {"));
        assertEquals(1, written.split("interface Gained \\{", -1).length - 1, "an event's class written once");
        assertTrue(written.contains("getTeam(): string;"));
        assertTrue(written.contains("readonly team: string;"));
        assertTrue(written.contains("readonly cancelled: boolean;"));
        assertTrue(written.contains("function gained(handler: (event: Gained) => void): void;"));
        assertTrue(written.contains("function lost(handler: (event: Gained) => void): void;"));
    }

    @Test
    void typeOf_takesTheScriptsOwnKindsAndAnyForTheRest() {
        assertEquals("number", ScriptTypings.typeOf(int.class));
        assertEquals("boolean", ScriptTypings.typeOf(Boolean.class));
        assertEquals("string[]", ScriptTypings.typeOf(String[].class));
        assertEquals("any", ScriptTypings.typeOf(UUID.class));
        assertEquals("any[]", ScriptTypings.typeOf(List.class));
    }

    /** A binding's calls, as a script sees them. */
    private static final class Statics {

        private Statics() {
        }

        public static String teamOf(final Object server, final UUID player) {
            return "";
        }

        public static List<String> names() {
            return List.of();
        }

        public static Optional<Integer> step(final String axis) {
            return Optional.empty();
        }

        public static Map<String, Double> levels() {
            return Map.of();
        }

        public void notStatic() {
        }
    }

    /** An event, as a handler is given it. */
    private static final class Gained {

        public String getTeam() {
            return "";
        }

        public boolean isCancelled() {
            return false;
        }
    }
}
