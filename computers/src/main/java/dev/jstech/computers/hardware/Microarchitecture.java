/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import java.util.Objects;

/**
 * The design of a chip: how its cores carry out instructions, and so how much each core does in a clock.
 *
 * <p>Two processors of one instruction set can be built very differently. A core of the newer design does more in
 * each tick of its clock, which is why clock and core count alone ranked an old high-clock chip above a better one
 * that came after it. The efficiency is that difference as a number: the work of one core in one gigahertz, against
 * the P6 design at 1.0.
 *
 * <p>The name is the one a part's tooltip shows (Haswell, K10, Kepler), or empty for a design the tooltip leaves
 * unnamed. It is a name and not a sentence, so it is the same in every language.
 *
 * @param id              what the design is known by in the code, unique among them
 * @param name            the name a player reads, or empty
 * @param efficiencyMilli the work of one core per gigahertz in thousandths, 1000 being the P6 design's
 * @param coresPerUnit    how many of the cores a part lists make one core of this design's count; 1, except for the
 *                        graphics designs that grouped their shaders in fives and are counted a group at a time
 */
public record Microarchitecture(String id, String name, int efficiencyMilli, int coresPerUnit) {

    public Microarchitecture {
        Objects.requireNonNull(id, "a design must have an id");
        Objects.requireNonNull(name, "a design's name may be empty but not missing");
        if (efficiencyMilli <= 0) {
            throw new IllegalArgumentException("a design does some work per clock; got " + efficiencyMilli);
        }
        if (coresPerUnit <= 0) {
            throw new IllegalArgumentException("a unit is at least one core; got " + coresPerUnit);
        }
    }

    /** A design counted a core at a time. */
    public Microarchitecture(final String id, final String name, final int efficiencyMilli) {
        this(id, name, efficiencyMilli, 1);
    }

    /** How many units of this design a part of {@code cores} listed cores has; never below one. */
    public int unitsOf(final int cores) {
        return Math.max(1, cores / coresPerUnit);
    }
}
