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

import java.util.List;
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
    void mod_namesEverythingInItsNamespace() {
        final ItemFilter filter = ItemFilter.only(new ItemFilter.Mod("minecraft", 0));
        final Thing modded = new Thing("jscore:basic_wrench", Set.of(), "");

        assertTrue(filter.allows(IRON));
        assertFalse(filter.allows(modded));
        assertTrue(ItemFilter.only(new ItemFilter.Mod("jscore", 0)).allows(modded));
        assertTrue(ItemFilter.only(new ItemFilter.Mod("minecraft", 0)).allows(new NamedSubject("stone")),
                "an id with no namespace is the game's own");
        assertThrows(IllegalArgumentException.class, () -> new ItemFilter.Mod("Not A Mod", 0));
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
    void withRule_replacesARuleOrAddsOnePastTheLast() {
        final ItemFilter filter = ItemFilter.only(new ItemFilter.Fuzzy("minecraft:iron_ingot", 5));

        final ItemFilter replaced = filter.withRule(0, new ItemFilter.Tag("minecraft:logs", 0));
        final ItemFilter added = filter.withRule(4, new ItemFilter.Tag("minecraft:logs", 0));

        assertEquals(1, replaced.rules().size());
        assertTrue(replaced.allows(OAK));
        assertFalse(replaced.allows(IRON));
        assertEquals(2, added.rules().size(), "a slot past the last adds the rule at the end");
        assertThrows(IndexOutOfBoundsException.class, () -> filter.withRule(-1, new ItemFilter.Mod("jscore", 0)));
    }

    @Test
    void without_dropsTheRuleAtItsPlace() {
        final ItemFilter filter = ItemFilter.only(new ItemFilter.Fuzzy("minecraft:iron_ingot", 0),
                new ItemFilter.Tag("minecraft:logs", 0));

        assertEquals(List.of(new ItemFilter.Tag("minecraft:logs", 0)), filter.without(0).rules());
        assertEquals(filter, filter.without(7), "a slot with no rule leaves the filter as it is");
    }

    @Test
    void withMode_keepsTheRules() {
        final ItemFilter filter = ItemFilter.only(new ItemFilter.Fuzzy("minecraft:iron_ingot", 0));

        final ItemFilter turned = filter.withMode(ItemFilter.Mode.ALL_BUT);

        assertEquals(filter.rules(), turned.rules());
        assertFalse(turned.allows(IRON));
        assertTrue(turned.allows(OAK));
    }

    @Test
    void withAmount_keepsWhatEachKindOfRuleNames() {
        assertEquals(new ItemFilter.Fuzzy("minecraft:stone", 9), new ItemFilter.Fuzzy("minecraft:stone", 0)
                .withAmount(9));
        assertEquals(new ItemFilter.Tag("minecraft:logs", 9), new ItemFilter.Tag("minecraft:logs", 1).withAmount(9));
        assertEquals(new ItemFilter.Mod("jscore", 9), new ItemFilter.Mod("jscore", 2).withAmount(9));
        assertEquals(new ItemFilter.Exact(SWORD, 9), new ItemFilter.Exact(SWORD, 3).withAmount(9));
        assertThrows(IllegalArgumentException.class, () -> new ItemFilter.Mod("jscore", 0).withAmount(-1));
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

    @Test
    void fuzzy_rejectsAnIdTheGameCannotRead() {
        assertThrows(IllegalArgumentException.class, () -> new ItemFilter.Fuzzy("Iron Sword", 0));
    }

    @Test
    void tag_rejectsAnIdTheGameCannotRead() {
        assertThrows(IllegalArgumentException.class, () -> new ItemFilter.Tag("minecraft:Logs", 0));
        assertThrows(IllegalArgumentException.class, () -> new ItemFilter.Tag("", 0));
    }

    @Test
    void fuzzy_acceptsNamespacedAndBareIds() {
        assertEquals("minecraft:iron_sword", new ItemFilter.Fuzzy("minecraft:iron_sword", 0).item());
        assertEquals("c:ingots/iron", new ItemFilter.Tag("c:ingots/iron", 0).tag());
        assertEquals("stick", new ItemFilter.Fuzzy("stick", 0).item());
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
