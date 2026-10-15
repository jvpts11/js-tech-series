/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock;

/**
 * A multiblock's shape, drawn as a pattern or given by a rule (a box, a floor and walls, a cuboid), checked against
 * the world from its controller. Pure: the world is read through an {@link IBlockProvider}.
 */
public interface IStructureShape {

    /** Checks the world with the controller at {@code controller}: formed, with what it gives, or the first wrong. */
    ShapeMatch check(IBlockProvider provider, long controller);
}
