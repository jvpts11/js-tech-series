/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer.studio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TranslationGapsTest {

    @Test
    void of_givesEachLanguageTheKeysTheOthersHaveAndItLacks() {
        final Map<String, List<String>> gaps = TranslationGaps.of(Map.of(
                "en_us", Set.of("block.a", "block.b", "item.c"),
                "pt_br", Set.of("block.a")));
        assertEquals(List.of(), gaps.get("en_us"));
        assertEquals(List.of("block.b", "item.c"), gaps.get("pt_br"));
    }

    @Test
    void keys_isEveryKeyOfEveryLanguageOnce() {
        assertEquals(Set.of("a", "b", "c"), TranslationGaps.keys(Map.of("en_us", Set.of("a", "b"),
                "pt_br", Set.of("b", "c"))));
    }
}
