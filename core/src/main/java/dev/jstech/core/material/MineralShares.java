/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.material;

import java.util.Optional;

/**
 * The rule a mineral's contents follow: its main element first, then its byproducts, each a share of the mineral's
 * mass, every share above nothing and all of them together no more than the whole. The main element is what the
 * mineral is mined for, which need not be most of it: the sulfur of chalcopyrite weighs more than its copper.
 *
 * <p>Pure: no game types.
 */
public final class MineralShares {

    /* How far over the whole the shares may add up, written as decimals, before they are too much. */
    private static final double SLACK = 1e-9;

    /** What can be wrong with a mineral's shares. */
    public enum Problem {
        /** It contains nothing. */
        NOTHING,
        /** A share is not between nothing and the whole. */
        OUTSIDE_THE_WHOLE,
        /** The shares add up to more than the whole. */
        MORE_THAN_THE_WHOLE
    }

    private MineralShares() {
    }

    /** What is wrong with {@code shares}, main first, or none. */
    public static Optional<Problem> problem(final double... shares) {
        if (shares.length == 0) {
            return Optional.of(Problem.NOTHING);
        }
        double total = 0;
        for (final double share : shares) {
            if (!(share > 0) || share > 1) {
                return Optional.of(Problem.OUTSIDE_THE_WHOLE);
            }
            total += share;
        }
        if (total > 1 + SLACK) {
            return Optional.of(Problem.MORE_THAN_THE_WHOLE);
        }
        return Optional.empty();
    }
}
