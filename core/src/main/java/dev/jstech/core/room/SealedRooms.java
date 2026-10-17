/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.room;

import dev.jstech.core.air.AirMix;
import dev.jstech.core.dimension.Climate;
import dev.jstech.core.dimension.DimensionRulesData;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Sealed rooms, for a mod: the room at a place, its air, gas put into it and taken out of it, and asking for the room
 * at a place to be found and kept. A room is found where a block asks for one ({@link #track}): a vent, a detector, an
 * oxygen generator; from then on it is worked out again whenever a block near it changes. What happens to rooms is
 * posted as a {@link RoomEvent}; how each block seals is data ({@link Seals}).
 *
 * <pre>{@code
 * SealedRooms.track(level, pos);                                    // a vent placed: its room is found
 * double given = SealedRooms.give(level, pos, AirMix.OXYGEN, 2.0);  // two moles of oxygen into it
 * Climate air = SealedRooms.climateAt(level, player.blockPosition()); // the room's air, or the place's
 * }</pre>
 */
public final class SealedRooms {

    /** The air of a place whose rules say only that it cannot be breathed: nitrogen, which breathes no one. */
    public static final AirMix INERT = new AirMix(Map.of(AirMix.NITROGEN, 1.0));

    private SealedRooms() {
    }

    /** The room at {@code pos}, if a sealed room holds it. */
    public static Optional<Room> roomAt(final ServerLevel level, final BlockPos pos) {
        final RoomStore store = Rooms.of(level).store();
        return store == null ? Optional.empty() : Optional.ofNullable(store.roomAt(pos.asLong()));
    }

    /**
     * The room at {@code pos} or, for a block in a room's wall such as a vent, the room beside it: up, then down,
     * then round.
     */
    public static Optional<Room> roomBeside(final ServerLevel level, final BlockPos pos) {
        final Optional<Room> at = roomAt(level, pos);
        if (at.isPresent()) {
            return at;
        }
        for (final Direction side : Direction.values()) {
            final Optional<Room> beside = roomAt(level, pos.relative(side));
            if (beside.isPresent()) {
                return beside;
            }
        }
        return Optional.empty();
    }

    /** The air at {@code pos}: its room's, or the place's, from its dimension's rules at that height. */
    public static Climate climateAt(final Level level, final BlockPos pos) {
        if (level instanceof ServerLevel server) {
            final Optional<Room> room = roomAt(server, pos);
            if (room.isPresent()) {
                return room.get().air().climate();
            }
        }
        return placeClimate(level, Vec3.atCenterOf(pos));
    }

    /** The air of the place at {@code at}, as if no room held it: its dimension's, at that height. */
    public static Climate placeClimate(final Level level, final Vec3 at) {
        return DimensionRulesData.of(level).at(at.y);
    }

    /** Whether the air at {@code pos} can be breathed. */
    public static boolean breathableAt(final ServerLevel level, final BlockPos pos) {
        return climateAt(level, pos).breathable();
    }

    /** The partial pressure of oxygen at {@code pos}, in kilopascals: in its room, or in the place's air. */
    public static double oxygenAt(final Level level, final BlockPos pos) {
        final Climate climate = climateAt(level, pos);
        return airOf(climate).partial(AirMix.OXYGEN, climate.pressure());
    }

    /**
     * What the air of a climate is made of: the air its rules say, or, where they say none, the overworld's air if it
     * can be breathed and {@link #INERT} if not.
     */
    public static AirMix airOf(final Climate climate) {
        return climate.air().orElse(climate.breathable() ? AirMix.EARTH : INERT);
    }

    /** Fills {@code room} with the air of {@code climate}, at its pressure, over what it held. */
    public static void fill(final Room room, final Climate climate) {
        fill(room, climate, room.gasVolume());
    }

    /** Adds to {@code room} the air of {@code climate} that fills {@code volume} cubic metres, at its pressure. */
    public static void fill(final Room room, final Climate climate, final double volume) {
        for (final Map.Entry<ResourceLocation, Double> gas : airOf(climate).shares().entrySet()) {
            room.add(gas.getKey(), GasMath.moles(climate.pressure() * gas.getValue(), volume, GasMath.TEMPERED));
        }
    }

    /**
     * Puts {@code moles} of {@code gas} into the room at or beside {@code pos}; how many went in, none where no room
     * is.
     */
    public static double give(final ServerLevel level, final BlockPos pos, final ResourceLocation gas,
                              final double moles) {
        final Optional<Room> room = roomBeside(level, pos);
        if (room.isEmpty() || moles <= 0) {
            return 0;
        }
        room.get().add(gas, moles);
        return moles;
    }

    /** Takes up to {@code moles} of {@code gas} out of the room at or beside {@code pos}; how many came out. */
    public static double take(final ServerLevel level, final BlockPos pos, final ResourceLocation gas,
                              final double moles) {
        return roomBeside(level, pos).map(room -> room.take(gas, moles)).orElse(0.0);
    }

    /**
     * Asks for the room at {@code pos} to be found and kept, as a block placed there does: looked for now, and again
     * whenever a block near it changes while no room holds it. For a block that is itself a wall, such as a vent or a
     * detector, the rooms on every side of it.
     */
    public static void track(final ServerLevel level, final BlockPos pos) {
        final LevelRooms rooms = Rooms.of(level);
        if (!wall(level, pos)) {
            rooms.seed(pos.asLong());
            return;
        }
        for (final Direction side : Direction.values()) {
            final BlockPos beside = pos.relative(side);
            if (!wall(level, beside)) {
                rooms.seed(beside.asLong());
            }
        }
    }

    /* Whether {@code pos} is a wall, read only from a chunk loaded whole; a place not yet loaded counts as none. */
    private static boolean wall(final ServerLevel level, final BlockPos pos) {
        final LevelChunk chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        return chunk != null && Seals.of(level, pos, chunk.getBlockState(pos)).wall();
    }

    /**
     * Forgets every room that reaches into {@code area} and every place in it asking for one, as if none had ever been
     * found there: for a mod that puts a whole area back as it was, or takes it away to put it elsewhere, whose air
     * goes with it. A block asking for a room there again finds it again, holding the place's air.
     *
     * @return how many rooms were forgotten
     */
    public static int forget(final ServerLevel level, final AABB area) {
        return Rooms.of(level).forget(area);
    }

    /** Stops asking for the room at {@code pos} and beside it, as a block taken away does; a room found stays. */
    public static void untrack(final ServerLevel level, final BlockPos pos) {
        final RoomStore store = Rooms.of(level).store();
        if (store == null) {
            return;
        }
        store.unseed(pos.asLong());
        for (final Direction side : Direction.values()) {
            store.unseed(pos.relative(side).asLong());
        }
    }
}
