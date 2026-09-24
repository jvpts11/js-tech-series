/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * A screen a player holds at a monitor: what the glass is showing, and to whom.
 *
 * <p>A monitor is one screen with one keyboard in front of it. Two players typing at the same one would each be
 * driving a session the other is reading, so whoever is at it keeps it until they walk away, and the one who
 * came second is told the monitor is taken.
 */
public interface IMonitorMenu {

    /** The monitor this screen is on. */
    BlockPos monitorPos();

    /**
     * Somebody other than {@code asking} who is at that monitor, or null when nobody else is.
     *
     * <p>Handed the players to look through rather than finding them, so the question is the same whoever asks it
     * and a test can put the players in front of it.
     */
    @Nullable
    static Player userOf(final Iterable<? extends Player> players, final BlockPos monitorPos, final Player asking) {
        for (final Player player : players) {
            if (player != asking && player.containerMenu instanceof IMonitorMenu at
                    && monitorPos.equals(at.monitorPos())) {
                return player;
            }
        }
        return null;
    }
}
