/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import dev.jstech.core.dimension.DimensionHazardEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * A space suit as another mod would make one, in the plainest form: a player whose name starts with
 * {@link #SUITED} breathes wherever they are, which is what the Core's hazard event lets a mod say.
 */
@EventBusSubscriber(modid = JsTests.MODID)
public final class TestSuits {

    /** What the name of a player in a suit starts with. */
    public static final String SUITED = "jstests-suited";

    private TestSuits() {
    }

    @SubscribeEvent
    public static void onHazard(final DimensionHazardEvent event) {
        if (event.hazard() == DimensionHazardEvent.Hazard.AIR
                && event.player().getGameProfile().getName().startsWith(SUITED)) {
            event.setCanceled(true);
        }
    }
}
