/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.line;

import dev.jstech.core.fluid.PipeLimits;
import dev.jstech.core.jstechseries.tier.IndustrialTier;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MatterState;
import java.util.Objects;
import java.util.Set;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;

/**
 * What a tank of J's Industrial says of itself (industrial/docs/design/fluids-and-gases.md): its tier, whether it holds
 * a liquid or a gas, the pressure a gas tank holds its gas at, the marks it is made for (acids, the corrosives, the
 * cryogenic liquids, as a pipe is), and how well it is insulated, which sets how fast a cold liquid boils off in it.
 * Its capacity is real volume: a block is a cubic metre, a thousand millibuckets, and a gas tank holds its volume
 * times its pressure, by the gas law.
 *
 * @param tier       its tier
 * @param state      what it holds: a liquid or a gas
 * @param pressure   for a gas tank, the pressure it holds its gas at, in times the open air's; 1 for a liquid tank
 * @param madeFor    the marks it is made for; what has a mark it is not made for, it refuses
 * @param insulation how much heat its walls let in, in watts a square metre a kelvin ({@code BoilOff.BARE} and on)
 */
public record TankRating(IndustrialTier tier, MatterState state, long pressure, Set<TagKey<Fluid>> madeFor,
                         double insulation) {

    /** The millibuckets of a cubic metre, a block. */
    public static final long CUBIC_METRE = 1_000L;

    public TankRating {
        Objects.requireNonNull(tier, "tier");
        Objects.requireNonNull(state, "state");
        if (state != MatterState.LIQUID && state != MatterState.GAS) {
            throw new IllegalArgumentException("a tank of J's Industrial holds a liquid or a gas, not " + state);
        }
        if (pressure < 1 || state == MatterState.LIQUID && pressure != 1) {
            throw new IllegalArgumentException("a gas tank holds at the open air's pressure at least, a liquid tank "
                    + "at it; got " + pressure);
        }
        madeFor = Set.copyOf(madeFor);
        if (!(insulation >= 0)) {
            throw new IllegalArgumentException("a tank's insulation is no less than nothing, not " + insulation);
        }
    }

    /** A tank of {@code tier} for liquids, insulated as {@code insulation} says, made for {@code marks}. */
    @SafeVarargs
    public static TankRating liquid(final IndustrialTier tier, final double insulation, final TagKey<Fluid>... marks) {
        return new TankRating(tier, MatterState.LIQUID, 1, Set.of(marks), insulation);
    }

    /** A vessel of {@code tier} for gas at {@code pressure} times the open air's, made for {@code marks}. */
    @SafeVarargs
    public static TankRating gas(final IndustrialTier tier, final long pressure, final double insulation,
                                 final TagKey<Fluid>... marks) {
        return new TankRating(tier, MatterState.GAS, pressure, Set.of(marks), insulation);
    }

    /** What it holds with {@code innerBlocks} blocks inside it, in millibuckets: its volume, times its pressure. */
    public long capacity(final long innerBlocks) {
        return Math.multiplyExact(Math.multiplyExact(Math.max(0L, innerBlocks), CUBIC_METRE), this.pressure);
    }

    /** Whether it holds {@code fluid}: of none of the marks it is not made for. */
    public boolean holds(final Fluid fluid) {
        return limits().carries(fluid);
    }

    /** Whether it holds matter of {@code material}: of none of the marks it is not made for. */
    public boolean holds(final Material material) {
        return limits().carries(material);
    }

    /* Its marks as a pipe's, standing any temperature: a tank is made for marks, and a cold one for the cold. */
    private PipeLimits limits() {
        return new PipeLimits(0, Integer.MAX_VALUE, this.madeFor);
    }
}
