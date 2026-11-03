/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.crew;

import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.region.Box;
import dev.jstech.space.ship.ShipFlight;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * A starship's airlock door, as it opens out of the hull: which way is out of it, in the ship's yard. The door itself
 * shuts and opens as a door of its airlock's model does ({@link ShipDoorRules}).
 */
public final class ShipHatches {

    private static final double HALF = 0.5;

    private ShipHatches() {
    }

    /**
     * The way out of {@code ship}'s hull at {@code door}, in its yard: away from the ship's middle line, across its
     * nose, as a door in its side opens; for a door on that line, at its nose or its tail, along the nose. None for a
     * structure that is no ship.
     */
    public static Optional<Direction> outward(final MovingStructure ship, final BlockPos door) {
        if (!(ship.motion() instanceof ShipFlight flight)) {
            return Optional.empty();
        }
        final Box bounds = ship.bounds();
        final double dx = door.getX() + HALF - (bounds.minX() + bounds.maxX() + 1) / 2.0;
        final double dz = door.getZ() + HALF - (bounds.minZ() + bounds.maxZ() + 1) / 2.0;
        final int acrossX = -flight.noseZ();
        final int acrossZ = flight.noseX();
        final double across = dx * acrossX + dz * acrossZ;
        if (Math.abs(across) >= HALF) {
            final int sign = across > 0 ? 1 : -1;
            return Optional.ofNullable(Direction.fromDelta(acrossX * sign, 0, acrossZ * sign));
        }
        final int sign = dx * flight.noseX() + dz * flight.noseZ() > 0 ? 1 : -1;
        return Optional.ofNullable(Direction.fromDelta(flight.noseX() * sign, 0, flight.noseZ() * sign));
    }
}
