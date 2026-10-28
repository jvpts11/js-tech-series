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
 * The Moon: landing on it, standing at its landmarks, driving on it and building a station by it. Its tab opens as a
 * player first sees the Earth rise over the Moon's horizon on arriving, as the crew of Apollo 8 did in 1968.
 */
public final class TheMoonAdvancements extends AdvancementTab {

    public TheMoonAdvancements() {
        super(JsSpace.MODID, SpaceAdvancementTab.THE_MOON.path(), SpaceAdvancementTabs.NIGHT_SKY);
        this.root(picture("jsspace:gui/advancements/moon"), "The Moon",
                "See the Earth rise over the Moon, as Apollo 8 did", on(SpaceEvents.EARTHRISE));
    }
}
