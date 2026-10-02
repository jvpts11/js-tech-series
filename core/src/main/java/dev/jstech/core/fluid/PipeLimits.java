/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.fluid;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;

/**
 * What a pipe is made for: the coldest and the hottest fluid it stands, in kelvin, and which of the marks a pipe has
 * to be made for it takes (a gas, which only a pipe that holds pressure keeps in; a corrosive fluid). A pipe carries a
 * fluid only when it stands its temperature and takes every mark it has; a run of pipes carries what every pipe of it
 * carries. A run whose pipes stand no temperature in common stands none: its hottest lies below its coldest, and it
 * carries nothing.
 *
 * @param coldest the coldest fluid it stands
 * @param hottest the hottest fluid it stands
 * @param takes   the marks it takes
 */
public record PipeLimits(int coldest, int hottest, Set<TagKey<Fluid>> takes) {

    /** The marks a pipe has to be made for. */
    public static final List<TagKey<Fluid>> MARKS = List.of(CoreFluidTags.GASES, CoreFluidTags.CORROSIVE);
    /** A pipe that stands any temperature and takes no mark. */
    public static final PipeLimits PLAIN = new PipeLimits(0, Integer.MAX_VALUE, Set.of());

    public PipeLimits {
        if (coldest < 0) {
            throw new IllegalArgumentException("nothing is colder than 0 K, and a pipe was said to stand " + coldest);
        }
        takes = Set.copyOf(Objects.requireNonNull(takes, "takes"));
    }

    /** Whether it carries {@code fluid}. */
    public boolean carries(final Fluid fluid) {
        final int temperature = fluid.getFluidType().getTemperature();
        if (temperature < this.coldest || temperature > this.hottest) {
            return false;
        }
        for (final TagKey<Fluid> mark : MARKS) {
            if (fluid.defaultFluidState().is(mark) && !this.takes.contains(mark)) {
                return false;
            }
        }
        return true;
    }

    /**
     * What a run of this pipe and {@code other} carries: what both stand and both take. Where the temperatures they
     * stand do not meet, the run stands none, so a fluid one pipe stands and the other does not never passes.
     */
    public PipeLimits and(final PipeLimits other) {
        final Set<TagKey<Fluid>> both = new HashSet<>(this.takes);
        both.retainAll(other.takes);
        return new PipeLimits(Math.max(this.coldest, other.coldest), Math.min(this.hottest, other.hottest), both);
    }
}
