/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.shipyard;

import static dev.jstech.space.SpaceContent.CONTENT;

import dev.jstech.core.audio.SoundKey;

/**
 * How the Small Shipyard sounds at work: its robot arms' servos as they set off for a block and as they swing it
 * over, a burst of welding as each block is set down on the ship, and the bay's floor rising and going down.
 */
public final class ShipyardSounds {

    /** A robot arm on the move, its servo in six takes. */
    public static final SoundKey ARM = CONTENT.sound("shipyard/arm").variants(6)
            .range(16).subtitle("Robot arm moves").register();
    /** A block welded onto the ship as its arm sets it down, in three bursts. */
    public static final SoundKey WELD = CONTENT.sound("shipyard/weld").variants(3)
            .range(20).subtitle("Arc welder crackles").register();
    /** The bay's floor rising under the ship or going down from under it, its hydraulic lift running. */
    public static final SoundKey DECK = CONTENT.sound("shipyard/deck").loop()
            .range(24).subtitle("Dock floor moves").register();

    private ShipyardSounds() {
    }

    /** Declares them, once, from the mod's constructor. */
    public static void declare() {
        // Loading the class declares them.
    }
}
