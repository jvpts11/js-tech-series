/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock.shape;

import java.util.Objects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import org.jetbrains.annotations.Nullable;

/**
 * A door of a formed model about to shut or open as a player uses it ({@link FormedDoors#use}), on the game's bus, on
 * the server: cancelled, it stays as it is, as an airlock keeps one of its doors shut while the other is open. The
 * mod cancelling it tells the player why.
 */
public final class FormedDoorEvent extends Event implements ICancellableEvent {

    private final ServerLevel level;
    private final @Nullable Player player;
    private final FormedDoors.Door door;
    private final boolean shutting;

    public FormedDoorEvent(final ServerLevel level, final @Nullable Player player, final FormedDoors.Door door,
                           final boolean shutting) {
        this.level = Objects.requireNonNull(level, "level");
        this.player = player;
        this.door = Objects.requireNonNull(door, "door");
        this.shutting = shutting;
    }

    /** The level the door is in. */
    public ServerLevel level() {
        return this.level;
    }

    /** The player using it, if a player is. */
    public @Nullable Player player() {
        return this.player;
    }

    /** The door. */
    public FormedDoors.Door door() {
        return this.door;
    }

    /** Whether it is shutting, or opening. */
    public boolean shutting() {
        return this.shutting;
    }
}
