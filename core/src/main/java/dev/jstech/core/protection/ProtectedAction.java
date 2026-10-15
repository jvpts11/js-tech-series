/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.protection;

import java.util.Locale;

/** What an actor would do to a place, which claims and protections answer for. */
public enum ProtectedAction {

    /** Break a block. */
    BREAK,
    /** Place a block. */
    PLACE,
    /** Use a block or open what is there: a door, a chest, a machine. */
    USE,
    /** Hurt an entity standing there. */
    HURT,
    /** Blow it up. */
    EXPLODE,
    /** Set it on fire, or let fire spread into it. */
    BURN,
    /** Let a fluid flow into it. */
    FLOW,
    /** Move into it, or push what is there: a moving structure. */
    MOVE,
    /** Hit what is there with something shot. */
    SHOOT;

    /** Whether the action changes the blocks of the place, as against using them or hurting what stands there. */
    public boolean changesBlocks() {
        return this != USE && this != HURT;
    }

    /** Its name in commands and logs: {@code break}, {@code place}. */
    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
