/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.unify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class UnifyPreviewTest {

    private static final List<String> PREFER = List.of("minecraft", "jscore");
    private static final Map<String, List<String>> TAGS = Map.of(
            "c:ingots/tin", List.of("othermod:tin_ingot", "jscore:tin_ingot", "anothermod:tin_ingot"),
            "c:dusts/iron", List.of("othermod:iron_dust", "jscore:iron_dust"),
            "c:ingots/lead", List.of("jscore:lead_ingot"));

    @Test
    void of_givesEachTagTheItemKeptAndThoseReplaced() {
        final UnifyPreview preview = preview(Map.of());

        final UnifyPreview.Tag tin = tag(preview, "c:ingots/tin");
        assertEquals("jscore:tin_ingot", tin.kept());
        assertEquals(List.of("anothermod:tin_ingot", "othermod:tin_ingot"), tin.replaced());
    }

    @Test
    void of_leavesOutATagWithNothingToReplace() {
        final UnifyPreview preview = preview(Map.of());

        assertEquals(List.of("c:dusts/iron", "c:ingots/tin"), preview.tags().stream().map(UnifyPreview.Tag::tag)
                .sorted().toList());
    }

    @Test
    void of_countsTheRecipesNamingATagsCopiesAndShowsTheFirst() {
        final UnifyPreview preview = preview(Map.of(
                "othermod:tin_dust_from_ingot", Set.of("othermod:tin_ingot"),
                "anothermod:tin_block", Set.of("anothermod:tin_ingot"),
                "othermod:iron_plate", Set.of("othermod:iron_dust", "othermod:tin_ingot")));

        final UnifyPreview.Tag tin = tag(preview, "c:ingots/tin");
        assertEquals(3, tin.recipes());
        assertEquals("anothermod:tin_block", tin.example());
        assertEquals("anothermod:tin_ingot", tin.before());
        assertEquals(1, tag(preview, "c:dusts/iron").recipes());
    }

    @Test
    void of_putsTheTagsThatChangeTheMostRecipesFirst() {
        final UnifyPreview preview = preview(Map.of(
                "a:one", Set.of("othermod:iron_dust"),
                "a:two", Set.of("othermod:iron_dust")));

        assertEquals("c:dusts/iron", preview.tags().getFirst().tag());
        assertEquals(0, preview.tags().get(1).recipes());
        assertTrue(preview.tags().get(1).example().isEmpty());
    }

    @Test
    void of_keepsWhatTheSettingsSay() {
        final UnifyPreview preview = UnifyPreview.of(true, true, PREFER, false, TAGS, UnifyPlan.of(TAGS, PREFER,
                Set.of()), Map.of());

        assertTrue(preview.on());
        assertTrue(preview.steppedAside());
        assertEquals(PREFER, preview.prefer());
        assertFalse(preview.drops());
    }

    private static UnifyPreview preview(final Map<String, Set<String>> changes) {
        return UnifyPreview.of(false, false, PREFER, true, TAGS, UnifyPlan.of(TAGS, PREFER, Set.of()), changes);
    }

    private static UnifyPreview.Tag tag(final UnifyPreview preview, final String name) {
        return preview.tags().stream().filter(tag -> tag.tag().equals(name)).findFirst().orElseThrow();
    }
}
