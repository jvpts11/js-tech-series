/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.vehicle;

/**
 * What whoever drives a vehicle asks of it this tick: ahead or back, to one side or the other, up or down. A player's
 * keys give it on their game; a robot, or a program, gives it on the server.
 *
 * @param forward how hard ahead, from -1 (full back) to 1 (full ahead)
 * @param strafe  how hard to turn, from -1 (full right) to 1 (full left), as the player's own keys go
 * @param up      whether to climb, for a vehicle that flies, or to jump, for one that does not
 * @param down    whether to sink, for a vehicle that flies
 */
public record DriverInput(float forward, float strafe, boolean up, boolean down) {

    /** Nothing asked: the vehicle coasts to a stop. */
    public static final DriverInput NONE = new DriverInput(0.0F, 0.0F, false, false);

    public DriverInput {
        forward = Math.max(-1.0F, Math.min(1.0F, forward));
        strafe = Math.max(-1.0F, Math.min(1.0F, strafe));
    }

    /** Whether anything is asked at all. */
    public boolean moving() {
        return forward != 0.0F || strafe != 0.0F || up || down;
    }
}
