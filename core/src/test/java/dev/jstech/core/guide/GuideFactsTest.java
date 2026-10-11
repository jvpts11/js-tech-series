/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class GuideFactsTest {

    @Test
    void fill_writesTheValuesASentenceNamesTheReadersWay() {
        GuideFacts.value("facts_test:slots", () -> new GuideBlock.GuideValue.Amount(4, ""));
        GuideFacts.value("facts_test:power", () -> new GuideBlock.GuideValue.Amount(12000, "FE"));

        assertEquals("It holds 4 modules and uses 12,000 FE.", GuideFacts.fill("It holds "
                + GuideFacts.token("facts_test:slots") + " modules and uses {{facts_test:power}}.", new Words()));
    }

    @Test
    void fill_leavesANameNoModRegisteredAsItIsWritten() {
        assertEquals("It holds {{facts_test:nothing}} modules.",
                GuideFacts.fill("It holds {{facts_test:nothing}} modules.", new Words()));
        assertEquals("An open {{ with no close.", GuideFacts.fill("An open {{ with no close.", new Words()));
    }

    @Test
    void fill_leavesANameWhoseAnswerFailsAsItIsWritten() {
        GuideFacts.value("facts_test:broken", () -> {
            throw new IllegalStateException("not loaded");
        });

        assertEquals("{{facts_test:broken}}", GuideFacts.fill("{{facts_test:broken}}", new Words()));
    }

    @Test
    void rowsOf_answersEachTimeAndLeavesOutARowThatDoesNotFit() {
        final int[] asked = {0};
        GuideFacts.rows("facts_test:rows", () -> {
            asked[0]++;
            return List.of(row("a", "b"), row("lone"), row("c", "d"));
        });

        assertEquals(List.of(row("a", "b"), row("c", "d")), GuideFacts.rowsOf("facts_test:rows", 2));
        GuideFacts.rowsOf("facts_test:rows", 2);
        assertEquals(2, asked[0], "the rows are asked for each time a manual is laid out");
        assertEquals(List.of(), GuideFacts.rowsOf("facts_test:unregistered", 2));
    }

    @Test
    void value_refusesAnIdRegisteredTwice() {
        GuideFacts.value("facts_test:twice", () -> new GuideBlock.GuideValue.Literal("x"));

        assertThrows(IllegalStateException.class,
                () -> GuideFacts.value("facts_test:twice", () -> new GuideBlock.GuideValue.Literal("y")));
    }

    private static GuideBlock.TableRow row(final String... cells) {
        return new GuideBlock.TableRow(List.of(cells).stream()
                .map(cell -> (GuideBlock.GuideValue) new GuideBlock.GuideValue.Literal(cell)).toList());
    }

    /** Words that are their own keys, and amounts grouped by thousands with a comma. */
    private record Words() implements IGuideText {

        @Override
        public String text(final String key, final Object... args) {
            return key;
        }

        @Override
        public int width(final String text, final TextSize size) {
            return text.length() * 6;
        }

        @Override
        public int recipeCount(final String type, final String output) {
            return 0;
        }

        @Override
        public String amount(final long value, final String unit) {
            final String number = String.format(Locale.ROOT, "%,d", value);
            return unit.isEmpty() ? number : number + " " + unit;
        }
    }
}
