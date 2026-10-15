/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.font;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

/** Hands the declared fonts to the client's resource reload, for every mod of the series and every addon at once. */
public final class CoreFontClient {

    private CoreFontClient() {
    }

    /** Hands the mod's bus what this answers, once, from the mod's constructor on a player's game. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterClientReloadListenersEvent.class, CoreFontClient::onRegisterReloadListeners);
    }

    public static void onRegisterReloadListeners(final RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new CellFontReloadListener());
    }
}
