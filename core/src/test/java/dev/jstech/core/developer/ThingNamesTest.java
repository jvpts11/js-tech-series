/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ThingNamesTest {

    @Test
    void names_holdsTheThingWholeBetweenCuts() {
        assertTrue(ThingNames.names("compressor_steam", "compressor"));
        assertTrue(ThingNames.names("machine/compressor.running", "compressor"));
        assertTrue(ThingNames.names("compressor", "compressor"));
        assertTrue(ThingNames.names("coal_generator-start", "coal_generator"));
    }

    @Test
    void names_isNotAThingInsideALongerWord() {
        assertFalse(ThingNames.names("compressors", "compressor"));
        assertFalse(ThingNames.names("decompressor", "compressor"));
    }

    @Test
    void names_findsALaterWholeMatchAfterAPartOne() {
        assertTrue(ThingNames.names("tanks/tank_fill", "tank"));
    }

    @Test
    void names_readsOnlyTheLastPartOfAPathInFolders() {
        assertTrue(ThingNames.names("press_working", "machines/press"));
    }

    @Test
    void names_isFalseForAnEmptyThing() {
        assertFalse(ThingNames.names("anything", "machines/"));
    }
}
