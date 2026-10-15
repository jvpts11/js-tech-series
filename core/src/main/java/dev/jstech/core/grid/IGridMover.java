/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.grid;

import java.util.Optional;
import net.minecraft.server.level.ServerLevel;

/**
 * The work that shares what one grid carries along its runs, in one dimension: one for each grid that carries
 * something, made by its policy, ticked after the machines have ticked.
 */
public interface IGridMover {

    /** Shares the tick's worth of what the grid carries through every run of it in {@code level}. */
    void tick(ServerLevel level);

    /** A wire came to meet a block or stopped meeting one: the runs are worked out again before the next tick. */
    void tapsChanged();

    /**
     * What the part of the grid the wire numbered {@code wire} is in did in the last tick, as an instrument reads it;
     * none for a wire of no part, or a grid that keeps no such account.
     */
    default Optional<LineSurvey> survey(final long wire) {
        return Optional.empty();
    }

    /**
     * Whether the part of the grid the wire numbered {@code wire} is in is live: something on it gave what the grid
     * carries in the last tick, so its bare wire shocks. Never, for a grid that carries no energy.
     */
    default boolean live(final long wire) {
        return false;
    }
}
