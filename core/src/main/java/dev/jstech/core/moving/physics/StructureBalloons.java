/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving.physics;

import dev.jstech.core.heat.Thermal;
import dev.jstech.core.material.Materials;
import dev.jstech.core.moving.StructureBalance;
import dev.jstech.core.moving.StructureFrame;
import dev.jstech.core.region.Box;
import dev.jstech.core.room.GasMath;
import dev.jstech.core.room.Room;
import dev.jstech.core.room.RoomStore;
import dev.jstech.core.room.SealedRooms;
import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3d;

/**
 * The balloons of a structure: its sealed rooms aboard, each lifted by the air round it as a balloon is. A room holds
 * up the weight of the air it puts aside, less the weight of the gas it holds: a room of hydrogen or helium lifts, one
 * of the air round it lifts nothing, one of a heavy gas weighs down. A burner opening into a room heats its air, as
 * in a hot air balloon: the warm air, lighter, spills out of it, and the room lifts the more the hotter it is, its
 * envelope losing heat to the cold air round it. A gas lighter than the air fills its envelope from the top, so the
 * lift is at the top, and a balloon hangs upright under it. The air round it is where the structure flies: thin up
 * high, so a balloon climbs until it lifts no more.
 */
final class StructureBalloons {

    /* Each room's warm air, by its id, in kelvin; a room with none is as warm as the air round it. */
    private final Long2DoubleOpenHashMap warm = new Long2DoubleOpenHashMap();
    /* The rooms aboard, looked for again every second; the places burners have asked rooms for. */
    private final List<Room> aboard = new ArrayList<>();
    private final LongSet asked = new LongOpenHashSet();
    /* Each room's blocks from the top down, summed, for where a gas filling it from the top has its middle. */
    private final Long2ObjectMap<double[]> tops = new Long2ObjectOpenHashMap<>();
    private final Vector3d centre = new Vector3d();
    private final Vector3d middle = new Vector3d();
    private long lookedAt = -LOOK_AGAIN;

    /* How often the rooms aboard are looked for again, in ticks. */
    private static final long LOOK_AGAIN = 20;
    /* The heat a kilogram of air takes a kelvin, and what an envelope loses a square metre a kelvin, in watts. */
    private static final double AIR_HEAT = 1_005;
    private static final double ENVELOPE_LOSS = 5;
    /* The molar mass of air, and of the gases a balloon is filled with, in kilograms a mole, where no material says. */
    private static final double AIR_MOLAR_MASS = 0.02896;
    private static final Map<String, Double> MOLAR_MASSES = Map.of("hydrogen", 0.002016, "helium", 0.004003,
            "nitrogen", 0.028014, "oxygen", 0.031998, "argon", 0.039948, "carbon_dioxide", 0.04401,
            "methane", 0.01604, "neon", 0.02018, "steam", 0.018015, "water", 0.018015);
    /* A full signal. */
    private static final double FULL = 15.0;
    /* Below this a room's air is too little to warm. */
    private static final double LEAST_AIR = 1.0e-3;
    private static final double HALF = 0.5;

    /**
     * Adds the lift of each room aboard the structure standing at {@code frame}, of yard {@code bounds}, to
     * {@code sum}, under the pull {@code gravity}, and warms the air of those its burners open into over
     * {@code seconds}. Whether a balloon is warming or cooling, which a structure at rest wakes for.
     */
    boolean gather(final ServerLevel level, final StructureFrame frame, final StructureParts parts, final Box bounds,
                   final Vector3d gravity, final double seconds, final ForceSum sum) {
        final long now = level.getGameTime();
        if (now - lookedAt >= LOOK_AGAIN) {
            look(level, parts, bounds);
            lookedAt = now;
        }
        boolean changing = false;
        for (final Room room : aboard) {
            final double volume = room.gasVolume();
            if (volume <= 0) {
                continue;
            }
            final AABB box = room.bounds();
            FrameMath.world(frame, (box.minX + box.maxX) / 2, (box.minY + box.maxY) / 2, (box.minZ + box.maxZ) / 2,
                    centre);
            final double outsideDensity = PhysicsWorld.airDensity(level, centre.y);
            final double outside = PhysicsWorld.airTemperature(level, centre.y);
            final double gas = gasMass(room);
            final double heat = heat(level, parts, room);
            double kelvin = warm.containsKey(room.id()) ? warm.get(room.id()) : outside;
            if (heat > 0 || kelvin > outside) {
                kelvin = warmed(kelvin, outside, heat, gas, room.faces().size(), seconds);
                changing = true;
                if (kelvin > outside) {
                    warm.put(room.id(), kelvin);
                } else {
                    warm.remove(room.id());
                }
            }
            // The room's gas is counted as at the temperature rooms keep; warmer, it fills more, and spills out.
            final double inside = gas / volume * GasMath.TEMPERED / kelvin;
            final double lift = (outsideDensity - inside) * volume;
            if (lift != 0 && StructureBalance.force(ForceSum.ForceKind.BALLOONS)) {
                // A gas lighter than the air rises to the top of its envelope: the lift is there, so a balloon
                // hangs upright under it.
                final double light = kelvin > outside ? 1 : lightShare(room);
                sum.add(ForceSum.ForceKind.BALLOONS, new Vector3d(gravity).mul(-lift),
                        FrameMath.world(frame, top(room, light), new Vector3d()));
            }
        }
        return changing;
    }

    /** Each warm room's air, for saving: by its id, in kelvin. */
    Long2DoubleMap warm() {
        return warm;
    }

