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
import dev.jstech.core.progression.IAxisStep;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * Industrial Tier (T0-T9): the central cross-module progression axis of J's Computers. Each tier declares its
 * level, counted from 0 without gaps, and that level is also its stable id.
 */
@TextHolder
public enum IndustrialTier implements IStableId, IAxisStep {
    T0(0), T1(1), T2(2), T3(3), T4(4), T5(5), T6(6), T7(7), T8(8), T9(9);

    private final int level;

    private static final StableIds<IndustrialTier> IDS = StableIds.of(IndustrialTier.class);
    /** A tier as a player reads it: "T3", the number put where each language puts it. */
    private static final TextKey NAMED = TextKey.of("jscore.tier.named", "T%s");

    IndustrialTier(final int level) {
        this.level = level;
    }

    public IndustrialTier next() {
        return this == T9 ? T9 : IDS.byId(level + 1, T9);
    }

    public IndustrialTier prev() {
        return this == T0 ? T0 : IDS.byId(level - 1, T0);
    }

    @Override
    public int level() {
        return level;
    }

    @Override
    public int id() {
        return level;
    }

    /** The name the tier is written by in data files: {@code t3}. */
    @Override
    public String serializedName() {
        return "t" + level;
    }

    /** The tier as a player reads it: "T3". */
    @Override
    public Text text() {
        return NAMED.with(level);
    }

    public boolean isAtLeast(IndustrialTier other) {
        return this.level() >= other.level();
    }

    public boolean isAtMost(IndustrialTier other) {
        return this.level() <= other.level();
    }
}
