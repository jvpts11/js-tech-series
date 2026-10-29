/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.hangar;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** What a Rocket Hangar's terminal tells the player who uses it: its bay's size, or what keeps it from forming. */
@TextHolder
public final class HangarTexts {

    public static final TextKey FORMED = TextKey.of("jsspace.hangar.formed",
            "Rocket Hangar formed: a bay %s by %s, %s high");
    public static final TextKey NOT_YET = TextKey.of("jsspace.hangar.not_yet",
            "The hangar has not looked at its bay yet");
    public static final TextKey NO_BAY = TextKey.of("jsspace.hangar.no_bay",
            "The terminal looks at a wall: set it in a wall, facing into the bay");
    public static final TextKey TOO_SMALL = TextKey.of("jsspace.hangar.too_small",
            "The bay is %s by %s, %s high: it must be at least %s across and deep, and %s high");
    public static final TextKey TOO_LARGE = TextKey.of("jsspace.hangar.too_large",
            "The bay is larger than the server allows: at most %s across and deep, and %s high");
    public static final TextKey OPEN = TextKey.of("jsspace.hangar.open",
            "The bay's walls, floor or roof are open at %s %s %s");
    public static final TextKey NO_RAILS = TextKey.of("jsspace.hangar.no_rails",
            "No line of Crane Rail runs along the top of two facing walls");
    public static final TextKey NO_BRIDGE = TextKey.of("jsspace.hangar.no_bridge",
            "No Crane Bridge spans the bay from rail to rail");
    public static final TextKey NO_HOIST = TextKey.of("jsspace.hangar.no_hoist",
            "No Crane Hoist hangs under the bridge");
    public static final TextKey NO_DOOR = TextKey.of("jsspace.hangar.no_door",
            "No Bay Door stands in a wall on the floor");

    private HangarTexts() {
    }
}
