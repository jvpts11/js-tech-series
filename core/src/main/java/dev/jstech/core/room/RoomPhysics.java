/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.room;

import dev.jstech.core.dimension.Climate;
import dev.jstech.core.dimension.DimensionRules;
import dev.jstech.core.dimension.DimensionRulesData;
import dev.jstech.core.moving.YardLayout;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;

/**
 * A second of the gas in a dimension's rooms, as real gas flows: through each room's cracks, open doors and holes to
 * the place round it, the room's pressure and its air going towards the place's, quickly through a hole and slowly
 * through a crack; between rooms joined by doors, their pressures evening out at once and their airs mixing over time;
 * and water rushing in through holes under water, as fast as the depth pushes it, until the air it squeezes holds it.
 */
final class RoomPhysics {

    /* The most blocks of water that rush into one room in a second. */
    private static final int MOST_FLOODED = 64;
    /* How far above the place's pressure a room must be for its air to rush out in a jet, in kilopascals. */
    private static final double JET_FROM = 5.0;
    /* The difference that makes the strongest jet, in kilopascals. */
    private static final double JET_FULL = 50.0;
    /* How near the place's pressure an open room's must be to count as the place's, and for how many seconds. */
    private static final double SETTLED = 1.0;
    private static final int FORGET_AFTER = 10;

    private RoomPhysics() {
    }

    /** A second of every room of {@code rooms} that stands in loaded chunks. */
    static void second(final LevelRooms rooms) {
        final RoomStore store = rooms.store();
        if (store == null) {
            return;
        }
        final ServerLevel level = rooms.level();
        for (final Room room : store.rooms().toArray(new Room[0])) {
            if (store.room(room.id()) == null || !loaded(level, room)) {
                continue;
            }
            aboard(level, room);
            final double[] flow = outside(store, room);
            // A room open to the place round it, its air long the place's, is forgotten: a block asking for it finds
            // it again once it is closed, holding the place's air, as it would have.
            final boolean settled = !Double.isNaN(flow[1]) && Math.abs(room.pressure() - flow[1]) < SETTLED;
            if (room.settleOpen(settled) >= FORGET_AFTER) {
                store.remove(room);
                NeoForge.EVENT_BUS.post(new RoomEvent.Removed(level, room));
                continue;
            }
            between(store, room);
            flood(rooms, room);
            RoomLiving.second(rooms, room);
            RoomConsequences.second(rooms, room);
            room.tickPressure(flow[0]);
            if (room.filledSince(room.air().breathable())) {
                NeoForge.EVENT_BUS.post(new RoomEvent.Filled(level, room));
            }
        }
    }

    /**
     * Whether every chunk a room and its walls stand in is loaded whole: a read of one that is not would load it in the
     * middle of the tick.
     */
    static boolean loaded(final ServerLevel level, final Room room) {
        final int minX = (int) Math.floor(room.bounds().minX) - 1;
        final int minZ = (int) Math.floor(room.bounds().minZ) - 1;
        final int maxX = (int) Math.floor(room.bounds().maxX);
        final int maxZ = (int) Math.floor(room.bounds().maxZ);
        for (int x = minX >> 4; x <= maxX >> 4; x++) {
            for (int z = minZ >> 4; z <= maxZ >> 4; z++) {
                if (level.getChunkSource().getChunkNow(x, z) == null) {
                    return false;
                }
            }
        }
        return true;
    }

    /*
     * A room aboard a moving structure has round it the air of where the structure is now, not of the yard its blocks
     * are kept in: what lies beyond its faces is taken again each second, at the height each is in the world, so a
     * balloon climbing into thin air swells against its walls, and bursts past what they hold.
     */
    private static void aboard(final ServerLevel level, final Room room) {
        if (!YardLayout.isYard(room.bounds().minX)) {
            return;
        }
        final DimensionRules rules = DimensionRulesData.of(level);
        final List<RoomFace> moved = new ArrayList<>(room.faces().size());
        for (final RoomFace face : room.faces()) {
            if (face.water()) {
                moved.add(face);
                continue;
            }
            final Climate climate = rules.at(RoomFinder.height(level, face.beyond()));
            moved.add(new RoomFace(face.cell(), face.side(), face.block(), face.kind(), face.area(), face.rating(),
                    face.beyond(), climate.pressure(), SealedRooms.airOf(climate), false));
        }
        room.outsideNow(moved);
    }

