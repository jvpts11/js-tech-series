/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.vehicle;

import java.util.List;
import net.minecraft.world.phys.Vec3;

/**
 * How a vehicle behaves: where its seats are, how fast it goes and turns, whether it flies, and the energy it holds
 * and spends.
 *
 * @param seats          where each seat is, from the vehicle's middle, facing ahead; the first is the driver's
 * @param maxSpeed       the fastest it goes, in blocks a tick
 * @param acceleration   how much faster it goes each tick it is driven ahead
 * @param turnDegrees    how far it turns each tick it is steered
 * @param climbSpeed     how fast it climbs and sinks, for one that flies; how high it jumps, for one that does not
 * @param flies          whether it flies, free of the pull of the ground
 * @param energyCapacity how much energy its battery holds, in FE; 0 for one that needs none
 * @param energyPerTick  what it spends each tick it is driven
 */
public record VehicleSpec(List<Vec3> seats, double maxSpeed, double acceleration, float turnDegrees,
                          double climbSpeed, boolean flies, int energyCapacity, int energyPerTick) {

    public VehicleSpec {
        seats = List.copyOf(seats);
        if (seats.isEmpty() || maxSpeed <= 0.0 || acceleration <= 0.0 || energyCapacity < 0 || energyPerTick < 0) {
            throw new IllegalArgumentException("a vehicle has a seat, a speed and no less than no energy");
        }
    }
}
