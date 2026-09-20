/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli.interac;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class InteracWordsTest {

    private static final List<String> VERBS = List.of("get", "put", "list", "craft", "where");

    private static InteracWords of(final String... typed) {
        return InteracWords.of(List.of(typed)).withVerbFrom(VERBS);
    }

    @Test
    void of_readsAVerbAQuantityAndAName() {
        final InteracWords words = of("get", "42", "cobblestone");

        assertEquals("get", words.verb());
        assertEquals(42L, words.quantity());
        assertEquals("cobblestone", words.item());
    }

    @Test
    void of_readsTheSameLineWrittenTheDosWay() {
        final InteracWords words = of("GET", "42", "COBBLESTONE", "/LOCAL");

        assertEquals("get", words.verb());
        assertEquals(42L, words.quantity());
        assertEquals("COBBLESTONE", words.item());
        assertEquals("local", words.option("to", ""));
    }

    @Test
    void of_readsAVerbWrittenAsAnOption() {
        final InteracWords words = of("--get", "42", "minecraft:cobblestone");

        assertEquals("get", words.verb());
        assertEquals(42L, words.quantity());
        assertEquals("minecraft:cobblestone", words.item());
    }

    @Test
    void of_takesTheWordAfterAnOptionThatNeedsOne() {
        final InteracWords spaced = of("get", "42", "cobblestone", "--to", "local");
        final InteracWords joined = of("get", "42", "cobblestone", "--to=local");

        assertEquals("local", spaced.option("to", ""));
        assertEquals("local", joined.option("to", ""));
        assertEquals("cobblestone", spaced.item(), "and the option is not taken for part of the name");
    }

    @Test
    void of_leavesAFlagWithNoValueAsAFlag() {
        final InteracWords words = of("list", "stone", "--local");

        assertTrue(words.has("local"));
        assertEquals("", words.option("local", ""));
        assertEquals("stone", words.item());
    }

    @Test
    void of_readsTheDosSortSwitch() {
        assertEquals("count", of("LIST", "STONE", "/S:COUNT").option("sort", "name"));
        assertEquals("name", of("LIST", "STONE").option("sort", "name"));
    }

    @Test
    void of_readsTheDosHelpSwitch() {
        assertTrue(of("/?").has("help"));
        assertTrue(of("--help").has("help"));
        assertFalse(of("list").has("help"));
    }

    @Test
    void item_takesTheQuotesOffANameOfSeveralWords() {
        assertEquals("oak log", of("get", "64", "\"oak", "log\"").item());
        assertEquals("oak log", of("where", "oak", "log").item());
    }

    @Test
    void quantity_readsANumberWrittenAsAListingShowsIt() {
        assertEquals(12480L, of("get", "12,480", "cobblestone").quantity());
        assertEquals(12480L, of("get", "12_480", "cobblestone").quantity());
    }

    @Test
    void quantity_isNotThereWhenTheWordsOpenWithAName() {
        final InteracWords words = of("where", "diamond");

        assertEquals(-1L, words.quantity());
        assertEquals("diamond", words.item());
    }

    @Test
    void of_withNothingTypedHasNoVerbAndNoWords() {
        final InteracWords words = InteracWords.of(List.of()).withVerbFrom(VERBS);

        assertEquals("", words.verb());
        assertEquals(0, words.wordCount());
        assertEquals("", words.item());
    }
}
