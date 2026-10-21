/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class StudioTestsTest {

    private static final List<String> TESTS = List.of("testkitgametests.testkitmachine_works",
            "testkitgametests.arena_is_empty", "compressortests.jams_when_full", "energytests.cable_carries");

    @Test
    void naming_findsTheMethodThatNamesTheThing() {
        assertEquals(List.of("testkitgametests.testkitmachine_works"), StudioTests.naming(TESTS, "test_kit_machine"));
    }

    @Test
    void naming_findsEveryTestOfAClassNamedAfterTheThing() {
        assertEquals(List.of("compressortests.jams_when_full"), StudioTests.naming(TESTS, "compressor"));
    }

    @Test
    void naming_readsOnlyTheLastPartOfAPathInFolders() {
        assertEquals(List.of("compressortests.jams_when_full"), StudioTests.naming(TESTS, "machine/compressor"));
    }

    @Test
    void naming_findsNothingForAThingNoTestNames() {
        assertEquals(List.of(), StudioTests.naming(TESTS, "furnace"));
    }

    @Test
    void naming_findsNothingForAPathOfNoLetters() {
        assertEquals(List.of(), StudioTests.naming(TESTS, "_/"));
    }

    @Test
    void squeezed_keepsOnlyLettersAndDigitsInLowerCase() {
        assertEquals("tank2way", StudioTests.squeezed("Tank_2-Way"));
    }
}
