/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.grid;

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
}
