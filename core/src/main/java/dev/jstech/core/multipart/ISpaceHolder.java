/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multipart;

import net.minecraft.world.phys.Vec3;

/**
 * A block entity whose block is a space of parts: the Core's cable block, where a cable's bundle runs through the
 * middle, and the space of parts alone. A microblock is placed into either the same way.
 */
public interface ISpaceHolder {

    /** The place in a space that holds no part, or that a ray meets something else in first. */
    int NO_PART = -1;

    /** The parts standing in its space. */
    SpaceParts parts();

    /**
     * The place of the part a ray from {@code start} to {@code end} meets first, or {@link #NO_PART} when it meets
     * none, or meets something else first, as a cable's wire.
     */
    default int aimedPart(final Vec3 start, final Vec3 end) {
        final SpaceParts.Hit hit = parts().aim(parts().hostPos(), start, end);
        return hit == null ? NO_PART : hit.slot();
    }
}
