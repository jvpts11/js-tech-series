/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import java.util.Objects;
import org.jetbrains.annotations.ApiStatus;

/**
 * A translator a system carries, which runs the programs built for one instruction set on a processor of another, as
 * Rosetta did across the real switches (computers/docs/design/quince.md): slower than they ran at home, and only on the
 * systems that carry it. It is itself a program of the processor it runs on, so a machine that runs that processor's
 * programs runs it.
 *
 * <p>It runs the programs built for {@code from} and nothing else, not even what {@code from} itself runs: Rosetta 2
 * took the 64-bit Intel programs and left the 32-bit ones behind.
 *
 * <pre>{@code
 * new IsaTranslator("myaddon:bridge", "Bridge", "jsc:x86_64", "myaddon:risc", 70)
 * }</pre>
 *
 * @param id           the translator's id, {@code namespace:path}; systems name it, so it never changes once released
 * @param name         what a person reads, such as {@code Champollion}
 * @param from         the id of the instruction set whose programs it runs
 * @param on           the id of the instruction set it is built for
 * @param speedPercent how fast a program runs through it, in hundredths of the speed it runs at on its own processor
 */
@ApiStatus.Experimental
public record IsaTranslator(String id, String name, String from, String on, int speedPercent) {

    public IsaTranslator {
        Objects.requireNonNull(id, "a translator must have an id");
        Objects.requireNonNull(name, "a translator must have a name");
        Objects.requireNonNull(from, "a translator must say whose programs it runs");
        Objects.requireNonNull(on, "a translator must say what it runs on");
        HardwareIds.requireNamespaced(id, "a translator");
        if (from.equals(on)) {
            throw new IllegalArgumentException("'" + id + "' translates " + from + " to itself");
        }
        if (speedPercent <= 0 || speedPercent > 100) {
            throw new IllegalArgumentException("a translator runs a program at 1 to 100 percent of its speed; got "
                    + speedPercent);
        }
    }

    /** Whether, on a processor of {@code machine}, this runs a program built for {@code built}. */
    public boolean runs(final IsaSpec machine, final String built) {
        return this.from.equals(built) && machine.runs(this.on);
    }
}
