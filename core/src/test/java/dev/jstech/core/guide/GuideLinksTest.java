/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class GuideLinksTest {

    private static final Function<String, String> NUMBERS = Map.of("mod:press", "1.2.3", "mod", "1")::get;

    @Test
    void runs_splitASentenceIntoWordsAndLinks() {
        assertEquals(List.of(new GuideLinks.Run("see ", ""), new GuideLinks.Run("the press (1.2.3)", "mod:press"),
                        new GuideLinks.Run(" first", "")),
                GuideLinks.runs("see [the press](mod:press) first", NUMBERS));
    }

    @Test
    void runs_writeALinkWithNoWordsAsItsNumber() {
        assertEquals(List.of(new GuideLinks.Run("(", ""), new GuideLinks.Run("1.2.3", "mod:press"),
                new GuideLinks.Run(")", "")), GuideLinks.runs("([](mod:press))", NUMBERS));
    }

    @Test
    void runs_leadToAChapterByItsNamespace() {
        assertEquals(List.of(new GuideLinks.Run("the mod (1)", "mod")), GuideLinks.runs("[the mod](mod)", NUMBERS));
    }

    @Test
    void runs_writeATargetTheManualDoesNotHoldAsPlainWords() {
        assertEquals(List.of(new GuideLinks.Run("the mill", "")), GuideLinks.runs("[the mill](mod:mill)", NUMBERS));
        assertEquals(List.of(new GuideLinks.Run("mill", "")), GuideLinks.runs("[](mod:mill)", NUMBERS));
    }

    @Test
    void runs_leaveBracketsThatAreNoLinkAsTheyAre() {
        assertEquals(List.of(new GuideLinks.Run("hold [M] to open it", "")),
                GuideLinks.runs("hold [M] to open it", NUMBERS));
    }

    @Test
    void plain_writesEveryLinkAsItsWordsAndNumber() {
        assertEquals("see the press (1.2.3), or 1.2.3",
                GuideLinks.plain("see [the press](mod:press), or [](mod:press)", NUMBERS));
    }

    @Test
    void targets_listWhatTheLinksLeadToInOrder() {
        assertEquals(List.of("mod:press", "mod:mill", "mod"),
                GuideLinks.targets("[a](mod:press) [](mod:mill) and [b](mod)"));
    }
}
