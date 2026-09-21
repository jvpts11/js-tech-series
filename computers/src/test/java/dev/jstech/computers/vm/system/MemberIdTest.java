/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class MemberIdTest {

    @Test
    void describe_writesTheCallTheWayAListingDoes() {
        assertEquals("Map.TryGet(K, out V)", new MemberId("Map", "TryGet", List.of("K", "out V")).describe());
        assertEquals("Program.Current()", new MemberId("Program", "Current", List.of()).describe());
    }

    @Test
    void parameters_stayAsTheyWereWhenTheIdWasMade() {
        final List<String> given = new ArrayList<>(List.of("int"));
        final MemberId id = new MemberId("Math", "Abs", given);
        given.add("int");
        assertEquals(List.of("int"), id.parameters());
    }

    @Test
    void equals_tellsOverloadsApartByTheTypesTheyTake() {
        assertEquals(new MemberId("Math", "Abs", List.of("int")), new MemberId("Math", "Abs", List.of("int")));
        assertNotEquals(new MemberId("Math", "Abs", List.of("int")), new MemberId("Math", "Abs", List.of("double")));
    }
}
