/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import net.neoforged.bus.api.Event;

/**
 * Posted on the game's bus after blocks were moved together, as a moving structure was made, set down or taken to
 * another dimension, their block entities already in their new places: for a mod that keeps the places of blocks
 * outside its block entities, or whose block entities it cannot change.
 */
public final class StructureBlocksMovedEvent extends Event {

    private final BlockMove move;

    public StructureBlocksMovedEvent(final BlockMove move) {
        this.move = move;
    }

    public BlockMove move() {
        return move;
    }
}
