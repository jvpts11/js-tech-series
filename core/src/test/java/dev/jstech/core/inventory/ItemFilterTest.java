/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

class ItemFilterTest {

    private static final Thing IRON = new Thing("minecraft:iron_ingot", Set.of("c:ingots"), "");
    private static final Thing SWORD = new Thing("minecraft:iron_sword", Set.of("minecraft:swords"), "");
    private static final Thing WORN_SWORD = new Thing("minecraft:iron_sword", Set.of("minecraft:swords"), "damage=40");
    private static final Thing OAK = new Thing("minecraft:oak_log", Set.of("minecraft:logs"), "");

    @Test
    void allows_everythingWhenNoRuleIsSet() {
        assertTrue(ItemFilter.EVERYTHING.allows(IRON));
        assertTrue(ItemFilter.only().allows(IRON), "an empty list filters nothing, whichever its mode");
    }

    @Test
    void allows_onlyWhatAnOnlyFilterNames() {
        final ItemFilter filter = ItemFilter.only(new ItemFilter.Fuzzy("minecraft:iron_ingot", 0));

        assertTrue(filter.allows(IRON));
        assertFalse(filter.allows(OAK));
    }

    @Test
    void allows_everythingButWhatAnAllButFilterNames() {
        final ItemFilter filter = ItemFilter.allBut(new ItemFilter.Fuzzy("minecraft:iron_ingot", 0));

        assertFalse(filter.allows(IRON));
        assertTrue(filter.allows(OAK));
    }

    @Test
    void exact_tellsAWornItemFromANewOne() {
        final ItemFilter filter = ItemFilter.only(new ItemFilter.Exact(SWORD, 0));

        assertTrue(filter.allows(SWORD));
        assertFalse(filter.allows(WORN_SWORD));
    }

    @Test
    void fuzzy_ignoresDamageAndComponents() {
        final ItemFilter filter = ItemFilter.only(new ItemFilter.Fuzzy("minecraft:iron_sword", 0));

        assertTrue(filter.allows(SWORD));
        assertTrue(filter.allows(WORN_SWORD));
    }

    @Test
    void tag_namesEveryItemOfTheTag() {
        final ItemFilter filter = ItemFilter.only(new ItemFilter.Tag("minecraft:logs", 0));

        assertTrue(filter.allows(OAK));
        assertFalse(filter.allows(IRON));
    }

    @Test
    void amountFor_isTheFirstMatchingRulesAmount() {
        final ItemFilter filter = ItemFilter.only(new ItemFilter.Exact(SWORD, 3),
                new ItemFilter.Fuzzy("minecraft:iron_sword", 8), new ItemFilter.Tag("c:ingots", 64));

        assertEquals(3, filter.amountFor(SWORD), "the exact rule comes first for a new sword");
        assertEquals(8, filter.amountFor(WORN_SWORD), "and the fuzzy one for a worn one");
        assertEquals(64, filter.amountFor(IRON));
        assertEquals(0, filter.amountFor(OAK), "nothing names oak");
    }

    @Test
    void with_addsARuleAtTheEnd() {
        final ItemFilter filter = ItemFilter.only().with(new ItemFilter.Tag("minecraft:logs", 0));

        assertEquals(1, filter.rules().size());
        assertTrue(filter.allows(OAK));
    }

    @Test
    void rules_refuseANegativeAmount() {
        assertThrows(IllegalArgumentException.class, () -> new ItemFilter.Fuzzy("minecraft:stone", -1));
    }

    @Test
    void mode_hasStableNames() {
        assertEquals("only", ItemFilter.Mode.ONLY.serializedName());
        assertEquals("all_but", ItemFilter.Mode.ALL_BUT.serializedName());
    }

    /** An item for the tests: its id, its tags, and its components written out. */
    private record Thing(String itemId, Set<String> tags, String components) implements IFilterSubject {

        @Override
        public boolean hasTag(final String tag) {
            return this.tags.contains(tag);
        }

        @Override
        public boolean sameAs(final IFilterSubject other) {
            return other instanceof Thing thing && thing.itemId.equals(this.itemId)
                    && thing.components.equals(this.components);
        }
    }
}
