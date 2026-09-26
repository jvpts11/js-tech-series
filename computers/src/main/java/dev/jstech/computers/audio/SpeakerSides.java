/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.audio;

import dev.jstech.core.audio.StereoSide;

/**
 * Which side of a stereo recording each of a computer's speakers plays, worked out from where it stands against the
 * monitor, the way a person sitting at the screen would set a pair of them down: the one on their left plays the left.
 * A speaker alone plays both sides, as does one with no monitor to stand beside or one standing square in front of or
 * behind it.
 */
public final class SpeakerSides {

    private SpeakerSides() {
    }

    /**
     * The side a speaker plays.
     *
     * @param speakers how many speakers the computer has
     * @param leftX    the east-west step toward the left of someone facing the monitor's screen
     * @param leftZ    the north-south step toward that left
     * @param dx       how far east of the monitor the speaker stands
     * @param dz       how far south of the monitor the speaker stands
     */
    public static StereoSide sideOf(final int speakers, final int leftX, final int leftZ, final int dx, final int dz) {
        if (speakers < 2) {
            return StereoSide.BOTH;
        }
        final long along = (long) dx * leftX + (long) dz * leftZ;
        if (along > 0) {
            return StereoSide.LEFT;
        }
        return along < 0 ? StereoSide.RIGHT : StereoSide.BOTH;
    }
}
