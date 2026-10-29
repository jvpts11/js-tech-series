/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock.shape;

import org.jetbrains.annotations.Nullable;

/**
 * The faces of the blocks of formed models, where the models are known: on a player's game, which draws them. A
 * server has no models, and aims and outlines by the shape files alone.
 */
public interface IFormedMeshes {

    /**
     * The faces of the block {@code place} in its own space, as the model stands facing north, nine numbers to a
     * triangle; none while its model is not known.
     */
    @Nullable
    float[] triangles(FormedPlace place, FormedShape shape);

    /** The lines of its outline in its own space, six numbers to a line; none while its model is not known. */
    @Nullable
    float[] lines(FormedPlace place, FormedShape shape);
}
