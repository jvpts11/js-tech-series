/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide.markdown;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FrontMatterTest {

    @Test
    void listOf_splitsOnCommasAndTakesQuotesOff() {
        assertEquals(List.of("mymod:kiln", "mymod:kiln_door", "J's Computers"),
                FrontMatter.listOf("[mymod:kiln, 'mymod:kiln_door', J's Computers]"));
    }

    @Test
    void listOf_keepsACommaInsideDoubleQuotes() {
        assertEquals(List.of("Frames 95, XP and 7,", "Second Edition"),
                FrontMatter.listOf("[\"Frames 95, XP and 7,\", Second Edition]"));
    }

    @Test
    void list_readsAQuotedItemFromTheHeader() {
        final FrontMatter head = FrontMatter.of("---\nkind: manual\ncover: [\"Sigma, the language\", Book]\n---\n# T");

        assertEquals(List.of("Sigma, the language", "Book"), head.list("cover"));
    }

    @Test
    void of_readsTheHeaderOfAFileSavedWithAByteOrderMark() {
        final FrontMatter head = FrontMatter.of("﻿---\nkind: manual\n---\n# T");

        assertEquals(Map.of("kind", "manual"), head.fields());
        assertEquals("# T", head.body());
    }
}
