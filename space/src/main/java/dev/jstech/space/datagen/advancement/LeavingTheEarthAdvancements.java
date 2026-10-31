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
import dev.jstech.space.ship.ShipKitBlocks;

/**
 * Leaving the Earth: the first rockets, satellites, crews, walks and dockings, and the stages that land again, each
 * named after what was said or done the first time. Its tab opens with a player's first rocket, as Goddard's first
 * liquid-fuelled one rose from his aunt's farm in 1926; the team's first person in orbit is named for what Gagarin saw
 * from it, and its first starship for the first Space Shuttle, rolled out in 1976.
 */
public final class LeavingTheEarthAdvancements extends AdvancementTab {

    /* The tab's root, which every other hangs from. */
    private static final String ROOT = "root";

    public LeavingTheEarthAdvancements() {
        super(JsSpace.MODID, SpaceAdvancementTab.LEAVING_THE_EARTH.path(), SpaceAdvancementTabs.NIGHT_SKY);
        this.root(picture("jsspace:gui/advancements/rocket"), "Leaving the Earth",
                "Launch your first rocket, as Goddard did from his Auntie Effie's farm",
                on(SpaceEvents.ROCKET_LAUNCHED));
        this.goal("the_overworld_is_blue", ROOT, RocketKitBlocks.CAPSULE_WINDOW.get(), "The Overworld Is Blue",
                "Be the first of your team in orbit, as Gagarin was", reached(SpaceMilestones.PERSON_IN_ORBIT));
        this.goal("enterprise", ROOT, ShipKitBlocks.FLIGHT_CONSOLE.get(), "Enterprise",
                "Let your team's first starship out of its dock, as the first Shuttle was rolled out",
                reached(SpaceMilestones.STARSHIP));
    }
}
