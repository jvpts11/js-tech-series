/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.help;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class HelpSearchTest {

    private static final List<String> ORDER = List.of("a", "b", "c", "d");

    @Test
    void nextHit_picksTheFirstHitAfterTheEntryBeingRead() {
        assertEquals("d", HelpSearch.nextHit(ORDER, List.of("a", "d"), "b"));
    }

    @Test
    void nextHit_wrapsToTheFirstHitWhenNoneComesAfter() {
        assertEquals("a", HelpSearch.nextHit(ORDER, List.of("a", "b"), "d"));
    }

    @Test
    void nextHit_startsFromTheTopWhenThePageIsNotAnEntry() {
        assertEquals("b", HelpSearch.nextHit(ORDER, List.of("b", "c"), null));
    }
}
