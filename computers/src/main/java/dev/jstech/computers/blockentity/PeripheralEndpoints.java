/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import net.minecraft.nbt.CompoundTag;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * What one computer has on the other end of its peripheral cables, each by the packed position of the
 * endpoint: the monitors at its desk, and whatever else such a cable reaches, a card reader among them.
 *
 * <p>The set is handed out as it stands rather than copied, because linking and unlinking a peripheral
 * are made on it directly by the owner support the computer implements.
 */
final class PeripheralEndpoints {

    private final Set<Long> linked = new LinkedHashSet<>();

    /* The name these positions have had on disk since the days when only a monitor could be one. */
    private static final String LINKED = "LinkedMonitors";

    /** The endpoints as they stand: the set itself, which is where a link is made and unmade. */
    Set<Long> all() {
        return this.linked;
    }

    void save(final CompoundTag tag) {
        if (this.linked.isEmpty()) {
            return;
        }
        tag.putLongArray(LINKED, this.linked.stream().mapToLong(Long::longValue).toArray());
    }

    void load(final CompoundTag tag) {
        this.linked.clear();
        for (final long endpoint : tag.getLongArray(LINKED)) {
            this.linked.add(endpoint);
        }
    }
}
