/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** What CDE's splash screen says while the desktop is coming up. Its own name and its maker's are data. */
@TextHolder
final class CdeSplashArtTexts {

    static final TextKey STARTING = TextKey.of("jsc.cde.splash.starting", "Starting the desktop...");
    static final TextKey STARTING_ON_WORKSTATION =
            TextKey.of("jsc.cde.splash.starting_on_workstation", "Starting the desktop on workstation %s...");

    private CdeSplashArtTexts() {
    }
}
