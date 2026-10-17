/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.datagen;

import dev.jstech.core.jstechseries.SeriesRecipes;
import dev.jstech.core.registry.CoreItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.data.event.GatherDataEvent;

/**
 * Entry point for the core's data generation, run via {@code ./gradlew :core:runData}: what its declared content
 * needs. The materials' things are not generated here: their art, tags and drops are made as the game loads, for the
 * things the installed mods use.
 */
public final class JsCoreDataGenerators {

    private JsCoreDataGenerators() {
    }

    /** Hands the mod's bus what this answers, once, from the mod's constructor. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(GatherDataEvent.class, JsCoreDataGenerators::onGatherData);
    }

    public static void onGatherData(final GatherDataEvent event) {
        final ContentData data = ContentData.gather(event, CoreItems.CONTENT);
        // The series' shared content is made of the game's own materials, so a mod used alone can make it.
        data.server(new SeriesRecipes(data.output(), data.lookup()));
        data.server(new BlockConductivityProvider(data.output(), data.lookup()));
        data.server(new BlockDensityProvider(data.output(), data.lookup()));
        data.server(new LightColoursProvider(data.output(), data.lookup()));
        data.server(new SealProvider(data.output(), data.lookup()));
        // The veins every biome's ores lie in, standing in for the game's scattered iron, copper and gold, and the
        // reservoirs under every biome.
        data.server(new UndergroundProvider(data.output(), data.lookup()));
        data.check();
    }
}
