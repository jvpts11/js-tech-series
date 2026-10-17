/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.atmosphere;

import dev.jstech.core.guide.ModGuide;
import dev.jstech.core.registry.CoreItems;

/** The Atmosphere Detector's page in the Core's section on the world. */
public final class AtmosphereGuide {

    private AtmosphereGuide() {
    }

    /** Declares the page, from the Core's constructor, in the section on the world. */
    public static void declare(final ModGuide.SectionRef world) {
        CoreItems.CONTENT.guide().page("atmosphere_detector", world).titled("Atmosphere Detector")
                .icon(AtmosphereDetector.BLOCK::asItem).covers(AtmosphereDetector.BLOCK::asItem)
                .whatItIs("A gauge for the air: it reads the air round it, a sealed room's or the open air's, and"
                        + " shows what it is made of, its pressure and the room it stands in.")
                .whatItIsFor("Knowing whether a room can be breathed before you take your helmet off, and finding"
                        + " where a room leaks its air.")
                .define("Sealed room", "A space walled in on every side by blocks that hold air. It keeps its own"
                        + " air: filled with oxygen it can be breathed on the Moon; a hole in it lets its air out.")
                .define("Kilopascal", "The unit of pressure on the display, kPa. The air at sea level presses at"
                        + " about 101 kPa; a breath needs oxygen at more than 16 kPa of it.")
                .howToGetIt("In creative, take it from the J's Tech tab; the mods that need it give it their"
                        + " recipes.")
                .howToUseIt("Place it in the room, or in its wall: a detector in a wall reads the room beside it.",
                        "Use it to open its screen. THE AIR lists each gas and its share, the pressure, and whether"
                                + " it is falling or rising.",
                        "THE ROOM draws the room from above, its leaks marked, with its size and how many leaks it"
                                + " has; OPEN AIR when no room holds it.",
                        "Press SHOW IN THE WORLD to see the room outlined round you, its leaks lit, while you walk"
                                + " it; press it again to stop.")
                .whatCanGoWrong("The HYPOXIA lamp is lit.",
                        "There is too little oxygen to breathe: fill the room with oxygen, or keep your helmet on.",
                        "The LEAKING lamp is lit.",
                        "The room loses its air somewhere: show it in the world and close the leaks lit, a missing"
                                + " block, an open door or a pane.",
                        "The VACUUM lamp is lit.",
                        "There is no air at all: the room is open to space, or was never filled.")
                .register();
    }
}
