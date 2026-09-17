/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.system;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.vm.program.PureFunctions;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The names the compiler writes into a listing are the names the machine answers to.
 *
 * <p>This is the one thing a green compiler and a green machine cannot prove between them: the compiler writes
 * a call, the machine looks one up, and if the two spell the owner differently then every part still works and
 * the program stops at a call nothing answers. The names live in one place so they cannot come apart, and these
 * are here so that moving one of them out of that place is caught rather than shipped.
 */
class IntrinsicTypesTest {

    private static void assertAnswered(final String owner, final String name, final String... written) {
        assertTrue(PureFunctions.REGISTRY.find(owner, name, List.of(written)) != null,
                () -> "the machine answers nothing called " + owner + "." + name);
    }

    /**
     * A name spelled any other way is answered by nothing, which is what gives the rest of these their teeth.
     *
     * <p>Without it they would pass just as happily against a registry that answered everything, and the whole
     * point of them is that it answers exactly what was registered.
     */
    @Test
    void registry_answersNothingUnderAnameItWasNotGiven() {
        assertNull(PureFunctions.REGISTRY.find("Strings", "Concat",
                List.of(IntrinsicTypes.TEXT, IntrinsicTypes.TEXT)));
        assertNull(PureFunctions.REGISTRY.find(IntrinsicTypes.TEXT, "Join",
                List.of(IntrinsicTypes.TEXT, IntrinsicTypes.TEXT)));
    }

    @Test
    void registry_answersTheTextCallsTheCompilerWrites() {
        assertAnswered(IntrinsicTypes.TEXT, "Concat", IntrinsicTypes.TEXT, IntrinsicTypes.TEXT);
    }

    @Test
    void registry_answersTheDelegateCallsTheCompilerWrites() {
        assertAnswered(IntrinsicTypes.DELEGATE, "Combine", "T", "T");
        assertAnswered(IntrinsicTypes.DELEGATE, "Remove", "T", "T");
    }

    @Test
    void registry_answersTheCollectionCallsTheCompilerWrites() {
        assertAnswered(IntrinsicTypes.LIST, "Get", "int");
        assertAnswered(IntrinsicTypes.MAP, "Get", "K");
    }
}
