/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.cable;

import dev.jstech.core.id.IStableId;
import dev.jstech.core.id.IStableName;
import dev.jstech.core.id.StableIds;
import dev.jstech.core.id.StableNames;
import org.jetbrains.annotations.Nullable;

/**
 * Where a wire runs through a cable block when it shares the block: one of nine lanes, three across by three up. A
 * line has one lane of its own, the same in every block and every era of it, so its wires line up from block to
 * block and two lines never take one place.
 *
 * <p>Across and up are the world's axes, the same for every run along one axis: along x, across is z and up is y;
 * along z, across is x and up is y; up or down, across is x and up is z. Lanes sit {@link #PITCH} pixels apart, so
 * four-pixel wires side by side keep a pixel between them.
 *
 * <p>Each lane is numbered from 0 to 8 and named, the number and name being what a block keeps its wires by.
 */
public enum Lane implements IStableId, IStableName {

    TOP_LEFT(0, "top_left", -1, 1),
    TOP(1, "top", 0, 1),
    TOP_RIGHT(2, "top_right", 1, 1),
    LEFT(3, "left", -1, 0),
    MIDDLE(4, "middle", 0, 0),
    RIGHT(5, "right", 1, 0),
    BOTTOM_LEFT(6, "bottom_left", -1, -1),
    BOTTOM(7, "bottom", 0, -1),
    BOTTOM_RIGHT(8, "bottom_right", 1, -1);

    private final int id;
    private final String serializedName;
    private final int across;
    private final int up;

    /** Pixels from one lane to the next. */
    public static final int PITCH = 5;
    /** How many lanes a block has; the lanes are numbered from 0 to one less. */
    public static final int COUNT = 9;
    private static final StableIds<Lane> IDS = StableIds.of(Lane.class);
    private static final StableNames<Lane> NAMES = StableNames.of(Lane.class);

    Lane(final int id, final String serializedName, final int across, final int up) {
        this.id = id;
        this.serializedName = serializedName;
        this.across = across;
        this.up = up;
    }

    /** The lane numbered {@code id}, or null when no lane is. */
    public static @Nullable Lane byId(final int id) {
        return IDS.find(id);
    }

    /** The lane named {@code name}, or null when no lane is. */
    public static @Nullable Lane byName(final @Nullable String name) {
        return NAMES.find(name);
    }

    @Override
    public int id() {
        return this.id;
    }

    @Override
    public String serializedName() {
        return this.serializedName;
    }

    /** Which column the lane is in: -1, 0 or 1. */
    public int across() {
        return this.across;
    }

    /** Which row the lane is in: -1, 0 or 1. */
    public int up() {
        return this.up;
    }
}
