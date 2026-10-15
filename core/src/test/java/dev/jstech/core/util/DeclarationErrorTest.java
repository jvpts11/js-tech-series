/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class DeclarationErrorTest {

    @Test
    void message_namesTheModTheDeclarationTheProblemAndTheFix() {
        final DeclarationError error = DeclarationError.of("mymod", "the block mymod:press", "has no name",
                "give it one with named(\"Press\") before register()");

        assertEquals("mymod declares the block mymod:press, which has no name. To fix it, give it one with"
                + " named(\"Press\") before register().", error.getMessage());
    }

    @Test
    void message_withoutAModStartsWithTheDeclaration() {
        final DeclarationError error = DeclarationError.of(null, "the setting power.limit", "has no name",
                "give it one with named before the file is built");

        assertEquals("The setting power.limit has no name. To fix it, give it one with named before the file is"
                + " built.", error.getMessage());
        assertNull(error.mod());
    }

    @Test
    void of_keepsEachPartForWhoeverShowsTheError() {
        final DeclarationError error = DeclarationError.of("mymod", "the cable mymod:wire", "is declared twice",
                "declare it once");

        assertEquals("mymod", error.mod());
        assertEquals("the cable mymod:wire", error.declaration());
        assertEquals("is declared twice", error.problem());
        assertEquals("declare it once", error.fix());
    }

    @Test
    void of_isAnIllegalStateExceptionSoCallersThatCatchOneStillDo() {
        assertInstanceOf(IllegalStateException.class, DeclarationError.of(null, "a thing", "is wrong", "mend it"));
    }

    @Test
    void of_refusesAMissingPart() {
        assertThrows(NullPointerException.class, () -> DeclarationError.of("mymod", "a thing", "is wrong", null));
    }
}
