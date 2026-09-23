/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class TextKeyTest {

    @Test
    void of_refusesAKeyThatIsNotWordsJoinedByDots() {
        assertThrows(IllegalArgumentException.class, () -> TextKey.of("nodots", "English"));
        assertThrows(IllegalArgumentException.class, () -> TextKey.of("has space.key", "English"));
        assertEquals("itemGroup.jsc.computing", TextKey.of("itemGroup.jsc.computing", "J's Computers").key());
    }

    @Test
    void of_refusesAKeyWithoutItsEnglish() {
        assertThrows(IllegalArgumentException.class, () -> TextKey.of("jscore.test.empty", " "));
        assertThrows(IllegalArgumentException.class, () -> TextKey.of("jscore.test.none", null));
    }

    @Test
    void text_isTheSentenceWithNothingPutIn() {
        final TextKey key = TextKey.of("jscore.test.alone", "alone");
        assertEquals(new Text.Translated(key, List.of()), key.text());
    }

    @Test
    void with_keepsTextAsTextAndDataAsData() {
        final TextKey outer = TextKey.of("jscore.test.outer", "%s and %s");
        final TextKey inner = TextKey.of("jscore.test.inner", "inner");
        assertEquals(new Text.Translated(outer, List.of(inner.text(), Text.literal("7"))), outer.with(inner, 7));
    }
}
