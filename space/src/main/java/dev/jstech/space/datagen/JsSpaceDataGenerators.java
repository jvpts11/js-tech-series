/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.datagen;

import dev.jstech.core.datagen.ContentData;
import dev.jstech.space.SpaceContent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.data.event.GatherDataEvent;

/**
 * Entry point for the space mod's data generation, run via {@code ./gradlew :space:runData}: what its declared content
 * needs, its block states, models, English names and loot. The run fails, saying what, when something the content
 * declares is missing.
 */
public final class JsSpaceDataGenerators {

    private JsSpaceDataGenerators() {
    }

    /** Hands the mod's bus the data generation, once, from the mod's constructor. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(GatherDataEvent.class, JsSpaceDataGenerators::onGatherData);
    }

    public static void onGatherData(final GatherDataEvent event) {
        ContentData.gather(event, SpaceContent.CONTENT).check();
    }
}
