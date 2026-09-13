/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.tier;

import dev.jstech.core.id.IStableId;
import dev.jstech.core.id.StableIds;

/**
 * Industrial Tier (T0-T9): the central cross-module progression axis of J's Computers. Each tier declares its
 * level, counted from 0 without gaps, and that level is also its stable id.
 */
public enum IndustrialTier implements IStableId {
    T0(0), T1(1), T2(2), T3(3), T4(4), T5(5), T6(6), T7(7), T8(8), T9(9);

    private static final StableIds<IndustrialTier> IDS = StableIds.of(IndustrialTier.class);

    private final int level;

    IndustrialTier(final int level) {
        this.level = level;
    }

    public IndustrialTier next() {
        return this == T9 ? T9 : IDS.byId(level + 1, T9);
    }

    public IndustrialTier prev() {
        return this == T0 ? T0 : IDS.byId(level - 1, T0);
    }

    public int level() {
        return level;
    }

    @Override
    public int id() {
        return level;
    }

    public boolean isAtLeast(IndustrialTier other) {
        return this.level() >= other.level();
    }

    public boolean isAtMost(IndustrialTier other) {
        return this.level() <= other.level();
    }
}
