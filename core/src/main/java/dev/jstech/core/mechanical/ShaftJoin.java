/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.mechanical;

import java.util.Objects;
import net.minecraft.core.BlockPos;

/**
 * A part joined to another across a distance, as a belt or a chain joins two pulleys: the other part's place, and how
 * fast it turns for each turn of this one. Both parts say it, each of the other; a join only one of them says is none.
 *
 * @param target the other part's place
 * @param ratio  how fast it turns for each turn of this part; the same way for a belt or a chain
 */
public record ShaftJoin(BlockPos target, double ratio) {

    public ShaftJoin {
        Objects.requireNonNull(target, "target");
        if (!Double.isFinite(ratio) || ratio == 0.0) {
            throw new IllegalArgumentException("a joined part turns at a ratio other than none: " + ratio);
        }
    }
}
