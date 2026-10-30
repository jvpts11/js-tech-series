/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.shipyard;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** What a Small Shipyard says to a network that asks it to build: why it will not, or why it stopped. */
@TextHolder
public final class ShipyardTexts {

    public static final TextKey NOT_FORMED = TextKey.of("jsspace.shipyard.not_formed",
            "The shipyard's dock is not formed");
    public static final TextKey NO_PLAN_NAMED = TextKey.of("jsspace.shipyard.no_plan_named",
            "The shipyard keeps no plan named %s");
    public static final TextKey NOTHING_SENT = TextKey.of("jsspace.shipyard.nothing_sent",
            "No plan was sent to the shipyard's dock, and none was named");
    public static final TextKey NOT_READY = TextKey.of("jsspace.shipyard.not_ready",
            "The plan %s is not ready to fly, or does not fit the dock");
    public static final TextKey STOPPED = TextKey.of("jsspace.shipyard.stopped",
            "The shipyard stopped building: its plan was taken back");
    public static final TextKey TAKEN = TextKey.of("jsspace.shipyard.taken",
            "Another request took the shipyard's arms");

    private ShipyardTexts() {
    }
}
