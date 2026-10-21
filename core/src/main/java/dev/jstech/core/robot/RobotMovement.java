/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.robot;

/** How a robot gets about: its paths, and what holds it up. */
public enum RobotMovement {

    /** On its feet or wheels, along the ground, as a person walks. */
    WALK,
    /** In the air, in three dimensions, held up by its rotors or its jets: it never falls. */
    FLY,
    /** In the water, in three dimensions, and on land between waters. */
    SWIM
}
