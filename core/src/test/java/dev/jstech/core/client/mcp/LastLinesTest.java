/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class LastLinesTest {

    @Test
    void since_findsWhatCameAfterAMomentOnceTheOldestDropOffTheFront() {
        final LastLines<String> lines = new LastLines<>(3);
        for (final String line : List.of("a", "b", "c")) {
            lines.add(line);
        }
        final long before = lines.added();
        lines.add("d");
        lines.add("e");
        assertEquals(List.of("c", "d", "e"), lines.all(), "only the last three are kept");
        assertEquals(List.of("d", "e"), lines.since(before), "what came after the moment, though the kept ones "
                + "are as many as before");
    }

    @Test
    void since_givesWhatIsStillKeptWhenMoreCameThanAreKept() {
        final LastLines<String> lines = new LastLines<>(2);
        final long before = lines.added();
        for (final String line : List.of("a", "b", "c", "d")) {
            lines.add(line);
        }
        assertEquals(List.of("c", "d"), lines.since(before));
        assertEquals(List.of(), lines.since(lines.added()), "nothing came after now");
    }
}
