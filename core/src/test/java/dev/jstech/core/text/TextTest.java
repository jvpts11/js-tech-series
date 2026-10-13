/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.text;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;

class TextTest {

    private static final TextKey INDEXED = TextKey.of("jscore.test.indexed", "%s ports, %s on this disk");
    private static final TextKey UNIT = TextKey.of("jscore.test.unit", "megabytes");

    /** A language with a sentence for some keys and none for the rest. */
    private static ITextLanguage portuguese() {
        final Map<String, String> words = Map.of(
                "jscore.test.indexed", "%s ports, %s neste disco",
                "jscore.test.unit", "megabytes");
        return words::get;
    }

    @Test
    void resolve_aLiteralReadsTheSameInEveryLanguage() {
        final Text path = Text.literal("/usr/ports");
        assertEquals("/usr/ports", path.resolve(ITextLanguage.ENGLISH));
        assertEquals("/usr/ports", path.resolve(portuguese()));
    }

    @Test
    void resolve_aSentenceReadsInTheLanguageAsked() {
        final Text indexed = INDEXED.with(35, "28 MB");
        assertEquals("35 ports, 28 MB on this disk", indexed.resolve(ITextLanguage.ENGLISH));
        assertEquals("35 ports, 28 MB neste disco", indexed.resolve(portuguese()));
    }

    @Test
    void resolve_aLanguageWithoutTheKeyFallsBackToTheDeclaredEnglish() {
        final TextKey missing = TextKey.of("jscore.test.missing", "only in English");
        assertEquals("only in English", missing.text().resolve(portuguese()));
    }

    @Test
    void resolve_translatesASentencePutIntoAnother() {
        assertEquals("1 ports, megabytes on this disk", INDEXED.with(1, UNIT).resolve(ITextLanguage.ENGLISH));
    }

    @Test
    void of_keepsATextAndTurnsAnythingElseIntoData() {
        final Text given = Text.literal("x");
        assertEquals(given, Text.of(given));
        assertEquals(UNIT.text(), Text.of(UNIT));
        assertEquals(Text.literal("42"), Text.of(42));
        assertEquals(Text.literal("null"), Text.of(null));
    }
}