    /** Takes back what was saved of the rooms' warm air. */
    void warm(final Map<Long, Double> saved) {
        warm.clear();
        saved.forEach(warm::put);
    }

    /* Finds the rooms aboard again, and asks for the rooms its burners open into. */
    private void look(final ServerLevel level, final StructureParts parts, final Box bounds) {
        for (final StructureParts.Part part : parts.parts()) {
            if (part.physics().burner().isPresent()) {
                final BlockPos above = part.pos().above();
                if (asked.add(above.asLong())) {
                    // Rooms are kept on the server's thread; worked beside it, it asks once the work is over.
                    PhysicsLens.onServerThread(() -> SealedRooms.track(level, above));
                }
            }
        }
        aboard.clear();
        tops.clear();
        final RoomStore store = RoomStore.find(level);
        if (store == null) {
            return;
        }
        for (final Room room : store.rooms()) {
            final AABB box = room.bounds();
            if (box.minX >= bounds.minX() && box.maxX <= bounds.maxX() + 1 && box.minY >= bounds.minY()
                    && box.maxY <= bounds.maxY() + 1 && box.minZ >= bounds.minZ()
                    && box.maxZ <= bounds.maxZ() + 1) {
                aboard.add(room);
                tops.put(room.id(), fromTheTop(room));
            }
        }
    }

    /*
     * The middle of the top {@code share} of the room, in the yard's places: where a gas filling it from the top, as
     * much of it as the share, has its middle.
     */
    private Vector3d top(final Room room, final double share) {
        final double[] sums = tops.get(room.id());
        if (sums == null || sums.length == 0) {
            final AABB box = room.bounds();
            return middle.set((box.minX + box.maxX) / 2, (box.minY + box.maxY) / 2, (box.minZ + box.maxZ) / 2);
        }
        final int cells = sums.length / 3;
        final int taken = Math.max(1, Math.min(cells, (int) Math.round(share * cells)));
        final int at = (taken - 1) * 3;
        return middle.set(sums[at] / taken + HALF, sums[at + 1] / taken + HALF, sums[at + 2] / taken + HALF);
    }

    /* The room's blocks from the top down, summed as they go: x, y and z of the first one, then of two, and on. */
    private static double[] fromTheTop(final Room room) {
        final long[] cells = room.cells().toLongArray();
        final Long[] sorted = new Long[cells.length];
        for (int i = 0; i < cells.length; i++) {
            sorted[i] = cells[i];
        }
        Arrays.sort(sorted, (a, b) -> Integer.compare(BlockPos.getY(b), BlockPos.getY(a)));
        final double[] sums = new double[sorted.length * 3];
        double x = 0;
        double y = 0;
        double z = 0;
        for (int i = 0; i < sorted.length; i++) {
            x += BlockPos.getX(sorted[i]);
            y += BlockPos.getY(sorted[i]);
            z += BlockPos.getZ(sorted[i]);
            sums[i * 3] = x;
            sums[i * 3 + 1] = y;
            sums[i * 3 + 2] = z;
        }
        return sums;
    }

    /* How much of the room's gas, by moles, is lighter than air: what fills it from the top. */
    private static double lightShare(final Room room) {
        double light = 0;
        double total = 0;
        for (final Map.Entry<ResourceLocation, Double> gas : room.gases().entrySet()) {
            total += gas.getValue();
            if (molarMass(gas.getKey()) < AIR_MOLAR_MASS) {
                light += gas.getValue();
            }
        }
        return total > 0 ? light / total : 1;
    }

    /* The heat its burners give a room, in watts: each burner whose block above is in the room, by its signal. */
    private static double heat(final ServerLevel level, final StructureParts parts, final Room room) {
        double watts = 0;
        for (final StructureParts.Part part : parts.parts()) {
            if (part.physics().burner().isPresent() && room.cells().contains(part.pos().above().asLong())) {
                final BlockPhysics.Burner burner = part.physics().burner().get();
                final double lit = burner.power().isEmpty() ? 1.0
                        : BlockPhysics.signal(level, part.pos(), part.way(), burner.power().get()) / FULL;
                watts += burner.watts() * lit;
            }
        }
        return watts;
    }

    /*
     * The room's air after {@code seconds}: warmed by {@code watts}, losing heat through its {@code faces} square
     * metres of envelope to the air round it, never colder than that air.
     */
    private static double warmed(final double kelvin, final double outside, final double watts, final double air,
                                 final int faces, final double seconds) {
        final double capacity = Math.max(LEAST_AIR, air) * AIR_HEAT;
        final double loss = ENVELOPE_LOSS * faces * (kelvin - outside);
        return Math.max(outside, kelvin + (watts - loss) * seconds / capacity);
    }

    /* The mass of the gas a room holds, in kilograms. */
    private static double gasMass(final Room room) {
        double kilograms = 0;
        for (final Map.Entry<ResourceLocation, Double> gas : room.gases().entrySet()) {
            kilograms += gas.getValue() * molarMass(gas.getKey());
        }
        return kilograms;
    }

    /* How much a mole of {@code gas} weighs, in kilograms: its material's, else the air's. */
    private static double molarMass(final ResourceLocation gas) {
        final Double said = Materials.byId(gas).flatMap(material -> material.value(Thermal.MOLAR_MASS)).orElse(null);
        if (said != null && said > 0) {
            return said;
        }
        return MOLAR_MASSES.getOrDefault(gas.getPath(), AIR_MOLAR_MASS);
    }
}
