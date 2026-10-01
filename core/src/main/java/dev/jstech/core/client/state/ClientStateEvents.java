/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.state;

import dev.jstech.core.JsCore;
import dev.jstech.core.state.ClientStates;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/** Lets every state's value go when the player leaves a server, so the next server's start from their defaults. */
@EventBusSubscriber(modid = JsCore.MODID, value = Dist.CLIENT)
public final class ClientStateEvents {

    private ClientStateEvents() {
    }

    @SubscribeEvent
    public static void onLoggingOut(final ClientPlayerNetworkEvent.LoggingOut event) {
        ClientStates.clear();
    }
}
