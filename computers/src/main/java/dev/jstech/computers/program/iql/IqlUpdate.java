/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.iql;

import dev.jstech.computers.workshop.UpdateAction;
import java.util.Objects;

/**
 * The {@code SET} part of an UPDATE: what the card does, and what that needs said. {@code offer} is which of the
 * enchanting table's three offers to take, counted from one, or {@link #NO_OFFER} to have them listed and nothing
 * changed; {@code name} is the name an anvil gives; {@code with} is the second item a repair or a combine takes from
 * the network, empty when a repair uses whatever mends the item. Pure logic: the items stay names until executed.
 */
public record IqlUpdate(UpdateAction action, int offer, String name, String with) {

    /** No offer was named: an ENCHANT that only lists the three. */
    public static final int NO_OFFER = 0;
    /** The offers an enchanting table makes. */
    public static final int OFFERS = 3;
    /** The longest name an anvil gives an item. */
    public static final int MAX_NAME = 50;

    public IqlUpdate {
        Objects.requireNonNull(action, "action must not be null");
        name = name == null ? "" : name;
        with = with == null ? "" : with;
    }

    /** An action that needs nothing more said: SMELT, a REPAIR with whatever mends the item. */
    public static IqlUpdate of(final UpdateAction action) {
        return new IqlUpdate(action, NO_OFFER, "", "");
    }

    /** Whether an offer was named. */
    public boolean hasOffer() {
        return offer != NO_OFFER;
    }

    /** Whether a second item was named. */
    public boolean hasWith() {
        return !with.isEmpty();
    }
}
