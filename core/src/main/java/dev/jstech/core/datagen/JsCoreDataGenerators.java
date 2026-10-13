/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.datagen;

import dev.jstech.core.JsCore;
import dev.jstech.core.registry.CoreItems;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

/**
 * Entry point for the core's data generation, run via {@code ./gradlew :core:runData}: what its declared content
 * needs, and the common tags of the materials.
 */
@EventBusSubscriber(modid = JsCore.MODID)
public final class JsCoreDataGenerators {

    private JsCoreDataGenerators() {
    }

    @SubscribeEvent
    public static void onGatherData(final GatherDataEvent event) {
        final ContentData data = ContentData.gather(event, CoreItems.CONTENT);
        data.server(new JsCoreItemTagsProvider(data.output(), data.lookup(), data.existingFiles()));
    }
}
