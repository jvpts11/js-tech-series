/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio;

import dev.jstech.core.id.IStableId;
import dev.jstech.core.id.StableIds;

/**
 * Which side of a stereo recording a speaker plays: both, mixed down to the one sound a place in the world makes, or
 * only the left or the right, so that two speakers set apart play a recording as it was made.
 */
public enum StereoSide implements IStableId {

    BOTH(0),

    LEFT(1),

    RIGHT(2);

    private static final StableIds<StereoSide> IDS = StableIds.of(StereoSide.class);

    private final int id;

    StereoSide(final int id) {
        this.id = id;
    }

    @Override
    public int id() {
        return id;
    }

    /** The side with that number; a number no side has reads as both, which every speaker can play. */
    public static StereoSide byId(final int id) {
        return IDS.byId(id, BOTH);
    }
}
