/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.launcher;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** What the parts of a mobile launcher are called, as the hologram of a rocket being built names them. */
@TextHolder
public final class LauncherTexts {

    public static final TextKey LAUNCHER = TextKey.of("jsspace.launcher.launcher", "Mobile Launcher");
    public static final TextKey TOWER = TextKey.of("jsspace.launcher.tower", "Umbilical Tower");
    public static final TextKey SWING_ARM = TextKey.of("jsspace.launcher.swing_arm", "Swing Arm");
    public static final TextKey CREW_ARM = TextKey.of("jsspace.launcher.crew_arm", "Crew Access Arm");

    private LauncherTexts() {
    }

    /** What {@code piece} is called. */
    public static TextKey of(final LauncherLayout.Piece piece) {
        return switch (piece) {
            case LAUNCHER -> LAUNCHER;
            case TOWER -> TOWER;
            case SWING_ARM -> SWING_ARM;
            case CREW_ARM -> CREW_ARM;
        };
    }
}
