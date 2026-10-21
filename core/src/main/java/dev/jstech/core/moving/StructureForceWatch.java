/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import dev.jstech.core.moving.physics.BlockPhysics;
import dev.jstech.core.moving.physics.ForceSum;
import dev.jstech.core.moving.physics.PhysicsWorld;
import dev.jstech.core.moving.physics.StructurePhysics;
import dev.jstech.core.region.Box;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3d;

/**
 * Telling the players who look at the forces on a moving structure, the diagram of forces open or a physics tool
 * holding it, what they are, a few times a second: of a structure moving by its physics, the forces it was last
 * worked with; of one at rest or moved by a rule, its weight alone, its mass weighed from its blocks once and again
 * only as their count changes. Only who looks is told, and only while they are near it.
 */
final class StructureForceWatch {

    /* For each player who looks, the entities of the structures they look at. */
    private static final Map<ServerPlayer, IntSet> WATCHING = new WeakHashMap<>();
    /* For each structure not moving by its physics, as last weighed: its blocks, its mass and its centre of mass. */
    private static final Map<MovingStructure, double[]> WEIGHED = new WeakHashMap<>();
    /* How often who looks is told, in ticks, and how far from the structure they may be. */
    private static final int EVERY = 5;
    private static final double REACH = 192;
    /* The most structures one player is told the forces of at once. */
    private static final int MOST_WATCHED = 32;
    /* A structure of more blocks than this not moving by its physics is weighed as its box would be, of rock. */
    private static final int MOST_WEIGHED = 65_536;
    /* What a block of rock weighs, in kilograms. */
    private static final double ROCK = 2_000;
    private static final double HALF = 0.5;

    private StructureForceWatch() {
    }

    /** Hands the game's bus the telling, once, as the kit starts. */
    static void listen() {
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, StructureForceWatch::tick);
    }

    /** {@code player} began or stopped looking at the forces on the structure of a {@code payload}'s entity. */
    static void watched(final ServerPlayer player, final StructureForcesWatchPayload payload) {
        final IntSet watched = WATCHING.computeIfAbsent(player, any -> new IntOpenHashSet());
        if (!payload.watching()) {
            watched.remove(payload.entity());
        } else if (watched.size() < MOST_WATCHED && tell(player, payload.entity())) {
            watched.add(payload.entity());
        }
        if (watched.isEmpty()) {
            WATCHING.remove(player);
        }
    }

    /** What the forces on {@code structure} are now, as who looks at them is told. */
    static StructureForcesPayload forcesOf(final MovingStructure structure) {
        final MovingStructure root = StructureMounts.rootOf(structure);
        final StructurePhysics physics = root.physics();
        final List<StructureForcesPayload.Force> forces = new ArrayList<>();
        if (physics != null && physics.active()) {
            final ForceSum sum = physics.forces();
            final Vector3d where = new Vector3d();
            for (final ForceSum.ForceKind kind : ForceSum.ForceKind.values()) {
                final Vector3d push = sum.of(kind);
                if (push.lengthSquared() > 0) {
                    sum.where(kind, where);
                    forces.add(new StructureForcesPayload.Force(kind, new Vec3(push.x, push.y, push.z),
                            new Vec3(where.x, where.y, where.z)));
                }
            }
            final Vector3d centre = physics.body().position();
            final Vector3d going = physics.body().velocity();
            final Vec3 at = new Vec3(centre.x, centre.y, centre.z);
            return new StructureForcesPayload(structure.entity().getId(), physics.mass(), at,
                    new Vec3(going.x, going.y, going.z), PhysicsWorld.airDensity(root.level(), at.y),
                    StructureReadings.below(root.level(), at), forces);
        }
        final double[] weighed = weigh(root);
        final Vec3 at = MovingStructures.toWorld(root, new Vec3(weighed[2], weighed[3], weighed[4]));
        final Vector3d pull = PhysicsWorld.gravity(root.level(), new Vector3d(at.x, at.y, at.z), new Vector3d());
        if (weighed[1] > 0 && pull.lengthSquared() > 0) {
            forces.add(new StructureForcesPayload.Force(ForceSum.ForceKind.GRAVITY, new Vec3(pull.x, pull.y, pull.z)
                    .scale(weighed[1]), at));
        }
        final double[] moved = root.velocity();
        return new StructureForcesPayload(structure.entity().getId(), weighed[1], at,
                new Vec3(moved[0], moved[1], moved[2]).scale(StructurePhysics.TICKS),
                PhysicsWorld.airDensity(root.level(), at.y), StructureReadings.below(root.level(), at), forces);
    }

    private static void tick(final ServerTickEvent.Post event) {
        if (WATCHING.isEmpty() || event.getServer().getTickCount() % EVERY != 0) {
            return;
        }
        for (final Map.Entry<ServerPlayer, IntSet> each : List.copyOf(WATCHING.entrySet())) {
            each.getValue().removeIf((int entity) -> !tell(each.getKey(), entity));
            if (each.getValue().isEmpty()) {
                WATCHING.remove(each.getKey());
            }
        }
    }

    /* Tells {@code player} the forces on the structure of {@code entity}; whether they still may look at it. */
    private static boolean tell(final ServerPlayer player, final int entity) {
        final Entity seen = player.level().getEntity(entity);
        if (player.isRemoved() || !(seen instanceof MovingStructureEntity carrier)
                || !(carrier.structure() instanceof MovingStructure structure)
                || structure.worldBox().distanceToSqr(player.position()) > REACH * REACH) {
            return false;
        }
        if (player.connection != null && player.connection.hasChannel(StructureForcesPayload.TYPE)) {
            PacketDistributor.sendToPlayer(player, forcesOf(structure));
        }
        return true;
    }

    /*
     * The structure's blocks, mass and centre of mass in its yard, weighed from its blocks once and again only as
     * their count changes.
     */
    private static double[] weigh(final MovingStructure structure) {
        final double[] known = WEIGHED.get(structure);
        if (known != null && known[0] == structure.blocks()) {
            return known;
        }
        final Box box = structure.bounds();
        final double[] out = {structure.blocks(), 0, 0, 0, 0};
        final long volume = (long) (box.maxX() - box.minX() + 1) * (box.maxY() - box.minY() + 1)
                * (box.maxZ() - box.minZ() + 1);
        if (volume > MOST_WEIGHED) {
            out[1] = structure.blocks() * ROCK;
            out[2] = (box.minX() + box.maxX() + 1) * HALF;
            out[3] = (box.minY() + box.maxY() + 1) * HALF;
            out[4] = (box.minZ() + box.maxZ() + 1) * HALF;
        } else {
            for (final BlockPos at : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(),
                    box.maxY(), box.maxZ())) {
                final BlockState state = structure.level().getBlockState(at);
                if (state.isAir()) {
                    continue;
                }
                final double kilograms = BlockPhysics.massOf(structure.level(), at, state);
                out[1] += kilograms;
                out[2] += kilograms * (at.getX() + HALF);
                out[3] += kilograms * (at.getY() + HALF);
                out[4] += kilograms * (at.getZ() + HALF);
            }
            if (out[1] > 0) {
                out[2] /= out[1];
                out[3] /= out[1];
                out[4] /= out[1];
            } else {
                out[2] = (box.minX() + box.maxX() + 1) * HALF;
                out[3] = (box.minY() + box.maxY() + 1) * HALF;
                out[4] = (box.minZ() + box.maxZ() + 1) * HALF;
            }
        }
        WEIGHED.put(structure, out);
        return out;
    }
}
