/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core Test Kit.
 */
package dev.jstech.testkit.gametest;

import dev.jstech.core.optional.OptionalDependencyCheck;
import dev.jstech.core.optional.OptionalMod;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

/**
 * Optional mods with and without: a mod's adapter is tested with the other mod present, as the run has it, and with
 * its absence simulated, every guard ({@link OptionalMod}) of it answering that it is not installed, so what the mod
 * does without it is shown in the same run.
 *
 * <pre>{@code
 * final List<Component> lines = OptionalMods.without("jei", () -> kiln.describe());
 * helper.assertTrue(lines.stream().noneMatch(...), "no recipe viewer line without JEI");
 * }</pre>
 *
 * <p>Only what asks a guard while it runs sees the absence: an adapter started as the game loaded stays started.
 * And the import rule that keeps an optional mod from becoming a required one by accident, for a unit test:
 * {@link #violations}.
 */
public final class OptionalMods {

    private OptionalMods() {
    }

    /** What {@code work} gives with {@code modid} seen as absent by every guard, run whole now. */
    public static <T> T without(final String modid, final Supplier<T> work) {
        OptionalMod.seenAbsent(modid, true);
        try {
            return work.get();
        } finally {
            OptionalMod.seenAbsent(modid, false);
        }
    }

    /** Runs {@code work} with {@code modid} seen as absent by every guard, whole, now. */
    public static void without(final String modid, final Runnable work) {
        without(modid, () -> {
            work.run();
            return null;
        });
    }

    /**
     * Every import, in the sources under {@code roots}, of a class of an optional mod whose packages start with one of
     * {@code packagePrefixes} ("mezz.jei.") outside the guarded adapters: what would make that mod a required one.
     */
    public static List<String> violations(final List<String> packagePrefixes, final Collection<Path> roots) {
        return OptionalDependencyCheck.of(packagePrefixes).violations(roots);
    }
}
