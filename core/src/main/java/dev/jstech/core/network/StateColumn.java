/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.network;

import dev.jstech.core.text.TextKey;
import java.util.Objects;

/**
 * A column of a state table: the name a query reads it by, the heading a screen shows over it, what it holds, and,
 * for a number, its unit.
 *
 * @param name  the name a query reads it by, in lower case: {@code temperature}; kept in saved queries, so it never
 *              changes once released
 * @param title the heading a screen shows over it, in the player's language
 * @param kind  what it holds
 * @param unit  the unit of a number, as its symbol ({@code °C}, {@code W}); empty for a number with none and for
 *              text and flags
 */
public record StateColumn(String name, TextKey title, StateValueKind kind, String unit) {

    public StateColumn {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(unit, "unit");
        if (!name.matches("[a-z][a-z0-9_]*")) {
            throw new IllegalArgumentException("a column is named in lower case, letters, digits and _: " + name);
        }
        if (kind != StateValueKind.NUMBER && !unit.isEmpty()) {
            throw new IllegalArgumentException("only a column of numbers has a unit, and " + name + " is none");
        }
    }
}
