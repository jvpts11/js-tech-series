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
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * How a Server Router spreads Operations across the Servers of one output-face section.
 */
@TextHolder
public enum LoadBalanceMode implements IStableId {
    ROUND_ROBIN(0, TextKey.of("jsc.load_balance.round_robin", "ROUND-ROBIN")),
    LEAST_LOADED(1, TextKey.of("jsc.load_balance.least_loaded", "LEAST-LOADED")),
    MANUAL(2, TextKey.of("jsc.load_balance.manual", "MANUAL"));

    private final int id;
    private final TextKey label;

    private static final StableIds<LoadBalanceMode> IDS = StableIds.of(LoadBalanceMode.class);

    LoadBalanceMode(final int id, final TextKey label) {
        this.id = id;
        this.label = label;
    }

    @Override
    public int id() {
        return id;
    }

    /** What the router's screen and the Cluster Manager call the mode, in English. */
    public String label() {
        return label.english();
    }

    /** The same, as text for a player to read in their language. */
    public Text text() {
        return label.text();
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
