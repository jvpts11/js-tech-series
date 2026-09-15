/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class IntrinsicSpecTest {

    private static final IPureFunction NOTHING = (context, target, arguments, line) -> null;

    @Test
    void pure_isAnsweredByItsFunctionAndCostsNothing() {
        final IntrinsicSpec spec = IntrinsicSpec.pure("Math", "Abs", List.of("int"), "int", false, NOTHING);
        assertEquals(MemberKind.PURE, spec.kind());
        assertEquals(CallCost.FREE, spec.cost());
        assertEquals(new MemberId("Math", "Abs", List.of("int")), spec.id());
        assertEquals("Math.Abs(int)", spec.describe());
    }

    @Test
    void new_refusesAPureCallWithNothingToAnswerIt() {
        assertThrows(IllegalArgumentException.class, () -> new IntrinsicSpec(
                new MemberId("Math", "Abs", List.of("int")), "int", false, MemberKind.PURE, CallCost.FREE, null));
    }

    @Test
    void new_refusesAFunctionOnACallTheMachineAnswers() {
        assertThrows(IllegalArgumentException.class, () -> new IntrinsicSpec(
                new MemberId("File", "Read", List.of("string")), "string", false, MemberKind.WORLD, CallCost.of(50),
                NOTHING));
    }

    @Test
    void accepts_keepsAnOutwardParameterApartAndLetsATypeParameterTakeAnyType() {
        final IntrinsicSpec tryGet =
                IntrinsicSpec.pure("Map", "TryGet", List.of("K", "out V"), "bool", true, NOTHING);
        assertTrue(tryGet.accepts(List.of("string", "out int")));
        assertFalse(tryGet.accepts(List.of("string", "int")));
    }
}
