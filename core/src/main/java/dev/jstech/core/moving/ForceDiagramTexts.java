/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** What the diagram of forces of a moving structure, and its keys, say. */
@TextHolder
public final class ForceDiagramTexts {

    public static final TextKey TITLE = TextKey.of("jscore.moving.forces.title", "Forces: %s");
    public static final TextKey VIEW_3D = TextKey.of("jscore.moving.forces.view_3d", "3D");
    public static final TextKey VIEW_SIDE = TextKey.of("jscore.moving.forces.view_side", "Side");
    public static final TextKey VIEW_TOP = TextKey.of("jscore.moving.forces.view_top", "Top");
    public static final TextKey APART = TextKey.of("jscore.moving.forces.apart", "Separate");
    public static final TextKey SUMMED = TextKey.of("jscore.moving.forces.summed", "Summed");
    public static final TextKey HEAD = TextKey.of("jscore.moving.forces.head", "Forces");
    public static final TextKey HINT = TextKey.of("jscore.moving.forces.hint", "each one can be turned off");
    public static final TextKey GRAVITY = TextKey.of("jscore.moving.forces.gravity", "Gravity");
    public static final TextKey BUOYANCY = TextKey.of("jscore.moving.forces.buoyancy", "Buoyancy");
    public static final TextKey THRUST = TextKey.of("jscore.moving.forces.thrust", "Thrust");
    public static final TextKey DRAG = TextKey.of("jscore.moving.forces.drag", "Drag");
    public static final TextKey LIFT = TextKey.of("jscore.moving.forces.lift", "Lift");
    public static final TextKey LEVITATION = TextKey.of("jscore.moving.forces.levitation", "Levitation");
    public static final TextKey MAGNETISM = TextKey.of("jscore.moving.forces.magnetism", "Magnetism");
    public static final TextKey WHEELS = TextKey.of("jscore.moving.forces.wheels", "Ground");
    public static final TextKey HELD = TextKey.of("jscore.moving.forces.held", "Held");
    public static final TextKey SUM = TextKey.of("jscore.moving.forces.sum", "Sum");
    public static final TextKey LABEL = TextKey.of("jscore.moving.forces.label", "%s %s");
    public static final TextKey CENTRE = TextKey.of("jscore.moving.forces.centre", "centre of mass");
    public static final TextKey UP = TextKey.of("jscore.moving.forces.up", "up");
    public static final TextKey FORWARD = TextKey.of("jscore.moving.forces.forward", "fwd");
    public static final TextKey HEIGHT = TextKey.of("jscore.moving.forces.height", "%s m");
    public static final TextKey FAR = TextKey.of("jscore.moving.forces.far", "over %s m");
    public static final TextKey WAITING = TextKey.of("jscore.moving.forces.waiting", "Reading its forces");
    public static final TextKey MASS_TILE = TextKey.of("jscore.moving.forces.tile.mass", "MASS");
    public static final TextKey SUM_TILE = TextKey.of("jscore.moving.forces.tile.sum", "SUM");
    public static final TextKey CLIMB_TILE = TextKey.of("jscore.moving.forces.tile.climb", "CLIMB");
    public static final TextKey SPEED_TILE = TextKey.of("jscore.moving.forces.tile.speed", "SPEED");
    public static final TextKey AIR_TILE = TextKey.of("jscore.moving.forces.tile.air", "AIR");
    public static final TextKey NEWTONS = TextKey.of("jscore.moving.forces.newtons", "%s %sN");
    public static final TextKey TONNES = TextKey.of("jscore.moving.forces.tonnes", "%s t");
    public static final TextKey KILOGRAMS = TextKey.of("jscore.moving.forces.kilograms", "%s kg");
    public static final TextKey CLIMBING = TextKey.of("jscore.moving.forces.climbing", "%s m/s");
    public static final TextKey GOING = TextKey.of("jscore.moving.forces.going", "%s m/s %s");
    public static final TextKey AIR = TextKey.of("jscore.moving.forces.air", "%s kg/m³");
    public static final TextKey SUM_WAY = TextKey.of("jscore.moving.forces.sum_way", "%s, %s");
    public static final TextKey WAYS = TextKey.of("jscore.moving.forces.ways", "%s and %s");
    public static final TextKey WAY_UP = TextKey.of("jscore.moving.forces.way.up", "up");
    public static final TextKey WAY_DOWN = TextKey.of("jscore.moving.forces.way.down", "down");
    public static final TextKey WAY_AHEAD = TextKey.of("jscore.moving.forces.way.ahead", "ahead");
    public static final TextKey WAY_BACK = TextKey.of("jscore.moving.forces.way.back", "back");
    public static final TextKey WAY_LEFT = TextKey.of("jscore.moving.forces.way.left", "left");
    public static final TextKey WAY_RIGHT = TextKey.of("jscore.moving.forces.way.right", "right");
    public static final TextKey STILL = TextKey.of("jscore.moving.forces.still", "still");
    public static final TextKey RESET = TextKey.of("jscore.moving.forces.reset", "Reset the view");
    public static final TextKey DONE = TextKey.of("jscore.moving.forces.done", "Done");
    public static final TextKey KEY = TextKey.of("jscore.moving.forces.key", "Show a moving structure's forces");
    public static final TextKey DEBUG_KEY = TextKey.of("jscore.moving.forces.debug_key",
            "Show the physics of moving structures");
    public static final TextKey NOTHING_SEEN = TextKey.of("jscore.moving.forces.nothing_seen",
            "Stand aboard a moving structure, sit in one, or look at one to see its forces");

    private ForceDiagramTexts() {
    }
}
