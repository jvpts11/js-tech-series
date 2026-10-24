/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock;

import java.util.Map;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;

/**
 * A multiblock's shape, drawn as a pattern or given by a rule (a box, a floor and walls, a cuboid), checked against
 * the world from its terminal. Pure: the world is read through an {@link IBlockProvider}.
 */
public interface IStructureShape {

    /** Checks the world with the terminal at {@code terminal}: formed, with what it gives, or the first wrong. */
    ShapeMatch check(IBlockProvider provider, long terminal);

    /**
     * The farthest from its terminal, along any axis, that checking it reads the world, the places beside it
     * counted: what has to be loaded for it to be checked, and what its terminal watches.
     */
    int reach();

    /** The least and the most of each count, by name. */
    Map<String, Limit> limits();

    /**
     * What fits the place a failure names as {@code wanted} (a pattern's letter, or a part of a shape such as
     * {@link ShapeMatch#FRAME}), to tell the player; none for what the shape does not know.
     */
    @Nullable
    IBlockMatcher wanted(String wanted);

    /** Whether it is also found mirrored, besides its four turns; a shape a rule gives is the same either way. */
    default boolean mirrors() {
        return false;
    }

    /** Whether it is also found lying on its side. */
    default boolean liesDown() {
        return false;
    }

    /**
     * The shape laid out as it stands facing north, to be shown before it is built: at its least size, grown by
     * {@code grow} (more repeats of a pattern's slice, longer sides of a box), or none for a shape that cannot be
     * shown.
     */
    default Optional<ShapePreview> preview(final int grow) {
        return Optional.empty();
    }
}
