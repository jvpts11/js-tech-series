/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import dev.jstech.core.text.Text;
import org.jetbrains.annotations.Nullable;

/**
 * What came of making, setting down or sending a moving structure: the structure, or why it could not be done, said
 * so a player can read it.
 *
 * @param structure the structure made or kept, or null when it was refused
 * @param refusal   why it was refused, or null when it was done
 * @param blocks    how many blocks it moved
 */
public record StructureResult(@Nullable MovingStructure structure, @Nullable Text refusal, int blocks) {

    /** It was done. */
    public static StructureResult done(@Nullable final MovingStructure structure, final int blocks) {
        return new StructureResult(structure, null, blocks);
    }

    /** It was refused, for {@code why}. */
    public static StructureResult refused(final Text why) {
        return new StructureResult(null, why, 0);
    }

    /** Whether it was done. */
    public boolean ok() {
        return refusal == null;
    }
}
