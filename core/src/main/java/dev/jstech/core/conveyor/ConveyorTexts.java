/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.conveyor;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** What the Core's conveyors say to a player. */
@TextHolder
public final class ConveyorTexts {

    /** The items a diverter turns aside, named one after another: "Diverts: Iron Ingot, Gold Ingot". */
    public static final TextKey DIVERTS = TextKey.of("jscore.conveyor.diverts", "Diverts: %s");
    /** A diverter whose filter names nothing, so everything goes straight on. */
    public static final TextKey DIVERTS_NOTHING = TextKey.of("jscore.conveyor.diverts_nothing",
            "Diverts nothing");
    /** The side a diverter turns its items to, after a wrench flips it. */
    public static final TextKey DIVERTS_LEFT = TextKey.of("jscore.conveyor.diverts_left", "Diverts to the left");
    /** The other side. */
    public static final TextKey DIVERTS_RIGHT = TextKey.of("jscore.conveyor.diverts_right", "Diverts to the right");

    private ConveyorTexts() {
    }
}
