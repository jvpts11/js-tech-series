/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.vehicle;

import java.util.function.Function;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Where a vehicle hears what its driver asks: from a driver of its own on the server, and from the player's keys on
 * the player's game, which only the client side of the Core can read and hands in here when it starts.
 */
public final class DriverInputs {

    /* What the local player's keys ask, read on their game; nothing on a server. */
    private static volatile Function<Player, DriverInput> localPlayer = player -> DriverInput.NONE;

    private DriverInputs() {
    }

    /** What {@code driver} asks of the vehicle it drives this tick. */
    public static DriverInput of(@Nullable final Entity driver) {
        if (driver instanceof IDriver own) {
            return own.driverInput();
        }
        if (driver instanceof Player player && player.isLocalPlayer()) {
            return localPlayer.apply(player);
        }
        if (driver instanceof ServerPlayer player) {
            /* The player's own game sends these keys to the server while they ride, as it does for a boat. */
            return new DriverInput(player.zza, player.xxa, false, player.isShiftKeyDown());
        }
        return DriverInput.NONE;
    }

    /** Hands in how the local player's keys are read, from the client side of the Core as it starts. */
    public static void readLocalPlayerWith(final Function<Player, DriverInput> reader) {
        localPlayer = reader;
    }
}
