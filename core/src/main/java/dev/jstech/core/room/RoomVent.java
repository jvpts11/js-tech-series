/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.room;

import dev.jstech.core.material.Material;
import dev.jstech.core.material.Materials;
import dev.jstech.core.matter.IMatterHandler;
import dev.jstech.core.matter.MatterStack;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

/**
 * A vent joining a room to the gas lines, for a block of any mod: each tick its block entity fills the room beside it
 * from its tank, or empties the room into it, as fast as it says. Gas in a tank is counted in millibuckets at the open
 * air's pressure, a mole to 22.4 of them; gas in a room in moles, by the material's id.
 *
 * <pre>{@code
 * RoomVent.fill(level, pos, tank, 100);     // up to 100 mB a tick of whatever the tank holds into the room
 * RoomVent.empty(level, pos, tank, 100);    // up to 100 mB a tick of the room's air into the tank
 * }</pre>
 */
public final class RoomVent {

    /** How many millibuckets of gas at the open air's pressure make a mole. */
    public static final double MILLIBUCKETS_PER_MOLE = 22.4;

    private RoomVent() {
    }

    /**
     * Fills the room beside {@code vent} with up to {@code millibuckets} of whatever gas {@code tank} holds; how many
     * millibuckets went in. Nothing goes where no room is.
     */
    public static long fill(final ServerLevel level, final BlockPos vent, final IMatterHandler tank,
                            final long millibuckets) {
        if (millibuckets <= 0 || SealedRooms.roomBeside(level, vent).isEmpty()) {
            return 0;
        }
        final Optional<MatterStack> out = tank.extractAny(millibuckets, false);
        if (out.isEmpty()) {
            return 0;
        }
        final MatterStack gas = out.get();
        SealedRooms.give(level, vent, gas.material().id(), gas.amount() / MILLIBUCKETS_PER_MOLE);
        return gas.amount();
    }

    /**
     * Empties up to {@code millibuckets} of the air of the room beside {@code vent} into {@code tank}, each gas in its
     * share of the air, as far as the tank takes it; how many millibuckets went in.
     */
    public static long empty(final ServerLevel level, final BlockPos vent, final IMatterHandler tank,
                             final long millibuckets) {
        final Optional<Room> found = SealedRooms.roomBeside(level, vent);
        if (millibuckets <= 0 || found.isEmpty()) {
            return 0;
        }
        final Room room = found.get();
        final RoomAir air = room.air();
        long moved = 0;
        for (final Map.Entry<ResourceLocation, Double> gas : air.moles().entrySet()) {
            final Optional<Material> material = Materials.byId(gas.getKey());
            final long wanted = (long) Math.floor(millibuckets * air.share(gas.getKey()));
            final long held = (long) Math.floor(gas.getValue() * MILLIBUCKETS_PER_MOLE);
            final long amount = Math.min(wanted, held);
            if (material.isEmpty() || amount <= 0) {
                continue;
            }
            final long taken = tank.insert(new MatterStack(material.get(), amount), false);
            room.take(gas.getKey(), taken / MILLIBUCKETS_PER_MOLE);
            moved += taken;
        }
        return moved;
    }
}
