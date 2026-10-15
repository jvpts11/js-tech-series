/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.grid;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The grids of one dimension, one of each kind but data, whose grid is the data network's own and carries the
 * identity of each network; the work that shares what each carries; and the numbers every grid of the dimension knows
 * its places by. Nothing here is saved: the cables put themselves back as they load.
 */
public final class LevelGrids {

    private final Map<GridKind, Grid> grids = new HashMap<>();
    private final Map<GridKind, Optional<IGridMover>> movers = new HashMap<>();
    private final GridPlaces places = new GridPlaces();

    /** The grid of {@code kind}, which is never data. */
    public Grid of(final GridKind kind) {
        if (kind.carriesNetwork()) {
            throw new IllegalArgumentException("the data grid is the network's own, kept with the network");
        }
        return this.grids.computeIfAbsent(kind, key -> new Grid());
    }

    /** The work that shares what the grid of {@code kind} carries, made by its policy the first time it is asked. */
    public Optional<IGridMover> mover(final GridKind kind) {
        return this.movers.computeIfAbsent(kind, key -> key.policy().mover(key));
    }

    /** The numbers the dimension's grids know their places by, the data grid's included. */
    public GridPlaces places() {
        return this.places;
    }
}
