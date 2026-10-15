/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.log;

import java.util.Locale;
import java.util.Optional;

/**
 * How much a system writes to the log, from the least to the most: a system at a level writes what is at that level
 * and every level above it, so one at {@link #WARN} writes its warnings and its errors.
 */
public enum LogLevel {

    /** Only what went wrong. */
    ERROR,
    /** What went wrong, and what may go wrong. */
    WARN,
    /** What happens that is worth knowing: the game's own default. */
    INFO,
    /** Enough to follow what the system does, for finding a fault. */
    DEBUG,
    /** Every step it takes. */
    TRACE;

    /** The level as it is written in a command, in lower case. */
    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The level written that way, in any case, or none. */
    public static Optional<LogLevel> byName(final String written) {
        for (final LogLevel level : values()) {
            if (level.name().equalsIgnoreCase(written)) {
                return Optional.of(level);
            }
        }
        return Optional.empty();
    }
}
