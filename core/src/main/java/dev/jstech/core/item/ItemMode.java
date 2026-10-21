/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.item;

import dev.jstech.core.text.TextKey;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * One of the modes an item switches between with the mode key: the id it is saved by in the item, and what a player
 * calls it. An item's modes are declared with it, in the order the key goes through them.
 *
 * @param id   what the item keeps to know its mode: lower case letters, digits and underscores
 * @param name what a player calls it, from a {@code @TextHolder} class of the mod
 */
public record ItemMode(String id, TextKey name) {

    /** The longest id a mode has: what the ring of modes sends back when the player chooses one. */
    public static final int LONGEST_ID = 64;
    private static final Pattern ID = Pattern.compile("[a-z0-9_]+");

    public ItemMode {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        if (!ID.matcher(id).matches() || id.length() > LONGEST_ID) {
            throw new IllegalArgumentException("a mode's id is up to " + LONGEST_ID + " lower case letters, digits "
                    + "and underscores, not " + id);
        }
    }

    /** The mode saved as {@code id}, called {@code name}. */
    public static ItemMode of(final String id, final TextKey name) {
        return new ItemMode(id, name);
    }
}
