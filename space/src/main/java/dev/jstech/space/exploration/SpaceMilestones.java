/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.exploration;

import dev.jstech.core.progression.Milestone;
import dev.jstech.core.progression.Milestones;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.space.JsSpace;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

/**
 * J's Space's milestones: the real firsts of spaceflight, in the order they happened, from the first flight to space
 * to the first galaxy. They belong to the team, kept with the world by J's Core's progression, and any mod can read
 * them; each also gives the team the knowledge of the same id, so a recipe can ask for it. The advancements of the
 * same firsts are earned by the team's milestone, by every member of it.
 */
@TextHolder
public final class SpaceMilestones {

    public static final TextKey SUBORBITAL_FLIGHT_NAME = TextKey.of("jsspace.milestone.suborbital_flight",
            "First suborbital flight");
    public static final TextKey SATELLITE_NAME = TextKey.of("jsspace.milestone.satellite", "First satellite");
    public static final TextKey PERSON_IN_ORBIT_NAME = TextKey.of("jsspace.milestone.person_in_orbit",
            "First person in orbit");
    public static final TextKey SPACEWALK_NAME = TextKey.of("jsspace.milestone.spacewalk", "First spacewalk");
    public static final TextKey DOCKING_NAME = TextKey.of("jsspace.milestone.docking", "First docking");
    public static final TextKey PROBE_ON_ANOTHER_WORLD_NAME = TextKey.of("jsspace.milestone.probe_on_another_world",
            "First probe on another world");
    public static final TextKey MOON_LANDING_NAME = TextKey.of("jsspace.milestone.moon_landing",
            "First landing on the Moon");
    public static final TextKey ROVER_NAME = TextKey.of("jsspace.milestone.rover", "First rover");
    public static final TextKey PLANET_LANDING_NAME = TextKey.of("jsspace.milestone.planet_landing",
            "First landing on another planet");
    public static final TextKey SAMPLE_RETURN_NAME = TextKey.of("jsspace.milestone.sample_return",
            "First sample brought back");
    public static final TextKey SPACE_STATION_NAME = TextKey.of("jsspace.milestone.space_station",
            "First space station");
    public static final TextKey BEYOND_THE_HELIOPAUSE_NAME = TextKey.of("jsspace.milestone.beyond_the_heliopause",
            "First probe beyond the heliopause");
    public static final TextKey PERSON_ON_ANOTHER_PLANET_NAME = TextKey.of(
            "jsspace.milestone.person_on_another_planet", "First person on another planet");
    public static final TextKey TERMINUS_FOUND_NAME = TextKey.of("jsspace.milestone.terminus_found", "Terminus found");
    public static final TextKey OCEANUS_FOUND_NAME = TextKey.of("jsspace.milestone.oceanus_found", "Oceanus found");
    public static final TextKey WORLD_KNOWN_WHOLE_NAME = TextKey.of("jsspace.milestone.world_known_whole",
            "First world known to 100%");
    public static final TextKey STARSHIP_NAME = TextKey.of("jsspace.milestone.starship", "First starship");
    public static final TextKey STAR_NAME = TextKey.of("jsspace.milestone.star", "First star");
    public static final TextKey GALAXY_NAME = TextKey.of("jsspace.milestone.galaxy", "First galaxy");

    public static final Milestone SUBORBITAL_FLIGHT = new Milestone(id("suborbital_flight"), SUBORBITAL_FLIGHT_NAME);
    public static final Milestone SATELLITE = new Milestone(id("satellite"), SATELLITE_NAME);
    public static final Milestone PERSON_IN_ORBIT = new Milestone(id("person_in_orbit"), PERSON_IN_ORBIT_NAME);
    public static final Milestone SPACEWALK = new Milestone(id("spacewalk"), SPACEWALK_NAME);
    public static final Milestone DOCKING = new Milestone(id("docking"), DOCKING_NAME);
    public static final Milestone PROBE_ON_ANOTHER_WORLD = new Milestone(id("probe_on_another_world"),
            PROBE_ON_ANOTHER_WORLD_NAME);
    public static final Milestone MOON_LANDING = new Milestone(id("moon_landing"), MOON_LANDING_NAME);
    public static final Milestone ROVER = new Milestone(id("rover"), ROVER_NAME);
    public static final Milestone PLANET_LANDING = new Milestone(id("planet_landing"), PLANET_LANDING_NAME);
    public static final Milestone SAMPLE_RETURN = new Milestone(id("sample_return"), SAMPLE_RETURN_NAME);
    public static final Milestone SPACE_STATION = new Milestone(id("space_station"), SPACE_STATION_NAME);
    public static final Milestone BEYOND_THE_HELIOPAUSE = new Milestone(id("beyond_the_heliopause"),
            BEYOND_THE_HELIOPAUSE_NAME);
    public static final Milestone PERSON_ON_ANOTHER_PLANET = new Milestone(id("person_on_another_planet"),
            PERSON_ON_ANOTHER_PLANET_NAME);
    public static final Milestone TERMINUS_FOUND = new Milestone(id("terminus_found"), TERMINUS_FOUND_NAME);
    public static final Milestone OCEANUS_FOUND = new Milestone(id("oceanus_found"), OCEANUS_FOUND_NAME);
    public static final Milestone WORLD_KNOWN_WHOLE = new Milestone(id("world_known_whole"), WORLD_KNOWN_WHOLE_NAME);
    public static final Milestone STARSHIP = new Milestone(id("starship"), STARSHIP_NAME);
    public static final Milestone STAR = new Milestone(id("star"), STAR_NAME);
    public static final Milestone GALAXY = new Milestone(id("galaxy"), GALAXY_NAME);

    /** The firsts, in the order they happened; each is reached by what does it, as that is built. */
    public static final List<Milestone> FIRSTS = Milestones.declare(id("firsts"), List.of(SUBORBITAL_FLIGHT,
            SATELLITE, PERSON_IN_ORBIT, SPACEWALK, DOCKING, PROBE_ON_ANOTHER_WORLD, MOON_LANDING, ROVER,
            PLANET_LANDING, SAMPLE_RETURN, SPACE_STATION, BEYOND_THE_HELIOPAUSE, PERSON_ON_ANOTHER_PLANET,
            TERMINUS_FOUND, OCEANUS_FOUND, WORLD_KNOWN_WHOLE, STARSHIP, STAR, GALAXY));

    private SpaceMilestones() {
    }

    /** Declares the milestones, before the game has loaded. */
    public static void declare() {
        // Loading the class declares them.
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, path);
    }
}
