/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.spacecraft;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.space.JsSpace;
import dev.jstech.space.api.OrbitKind;
import dev.jstech.space.api.ProbeType;
import dev.jstech.space.api.ProbeTypes;
import dev.jstech.space.api.SatelliteType;
import dev.jstech.space.api.SatelliteTypes;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

/**
 * The kinds of satellite and of probe J's Space declares, each with the orbits it works best in or whether it goes
 * down onto its world, as the real ones they follow: the communication, relay, navigation, mapping, weather and
 * observation satellites, the space telescope, the solar power, antiproton and debris collectors, the laser and the
 * planetary climate satellite; the flyby, orbiter, lander, rover, drone, sample return, aeroprobe, mining probe,
 * impactor, solar probe, interstellar probe and solar gravitational lens probe. What each does is for the content
 * past the skeleton; here they are the kinds a satellite or probe can be built as.
 */
@TextHolder
public final class SkeletonSpacecraft {

    public static final TextKey COMMUNICATION = TextKey.of("jsspace.satellite.communication",
            "Communication Satellite");
    public static final TextKey RELAY = TextKey.of("jsspace.satellite.relay", "Relay Satellite");
    public static final TextKey NAVIGATION = TextKey.of("jsspace.satellite.navigation", "Navigation Satellite");
    public static final TextKey MAPPING = TextKey.of("jsspace.satellite.mapping", "Mapping Satellite");
    public static final TextKey WEATHER = TextKey.of("jsspace.satellite.weather", "Weather Satellite");
    public static final TextKey OBSERVATION = TextKey.of("jsspace.satellite.observation", "Observation Satellite");
    public static final TextKey TELESCOPE = TextKey.of("jsspace.satellite.space_telescope", "Space Telescope");
    public static final TextKey SOLAR_POWER = TextKey.of("jsspace.satellite.solar_power", "Solar Power Satellite");
    public static final TextKey ANTIPROTON = TextKey.of("jsspace.satellite.antiproton_collector",
            "Antiproton Collector");
    public static final TextKey DEBRIS_COLLECTOR = TextKey.of("jsspace.satellite.debris_collector",
            "Debris Collector");
    public static final TextKey LASER = TextKey.of("jsspace.satellite.laser", "Laser Satellite");
    public static final TextKey CLIMATE = TextKey.of("jsspace.satellite.planetary_climate",
            "Planetary Climate Satellite");
    public static final TextKey FLYBY = TextKey.of("jsspace.probe.flyby", "Flyby Probe");
    public static final TextKey ORBITER = TextKey.of("jsspace.probe.orbiter", "Orbiter");
    public static final TextKey LANDER = TextKey.of("jsspace.probe.lander", "Lander");
    public static final TextKey ROVER = TextKey.of("jsspace.probe.rover", "Rover");
    public static final TextKey DRONE = TextKey.of("jsspace.probe.drone", "Drone");
    public static final TextKey SAMPLE_RETURN = TextKey.of("jsspace.probe.sample_return", "Sample Return Probe");
    public static final TextKey AEROPROBE = TextKey.of("jsspace.probe.aeroprobe", "Aeroprobe");
    public static final TextKey MINING = TextKey.of("jsspace.probe.mining", "Mining Probe");
    public static final TextKey IMPACTOR = TextKey.of("jsspace.probe.impactor", "Impactor");
    public static final TextKey SOLAR = TextKey.of("jsspace.probe.solar", "Solar Probe");
    public static final TextKey INTERSTELLAR = TextKey.of("jsspace.probe.interstellar", "Interstellar Probe");
    public static final TextKey LENS = TextKey.of("jsspace.probe.solar_gravitational_lens",
            "Solar Gravitational Lens Probe");

    private SkeletonSpacecraft() {
    }

    /** Declares the kinds, once, from the mod's constructor. */
    public static void declare() {
        satellite("communication", COMMUNICATION, OrbitKind.GEOSTATIONARY, OrbitKind.LOW);
        satellite("relay", RELAY, OrbitKind.GEOSTATIONARY, OrbitKind.DEEP_SPACE);
        satellite("navigation", NAVIGATION, OrbitKind.LOW);
        satellite("mapping", MAPPING, OrbitKind.POLAR);
        satellite("weather", WEATHER, OrbitKind.GEOSTATIONARY, OrbitKind.DEEP_SPACE);
        satellite("observation", OBSERVATION, OrbitKind.LOW, OrbitKind.GEOSTATIONARY);
        satellite("space_telescope", TELESCOPE, OrbitKind.LOW, OrbitKind.DEEP_SPACE);
        satellite("solar_power", SOLAR_POWER, OrbitKind.GEOSTATIONARY);
        satellite("antiproton_collector", ANTIPROTON, OrbitKind.RADIATION_BELT);
        // It goes where the debris is, and the climate satellite where its terraforming wants it.
        satellite("debris_collector", DEBRIS_COLLECTOR);
        satellite("laser", LASER, OrbitKind.LOW);
        satellite("planetary_climate", CLIMATE);
        probe("flyby", FLYBY, false);
        probe("orbiter", ORBITER, false);
        probe("lander", LANDER, true);
        probe("rover", ROVER, true);
        probe("drone", DRONE, true);
        probe("sample_return", SAMPLE_RETURN, true);
        probe("aeroprobe", AEROPROBE, false);
        probe("mining", MINING, true);
        probe("impactor", IMPACTOR, false);
        probe("solar", SOLAR, false);
        probe("interstellar", INTERSTELLAR, false);
        probe("solar_gravitational_lens", LENS, false);
    }

    /** The id of the skeleton's kind of satellite or probe {@code path}. */
    public static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, path);
    }

    private static void satellite(final String path, final TextKey name, final OrbitKind... orbits) {
        SatelliteTypes.declare(id(path), new SatelliteType(name.key(), List.of(orbits)));
    }

    private static void probe(final String path, final TextKey name, final boolean lands) {
        ProbeTypes.declare(id(path), new ProbeType(name.key(), lands));
    }
}
