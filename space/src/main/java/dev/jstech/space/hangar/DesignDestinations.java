/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.hangar;

import dev.jstech.core.text.TextKey;
import dev.jstech.space.flight.TransferPlan;
import dev.jstech.space.orbit.Rocketry;
import dev.jstech.space.universe.SolarBody;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Where a rocket designed on a world can go, and the delta-v each asks for, summed from the legs of the trip as the
 * Flight Computer plans them: the climb off the world it leaves to a low orbit; for another world, the transfer to it
 * in its window; and the burn down to its ground where it has no air to brake in, as the Moon has none. From the
 * Overworld, a low orbit asks 9,400 m/s, the Moon 15,200, Mars 13,000.
 */
public final class DesignDestinations {

    /* The worlds a rocket can go to, in the order the screen lists them. */
    private static final List<SolarBody> WORLDS = List.of(SolarBody.OVERWORLD, SolarBody.MOON, SolarBody.MARS);

    private DesignDestinations() {
    }

    /** The destinations of a rocket that leaves {@code from}: its low orbit first, then the other worlds. */
    public static List<Reach> from(final SolarBody from) {
        final double climb = climb(from);
        final List<Reach> reaches = new ArrayList<>();
        reaches.add(new Reach(DesignTexts.LOW_ORBIT, Optional.empty(), climb));
        for (final SolarBody to : WORLDS) {
            final Optional<TransferPlan> plan = TransferPlan.of(from, to, 0);
            if (plan.isPresent()) {
                final double landing = plan.get().aerobraking() ? 0 : climb(to);
                reaches.add(new Reach(name(to), Optional.of(to), climb + plan.get().deltaVWindow() + landing));
            }
        }
        return List.copyOf(reaches);
    }

    /* What it costs to climb from a world's ground to its low orbit, and to come back down where there is no air. */
    private static double climb(final SolarBody body) {
        return switch (body) {
            case OVERWORLD -> Rocketry.SURFACE_TO_LOW_ORBIT;
            case MOON -> Rocketry.MOON_ORBIT_TO_SURFACE;
            case MARS -> Rocketry.MARS_SURFACE_TO_ORBIT;
        };
    }

    private static TextKey name(final SolarBody body) {
        return switch (body) {
            case OVERWORLD -> DesignTexts.OVERWORLD;
            case MOON -> DesignTexts.MOON;
            case MARS -> DesignTexts.MARS;
        };
    }

    /**
     * A destination, and what it asks.
     *
     * @param name  its name
     * @param world the world it is, or none for the low orbit of the world left
     * @param need  the delta-v it asks for, from the ground of the world left, in metres a second
     */
    public record Reach(TextKey name, Optional<SolarBody> world, double need) {

        /** How far short of it {@code deltaV} falls, in metres a second; nothing when it reaches. */
        public double shortBy(final double deltaV) {
            return Math.max(0, this.need - deltaV);
        }
    }
}
