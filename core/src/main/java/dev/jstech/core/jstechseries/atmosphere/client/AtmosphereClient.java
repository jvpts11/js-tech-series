/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.atmosphere.client;

import dev.jstech.core.jstechseries.atmosphere.AtmosphereDetector;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** The Atmosphere Detector on the player's side: its screen. */
public final class AtmosphereClient {

    private AtmosphereClient() {
    }

    /** Gives the detector its screen, once, from the Core's client. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterMenuScreensEvent.class, event -> event.register(AtmosphereDetector.MENU.get(),
                AtmosphereDetectorScreen::new));
    }
}
