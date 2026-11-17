/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.hangar;

/**
 * A personnel door of the Rocket Hangar swinging, as the approved storyboard draws it: its leaf turns a quarter turn
 * about its hinge into the room, opening or shutting, easing out of rest and into it, in under a second. Pure, so it
 * is tested without the game.
 */
public final class PersonnelDoorSwing {

    /** How long a swing takes, in ticks. */
    public static final int TICKS = 15;
    /** How far its leaf stands turned open, in degrees. */
    public static final double OPEN = 90;

    private PersonnelDoorSwing() {
    }

    /**
     * How far the leaf stands turned from shut, in degrees, {@code ticks} after it began to swing open
     * ({@code opening}) or shut; at rest open or shut long after.
     */
    public static double angle(final boolean opening, final double ticks) {
        final double share = Math.clamp(ticks / TICKS, 0, 1);
        // Out of rest slowly, fastest half way, into rest slowly.
        final double eased = share * share * (3 - 2 * share);
        return OPEN * (opening ? eased : 1 - eased);
    }
}
