/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.guide;

import dev.jstech.core.api.client.GuideBlockRenderers;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/** What J's Computers draws on the manuals' pages itself: its machines' firmware screens. */
public final class ComputersGuideClient {

    private ComputersGuideClient() {
    }

    /** Hands the mod's bus what this answers, once, from the mod's client constructor. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(FMLClientSetupEvent.class, ComputersGuideClient::onClientSetup);
    }

    public static void onClientSetup(final FMLClientSetupEvent event) {
        GuideBlockRenderers.register(FirmwarePictures.KIND, new FirmwarePictures());
    }
}
