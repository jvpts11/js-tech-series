/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.hazard;

import dev.jstech.core.guide.ModGuide;
import dev.jstech.core.registry.CoreItems;

/** The section on hazards in the Core's manual: the hazard devices and the grass contamination kills. */
public final class HazardGuide {

    private HazardGuide() {
    }

    /** Declares the section and its pages, from the Core's constructor, after the Core's chapter. */
    public static void declare() {
        final ModGuide.SectionRef hazards = CoreItems.CONTENT.guide().section("hazards")
                .titled("Hazards").icon(HazardDevices.GEIGER_COUNTER::get).register();
        CoreItems.CONTENT.guide().page("geiger_counter", hazards).titled("Geiger Counter")
                .icon(HazardDevices.GEIGER_COUNTER::get).covers(HazardDevices.GEIGER_COUNTER::get)
                .whatItIs("A radiation meter in a yellow case: its needle and its figures show how much radiation"
                        + " comes at you where you stand, and it clicks faster as it climbs.")
                .whatItIsFor("Knowing where radiation is before it doses you, and marking on your hazard map the"
                        + " chunks you walk through.")
                .define("Dose rate", "How fast radiation doses you where you stand, in millisieverts an hour"
                        + " (mSv/h). The ground gives off a little everywhere; near waste or a reactor it climbs.")
                .howToGetIt("In creative, take it from the J's Tech tab; the mods that need it give it their"
                        + " recipes.")
                .howToUseIt("Keep it in your hotbar or your off hand: it shows on your screen and reads as you walk.",
                        "Use it to hold it up. RANGE reads ten and then a hundred times higher, for a place where"
                                + " the needle is pinned; SOUND turns its clicks off and on.",
                        "Where it shows on your screen, and how big, is in the hazard devices' settings.")
                .whatCanGoWrong("It shows OFF.",
                        "There is no radiation in this world: the server turned it off, or no mod gives it.",
                        "The needle is pinned at the right.",
                        "The rate is past its scale: press RANGE, and get away.")
                .register();
        CoreItems.CONTENT.guide().page("pocket_dosimeter", hazards).titled("Pocket Dosimeter")
                .icon(HazardDevices.POCKET_DOSIMETER::get).covers(HazardDevices.POCKET_DOSIMETER::get)
                .whatItIs("A small meter you carry: it shows the dose of radiation your body has taken, and the"
                        + " band of radiation sickness it reached.")
                .whatItIsFor("Knowing how much more you can take before you have to rest away from the sources.")
                .define("Sievert", "The unit of a dose, Sv. About one sievert brings a mild sickness, Rad I; eight"
                        + " and more come close to deadly, Rad V.")
                .howToGetIt("In creative, take it from the J's Tech tab; the mods that need it give it their"
                        + " recipes.")
                .howToUseIt("Carry it anywhere on you: it shows on your screen.",
                        "Use it to hold it up; LIGHT lights its face for the dark.")
                .whatCanGoWrong("A lamp is lit.",
                        "You reached a band of radiation sickness: get away from the sources, and your body heals"
                                + " slowly.")
                .register();
        CoreItems.CONTENT.guide().page("hazard_map", hazards).titled("Hazard Map")
                .icon(HazardDevices.HAZARD_MAP::get).covers(HazardDevices.HAZARD_MAP::get)
                .whatItIs("A handheld map of the land round you, each chunk coloured by the hazard read there:"
                        + " radiation, or a pollution.")
                .whatItIsFor("Seeing where it is safe to go, and where you measured last and how long ago.")
                .define("Chunk", "A column of the world 16 blocks wide. The map knows only the chunks an"
                        + " instrument in your hands or a sensor on a network read; the rest stays grey.")
                .howToGetIt("In creative, take it from the J's Tech tab; the mods that need it give it their"
                        + " recipes.")
                .howToUseIt("Hold it: it shows on your screen, live, with what is read here.",
                        "Use it to open the whole map. Pick the hazard with the buttons at the top; the legend"
                                + " says what each colour means. Click a chunk for its reading and its age; drag to"
                                + " look further.")
                .whatCanGoWrong("The map is all grey.",
                        "Nothing was measured here yet: walk the land with a Geiger counter in your hotbar.")
                .register();
        CoreItems.CONTENT.guide().page("withered_grass", hazards).titled("Withered Grass")
                .icon(WitheredGrass.BLOCK::asItem).covers(WitheredGrass.BLOCK::asItem)
                .whatItIs("Grass killed by contamination: where radiation or a pollution is high, grass dies,"
                        + " flowers and saplings wither to dead bushes and crops stop growing.")
                .whatItIsFor("A warning you can see from afar: the land is fouled here.")
                .howToGetIt("It forms by itself where the land is contaminated; break it with silk touch to keep"
                        + " it, or it gives dirt.")
                .howToUseIt("Clean the land, or wait for the contamination to fall: the grass grows back slowly.")
                .whatCanGoWrong("The grass keeps dying.",
                        "The land is still contaminated: find what fouls it with a Geiger counter or the hazard map.")
                .register();
    }
}
