/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.motion;

import dev.jstech.core.JsCore;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

/** Hands the motion profiles to the client's resource reload, for every mod of the series and every addon at once. */
@EventBusSubscriber(modid = JsCore.MODID, value = Dist.CLIENT)
public final class CoreMotionClient {

    private CoreMotionClient() {
    }

    @SubscribeEvent
    public static void onRegisterReloadListeners(final RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new MotionReloadListener());
    }
}
