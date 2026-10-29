/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.id.IStableName;
import dev.jstech.core.id.StableCodecs;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/**
 * A part of a rocket, a multiblock of one of the standard diameters, and the rocket's delta-v comes from the parts
 * that are there: what it is, how wide at its foot and at its top (an adapter narrows), how tall, whether it is built
 * as tall as the player likes (a tank, a layer at a time), how heavy empty, the propellant it holds, its engine, its
 * seats and its tier. A part built in layers says its mass and propellant for a layer.
 *
 * <pre>{@code
 * {"name": "jsspace.rocket_part.tank", "kind": "tank", "diameter": 3, "height": 1, "stretches": true,
 *  "mass": 300, "propellant": 5800, "tier": 1}
 * {"name": "jsspace.rocket_part.vacuum_engine", "kind": "engine", "diameter": 3, "height": 3, "mass": 1800,
 *  "engine": {"thrust": 1900000, "sea_level_isp": 200, "vacuum_isp": 380, "propellant": "jsspace:methalox"}}
 * }</pre>
 *
 * @param name        the key of its name in the language files
 * @param kind        what it is
 * @param diameter    how wide its foot is, in blocks: one of the {@link #DIAMETERS}
 * @param topDiameter how wide its top is: its foot's, but for an adapter
 * @param height      how tall it is, in blocks; for a part built in layers, a layer's
 * @param stretches   whether it is built in layers, as tall as the player likes
 * @param mass        its mass empty, in kilograms; for a part built in layers, a layer's
 * @param propellant  the propellant it holds, in kilograms; for a part built in layers, a layer's
 * @param engine      its engine, if it has one
 * @param seats       the crew it seats
 * @param tier        the tier its label carries
 */
