/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.conveyor;

/**
 * What a conveyor does with an item that reaches its end with nowhere to go: no belt and no inventory in front of it.
 */
public enum ConveyorEnd {
    /** It holds it at the end, and the items behind it wait. */
    HOLD,
    /** It lets it fall off the end into the world, as an item lying there. */
    DROP
}
