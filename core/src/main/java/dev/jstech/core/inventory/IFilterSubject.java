/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.inventory;

/**
 * What an {@link ItemFilter} looks at: an item, by its id, its tags, and whether it is the same as another down to its
 * components. The game's stacks are looked at through {@link StackSubject}.
 */
public interface IFilterSubject {

    /** The item's id: {@code minecraft:iron_ingot}. */
    String itemId();

    /** Whether the item carries the tag {@code tag}: {@code minecraft:logs}. */
    boolean hasTag(String tag);

    /** Whether this is the same item as {@code other}, with the same components (its damage among them). */
    boolean sameAs(IFilterSubject other);
}
