/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

/**
 * How far a program run through a translator gets on the instructions it is dealt: the translator's share of them,
 * the rest going on the translating, as Rosetta's programs ran slower than at home (computers/docs/design/quince.md).
 *
 * <p>What a grant leaves over of an instruction is kept for the next one, so a program at half speed gets through
 * exactly half of what it was dealt in a tick, however the tick was cut into rounds. A program that stops short of its
 * share is charged only for what its share of instructions cost, so the rest of the tick goes to the others.
 */
final class TranslatedPace {

    private final int percent;
    /* Hundredths of an instruction owed to the program from the grants before, never a whole one. */
    private int carried;

    private static final int WHOLE = 100;

    /** A pace of {@code percent} hundredths of the program's own speed, from 1 to 100. */
    TranslatedPace(final int percent) {
        if (percent <= 0 || percent > WHOLE) {
            throw new IllegalArgumentException("a translated program runs at 1 to 100 percent; got " + percent);
        }
        this.percent = percent;
    }

    /** How many instructions the program runs out of a grant of {@code budget}. */
    int share(final int budget) {
        final long owed = (long) Math.max(0, budget) * this.percent + this.carried;
        this.carried = (int) (owed % WHOLE);
        return (int) (owed / WHOLE);
    }

    /**
     * What a grant of {@code budget} costs the machine when the program ran {@code used} of its {@code share}: the
     * whole grant, unless the program stopped short, when it costs what the instructions it did run cost, and nothing
     * is carried past a program that stopped.
     */
    int charged(final int budget, final int share, final int used) {
        if (used >= share) {
            return budget;
        }
        this.carried = 0;
        return (int) Math.min(budget, ((long) Math.max(0, used) * WHOLE + this.percent - 1) / this.percent);
    }
}
