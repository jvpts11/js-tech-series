/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.mechanical;

import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

/**
 * A block that turns with a network of mechanical power: a shaft, a gear, a gearbox, a water wheel, a motor, a
 * machine that turns. It says how each of its faces meets its neighbour, what it weighs in turning and holds back by
 * friction, and what it drives or asks; the level's mechanics ({@link LevelMechanics}) join the parts into networks
 * and tell each how fast it turns. Offered on {@link MechanicalPorts#MECHANICAL}; a block entity that implements it
 * is offered with nothing to register.
 */
public interface IMechanicalPart {

    /** How its {@code face} meets the neighbour on that side. */
    ShaftContact contact(Direction face);

    /** What it weighs in turning, in kilogram square metres. */
    double inertia();

    /** What it holds back by friction, in newton-metres, at its own shaft. */
    double friction();

    /** What it drives or asks at each of its own speeds; none for a part that only turns. */
    default @Nullable ITorque torque() {
        return null;
    }

    /** How fast it turns now, in revolutions a minute, as the level's mechanics last told it. */
    double rpm();

    /** Tells it how fast it turns now, in revolutions a minute; told only when that changes. */
    void turning(double rpm);
}
