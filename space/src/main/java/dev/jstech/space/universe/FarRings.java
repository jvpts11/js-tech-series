/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.universe;

import java.util.List;

/**
 * The rings the far map of the way up comes in ({@link FarMaps}): each as many pixels each way, each four times the
 * last one's reach with pixels eight times as wide, the finest for the ground just past what is loaded, the widest out
 * to where the climb's horizon lies ({@link ClimbView#REACH}). Pure: no game types.
 */
public final class FarRings {

    /** How many pixels a ring has each way. */
    public static final int SIZE = 512;
    /** How many blocks wide a pixel of each ring is, the finest first. */
    public static final List<Integer> BLOCKS = List.of(16, 128);

    private FarRings() {
    }

    /** How far the widest ring reaches from its middle, in blocks. */
    public static double reach() {
        return SIZE * BLOCKS.getLast() / 2.0;
    }
}
