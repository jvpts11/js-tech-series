/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.grid;

/**
 * What a grid carries. Each kind has one grid in each dimension, which every line of that kind shares without the
 * lines ever joining. Only data carries the identity of a network along its grid; the others carry what they carry
 * and nothing else.
 */
public enum GridKind {

    POWER,
    FLUID,
    HEAT,
    GAS,
    MOTION,
    DATA;

    /** Whether the parts of this kind's grid belong to networks, each with its identity. */
    public boolean carriesNetwork() {
        return this == DATA;
    }
}