    /*
     * The room's gas going towards the place's through every face it leaks to the outside: each gas towards the
     * amount the place's air would hold in the room, as fast as the openings let it. How hard the air rushes out
     * through its holes, for the jet, and the place's pressure beyond its leaks, not a number where it leaks nowhere.
     */
    private static double[] outside(final RoomStore store, final Room room) {
        double area = 0;
        double holes = 0;
        double weighted = 0;
        final Object2DoubleOpenHashMap<ResourceLocation> partials = new Object2DoubleOpenHashMap<>();
        for (final RoomFace face : room.faces()) {
            if (!face.leaks() || face.water() || store.roomAt(face.beyond().asLong()) != null) {
                continue;
            }
            area += face.area();
            weighted += face.area() * face.outside();
            if (face.kind() == RoomFace.Kind.HOLE || face.kind() == RoomFace.Kind.DOOR) {
                holes += face.area();
            }
            for (final Map.Entry<ResourceLocation, Double> gas : face.outsideAir().shares().entrySet()) {
                partials.addTo(gas.getKey(), face.area() * face.outside() * gas.getValue());
            }
        }
        if (area <= 0 || room.gasVolume() <= 0) {
            return new double[] {0, Double.NaN};
        }
        final double volume = room.gasVolume();
        final double kept = GasMath.kept(GasMath.rate(area, GasMath.FLOW, volume), 1);
        final Set<ResourceLocation> gases = new HashSet<>(room.gases().keySet());
        gases.addAll(partials.keySet());
        final double before = room.pressure();
        for (final ResourceLocation gas : gases) {
            final double target = GasMath.moles(partials.getDouble(gas) / area, volume, GasMath.TEMPERED);
            final double now = room.moles(gas);
            room.add(gas, target + (now - target) * kept - now);
        }
        final double place = weighted / area;
        if (holes <= 0 || before - place < JET_FROM) {
            return new double[] {0, place};
        }
        return new double[] {Math.min(1, (before - place) / JET_FULL), place};
    }

    /*
     * The room's gas and its neighbours', through the doors, cracks and holes between them, each pair once: their
     * pressures even out as fast as gas crosses an opening, carrying the fuller room's air; then their airs mix, gas
     * by gas, as slowly as two airs mix.
     */
    private static void between(final RoomStore store, final Room room) {
        final Long2DoubleOpenHashMap areas = new Long2DoubleOpenHashMap();
        for (final RoomFace face : room.faces()) {
            if (!face.leaks()) {
                continue;
            }
            final Room other = store.roomAt(face.beyond().asLong());
            if (other != null && other != room && other.id() > room.id()) {
                areas.addTo(other.id(), face.area());
            }
        }
        for (final Long2DoubleOpenHashMap.Entry pair : areas.long2DoubleEntrySet()) {
            final Room other = store.room(pair.getLongKey());
            if (other == null || other.gasVolume() <= 0 || room.gasVolume() <= 0) {
                continue;
            }
            even(room, other, pair.getDoubleValue());
            mix(room, other, pair.getDoubleValue());
        }
    }

    /* Two rooms' pressures evening out: the fuller sends gas of its own air to the other. */
    private static void even(final Room first, final Room second, final double area) {
        final double firstTotal = first.air().total();
        final double secondTotal = second.air().total();
        final double moved = GasMath.between(firstTotal, first.gasVolume(), secondTotal, second.gasVolume(), area,
                GasMath.FLOW, 1);
        final Room from = moved >= 0 ? first : second;
        final Room to = moved >= 0 ? second : first;
        final double amount = Math.abs(moved);
        final double fromTotal = from.air().total();
        if (amount <= 0 || fromTotal <= 0) {
            return;
        }
        for (final Map.Entry<ResourceLocation, Double> gas : from.gases().entrySet()) {
            final double part = amount * gas.getValue() / fromTotal;
            from.add(gas.getKey(), -part);
            to.add(gas.getKey(), part);
        }
    }

    /* Two rooms' airs mixing, gas by gas, towards the same share of each. */
    private static void mix(final Room first, final Room second, final double area) {
        final Object2DoubleOpenHashMap<ResourceLocation> moved = new Object2DoubleOpenHashMap<>();
        final Set<ResourceLocation> gases = new HashSet<>(first.gases().keySet());
        gases.addAll(second.gases().keySet());
        for (final ResourceLocation gas : gases) {
            moved.put(gas, GasMath.between(first.moles(gas), first.gasVolume(), second.moles(gas),
                    second.gasVolume(), area, GasMath.MIX, 1));
        }
        for (final Object2DoubleMap.Entry<ResourceLocation> gas : moved.object2DoubleEntrySet()) {
            first.add(gas.getKey(), -gas.getDoubleValue());
            second.add(gas.getKey(), gas.getDoubleValue());
        }
    }

    /*
     * Water rushing in through the room's openings under water, filling it from the floor up, as fast as the water's
     * pressure beyond is greater than the air's inside; the air it squeezes into less room presses back.
     */
    private static void flood(final LevelRooms rooms, final Room room) {
        double rushing = 0;
        for (final RoomFace face : room.faces()) {
            if (face.water() && face.leaks()) {
                rushing += GasMath.flooding(face.area(), face.outside() - room.pressure());
            }
        }
        if (rushing <= 0) {
            room.setFloodDebt(0);
            return;
        }
        double debt = room.floodDebt() + rushing;
        int placed = 0;
        final ServerLevel level = rooms.level();
        for (final long cell : room.lowestFirst()) {
            if (debt < 1 || placed >= MOST_FLOODED) {
                break;
            }
            final BlockPos pos = BlockPos.of(cell);
            final BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                rooms.set(pos, Blocks.WATER.defaultBlockState());
                room.flood(cell, true);
                debt -= 1;
                placed++;
            }
        }
        room.setFloodDebt(Math.min(debt, 1));
    }
}
