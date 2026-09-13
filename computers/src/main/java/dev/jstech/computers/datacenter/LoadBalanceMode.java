/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.datacenter;

import dev.jstech.core.id.IStableId;
import dev.jstech.core.id.StableIds;

/**
 * How a Server Router spreads Operations across the Servers of one output-face section.
 */
public enum LoadBalanceMode implements IStableId {
    ROUND_ROBIN(0, "ROUND-ROBIN"),
    LEAST_LOADED(1, "LEAST-LOADED"),
    MANUAL(2, "MANUAL");

    private static final StableIds<LoadBalanceMode> IDS = StableIds.of(LoadBalanceMode.class);

    private final int id;
    private final String label;

    LoadBalanceMode(final int id, final String label) {
        this.id = id;
        this.label = label;
    }

    @Override
    public int id() {
        return id;
    }

    /** What the router's screen and the Cluster Manager call the mode. */
    public String label() {
        return label;
    }

    /** The mode the router's button moves on to from this one. */
    public LoadBalanceMode next() {
        return switch (this) {
            case ROUND_ROBIN -> LEAST_LOADED;
            case LEAST_LOADED -> MANUAL;
            case MANUAL -> ROUND_ROBIN;
        };
    }

    /** The mode that declares {@code id}; an id no mode declares reads as {@link #ROUND_ROBIN}, a new section's mode. */
    public static LoadBalanceMode byId(final int id) {
        return IDS.byId(id, ROUND_ROBIN);
    }
}
