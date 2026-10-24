/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock;

import java.util.Optional;
import net.minecraft.core.BlockPos;

/**
 * The block entity of a structure's terminal, the block the player works the multiblock from, as what stands round it
 * and what draws it ask: whether a place is one of its structure's, and how its structure stands once formed, on the
 * server and on the players' games alike.
 */
public interface IStructureTerminal {

    /** Whether its formed structure holds the place {@code pos}, as saved, before it is checked again. */
    boolean holdsPlace(BlockPos pos);

    /** How its structure stands, once formed; none while it is not. */
    Optional<Orientation> formedOrientation();

    /** Whether its formed structure's blocks have merged into its model. */
    boolean merged();
}
