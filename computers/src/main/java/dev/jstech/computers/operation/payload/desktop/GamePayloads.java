/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.desktop;

import dev.jstech.computers.advancement.JscEvents;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.operation.payload.GameWonPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** The desktop games' word to the server: what a player won, for the advancements that ask for it. */
public final class GamePayloads {

    public static final String MINESWEEPER = "minesweeper";
    public static final String HARDEST_BOARD = "expert";

    private GamePayloads() {
    }

    public static void register(final PayloadRegistrar registrar) {
        ComputerAccess.accept(registrar, GameWonPayload.TYPE, GameWonPayload.STREAM_CODEC,
                ComputerAccess.machine(GameWonPayload::hostPos), GamePayloads::handleWon);
    }

    private static void handleWon(final GameWonPayload payload, final ServerPlayer player, final ServerLevel level) {
        if (MINESWEEPER.equals(payload.game()) && HARDEST_BOARD.equals(payload.board())) {
            JscEvents.award(player, JscEvents.MINESWEEPER_EXPERT);
        }
    }
}
