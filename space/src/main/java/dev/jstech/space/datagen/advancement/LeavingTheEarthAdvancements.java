/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.datagen.advancement;

import dev.jstech.core.datagen.advancement.AdvancementTab;
import dev.jstech.space.JsSpace;
import dev.jstech.space.advancement.SpaceEvents;
import dev.jstech.space.api.SpaceAdvancementTab;

/**
 * Leaving the Earth: the first rockets, satellites, crews, walks and dockings, and the stages that land again, each
 * named after what was said or done the first time. Its tab opens with a player's first rocket, as Goddard's first
 * liquid-fuelled one rose from his aunt's farm in 1926.
 */
public final class LeavingTheEarthAdvancements extends AdvancementTab {

    public LeavingTheEarthAdvancements() {
        super(JsSpace.MODID, SpaceAdvancementTab.LEAVING_THE_EARTH.path(), SpaceAdvancementTabs.NIGHT_SKY);
        this.root(picture("jsspace:gui/advancements/rocket"), "Leaving the Earth",
                "Launch your first rocket, as Goddard did from his Auntie Effie's farm",
                on(SpaceEvents.ROCKET_LAUNCHED));
    }
}
