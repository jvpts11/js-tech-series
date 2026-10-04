/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.workshop;

import java.util.Locale;
import java.util.Optional;

/**
 * What an UPDATE asks a personal-use card to do to an item the network holds, each through the card that does it:
 * smelting through the Furnace Card, enchanting through the Enchanting Card, and repairing, combining with a second
 * item and naming through the Anvil Card. The Crafting Table Card has none: a grid filled from the network is what a
 * CRAFT is for.
 *
 * <p>The word each is written with in the network's language is its name.
 */
public enum UpdateAction {

    SMELT(1, WorkshopCard.FURNACE),
    ENCHANT(2, WorkshopCard.ENCHANTING),
    REPAIR(3, WorkshopCard.ANVIL),
    COMBINE(4, WorkshopCard.ANVIL),
    NAME(5, WorkshopCard.ANVIL);

    private final int id;
    private final WorkshopCard card;

    UpdateAction(final int id, final WorkshopCard card) {
        this.id = id;
        this.card = card;
    }

    /** The action numbered {@code id}, as a packet carries it, or empty for none. */
    public static Optional<UpdateAction> byId(final int id) {
        for (final UpdateAction action : values()) {
            if (action.id == id) {
                return Optional.of(action);
            }
        }
        return Optional.empty();
    }

    /** The number a packet carries it as. */
    public int id() {
        return id;
    }

    /** The action written {@code keyword}, in any case. */
    public static Optional<UpdateAction> fromKeyword(final String keyword) {
        if (keyword == null) {
            return Optional.empty();
        }
        final String wanted = keyword.trim().toUpperCase(Locale.ROOT);
        for (final UpdateAction action : values()) {
            if (action.name().equals(wanted)) {
                return Optional.of(action);
            }
        }
        return Optional.empty();
    }

    /** The card that does it. */
    public WorkshopCard card() {
        return card;
    }

    /** Whether it costs whoever asked experience, which only smelting does not. */
    public boolean paid() {
        return this != SMELT;
    }

    /** Whether it goes through the anvil. */
    public boolean anvil() {
        return card == WorkshopCard.ANVIL;
    }

    /** Whether it takes a second item from the network: a repair's material, or what a combine adds. */
    public boolean takesSecond() {
        return this == REPAIR || this == COMBINE;
    }
}
