/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.machine;

import dev.jstech.core.energy.EnergyClass;
import dev.jstech.core.jstechseries.energy.JsEnergy;
import dev.jstech.core.jstechseries.tier.IndustrialTier;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * What every machine of J's Industrial says of itself: the tier it comes at, which sets its upgrade slots; the voltage
 * class of its port, which its tooltip shows; the power it draws as it works, or a generator makes, in watts; and what
 * it gives off as it works (industrial/docs/design/machines.md).
 *
 * <pre>{@code
 * public static final MachineRating RATING = MachineRating.of(IndustrialTier.T1, JsEnergy.LOW, 4_000);
 * }</pre>
 *
 * @param tier      the tier it comes at
 * @param portClass the voltage class of its port, one of J's Energy's
 * @param watts     the power it draws as it works, or a generator makes at most
 * @param emissions what it gives off as it works
 */
public record MachineRating(IndustrialTier tier, EnergyClass portClass, long watts, List<Emission> emissions) {

    public MachineRating {
        Objects.requireNonNull(tier, "tier");
        Objects.requireNonNull(portClass, "portClass");
        if (!JsEnergy.ENERGY.classes().contains(portClass)) {
            throw new IllegalArgumentException("a machine of J's Industrial has a port of J's Energy, not of "
                    + portClass.path());
        }
        if (watts <= 0) {
            throw new IllegalArgumentException("a machine draws or makes a power above nothing; got " + watts);
        }
        emissions = List.copyOf(emissions);
    }

    /** A machine of {@code tier} on a port of {@code portClass}, of {@code watts}, giving off nothing. */
    public static MachineRating of(final IndustrialTier tier, final EnergyClass portClass, final long watts) {
        return new MachineRating(tier, portClass, watts, List.of());
    }

    /** The same, giving off {@code emission} too as it works. */
    public MachineRating emitting(final Emission emission) {
        final List<Emission> more = new ArrayList<>(this.emissions);
        more.add(Objects.requireNonNull(emission, "emission"));
        return new MachineRating(this.tier, this.portClass, this.watts, more);
    }

    /** The energy it draws or makes in a tick, in joules: its watts over the twenty ticks of a second, rounded up. */
    public long joulesPerTick() {
        return IndustrialMachines.joulesPerTick(this.watts);
    }
}
