/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.datagen;

import dev.jstech.core.datagen.ContentData;
import dev.jstech.industrial.IndustrialModule;
import dev.jstech.industrial.JsIndustrial;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

/**
 * Entry point for the industrial mod's data generation, run via {@code ./gradlew :industrial:runData}: what its
 * declared machines need, and its recipes.
 */
@EventBusSubscriber(modid = JsIndustrial.MODID)
public final class JsIndustrialDataGenerators {

    private JsIndustrialDataGenerators() {
    }

    @SubscribeEvent
    public static void onGatherData(final GatherDataEvent event) {
        final ContentData data = ContentData.gather(event, IndustrialModule.CONTENT);
        data.server(new JsIndustrialRecipeProvider(data.output(), data.lookup()));
    }
}
