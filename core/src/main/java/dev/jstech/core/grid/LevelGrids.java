/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.grid;

import java.util.EnumMap;
import java.util.Map;

/**
 * The grids of one dimension, one of each kind but data, whose grid is the data network's own and carries the
 * identity of each network. Nothing here is saved: the cables put themselves back as they load.
 */
public final class LevelGrids {

    private final Map<GridKind, Grid> grids = new EnumMap<>(GridKind.class);

    /** The grid of {@code kind}, which is never data. */
    public Grid of(final GridKind kind) {
        if (kind.carriesNetwork()) {
            throw new IllegalArgumentException("the data grid is the network's own, kept with the network");
        }
        return this.grids.computeIfAbsent(kind, key -> new Grid());
    }
}
