/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.guide;

import dev.jstech.computers.JsComputers;
import dev.jstech.core.api.client.GuideBlockRenderers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/** What J's Computers draws on the manuals' pages itself: its machines' firmware screens. */
@EventBusSubscriber(modid = JsComputers.MODID, value = Dist.CLIENT)
public final class ComputersGuideClient {

    private ComputersGuideClient() {
    }

    @SubscribeEvent
    public static void onClientSetup(final FMLClientSetupEvent event) {
        GuideBlockRenderers.register(FirmwarePictures.KIND, new FirmwarePictures());
    }
}
