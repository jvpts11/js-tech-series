/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock;

/**
 * A structure's terminal as the level's index of structures knows it ({@link LevelStructures}): told when a place it
 * watches changes, so it checks itself again then and never every tick, and asked whether its formed structure is
 * made of a place, for whoever guards what it is made of.
 */
public interface IStructureWatcher {

    /** A place it watches changed: it checks itself again soon. */
    void changed();

    /** Whether its structure is formed and made of the place {@code pos}, packed as one number. */
    boolean holds(long pos);
}
