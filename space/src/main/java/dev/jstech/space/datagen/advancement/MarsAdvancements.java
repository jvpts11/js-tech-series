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
 * Mars: its blue sunsets, its water. Its tab opens with a player's first landing on it, through the thin air that
 * gives a descent the seven minutes of terror Curiosity's was named for.
 */
public final class MarsAdvancements extends AdvancementTab {

    /** The world landed on, as the landing reports it. */
    public static final String MARS = "jsspace:mars";

    public MarsAdvancements() {
        super(JsSpace.MODID, SpaceAdvancementTab.MARS.path(), SpaceAdvancementTabs.NIGHT_SKY);
        this.root(picture("jsspace:gui/advancements/mars"), "Mars",
                "Land on Mars, through the seven minutes of terror", on(SpaceEvents.LANDED, MARS));
    }
}
