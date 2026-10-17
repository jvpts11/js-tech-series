/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.persistor.client;

import dev.jstech.core.client.machine.MachineScreens;
import dev.jstech.core.jstechseries.persistor.SpacePersistorBlockEntity;

/** The Space Persistor on the player's side: its own screen in place of the standard machine screen. */
public final class PersistorClient {

    private PersistorClient() {
    }

    /** Gives the Persistor its screen, once, from the Core's client. */
    public static void listen() {
        MachineScreens.register(machine -> machine instanceof SpacePersistorBlockEntity, SpacePersistorScreen::new);
    }
}
