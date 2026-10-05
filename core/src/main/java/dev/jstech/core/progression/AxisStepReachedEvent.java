/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.progression;

import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.Event;

import java.util.UUID;

/**
 * A player reached a step of an axis that is further than any they had reached before, posted on the game's event
 * bus. It is posted once for each move forward, whether the player is online or not, and never when a step already
 * passed is reached again.
 */
public final class AxisStepReachedEvent extends Event {

    private final MinecraftServer server;
    private final UUID player;
    private final ProgressionAxis<?> axis;
    private final IAxisStep from;
    private final IAxisStep to;

    public AxisStepReachedEvent(final MinecraftServer server, final UUID player, final ProgressionAxis<?> axis,
                                final IAxisStep from, final IAxisStep to) {
        this.server = server;
        this.player = player;
        this.axis = axis;
        this.from = from;
        this.to = to;
    }

    public MinecraftServer server() {
        return server;
    }

    /** The player who moved forward. */
    public UUID player() {
        return player;
    }

    public ProgressionAxis<?> axis() {
        return axis;
    }

    /** The furthest step the player had reached before. */
    public IAxisStep from() {
        return from;
    }

    /** The step reached now. */
    public IAxisStep to() {
        return to;
    }
}
