/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio;

/**
 * How much of a sound gets through the walls between it and the listener: every solid block on the way takes a share
 * of it, and a sound behind many walls is faint but never quite gone, the way a machine next door still hums.
 */
public final class Occlusion {

    /** How much of a sound each solid block on the way lets through. */
    private static final float THROUGH_ONE_BLOCK = 0.6F;
    /** The least that gets through, however many blocks are in the way. */
    private static final float FLOOR = 0.15F;

    private Occlusion() {
    }

    /** The share of a sound that gets through {@code solidBlocks} solid blocks, from 1 for none. */
    public static float factor(final int solidBlocks) {
        if (solidBlocks <= 0) {
            return 1.0F;
        }
        return Math.max(FLOOR, (float) Math.pow(THROUGH_ONE_BLOCK, solidBlocks));
    }
}
