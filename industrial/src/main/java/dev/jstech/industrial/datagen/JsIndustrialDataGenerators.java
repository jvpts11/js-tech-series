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
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.data.event.GatherDataEvent;

/**
 * Entry point for the industrial mod's data generation, run via {@code ./gradlew :industrial:runData}: what its
 * declared machines need, and its recipes. The run fails, saying what, when something the content declares is
 * missing.
 */
public final class JsIndustrialDataGenerators {

    private JsIndustrialDataGenerators() {
    }

    /** Hands the mod's bus the data generation, once, from the mod's constructor. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(GatherDataEvent.class, JsIndustrialDataGenerators::onGatherData);
    }

    public static void onGatherData(final GatherDataEvent event) {
        final ContentData data = ContentData.gather(event, IndustrialModule.CONTENT);
        data.server(new JsIndustrialRecipeProvider(data.output(), data.lookup()));
        data.check();
    }
}
