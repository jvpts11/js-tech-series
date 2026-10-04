/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.desktop;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** The words a machine's snapshot puts together for what holds its video memory. */
@TextHolder
final class SettingsSnapshotsTexts {

    /** A big screen, by its monitors' name and how many wide and tall it is. */
    static final TextKey BIG_SCREEN = TextKey.of("jsc.settings_snapshot.big_screen", "%s (%s x %s)");

    private SettingsSnapshotsTexts() {
    }
}
