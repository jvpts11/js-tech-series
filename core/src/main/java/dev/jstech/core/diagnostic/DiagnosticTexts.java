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

/** What the debug screen (F3) says of how long the series' work takes. */
@TextHolder
public final class DiagnosticTexts {

    public static final TextKey TIMINGS = TextKey.of("jscore.diagnostic.timings", "Timings");
    /** A piece of work, then its average and its longest time, in milliseconds. */
    public static final TextKey TIMING = TextKey.of("jscore.diagnostic.timing", "%s: %s ms, at most %s ms");

    private DiagnosticTexts() {
    }
}
