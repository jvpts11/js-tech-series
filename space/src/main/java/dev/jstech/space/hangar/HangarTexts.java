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
 * What a Rocket Hangar's control room tells the player who uses it, and what its terminal tells the network asking it
 * to build a rocket.
 */
@TextHolder
public final class HangarTexts {

    /** The design screen's name. */
    public static final TextKey DESIGN = TextKey.of("jsspace.hangar.design", "Rocket Design");
    /* The Item Port's screen: its name, its hold's label, its display's lines and why it refuses a block. */
    public static final TextKey ITEM_PORT = TextKey.of("jsspace.item_port.title", "Item Port");
    public static final TextKey PORT_HOLD = TextKey.of("jsspace.item_port.hold", "Port hold  -  to the belt");
    public static final TextKey PORT_BLOCKS = TextKey.of("jsspace.item_port.blocks", "%s of %s blocks");
    public static final TextKey PORT_ROW = TextKey.of("jsspace.item_port.row", "%s of %s");
    public static final TextKey PORT_OK = TextKey.of("jsspace.item_port.ok", "ok");
    public static final TextKey PORT_IDLE = TextKey.of("jsspace.item_port.idle", "No ship is being built");
    public static final TextKey PORT_REFUSES = TextKey.of("jsspace.item_port.refuses", "%s does not need it");
    public static final TextKey PORT_NO_DOCK = TextKey.of("jsspace.item_port.no_dock",
            "The port is in no formed dock");
    public static final TextKey NO_HANGAR = TextKey.of("jsspace.hangar.no_hangar",
            "This stands in no formed Rocket Hangar");
    /* What the bay door's console says as it is pressed. */
    public static final TextKey DOOR_OPENING = TextKey.of("jsspace.hangar.door_opening", "Bay door opening");
    public static final TextKey DOOR_SHUTTING = TextKey.of("jsspace.hangar.door_shutting", "Bay door shutting");
    /* Why the hangar will not build, or stopped building, a rocket the network asked for. */
    public static final TextKey NOT_FORMED = TextKey.of("jsspace.hangar.not_formed",
            "The hangar's building is not whole");
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