public record RocketPart(String name, Kind kind, int diameter, int topDiameter, int height, boolean stretches,
                         double mass, double propellant, Optional<Engine> engine, int seats, int tier) {

    /** The standard diameters, in blocks: small rockets, large ones for the Moon and Mars, heavy, super-heavy. */
    public static final List<Integer> DIAMETERS = List.of(3, 5, 7, 9);
    private static final Codec<Integer> DIAMETER = Codec.INT.comapFlatMap(d -> DIAMETERS.contains(d)
            ? DataResult.success(d) : DataResult.error(() -> "a rocket's diameter is one of " + DIAMETERS + ", not "
            + d), d -> d);
    /** As a data pack's file writes one. */
    public static final Codec<RocketPart> CODEC = RecordCodecBuilder.<RocketPart>create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(RocketPart::name),
            Kind.CODEC.fieldOf("kind").forGetter(RocketPart::kind),
            DIAMETER.fieldOf("diameter").forGetter(RocketPart::diameter),
            DIAMETER.optionalFieldOf("top_diameter").forGetter(part -> part.topDiameter() == part.diameter()
                    ? Optional.empty() : Optional.of(part.topDiameter())),
            Codec.intRange(1, 64).fieldOf("height").forGetter(RocketPart::height),
            Codec.BOOL.optionalFieldOf("stretches", false).forGetter(RocketPart::stretches),
            Codec.doubleRange(0, 1e9).fieldOf("mass").forGetter(RocketPart::mass),
            Codec.doubleRange(0, 1e9).optionalFieldOf("propellant", 0.0).forGetter(RocketPart::propellant),
            Engine.CODEC.optionalFieldOf("engine").forGetter(RocketPart::engine),
            Codec.intRange(0, 64).optionalFieldOf("seats", 0).forGetter(RocketPart::seats),
            Codec.intRange(0, 9).optionalFieldOf("tier", 1).forGetter(RocketPart::tier)
    ).apply(instance, (name, kind, diameter, top, height, stretches, mass, propellant, engine, seats, tier) ->
            new RocketPart(name, kind, diameter, top.orElse(diameter), height, stretches, mass, propellant, engine,
                    seats, tier)));

    public RocketPart {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(engine, "engine");
    }

    /** What a part is. */
    public enum Kind implements IStableName {
        /** Combustion chamber, turbopump and nozzle: for the air or for a vacuum. */
        ENGINE("engine"),
        /** The base of a stage, holding one large engine or a cluster of small ones. */
        ENGINE_MOUNT("engine_mount"),
        /** Propellant for its engines, as tall as it is built. */
        TANK("tank"),
        /** A solid booster's segments, stacked, fixed at the side. */
        SOLID_BOOSTER("solid_booster"),
        /** Marks where a stage ends, and drops it. */
        SEPARATION_RING("separation_ring"),
        /** Holds boosters at the side, and drops them. */
        RADIAL_SEPARATOR("radial_separator"),
        /** A ring that narrows, joining two diameters. */
        ADAPTER("adapter"),
        /** The flight computer and the communications antenna. */
        INSTRUMENT_RING("instrument_ring"),
        /** Shelters the payload on the way up, and comes off. */
        FAIRING("fairing"),
        /** Holds satellites, probes or a cargo container. */
        PAYLOAD_ADAPTER("payload_adapter"),
        /** The crew, with windows, a heat shield, parachutes and a docking port. */
        CAPSULE("capsule"),
        /** Pulls the capsule away if the rocket fails on the way up. */
        ESCAPE_TOWER("escape_tower"),
        /** A sealed interior for long missions. */
        HABITATION("habitation"),
        /** Power, manoeuvring propellant and radiators. */
        SERVICE_MODULE("service_module"),
        /** Small jets to turn and dock. */
        CONTROL_THRUSTERS("control_thrusters"),
        /** A lander's descent stage: its legs, engine and tanks, left on the ground. */
        LANDER_DESCENT("lander_descent"),
        /** A lander's ascent stage: its cabin and engine. */
        LANDER_ASCENT("lander_ascent"),
        /** Legs for a stage that comes back and lands. */
        LANDING_LEGS("landing_legs"),
        /** Fins to steer a stage coming back. */
        GRID_FINS("grid_fins"),
        /** Joins a rocket to a station. */
        DOCKING_PORT("docking_port"),
        /** Cargo for the automatic routes. */
        CARGO_CONTAINER("cargo_container");

        public static final Codec<Kind> CODEC = StableCodecs.byName(Kind.class);

        private final String serializedName;

        Kind(final String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String serializedName() {
            return this.serializedName;
        }

        /** Whether a part of this kind ends its stage, dropping with it: a separation ring, a lander's descent. */
        public boolean endsStage() {
            return this == SEPARATION_RING || this == LANDER_DESCENT;
        }
    }

    /**
     * A part's engine: its thrust in a vacuum, how well it burns its propellant in the air and in a vacuum, and what it
     * burns. A sea-level engine's small nozzle does well in the air; a vacuum engine's large one gives more up high and
     * does badly on the ground.
     *
     * @param thrust      its thrust in a vacuum, in newtons
     * @param seaLevelIsp its specific impulse at sea level, in seconds
     * @param vacuumIsp   its specific impulse in a vacuum, in seconds
     * @param propellant  the propellant it burns
     */
    public record Engine(double thrust, double seaLevelIsp, double vacuumIsp, ResourceLocation propellant) {

        public static final Codec<Engine> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.doubleRange(0, 1e12).fieldOf("thrust").forGetter(Engine::thrust),
                Codec.doubleRange(0, 1e6).fieldOf("sea_level_isp").forGetter(Engine::seaLevelIsp),
                Codec.doubleRange(1, 1e6).fieldOf("vacuum_isp").forGetter(Engine::vacuumIsp),
                ResourceLocation.CODEC.fieldOf("propellant").forGetter(Engine::propellant)
        ).apply(instance, Engine::new));

        public Engine {
            Objects.requireNonNull(propellant, "propellant");
        }
    }
}
