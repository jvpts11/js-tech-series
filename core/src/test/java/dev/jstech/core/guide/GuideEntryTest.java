/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuideEntryTest {

    private static final Set<String> GAME = Set.of("mymod:pump", "mymod:valve");

    @Test
    void aboutAbsentItems_isTrueWhenNoItemItIsThePageOfIsInTheGame() {
        assertTrue(entry("othermod:controller", "othermod:card").aboutAbsentItems(GAME::contains));
    }

    @Test
    void aboutAbsentItems_isFalseWhenOneOfItsItemsIsInTheGame() {
        assertFalse(entry("othermod:controller", "mymod:pump").aboutAbsentItems(GAME::contains));
    }

    @Test
    void aboutAbsentItems_isFalseForAnEntryAboutAnIdea() {
        assertFalse(entry().aboutAbsentItems(GAME::contains));
    }

    private static GuideEntry entry(final String... items) {
        return new GuideEntry("mymod:page", "mymod:section", 0, "mymod.guide.page.title", "", List.of(items),
                List.of());
    }
}
