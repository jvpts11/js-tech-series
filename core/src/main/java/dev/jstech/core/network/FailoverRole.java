/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.network;

import dev.jstech.core.id.IStableId;
import dev.jstech.core.id.StableIds;

/**
 * The role a Mainframe plays in a Failover redundancy pair.
 */
public enum FailoverRole implements IStableId {

    NONE(0),

    ACTIVE(1),

    PASSIVE(2);

    private static final StableIds<FailoverRole> IDS = StableIds.of(FailoverRole.class);

    private final int id;

    FailoverRole(final int id) {
        this.id = id;
    }

    @Override
    public int id() {
        return id;
    }

    public boolean isPaired() {
        return this == ACTIVE || this == PASSIVE;
    }

    /** The role that declares {@code id}; an id no role declares reads as {@link #NONE}. */
    public static FailoverRole byId(final int id) {
        return IDS.byId(id, NONE);
    }
}
