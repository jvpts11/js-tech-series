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

/**
 * What a Rocket Hangar's terminal tells the player who uses it, its bay's size or what keeps it from forming; and what
 * it tells the network asking it to build a rocket.
 */
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
    /** The design screen's name. */
    public static final TextKey DESIGN = TextKey.of("jsspace.hangar.design", "Rocket Design");
    public static final TextKey NO_HANGAR = TextKey.of("jsspace.hangar.no_hangar",
            "This console stands in no formed Rocket Hangar's bay");
    /* Why the hangar will not build, or stopped building, a rocket the network asked for. */
    public static final TextKey NOT_FORMED = TextKey.of("jsspace.hangar.not_formed",
            "The hangar's bay is not formed");
    public static final TextKey NO_PLAN_NAMED = TextKey.of("jsspace.hangar.no_plan_named",
            "The hangar keeps no plan named %s");
    public static final TextKey NOTHING_SENT = TextKey.of("jsspace.hangar.nothing_sent",
            "No plan was sent to the hangar's bay, and none was named");
    public static final TextKey NOT_READY = TextKey.of("jsspace.hangar.not_ready",
            "The plan %s is not ready to fly from this bay");
    public static final TextKey STOPPED = TextKey.of("jsspace.hangar.stopped",
            "The hangar stopped building: its bay came apart, or its plan was taken back");
    public static final TextKey TAKEN = TextKey.of("jsspace.hangar.taken",
            "Another request took the hangar's crane");

    private HangarTexts() {
    }
}
