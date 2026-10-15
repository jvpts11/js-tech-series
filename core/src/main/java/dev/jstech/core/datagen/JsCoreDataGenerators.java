/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.datagen;

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
        data.check();
    }
}
