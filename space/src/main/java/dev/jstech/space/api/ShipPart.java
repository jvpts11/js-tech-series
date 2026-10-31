/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.id.StableCodecs;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/**
 * A part of a starship, as a data pack's file or a declaration says it: a multiblock of its own, of one class, going in
 * one place on the ship; a fuselage section's length along the spine; how much of the hull's room a module takes;
 * whether it holds an airlock, or, for wings, whether they carry nacelles; its maker, which gives it its shape and its
 * interior; its figures, what it weighs, pushes, lifts, makes or takes of power, and whom it seats and beds; and, for
 * gear, whether it rolls.
 *
 * @param name      its name's key
 * @param kind      what it is
 * @param shipClass the class of ship it is for
 * @param place     where it goes
 * @param length    how long it is along the spine, in blocks; nought for a part that hangs on it
 * @param room      how much of the hull's room it takes: one for a small module, three for a medium one
 * @param airlock   whether it holds an airlock
 * @param carries   for wings, whether they carry nacelles
 * @param maker     its maker's id among the ship makers, if it has one
 * @param figures   its figures
 * @param rolls     for gear, whether it rolls on wheels, on the land and a runway; feet stand where they come down
 */
public record ShipPart(String name, ShipPartKind kind, ShipClass shipClass, ShipPlace place, double length, int room,
                       boolean airlock, boolean carries, Optional<ResourceLocation> maker, Figures figures,
                       boolean rolls) {

    /** As a data pack's file writes one. */
    public static final Codec<ShipPart> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(ShipPart::name),
            StableCodecs.byName(ShipPartKind.class).fieldOf("kind").forGetter(ShipPart::kind),
            StableCodecs.byName(ShipClass.class).fieldOf("class").forGetter(ShipPart::shipClass),
            StableCodecs.byName(ShipPlace.class).fieldOf("place").forGetter(ShipPart::place),
            Codec.doubleRange(0, 64).optionalFieldOf("length", 0.0).forGetter(ShipPart::length),
            Codec.intRange(0, 64).optionalFieldOf("room", 0).forGetter(ShipPart::room),
            Codec.BOOL.optionalFieldOf("airlock", false).forGetter(ShipPart::airlock),
            Codec.BOOL.optionalFieldOf("carries", false).forGetter(ShipPart::carries),
            ResourceLocation.CODEC.optionalFieldOf("maker").forGetter(ShipPart::maker),
            Figures.CODEC.optionalFieldOf("figures", Figures.NONE).forGetter(ShipPart::figures),
            Codec.BOOL.optionalFieldOf("rolls", false).forGetter(ShipPart::rolls)
    ).apply(instance, ShipPart::new));

    public ShipPart {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(shipClass, "shipClass");
        Objects.requireNonNull(place, "place");
        Objects.requireNonNull(maker, "maker");
        Objects.requireNonNull(figures, "figures");
    }

    /** A part that says no figures. */
    public ShipPart(final String name, final ShipPartKind kind, final ShipClass shipClass, final ShipPlace place,
                    final double length, final int room, final boolean airlock, final boolean carries,
                    final Optional<ResourceLocation> maker) {
        this(name, kind, shipClass, place, length, room, airlock, carries, maker, Figures.NONE);
    }

    /** A part that does not roll. */
    public ShipPart(final String name, final ShipPartKind kind, final ShipClass shipClass, final ShipPlace place,
                    final double length, final int room, final boolean airlock, final boolean carries,
                    final Optional<ResourceLocation> maker, final Figures figures) {
        this(name, kind, shipClass, place, length, room, airlock, carries, maker, figures, false);
    }

    /**
     * What a part weighs, pushes, lifts, makes or takes of power, and whom it seats and beds; the shipyard's builder
     * adds them up into the ship's, and the ship flies by them.
     *
     * @param mass   its mass empty, in kilograms
     * @param thrust what its main engines push forward, in newtons
     * @param lift   what its air engines lift, their nozzles turned down, in newtons; they push as hard forward
     * @param power  the power it makes, in watts, or, below nought, the power it takes
     * @param seats  the crew it seats
     * @param beds   the crew it beds
     * @param wing   the weight its wings hold up at {@link #WING_SPEED} in the overworld's air at sea level, in
     *               newtons: more as the square of the speed, less in thinner air
     */
    public record Figures(double mass, double thrust, double lift, double power, int seats, int beds, double wing) {

        /** The speed a wing's figure is given at, in metres a second. */
        public static final double WING_SPEED = 20.0;
        public static final Figures NONE = new Figures(0, 0, 0, 0, 0, 0, 0);
        public static final Codec<Figures> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.doubleRange(0, Double.MAX_VALUE).optionalFieldOf("mass", 0.0).forGetter(Figures::mass),
                Codec.doubleRange(0, Double.MAX_VALUE).optionalFieldOf("thrust", 0.0).forGetter(Figures::thrust),
                Codec.doubleRange(0, Double.MAX_VALUE).optionalFieldOf("lift", 0.0).forGetter(Figures::lift),
                Codec.DOUBLE.optionalFieldOf("power", 0.0).forGetter(Figures::power),
                Codec.intRange(0, 64).optionalFieldOf("seats", 0).forGetter(Figures::seats),
                Codec.intRange(0, 64).optionalFieldOf("beds", 0).forGetter(Figures::beds),
                Codec.doubleRange(0, Double.MAX_VALUE).optionalFieldOf("wing", 0.0).forGetter(Figures::wing)
        ).apply(instance, Figures::new));

        /** The figures of a part with no wings. */
        public Figures(final double mass, final double thrust, final double lift, final double power,
                       final int seats, final int beds) {
            this(mass, thrust, lift, power, seats, beds, 0);
        }
    }
}
