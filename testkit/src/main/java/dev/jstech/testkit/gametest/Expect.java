/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core Test Kit.
 */
package dev.jstech.testkit.gametest;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestAssertPosException;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Assertions that explain: each failure says what was looked at, what was expected, what was found and at which
 * block, so a failing run reads as the reason, and the GameTest framework marks the block in the world.
 *
 * <pre>{@code
 * Expect.equal(helper, KILN, "the kiln's output", Items.IRON_INGOT, kiln.output().getItem());
 * Expect.near(helper, KILN, "the kiln's temperature", 900.0, kiln.kelvin(), 5.0);
 * // the kiln's output: expected iron_ingot, found air at 130,-58,40 (relative: 2,2,2) (t=12)
 * }</pre>
 *
 * <p>The positions given are the arena's, as {@code helper.setBlock}'s are.
 */
public final class Expect {

    private Expect() {
    }

    /** That {@code holds}; else a failure saying {@code what} it was and what was {@code found} there. */
    public static void that(final GameTestHelper helper, final BlockPos at, final boolean holds, final String what,
                            final Object found) {
        if (!holds) {
            throw failure(helper, at, what + ": found " + found);
        }
    }

    /** That {@code found} is {@code expected}; else a failure naming both. */
    public static void equal(final GameTestHelper helper, final BlockPos at, final String what, final Object expected,
                             final Object found) {
        if (!Objects.equals(expected, found)) {
            throw failure(helper, at, what + ": expected " + expected + ", found " + found);
        }
    }

    /** That {@code found} is within {@code within} of {@code expected}; else a failure naming both and the gap. */
    public static void near(final GameTestHelper helper, final BlockPos at, final String what, final double expected,
                            final double found, final double within) {
        if (!(Math.abs(found - expected) <= within)) {
            throw failure(helper, at, what + ": expected " + expected + " within " + within + ", found " + found
                    + ", off by " + Math.abs(found - expected));
        }
    }

    /** That {@code found} is at least {@code least}; else a failure naming both. */
    public static void atLeast(final GameTestHelper helper, final BlockPos at, final String what, final double least,
                               final double found) {
        if (!(found >= least)) {
            throw failure(helper, at, what + ": expected at least " + least + ", found " + found);
        }
    }

    /** That {@code found} is at most {@code most}; else a failure naming both. */
    public static void atMost(final GameTestHelper helper, final BlockPos at, final String what, final double most,
                              final double found) {
        if (!(found <= most)) {
            throw failure(helper, at, what + ": expected at most " + most + ", found " + found);
        }
    }

    /** The value {@code found} holds; else a failure saying {@code what} was missing. */
    public static <T> T present(final GameTestHelper helper, final BlockPos at, final String what,
                                final Optional<T> found) {
        return found.orElseThrow(() -> failure(helper, at, what + ": expected one, found none"));
    }

    /** The value {@code found} as a {@code type}; else a failure saying what it was instead. */
    public static <T> T instance(final GameTestHelper helper, final BlockPos at, final String what,
                                 final Class<T> type, final Object found) {
        if (!type.isInstance(found)) {
            throw failure(helper, at, what + ": expected a " + type.getSimpleName() + ", found "
                    + (found == null ? "nothing" : found.getClass().getSimpleName()));
        }
        return type.cast(found);
    }

    /**
     * A failure at the arena's place {@code at}: its message ends with where, in the world and in the arena, and on
     * which tick, and the framework marks the block with it.
     */
    public static GameTestAssertPosException failure(final GameTestHelper helper, final BlockPos at,
                                                     final String message) {
        return new GameTestAssertPosException(message, helper.absolutePos(at), at, helper.getTick());
    }
}
