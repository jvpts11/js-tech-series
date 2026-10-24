/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.fluid;

import dev.jstech.core.cable.LineBreach;
import dev.jstech.core.material.Material;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;

/**
 * What a pipe is made for: the coldest and the hottest fluid it stands, in kelvin, which of the marks a pipe has to be
 * made for it takes ({@link PipeMarks}: a gas, which only a pipe that holds pressure keeps in; a corrosive fluid), and
 * the marks that wear it, with how many ticks of carrying each it stands. A pipe carries a fluid only when it stands
 * its temperature and takes every mark of it that asks to be made for; matter, which has no temperature of its own in
 * a line, only when it takes its marks. A run of pipes carries what every pipe of it carries. A run whose pipes stand
 * no temperature in common stands none: its hottest lies below its coldest, and it carries nothing.
 *
 * @param coldest the coldest fluid it stands
 * @param hottest the hottest fluid it stands
 * @param takes   the marks it is made for
 * @param wornBy  the marks that wear it, each with the ticks of carrying it stands before it is worn through
 */
public record PipeLimits(int coldest, int hottest, Set<TagKey<Fluid>> takes, Map<TagKey<Fluid>, Integer> wornBy) {

    /** A pipe that stands any temperature, takes no mark and nothing wears. */
    public static final PipeLimits PLAIN = new PipeLimits(0, Integer.MAX_VALUE, Set.of());

    public PipeLimits {
        if (coldest < 0) {
            throw new IllegalArgumentException("nothing is colder than 0 K, and a pipe was said to stand " + coldest);
        }
        takes = Set.copyOf(Objects.requireNonNull(takes, "takes"));
        wornBy = Map.copyOf(Objects.requireNonNull(wornBy, "wornBy"));
        for (final int ticks : wornBy.values()) {
            if (ticks < 1) {
                throw new IllegalArgumentException("a pipe stands a tick at least of what wears it, not " + ticks);
            }
        }
    }

    /** A pipe that stands from {@code coldest} to {@code hottest}, made for {@code takes}, which nothing wears. */
    public PipeLimits(final int coldest, final int hottest, final Set<TagKey<Fluid>> takes) {
        this(coldest, hottest, takes, Map.of());
    }

    /** Whether it carries {@code fluid}. */
    public boolean carries(final Fluid fluid) {
        return beyond(fluid).isEmpty();
    }

    /** Whether it carries matter of {@code material}. */
    public boolean carries(final Material material) {
        return beyond(material).isEmpty();
    }

    /** Why it does not carry {@code fluid}: too hot, too cold, or a mark it is not made for; none when it carries it. */
    public Optional<LineBreach.Reason> beyond(final Fluid fluid) {
        final int temperature = fluid.getFluidType().getTemperature();
        if (temperature > this.hottest) {
            return Optional.of(LineBreach.Reason.TOO_HOT);
        }
        if (temperature < this.coldest) {
            return Optional.of(LineBreach.Reason.TOO_COLD);
        }
        return lacks(mark -> mark.on(fluid));
    }

    /** Why it does not carry matter of {@code material}: a mark it is not made for; none when it carries it. */
    public Optional<LineBreach.Reason> beyond(final Material material) {
        return lacks(mark -> mark.on(material));
    }

    /** How many ticks of carrying {@code fluid} it stands, by the marks of it that wear it; none when nothing does. */
    public OptionalInt wearsIn(final Fluid fluid) {
        return wearsIn(mark -> mark.on(fluid));
    }

    /** How many ticks of carrying matter of {@code material} it stands; none when nothing of it wears it. */
    public OptionalInt wearsIn(final Material material) {
        return wearsIn(mark -> mark.on(material));
    }

    /** Whether anything wears it. */
    public boolean wears() {
        return !this.wornBy.isEmpty();
    }

    /**
     * What a run of this pipe and {@code other} carries: what both stand and both take. Where the temperatures they
     * stand do not meet, the run stands none, so a fluid one pipe stands and the other does not never passes. What
     * wears either wears the run, in the shorter time.
     */
    public PipeLimits and(final PipeLimits other) {
        final Set<TagKey<Fluid>> both = new HashSet<>(this.takes);
        both.retainAll(other.takes);
        final Map<TagKey<Fluid>, Integer> worn = new HashMap<>(this.wornBy);
        other.wornBy.forEach((mark, ticks) -> worn.merge(mark, ticks, Math::min));
        return new PipeLimits(Math.max(this.coldest, other.coldest), Math.min(this.hottest, other.hottest), both,
                worn);
    }

    /* A mark asking to be made for that what {@code has} says has, and this pipe is not made for. */
    private Optional<LineBreach.Reason> lacks(final Predicate<PipeMarks.Mark> has) {
        for (final PipeMarks.Mark mark : PipeMarks.all()) {
            if (mark.kind() == PipeMarks.Kind.MADE_FOR && !this.takes.contains(mark.tag()) && has.test(mark)) {
                return Optional.of(LineBreach.Reason.NOT_MADE_FOR);
            }
        }
        return Optional.empty();
    }

    /* The fewest ticks it stands of the marks wearing it that what {@code has} says has. */
    private OptionalInt wearsIn(final Predicate<PipeMarks.Mark> has) {
        int least = Integer.MAX_VALUE;
        for (final Map.Entry<TagKey<Fluid>, Integer> worn : this.wornBy.entrySet()) {
            final Optional<PipeMarks.Mark> mark = PipeMarks.of(worn.getKey());
            if (mark.isPresent() && has.test(mark.get())) {
                least = Math.min(least, worn.getValue());
            }
        }
        return least == Integer.MAX_VALUE ? OptionalInt.empty() : OptionalInt.of(least);
    }
}
