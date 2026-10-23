/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.chemistry;

import java.util.Optional;

/**
 * The groups the rare earths are separated in, and the stages of solvent extraction each takes: the more alike the
 * neighbours, the longer the cascade, as in real plants, where the number of stages decides the separation. The
 * numbers are estimates (industrial/docs/design/chemistry.md).
 *
 * <p>Pure: no game types.
 */
public enum RareEarthCascade {
    /** Lanthanum to neodymium. */
    LIGHT(4),
    /** Samarium to gadolinium. */
    MIDDLE(8),
    /** Terbium to lutetium, and yttrium. */
    HEAVY(16);

    private final int stages;

    RareEarthCascade(final int stages) {
        this.stages = stages;
    }

    /** The stages it takes to separate the group. */
    public int stages() {
        return this.stages;
    }

    /** The group a cascade of {@code stages} separates up to, the lightest needing the fewest; none below four. */
    public static Optional<RareEarthCascade> separates(final int stages) {
        RareEarthCascade reached = null;
        for (final RareEarthCascade group : values()) {
            if (stages >= group.stages) {
                reached = group;
            }
        }
        return Optional.ofNullable(reached);
    }
}
