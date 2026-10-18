/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.fire;

import dev.jstech.core.heat.HeatClimate;
import dev.jstech.core.room.Room;
import dev.jstech.core.room.SealedRooms;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * What a sensor feels of the fires near it: how hot they make the air where it is, and how much of their smoke rose
 * to it. Smoke rises, so a fire above a place sends it none; in a sealed room the smoke of every fire in it fills it.
 */
public final class FireSenses {

    /* How far a fire is felt by its heat, and seen by its smoke, in blocks. */
    private static final int HEAT_REACH = 8;
    private static final int SMOKE_REACH = 12;
    /* How much smoke each fire in a sealed room gives the whole room, as a fire's smoke weighs. */
    private static final double ROOM_SMOKE = 0.5;

    private FireSenses() {
    }

    /** How hot the air at {@code pos} is, in kelvin: the place's air, and the heat of the fires near it. */
    public static double heatAt(final ServerLevel level, final BlockPos pos) {
        final double[] rise = {0};
        Fires.of(level).near(pos, HEAT_REACH, fire -> {
            final double metres = Math.sqrt(pos.distSqr(BlockPos.of(fire)));
            if (metres <= HEAT_REACH) {
                rise[0] += FireMath.heatFrom(metres);
            }
        });
        return FireMath.heated(HeatClimate.ambient(level, pos), rise[0]);
    }

    /** How much of the light the smoke at {@code pos} takes away, from 0 to 1. */
    public static double smokeAt(final ServerLevel level, final BlockPos pos) {
        final Optional<Room> room = SealedRooms.roomAt(level, pos);
        if (room.isPresent()) {
            return FireMath.obscuration(room.get().fires().size() * ROOM_SMOKE);
        }
        final double[] smoke = {0};
        Fires.of(level).near(pos, SMOKE_REACH, key -> {
            final BlockPos fire = BlockPos.of(key);
            final int dx = fire.getX() - pos.getX();
            final int dz = fire.getZ() - pos.getZ();
            final int up = pos.getY() - fire.getY();
            if (Math.abs(dx) <= SMOKE_REACH && Math.abs(dz) <= SMOKE_REACH && up <= SMOKE_REACH) {
                smoke[0] += FireMath.smokeFrom(up, Math.sqrt((double) dx * dx + (double) dz * dz));
            }
        });
        return FireMath.obscuration(smoke[0]);
    }
}
