/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.fire;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;

/* What a thing hung under a ceiling needs: a block above whose underside holds it. */
final class Ceilings {

    private Ceilings() {
    }

    /* Whether the block above {@code pos} can hold up what hangs under it. */
    static boolean holds(final LevelReader level, final BlockPos pos) {
        final BlockPos above = pos.above();
        return level.getBlockState(above).isFaceSturdy(level, above, Direction.DOWN);
    }
}
