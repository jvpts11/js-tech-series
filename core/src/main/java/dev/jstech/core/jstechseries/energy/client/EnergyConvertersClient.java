/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.energy.client;

import dev.jstech.core.jstechseries.energy.EnergyConverters;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** The converters on the player's game: their screen. */
public final class EnergyConvertersClient {

    private EnergyConvertersClient() {
    }

    /** Hands the mod's bus what this answers, once, from the Core's client. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterMenuScreensEvent.class, event -> event.register(
                EnergyConverters.CONVERTER_MENU.get(), EnergyConverterScreen::new));
    }
}
