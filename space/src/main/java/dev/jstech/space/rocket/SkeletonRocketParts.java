/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.rocket;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.space.JsSpace;
import dev.jstech.space.api.RocketPart;
import dev.jstech.space.api.RocketParts;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/**
 * The rocket parts of J's Space's skeleton, in the diameters of 3 and 5 blocks, all burning methalox: the sea-level and
 * vacuum engines, the tank built in layers, the separation and instrument rings, the capsule, the landing legs, and the
 * two stages of the lunar lander. Their heights are those of their approved kits; their masses, thrusts and specific
 * impulses are estimates from the real parts they follow (Raptor's engines, methalox tanks, Orion's and Apollo's
 * capsules and lunar module), to be set by playing.
 */
@TextHolder
public final class SkeletonRocketParts {

    public static final TextKey SEA_LEVEL_ENGINE = TextKey.of("jsspace.rocket_part.sea_level_engine",
            "Sea-Level Engine");
    public static final TextKey VACUUM_ENGINE = TextKey.of("jsspace.rocket_part.vacuum_engine", "Vacuum Engine");
    public static final TextKey TANK = TextKey.of("jsspace.rocket_part.tank", "Tank");
    public static final TextKey SEPARATION_RING = TextKey.of("jsspace.rocket_part.separation_ring",
            "Separation Ring");
    public static final TextKey INSTRUMENT_RING = TextKey.of("jsspace.rocket_part.instrument_ring",
            "Instrument Ring");
    public static final TextKey CAPSULE = TextKey.of("jsspace.rocket_part.capsule", "Capsule");
    public static final TextKey LANDING_LEGS = TextKey.of("jsspace.rocket_part.landing_legs", "Landing Legs");
    public static final TextKey LANDER_DESCENT = TextKey.of("jsspace.rocket_part.lander_descent",
            "Lunar Lander Descent Stage");
    public static final TextKey LANDER_ASCENT = TextKey.of("jsspace.rocket_part.lander_ascent",
            "Lunar Lander Ascent Stage");
    /** The propellant the skeleton's engines burn: methane and liquid oxygen. */
    public static final ResourceLocation METHALOX = SkeletonPropellants.METHALOX;
    /* The tier each diameter's parts carry. */
    private static final int SMALL_TIER = 1;
    private static final int LARGE_TIER = 2;

    private SkeletonRocketParts() {
    }

    /** Declares the parts, once, from the mod's constructor. */
    public static void declare() {
        engine("sea_level_engine_3", SEA_LEVEL_ENGINE, 3, 2, 1_600, 1_800_000, 327, 350, SMALL_TIER);
        engine("vacuum_engine_3", VACUUM_ENGINE, 3, 3, 1_800, 1_900_000, 200, 380, SMALL_TIER);
        part("tank_3", TANK, RocketPart.Kind.TANK, 3, 1, true, 300, 5_800, 0, SMALL_TIER);
        part("separation_ring_3", SEPARATION_RING, RocketPart.Kind.SEPARATION_RING, 3, 1, false, 300, 0, 0,
                SMALL_TIER);
        part("instrument_ring_3", INSTRUMENT_RING, RocketPart.Kind.INSTRUMENT_RING, 3, 1, false, 400, 0, 0,
                SMALL_TIER);
        part("capsule_3", CAPSULE, RocketPart.Kind.CAPSULE, 3, 3, false, 3_500, 0, 1, SMALL_TIER);
        part("landing_legs_3", LANDING_LEGS, RocketPart.Kind.LANDING_LEGS, 3, 1, false, 400, 0, 0, SMALL_TIER);
        engine("sea_level_engine_5", SEA_LEVEL_ENGINE, 5, 3, 4_000, 4_500_000, 330, 355, LARGE_TIER);
        engine("vacuum_engine_5", VACUUM_ENGINE, 5, 4, 4_500, 4_800_000, 220, 382, LARGE_TIER);
        part("tank_5", TANK, RocketPart.Kind.TANK, 5, 1, true, 800, 16_300, 0, LARGE_TIER);
        part("separation_ring_5", SEPARATION_RING, RocketPart.Kind.SEPARATION_RING, 5, 1, false, 900, 0, 0,
                LARGE_TIER);
        part("instrument_ring_5", INSTRUMENT_RING, RocketPart.Kind.INSTRUMENT_RING, 5, 1, false, 1_100, 0, 0,
                LARGE_TIER);
        part("capsule_5", CAPSULE, RocketPart.Kind.CAPSULE, 5, 4, false, 9_000, 0, 3, LARGE_TIER);
        part("landing_legs_5", LANDING_LEGS, RocketPart.Kind.LANDING_LEGS, 5, 1, false, 1_200, 0, 0, LARGE_TIER);
        RocketParts.declare(id("lunar_lander_descent"), new RocketPart(LANDER_DESCENT.key(),
                RocketPart.Kind.LANDER_DESCENT, 5, 5, 3, false, 2_100, 8_200, Optional.of(new RocketPart.Engine(
                45_000, 300, 345, METHALOX)), 0, LARGE_TIER));
        RocketParts.declare(id("lunar_lander_ascent"), new RocketPart(LANDER_ASCENT.key(),
                RocketPart.Kind.LANDER_ASCENT, 5, 5, 3, false, 2_200, 2_400, Optional.of(new RocketPart.Engine(
                16_000, 290, 340, METHALOX)), 1, LARGE_TIER));
    }

    /** The id of the skeleton's part {@code path}. */
    public static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, path);
    }

    private static void engine(final String path, final TextKey name, final int diameter, final int height,
                               final double mass, final double thrust, final double seaLevelIsp,
                               final double vacuumIsp, final int tier) {
        RocketParts.declare(id(path), new RocketPart(name.key(), RocketPart.Kind.ENGINE, diameter, diameter, height,
                false, mass, 0, Optional.of(new RocketPart.Engine(thrust, seaLevelIsp, vacuumIsp, METHALOX)), 0,
                tier));
    }

    private static void part(final String path, final TextKey name, final RocketPart.Kind kind, final int diameter,
                             final int height, final boolean stretches, final double mass, final double propellant,
                             final int seats, final int tier) {
        RocketParts.declare(id(path), new RocketPart(name.key(), kind, diameter, diameter, height, stretches, mass,
                propellant, Optional.empty(), seats, tier));
    }
}
