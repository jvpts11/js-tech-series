/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.machine;

import dev.jstech.core.energy.EnergyClass;
import dev.jstech.core.jstechseries.energy.ConverterLimit;
import dev.jstech.core.jstechseries.energy.GridEquipment;
import dev.jstech.core.jstechseries.energy.JsEnergy;
import dev.jstech.core.jstechseries.energy.VoltageClass;
import dev.jstech.core.jstechseries.tier.IndustrialTier;
import java.util.Objects;
import java.util.Optional;

/**
 * What a piece of J's Industrial's grid equipment says of itself (industrial/docs/design/energy.md): the two
 * neighbouring voltage classes it joins, by the equipment J's Core says joins them, a transformer between two classes
 * of alternating current and an HVDC converter station between the extra-high class and the ultra-high one, which is
 * direct current; its tier; and its ceiling, the ceiling of the higher class it joins.
 *
 * <pre>{@code
 * public static final JoinerRating DISTRIBUTION = JoinerRating.transformer(IndustrialTier.T1, VoltageClass.LOW,
 *         VoltageClass.MEDIUM);
 * }</pre>
 *
 * @param equipment what it is: a transformer or an HVDC converter station
 * @param tier      its tier
 * @param lower     the lower class it joins
 * @param higher    the higher, the next one up
 */
public record JoinerRating(GridEquipment equipment, IndustrialTier tier, VoltageClass lower, VoltageClass higher) {

    public JoinerRating {
        Objects.requireNonNull(equipment, "equipment");
        Objects.requireNonNull(tier, "tier");
        Objects.requireNonNull(lower, "lower");
        Objects.requireNonNull(higher, "higher");
        if (lower.rank() >= higher.rank()) {
            throw new IllegalArgumentException("the lower class comes first: " + lower + " and " + higher);
        }
        final Optional<GridEquipment> joins = lower.joinedTo(higher);
        if (joins.isEmpty() || joins.get() != equipment) {
            throw new IllegalArgumentException("a " + equipment + " does not join " + lower + " and " + higher
                    + joins.map(found -> "; a " + found + " does").orElse("; nothing does: they are not neighbours"));
        }
    }

    /** A transformer of {@code tier}, joining {@code lower} and the next class up, {@code higher}. */
    public static JoinerRating transformer(final IndustrialTier tier, final VoltageClass lower,
                                           final VoltageClass higher) {
        return new JoinerRating(GridEquipment.TRANSFORMER, tier, lower, higher);
    }

    /** An HVDC converter station of {@code tier}, joining the extra-high class and the ultra-high one. */
    public static JoinerRating hvdcStation(final IndustrialTier tier) {
        return new JoinerRating(GridEquipment.HVDC_STATION, tier, VoltageClass.EXTRA_HIGH, VoltageClass.ULTRA_HIGH);
    }

    /** The most it carries, in watts: the ceiling of the higher class it joins. */
    public long watts() {
        return ConverterLimit.ceiling(this.higher);
    }

    /** The most it carries in a tick, in joules. */
    public long joulesPerTick() {
        return IndustrialMachines.joulesPerTick(watts());
    }

    /** The port class of J's Energy of its lower side. */
    public EnergyClass lowerClass() {
        return JsEnergy.classOf(this.lower);
    }

    /** The port class of J's Energy of its higher side. */
    public EnergyClass higherClass() {
        return JsEnergy.classOf(this.higher);
    }
}
