/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.live;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** What the action bar says while the crosshair is on a part of a screen in the world that a press works. */
@TextHolder
public final class TouchTexts {

    /** The key that presses, and what a press there does: "Right click: Start". */
    public static final TextKey HINT = TextKey.of("hud.jscore.touch.hint", "%s: %s");
    /** The use key when it is on the right mouse button, as the hint names it. */
    public static final TextKey RIGHT_CLICK = TextKey.of("hud.jscore.touch.right_click", "Right click");

    private TouchTexts() {
    }
}
