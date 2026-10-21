/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ReaderMarksTest {

    @Test
    void of_isNewWhileAnEntryIsUnseen() {
        final List<GuideEntry> entries = List.of(entry("test:a", ""), entry("test:b", ""));

        assertEquals(ReaderMarks.Mark.NEW, ReaderMarks.of(entries, Set.of("test:a"), known -> true));
        assertEquals(ReaderMarks.Mark.NONE, ReaderMarks.of(entries, Set.of("test:a", "test:b"), known -> true));
        assertEquals(ReaderMarks.Mark.NONE, ReaderMarks.of(List.of(), Set.of(), known -> true));
    }

    @Test
    void of_isNotYetWhenEveryEntryNeedsWhatIsNotKnown() {
        final Set<String> seen = Set.of("test:a", "test:b");
        final List<GuideEntry> locked = List.of(entry("test:a", "test:smelting"), entry("test:b", "test:alloys"));
        final List<GuideEntry> mixed = List.of(entry("test:a", "test:smelting"), entry("test:b", ""));

        assertEquals(ReaderMarks.Mark.NOT_YET, ReaderMarks.of(locked, seen, known -> false));
        assertEquals(ReaderMarks.Mark.NONE, ReaderMarks.of(locked, seen, "test:smelting"::equals),
                "known for one, the section is open");
        assertEquals(ReaderMarks.Mark.NONE, ReaderMarks.of(mixed, seen, known -> false),
                "an entry that needs nothing opens its section");
        assertEquals(ReaderMarks.Mark.NEW, ReaderMarks.of(locked, Set.of(), known -> false),
                "new before not yet: the reader is told it is there");
    }

    @Test
    void anyNew_findsOneUnseenEntry() {
        assertTrue(ReaderMarks.anyNew(List.of(entry("test:a", "")), Set.of()));
        assertFalse(ReaderMarks.anyNew(List.of(entry("test:a", "")), Set.of("test:a")));
    }

    @Test
    void nextColour_takesTheLeastWornFirst() {
        assertEquals(0, ReaderMarks.nextColour(List.of(), 3));
        assertEquals(1, ReaderMarks.nextColour(List.of(0), 3));
        assertEquals(2, ReaderMarks.nextColour(List.of(0, 1), 3));
        assertEquals(0, ReaderMarks.nextColour(List.of(0, 1, 2), 3), "every colour once: the first again");
        assertEquals(1, ReaderMarks.nextColour(List.of(0, 2, 0), 3));
    }

    private static GuideEntry entry(final String id, final String needs) {
        return new GuideEntry(id, "test:section", 0, "title", "", List.of(), List.of(), List.of(), needs);
    }
}
