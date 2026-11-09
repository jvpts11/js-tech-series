/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;

/**
 * Which of a program's builds a machine runs, and how fast: a program is built for one instruction set, or, as a
 * universal build, for several at once (computers/docs/design/quince.md), and a machine runs the one its processor
 * understands, or one a translator of its system carries, more slowly.
 *
 * @param build        which of the builds, counted from 0 in the order the program carries them
 * @param speedPercent how fast it runs, in hundredths of its own speed: {@link #FULL_SPEED} unless translated
 * @param through      the translator it runs through, or null when the processor runs it as it is
 */
public record IsaFit(int build, int speedPercent, @Nullable IsaTranslator through) {

    /** The speed of a program the processor runs as it is. */
    public static final int FULL_SPEED = 100;

    /**
     * The build of {@code builds} a processor of {@code machine} runs, given the translators its system carries.
     *
     * <p>One the processor runs as it is comes first, and of those the one built for that very processor, since a
     * universal build that also carries an older one of the line was given the newer for a reason. Only when none runs
     * is a translator asked, and the fastest one that carries a build is the one taken.
     *
     * @param builds      the instruction set ids the program was built for, in the order it carries them
     * @param translators what the machine's system carries
     * @return the build it runs, or nothing when it runs none of them
     */
    public static Optional<IsaFit> of(final IsaSpec machine, final List<String> builds,
                                      final List<IsaTranslator> translators) {
        int asItIs = -1;
        for (int i = 0; i < builds.size(); i++) {
            if (builds.get(i).equals(machine.id())) {
                return Optional.of(new IsaFit(i, FULL_SPEED, null));
            }
            if (asItIs < 0 && machine.runs(builds.get(i))) {
                asItIs = i;
            }
        }
        if (asItIs >= 0) {
            return Optional.of(new IsaFit(asItIs, FULL_SPEED, null));
        }
        IsaFit best = null;
        for (int i = 0; i < builds.size(); i++) {
            for (final IsaTranslator translator : translators) {
                if (translator.runs(machine, builds.get(i))
                        && (best == null || translator.speedPercent() > best.speedPercent())) {
                    best = new IsaFit(i, translator.speedPercent(), translator);
                }
            }
        }
        return Optional.ofNullable(best);
    }

    /** Whether the program runs through a translator rather than as it is. */
    public boolean translated() {
        return this.through != null;
    }
}
