/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.util.FakePlayer;

/**
 * A machine that keeps track of who has its console on screen, so that what its terminal prints while nobody
 * typed anything has somebody to go to, and so that a tick never walks the level's players to find them.
 *
 * <p>The menus say when they open and when they close, through here, whatever kind of machine is behind them.
 */
public interface IWatchedConsole {

    /** A player opened this machine's desktop or prompt. */
    void consoleOpenedBy(ServerPlayer viewer);

    /** A player closed this machine's desktop or prompt. */
    void consoleClosedBy(ServerPlayer viewer);

    /** Tells the machine at {@code host} that this player opened its desktop or prompt; nothing on the client. */
    static void opened(final Player player, final BlockPos host) {
        if (player instanceof ServerPlayer viewer
                && !(player instanceof FakePlayer)
                && viewer.level().isLoaded(host)
                && viewer.level().getBlockEntity(host) instanceof IWatchedConsole machine) {
            machine.consoleOpenedBy(viewer);
        }
    }

    /** Tells the machine at {@code host} that this player closed its desktop or prompt. */
    static void closed(final Player player, final BlockPos host) {
        if (player instanceof ServerPlayer viewer && viewer.level().isLoaded(host)
                && viewer.level().getBlockEntity(host) instanceof IWatchedConsole machine) {
            machine.consoleClosedBy(viewer);
        }
    }
}
