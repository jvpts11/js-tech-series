/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.text;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class TextFormatTest {

    @Test
    void apply_putsTheArgumentsInInTheirOrder() {
        assertEquals("3 ports, 28 MB", TextFormat.apply("%s ports, %s", List.of("3", "28 MB")));
    }

    @Test
    void apply_putsANumberedArgumentWhereverItStands() {
        assertEquals("28 MB for 3 ports", TextFormat.apply("%2$s for %1$s ports", List.of("3", "28 MB")));
    }

    @Test
    void apply_writesADoublePercentAsOne() {
        assertEquals("10% less", TextFormat.apply("%s%% less", List.of("10")));
    }

    @Test
    void apply_leavesNothingWhereAnArgumentIsMissing() {
        assertEquals(" of ", TextFormat.apply("%s of %s", List.of("")));
        assertEquals("a  b", TextFormat.apply("a %3$s b", List.of("x")));
    }

    @Test
    void apply_leavesAnyOtherMarkAsItIsWritten() {
        assertEquals("%d items and %", TextFormat.apply("%d items and %", List.of()));
    }

    @Test
    void apply_returnsASentenceWithNoMarksUntouched() {
        final String plain = "nothing to put in";
        assertEquals(plain, TextFormat.apply(plain, List.of("unused")));
    }
}
