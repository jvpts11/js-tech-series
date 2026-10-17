/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * How contaminated a place is for the living things that grow there, as a mod's hazard makes it: radiation by its dose
 * rate, a pollution by how much lies on the chunk.
 */
@FunctionalInterface
public interface IContamination {

    /**
     * How contaminated {@code pos} of {@code level} is, as a share of what plants bear: from 1 grass dies and plants
     * wither, below a half what died comes back. Asked of loaded places only, a few at a time, on the server's thread.
     */
    double at(ServerLevel level, BlockPos pos);
}
