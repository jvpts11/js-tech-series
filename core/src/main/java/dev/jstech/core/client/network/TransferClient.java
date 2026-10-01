/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.network;

import dev.jstech.core.JsCore;
import dev.jstech.core.network.transfer.BigPayloads;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/** A player's game forgets the large values a server was still sending it as it leaves that server. */
@EventBusSubscriber(modid = JsCore.MODID, value = Dist.CLIENT)
public final class TransferClient {

    private TransferClient() {
    }

    @SubscribeEvent
    public static void onLoggingOut(final ClientPlayerNetworkEvent.LoggingOut event) {
        BigPayloads.forgetServer();
    }
}
