/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.cable;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** What a cable's item says of how far the cable reaches, read from the cable itself. */
@TextHolder
public final class CableTexts {

    /** How many cables one run of it may be: past that the run carries nothing. */
    public static final TextKey REACH = TextKey.of("jscore.cable.reach", "Reaches %s cables in one run");
    /** A cable with no limit on the length of a run. */
    public static final TextKey ANY_LENGTH = TextKey.of("jscore.cable.any_length", "Reaches any length");

    private CableTexts() {
    }
}
