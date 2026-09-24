/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TextListsTest {

    private static final TextKey COAL = TextKey.of("jscore.test.coal", "Coal");

    /** A language that shouts, with a word for coal and the joining sentence left as it is. */
    private static ITextLanguage shouting() {
        final Map<String, String> words = Map.of("jscore.test.coal", "COAL", "jscore.text.joined", "%s%s%s");
        return words::get;
    }

    @Test
    void join_putsTheSeparatorBetweenEachTwoParts() {
        final Text joined = TextLists.join(", ", List.of(Text.literal("Iron Ingot"), COAL.text(), Text.literal("Sand")));
        assertEquals("Iron Ingot, Coal, Sand", joined.english());
    }

    @Test
    void join_readsEachPartInTheLanguageAsked() {
        final Text joined = TextLists.join(" -> ", List.of(Text.literal("Macerator"), COAL.text()));
        assertEquals("Macerator -> COAL", joined.resolve(shouting()));
    }

    @Test
    void join_ofOnePartIsThatPartAndOfNoneIsNothing() {
        assertEquals(COAL.text(), TextLists.join(", ", List.of(COAL.text())));
        assertTrue(TextLists.join(", ", List.of()).isEmpty());
    }

    @Test
    void join_nestsOnlyAsDeepAsTheLogarithmOfTheLength() {
        final List<Text> parts = new ArrayList<>();
        for (int i = 0; i < 64; i++) {
            parts.add(Text.literal(Integer.toString(i)));
        }
        final Text joined = TextLists.join(",", parts);
        assertEquals(6, depth(joined));
        assertEquals(String.join(",", parts.stream().map(Text::english).toList()), joined.english());
    }

    private static int depth(final Text text) {
        if (!(text instanceof Text.Translated translated)) {
            return 0;
        }
        int deepest = 0;
        for (final Text arg : translated.args()) {
            deepest = Math.max(deepest, depth(arg));
        }
        return deepest + 1;
    }
}
