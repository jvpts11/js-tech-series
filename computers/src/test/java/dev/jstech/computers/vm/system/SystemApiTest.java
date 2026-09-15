/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SystemApiTest {

    @Test
    void types_areEachDeclaredOnce() {
        final Set<String> names = new HashSet<>();
        for (final TypeSpec type : SystemApi.types()) {
            assertTrue(names.add(type.name()), () -> type.name() + " is declared twice");
        }
    }

    @Test
    void methods_areEachDeclaredOnce() {
        final Set<MemberId> ids = new HashSet<>();
        for (final TypeSpec type : SystemApi.types()) {
            for (final MethodSpec method : type.methods()) {
                assertTrue(ids.add(method.id()), () -> method.id().describe() + " is declared twice");
            }
        }
    }

    @Test
    void types_liveInTheSystemsNamespaces() {
        for (final TypeSpec type : SystemApi.types()) {
            assertTrue(type.namespace().startsWith("System."), () -> type.name() + " is in " + type.namespace());
        }
    }

    @Test
    void type_findsATypeByItsName() {
        assertEquals("System.Utils", SystemApi.type("Math").namespace());
        assertNull(SystemApi.type("Nothing"));
    }

    @Test
    void new_refusesAMethodOfAnotherType() {
        final MethodSpec abs = new MethodSpec(new MemberId("Math", "Abs", List.of("int")), "int", true,
                MemberKind.PURE, CallCost.FREE);
        assertThrows(IllegalArgumentException.class,
                () -> new TypeSpec("System.Utils", "Convert", List.of(abs)));
    }
}
