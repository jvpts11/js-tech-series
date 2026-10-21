/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Example.
 */
package dev.jstech.example;

import com.mojang.serialization.Codec;
import dev.jstech.core.state.CoreState;
import dev.jstech.core.state.CoreStates;
import dev.jstech.core.state.PlayerState;
import dev.jstech.core.state.ServerState;

/**
 * What the example mod remembers beyond any block: one number for the whole server and one for each player, each
 * saved with the world by the Core, which also writes and reads the file.
 */
public final class ExampleState {

    /** All the energy the generators of this server ever made. */
    public static final ServerState<Long> ENERGY_MADE = CoreState.builder(JsExample.id("energy_made"), Codec.LONG, 0L)
            .server();
    /** How many times each player opened the logbook. */
    public static final PlayerState<Integer> LOGBOOK_OPENED = CoreState.builder(JsExample.id("logbook_opened"),
            Codec.INT, 0).player();

    private ExampleState() {
    }

    /** Hands both to the Core, from the mod's constructor. */
    public static void register() {
        CoreStates.register(ENERGY_MADE);
        CoreStates.register(LOGBOOK_OPENED);
    }
}
