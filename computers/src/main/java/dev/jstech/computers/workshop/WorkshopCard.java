/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.workshop;

import org.jetbrains.annotations.Nullable;

/**
 * The four personal-use cards, each the block it stands in for: the crafting table, the furnace, the enchanting table
 * and the anvil. The Workshop has a tab for each, in the order of {@link #index()}, and a card's item, its picture in
 * the Device Manager and its part in the case go by {@link #id()}.
 */
public enum WorkshopCard {

    CRAFTING_TABLE("crafting_table_card", 0),
    FURNACE("furnace_card", 1),
    ENCHANTING("enchanting_card", 2),
    ANVIL("anvil_card", 3);

    private final String id;
    private final int index;

    WorkshopCard(final String id, final int index) {
        this.id = id;
        this.index = index;
    }

    /** The card whose tab is {@code index}, or null for none. */
    @Nullable
    public static WorkshopCard byIndex(final int index) {
        for (final WorkshopCard card : values()) {
            if (card.index == index) {
                return card;
            }
        }
        return null;
    }

    /** The id of the card's item, which its pictures share. */
    public String id() {
        return id;
    }

    /** Its tab in the Workshop, from the left, and the bit it sets in a mask of the cards a computer holds. */
    public int index() {
        return index;
    }

    /** The bit this card sets in a mask of the cards a computer holds. */
    public int bit() {
        return 1 << index;
    }

    /** Whether {@code mask} holds this card. */
    public boolean in(final int mask) {
        return (mask & bit()) != 0;
    }
}
