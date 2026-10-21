/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.vehicle;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * A vehicle as its parts make it, worked out once whenever they change: its whole mass, its engine and gearbox, its
 * wheels and where they stand, what its tanks and battery hold, its lights, its horn and what its rotors and propellers
 * give. A broken part counts for nothing but its mass.
 *
 * @param mass        its mass with its parts, in kilograms
 * @param engine      its engine, or none
 * @param gearbox     its gearbox, a single gear when it has none
 * @param wheels      its wheels
 * @param tank        what its fuel tanks hold, in millibuckets
 * @param battery     what its batteries hold, in joules
 * @param lights      its lights
 * @param horn        its horn, or none
 * @param lift        what its rotors lift at full power, in newtons
 * @param push        what its propellers push afloat at full power, in newtons
 * @param wheelbase   how far its steering wheels stand ahead of the rest, in metres
 * @param track       how far apart its wheels stand across it, in metres
 */
public record VehicleBuild(double mass, Optional<PartAt<VehiclePartDef.Engine>> engine, VehicleDrive.Gearbox gearbox,
                           List<PartAt<VehiclePartDef.Wheel>> wheels, int tank, int battery,
                           List<PartAt<VehiclePartDef.Light>> lights, Optional<PartAt<VehiclePartDef.Horn>> horn,
                           double lift, double push, double wheelbase, double track) {

    /* A vehicle without wheels is as long as a car between where it would stand on them. */
    private static final double SOME_WHEELBASE = 2.5;
    private static final float BROKEN = 0;

    public VehicleBuild {
        wheels = List.copyOf(wheels);
        lights = List.copyOf(lights);
    }

    /** {@code def} made of the parts in {@code parts}, as the side {@code level} is on knows them. */
    public static VehicleBuild of(final Level level, final VehicleDef def, final FittedParts parts) {
        double mass = def.body().mass();
        PartAt<VehiclePartDef.Engine> engine = null;
        VehicleDrive.Gearbox gearbox = null;
        final List<PartAt<VehiclePartDef.Wheel>> wheels = new ArrayList<>();
        final List<PartAt<VehiclePartDef.Light>> lights = new ArrayList<>();
        PartAt<VehiclePartDef.Horn> horn = null;
        int tank = 0;
        int battery = 0;
        double lift = 0;
        double push = 0;
        for (final VehicleDef.Place place : def.places()) {
            final Optional<FittedParts.Fitted> fitted = parts.at(place.name());
            if (fitted.isEmpty()) {
                continue;
            }
            final Optional<VehiclePartDef> found = VehicleParts.get(level, fitted.get().part());
            if (found.isEmpty() || found.get().kind() != place.kind()) {
                continue;
            }
            final VehiclePartDef part = found.get();
            final float health = fitted.get().health();
            mass += part.mass();
            if (health <= BROKEN) {
                continue;
            }
            switch (part.kind()) {
                case ENGINE -> engine = engine == null ? new PartAt<>(place, part.engine().orElseThrow(), health)
                        : engine;
                case WHEEL -> wheels.add(new PartAt<>(place, part.wheel().orElseThrow(), health));
                case TANK -> tank += part.tank().orElseThrow().capacity();
                case BATTERY -> battery += part.battery().orElseThrow().capacity();
                case GEARBOX -> gearbox = gearbox == null ? part.gearbox().orElseThrow().drive() : gearbox;
                case LIGHT -> lights.add(new PartAt<>(place, part.light().orElseThrow(), health));
                case HORN -> horn = horn == null ? new PartAt<>(place, part.horn().orElseThrow(), health) : horn;
                case ROTOR -> lift += part.rotor().orElseThrow().thrust() * health;
                case PROPELLER -> push += part.propeller().orElseThrow().thrust() * health;
            }
        }
        return new VehicleBuild(mass, Optional.ofNullable(engine),
                gearbox != null ? gearbox : VehicleDrive.Gearbox.SINGLE, wheels, tank, battery, lights,
                Optional.ofNullable(horn), lift, push, wheelbase(wheels), track(wheels));
    }

    /** The fuels its engine burns, by id; none without an engine or for an electric one. */
    public List<ResourceLocation> fuels() {
        return engine.map(at -> at.spec().electric() ? List.<ResourceLocation>of() : at.spec().fuels())
                .orElse(List.of());
    }

    /** Whether its engine draws on its battery. */
    public boolean electric() {
        return engine.map(at -> at.spec().electric()).orElse(false);
    }

    /** The mean radius of its driven wheels, or of all when none is driven, in metres; a hand's span with none. */
    public double drivenRadius() {
        double sum = 0;
        int count = 0;
        for (final PartAt<VehiclePartDef.Wheel> wheel : wheels) {
            if (wheel.place().driven()) {
                sum += wheel.spec().radius();
                count++;
            }
        }
        if (count == 0) {
            for (final PartAt<VehiclePartDef.Wheel> wheel : wheels) {
                sum += wheel.spec().radius();
                count++;
            }
        }
        return count == 0 ? 0.3 : sum / count;
    }

    /** How many of its wheels are driven. */
    public int drivenWheels() {
        int count = 0;
        for (final PartAt<VehiclePartDef.Wheel> wheel : wheels) {
            if (wheel.place().driven()) {
                count++;
            }
        }
        return count;
    }

    private static double wheelbase(final List<PartAt<VehiclePartDef.Wheel>> wheels) {
        if (wheels.size() < 2) {
            return SOME_WHEELBASE;
        }
        double front = -Double.MAX_VALUE;
        double back = Double.MAX_VALUE;
        for (final PartAt<VehiclePartDef.Wheel> wheel : wheels) {
            front = Math.max(front, wheel.place().at().z);
            back = Math.min(back, wheel.place().at().z);
        }
        return front - back > 0.1 ? front - back : SOME_WHEELBASE;
    }

    private static double track(final List<PartAt<VehiclePartDef.Wheel>> wheels) {
        double left = -Double.MAX_VALUE;
        double right = Double.MAX_VALUE;
        for (final PartAt<VehiclePartDef.Wheel> wheel : wheels) {
            left = Math.max(left, wheel.place().at().x);
            right = Math.min(right, wheel.place().at().x);
        }
        return wheels.isEmpty() ? 0 : left - right;
    }

    /**
     * A part in its place.
     *
     * @param place  the place
     * @param spec   what the part does
     * @param health how worn it is
     * @param <S>    the sort of spec
     */
    public record PartAt<S>(VehicleDef.Place place, S spec, float health) {
    }
}
