/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.diagnostic;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * What the debug screen (F3) says of how long the series' work takes, and what the operators are told when a part of
 * it stalls or slows itself.
 */
@TextHolder
public final class DiagnosticTexts {

    public static final TextKey TIMINGS = TextKey.of("jscore.diagnostic.timings", "Timings");
    /** A piece of work, then its average and its longest time, in milliseconds. */
    public static final TextKey TIMING = TextKey.of("jscore.diagnostic.timing", "%s: %s ms, at most %s ms");
    /* The section, and how long it had run, in milliseconds. */
    public static final TextKey STALLED = TextKey.of("jscore.diagnostic.stalled",
            "%s has run for %s ms without ending; what it was doing is in the server's log");
    /* The section, what it takes a tick, its budget, and how many ticks it now spreads its work over. */
    public static final TextKey SLOWED = TextKey.of("jscore.diagnostic.slowed",
            "%s takes %s ms a tick against its budget of %s ms, and now runs one tick in %s");
    public static final TextKey RECOVERED = TextKey.of("jscore.diagnostic.recovered",
            "%s is within its budget again and runs every tick");
    /* What went wrong. */
    public static final TextKey HEALTH_FAILED = TextKey.of("jscore.health.failed", "Its state could not be read: %s");

    private DiagnosticTexts() {
    }
}
