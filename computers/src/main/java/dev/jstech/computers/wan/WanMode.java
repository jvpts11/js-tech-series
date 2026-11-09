/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.wan;

import dev.jstech.core.id.IStableId;
import dev.jstech.core.id.StableIds;

/**
 * What a line that is up does between the two places it joins (computers/docs/design/network.md). Sent by its id.
 */
public enum WanMode implements IStableId {
    /** The line is down, or its port has no card: it joins nothing. */
    NONE(0),
    /** One place has no Mainframe of its own: it is part of the other's network, a branch office. */
    EXTENSION(1),
    /** Each place has a Mainframe: two networks linked, never merged. */
    FEDERATION(2);

    private final int id;

    private static final StableIds<WanMode> IDS = StableIds.of(WanMode.class);

    WanMode(final int id) {
        this.id = id;
    }

    /** The mode that declares {@code id}; an id no mode declares reads as NONE. */
    public static WanMode byId(final int id) {
        return IDS.byId(id, NONE);
    }

    @Override
    public int id() {
        return this.id;
    }
}
