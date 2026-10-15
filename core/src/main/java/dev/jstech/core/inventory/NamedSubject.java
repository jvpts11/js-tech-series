/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.inventory;

import java.util.Objects;

/**
 * Something with an id and no tags, as an {@link ItemFilter} looks at it: a material in a line of gas, another mod's
 * chemical. Pure: no game types.
 *
 * @param id its id, as {@code namespace:path}
 */
public record NamedSubject(String id) implements IFilterSubject {

    public NamedSubject {
        Objects.requireNonNull(id, "id");
    }

    @Override
    public String itemId() {
        return this.id;
    }

    @Override
    public boolean hasTag(final String tag) {
        return false;
    }

    @Override
    public boolean sameAs(final IFilterSubject other) {
        return other instanceof NamedSubject named && named.id.equals(this.id);
    }
}
