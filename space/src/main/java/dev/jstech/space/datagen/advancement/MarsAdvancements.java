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
import dev.jstech.space.exploration.SpaceMilestones;
import dev.jstech.space.rocket.RocketKitBlocks;

/**
 * Mars: its blue sunsets, its water. Its tab opens with a player's first landing on it; the team's first landing on
 * another planet is named for the seven minutes of terror Curiosity's descent through the thin air was named for.
 */
public final class MarsAdvancements extends AdvancementTab {

    /** The world landed on, as the landing reports it. */
    public static final String MARS = "jsspace:mars";
    /* The tab's root, which every other hangs from. */
    private static final String ROOT = "root";

    public MarsAdvancements() {
        super(JsSpace.MODID, SpaceAdvancementTab.MARS.path(), SpaceAdvancementTabs.NIGHT_SKY);
        this.root(picture("jsspace:gui/advancements/mars"), "Mars",
                "Land on Mars, through its thin air", on(SpaceEvents.LANDED, MARS));
        this.goal("seven_minutes_of_terror", ROOT, RocketKitBlocks.HEAT_SHIELD.get(), "Seven Minutes of Terror",
                "Be the first of your team to land on another planet", reached(SpaceMilestones.PLANET_LANDING));
    }
}
