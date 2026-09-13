/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.uuid;

import dev.jstech.core.id.IStableId;
import dev.jstech.core.id.StableIds;

/**
 * Lifecycle state of a network UUID.
 */
public enum NetworkUuidState implements IStableId {

    ACTIVE(0),

    ORPHANED(1),

    CONFLICTED(2);

    private static final StableIds<NetworkUuidState> IDS = StableIds.of(NetworkUuidState.class);

    private final int id;

    NetworkUuidState(final int id) {
        this.id = id;
    }

    @Override
    public int id() {
        return id;
    }

    public boolean isOperational() {
        return this == ACTIVE;
    }

    public boolean isRecoverable() {
        return this == ORPHANED || this == CONFLICTED;
    }

    /** The state that declares {@code id}; an id no state declares reads as {@link #ACTIVE}. */
    public static NetworkUuidState byId(final int id) {
        return IDS.byId(id, ACTIVE);
    }
}
