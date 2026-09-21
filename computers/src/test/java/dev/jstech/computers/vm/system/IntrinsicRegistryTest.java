/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class IntrinsicRegistryTest {

    private static final IPureFunction NOTHING = (context, target, arguments, line) -> null;

    private static IntrinsicRegistry registry() {
        return IntrinsicRegistry.builder()
                .onObject("List", "Add", "void", NOTHING, "T")
                .onObject("Map", "TryGet", "bool", NOTHING, "K", "out V")
                .onType("Math", "Abs", "int", NOTHING, "int")
                .onType("Math", "Abs", "double", NOTHING, "double")
                .build();
    }

    @Test
    void find_letsATypeParameterTakeWhateverTypeTheCallIsWrittenWith() {
        final IntrinsicRegistry registry = registry();
        assertNotNull(registry.find("List", "Add", List.of("int")));
        assertNotNull(registry.find("List", "Add", List.of("List<string>")));
        assertNotNull(registry.find("List", "Add", List.of("T")));
        assertNotNull(registry.find("Map", "TryGet", List.of("string", "out int")));
    }

    @Test
    void find_tellsOverloadsApartByTheTypesTheyTake() {
        final IntrinsicRegistry registry = registry();
        assertEquals("int", registry.find("Math", "Abs", List.of("int")).returns());
        assertEquals("double", registry.find("Math", "Abs", List.of("double")).returns());
        assertNull(registry.find("Math", "Abs", List.of("long")));
    }

    @Test
    void find_keepsAnOutwardParameterApartFromAnInwardOne() {
        final IntrinsicRegistry registry = registry();
        assertNull(registry.find("Map", "TryGet", List.of("string", "int")));
        assertNull(registry.find("List", "Add", List.of("out int")));
    }

    @Test
    void find_answersNothingForACallNobodyRegistered() {
        final IntrinsicRegistry registry = registry();
        assertNull(registry.find("Math", "Cube", List.of("int")));
        assertNull(registry.find("List", "Add", List.of("int", "int")));
        assertNull(registry.find("Tests.List", "Add", List.of("int")));
    }

    @Test
    void builder_refusesTheSameCallTwice() {
        final IntrinsicRegistry.Builder builder = IntrinsicRegistry.builder().onType("Math", "Abs", "int", NOTHING, "int");
        assertThrows(IllegalStateException.class, () -> builder.onType("Math", "Abs", "int", NOTHING, "int"));
    }

    @Test
    void builder_takesNothingOnceTheRegistryIsBuilt() {
        final IntrinsicRegistry.Builder builder = IntrinsicRegistry.builder();
        builder.build();
        assertThrows(IllegalStateException.class, () -> builder.onType("Math", "Abs", "int", NOTHING, "int"));
    }
}
