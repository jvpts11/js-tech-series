/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.mechanical;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/**
 * A block that turns with a network of mechanical power: a shaft, a gear, a gearbox, a water wheel, a motor, a
 * machine that turns. It says how each of its faces meets its neighbour and what it is joined to across a distance,
 * what it weighs in turning and holds back by friction, what it drives or asks, and the most torque it carries; the
 * level's mechanics ({@link LevelMechanics}) join the parts into networks and tell each how fast it turns. Offered on
 * {@link MechanicalPorts#MECHANICAL}; a block entity that implements it is offered with nothing to register.
 */
public interface IMechanicalPart {

    /** How its {@code face} meets the neighbour on that side. */
    ShaftContact contact(Direction face);

    /** The parts it is joined to across a distance, by a belt or a chain; none unless it says. */
    default List<ShaftJoin> joins() {
        return List.of();
    }

    /** What it weighs in turning, in kilogram square metres. */
    double inertia();

    /** What it holds back by friction, in newton-metres, at its own shaft. */
    double friction();

    /** What it drives or asks at each of its own speeds; none for a part that only turns. */
    default @Nullable ITorque torque() {
        return null;
    }

    /**
     * The most torque it carries through it, in newton-metres, before {@link #overloaded} is called; none for no
     * limit. Only a part every path of its network crosses carries a torque that can be told.
     */
    default double rating() {
        return 0.0;
    }

    /** The torque through it passed its rating: a shear pin shears, a part breaks where the server lets it. */
    default void overloaded(final ServerLevel level, final BlockPos pos) {
    }

    /** Whether it measures the torque through it, as a torque meter does, and is told it each tick. */
    default boolean measures() {
        return false;
    }

    /** Tells a part that measures the torque through it now, in newton-metres, where it can be told. */
    default void carrying(final double newtonMetres) {
    }

    /** How fast it turns now, in revolutions a minute, as the level's mechanics last told it. */
    double rpm();

    /** Tells it how fast it turns now, in revolutions a minute; told only when that changes. */
    void turning(double rpm);
}
