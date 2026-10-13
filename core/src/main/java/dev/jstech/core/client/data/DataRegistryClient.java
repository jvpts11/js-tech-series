/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.data;

import dev.jstech.core.JsCore;
import dev.jstech.core.data.DataRegistries;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/** A player's game forgets what a server's data registries sent it as it leaves that server. */
@EventBusSubscriber(modid = JsCore.MODID, value = Dist.CLIENT)
public final class DataRegistryClient {

    private DataRegistryClient() {
    }

    @SubscribeEvent
    public static void onLoggingOut(final ClientPlayerNetworkEvent.LoggingOut event) {
        DataRegistries.forgetClientValues();
    }
}
