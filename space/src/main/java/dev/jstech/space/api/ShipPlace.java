/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.api;

import dev.jstech.core.id.IStableName;

/**
 * Where a part of a starship goes. Its fuselage is a spine of sections from the nose to the tail, joined by the
 * standard joint of its class; the other parts hang on it: the wings root at the second section behind the cockpit,
 * the nacelles go on wings that carry them, the fins and the tail engine at the spine's end, the side intakes along
 * the front, the thrusters round it and the gear under it.
 */
public enum ShipPlace implements IStableName {

    /** The front of the spine: the cockpit. */
    NOSE("nose"),
    /** Anywhere along the spine behind the nose. */
    SPINE("spine"),
    /** The spine's last section only, as a hold whose ramp opens at the tail. */
    TAIL("tail"),
    /** Behind the spine's end: the tail engine and the fins. */
    SPINE_END("spine_end"),
    /** Rooted at the second section behind the cockpit, or at the first if it is alone. */
    WING_ROOT("wing_root"),
    /** On wings that carry nacelles. */
    ON_WINGS("on_wings"),
    /** Along the sides of the front. */
    SIDES("sides"),
    /** Round the ship, at the nose and the tail. */
    AROUND("around"),
    /** Under the ship. */
    UNDER("under");

    private final String serializedName;

    ShipPlace(final String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String serializedName() {
        return this.serializedName;
    }
}
