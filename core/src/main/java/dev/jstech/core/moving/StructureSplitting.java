/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import dev.jstech.core.moving.joint.JointEnd;
import dev.jstech.core.moving.joint.StructureJoints;
import dev.jstech.core.moving.physics.BlockPhysics;
import dev.jstech.core.moving.physics.PhysicsMotion;
import dev.jstech.core.moving.physics.StructurePhysics;
import dev.jstech.core.region.Box;
import dev.jstech.core.uuid.StructureUuid;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

/**
 * A moving structure whose blocks are no longer all joined becomes as many structures as it now is: a ship cut in two,
 * a rocket's stage let go. Each block lost leaves the blocks beside it to be looked at, once a tick; a piece no longer
 * joined to the rest is given a plot of its own, its blocks are moved there whole, and it goes on as a structure of its
 * own from where it was, at the speed it had there, by its physics: falling, flying on, or left in orbit. The part
 * that goes on as the structure keeps its name, its rule and what it holds; what stood on a piece, the joints tied to
 * it and what it held go with the piece. A setting turns it off ({@link StructureBalance#split()}).
 */
final class StructureSplitting {

    /* Ticks in a second. */
    private static final double TICKS = 20;
    private static final double HALF = 0.5;

    private StructureSplitting() {
    }

    /*
     * Looks at the blocks beside those {@code structure} lost, its {@code seeds}, and parts each piece no longer joined
     * to the rest, once its plot has loaded; whether one is still waiting for its plot.
     */
    static boolean look(final ServerLevel level, final MovingStructure structure, final Set<Long> seeds,
                        final List<Waiting> waiting) {
        if (!waiting.isEmpty()) {
            return partWaiting(level, structure, waiting);
        }
        if (seeds.isEmpty() || !StructureBalance.split()) {
            seeds.clear();
            return false;
        }
        final Box bounds = structure.bounds();
        final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        final Set<Long> looked = Set.copyOf(seeds);
        final List<Set<Long>> pieces = SplitSearch.pieces(looked, pos -> {
            at.set(pos);
            return bounds.contains(at.getX(), at.getY(), at.getZ()) && !level.getBlockState(at).isAir();
        });
        seeds.clear();
        for (final Set<Long> piece : pieces) {
            final Waiting next = take(level, piece, looked);
            if (next != null) {
                waiting.add(next);
            }
        }
        return partWaiting(level, structure, waiting);
    }

    /* Parts the pieces waiting whose plots have loaded; whether one is still waiting. */
    private static boolean partWaiting(final ServerLevel level, final MovingStructure structure,
                                       final List<Waiting> waiting) {
        waiting.removeIf(piece -> {
            if (!StructureYard.loaded(level, piece.bounds)) {
                return false;
            }
            partOff(level, structure, piece);
            return true;
        });
        if (waiting.isEmpty()) {
            structure.aboard().clearPushes();
            structure.shrinkToBlocks(level);
        }
        return !waiting.isEmpty();
    }

    /* Takes a plot for {@code piece}, its blocks to come into it, and asks for its chunks; null when none is left. */
    private static @Nullable Waiting take(final ServerLevel level, final Set<Long> piece, final Set<Long> seeds) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (final long pos : piece) {
            minX = Math.min(minX, BlockPos.getX(pos));
            minY = Math.min(minY, BlockPos.getY(pos));
            minZ = Math.min(minZ, BlockPos.getZ(pos));
            maxX = Math.max(maxX, BlockPos.getX(pos));
            maxY = Math.max(maxY, BlockPos.getY(pos));
            maxZ = Math.max(maxZ, BlockPos.getZ(pos));
        }
        final StructureUuid id = StructureUuid.random();
        final int plot;
        try {
            plot = StructureYard.take(level, id);
        } catch (final IllegalStateException full) {
            return null;
        }
        final int dx = YardLayout.centreX(plot) - (minX + maxX) / 2;
        final int dz = YardLayout.centreZ(plot) - (minZ + maxZ) / 2;
        final Box bounds = new Box(minX + dx, minY, minZ + dz, maxX + dx, maxY, maxZ + dz);
        StructureYard.hold(level, bounds);
        return new Waiting(id, plot, piece, bounds, dx, dz, seeds);
    }

    /*
     * Moves a piece's blocks into its own plot and makes it a structure of its own where it was, going as fast as that
     * part of the structure went; what stood on it, the joints tied to it and what it held go with it.
     */
    private static void partOff(final ServerLevel level, final MovingStructure structure, final Waiting piece) {
        final Map<BlockPos, BlockPos> moves = new LinkedHashMap<>();
        final Vector3d heavy = new Vector3d();
        double mass = 0;
        for (final long place : piece.places) {
            final BlockPos at = BlockPos.of(place);
            final BlockState state = level.getBlockState(at);
            if (state.isAir()) {
                continue;
            }
            moves.put(at, at.offset(piece.dx, 0, piece.dz));
            final double kilograms = BlockPhysics.massOf(level, at, state);
            mass += kilograms;
            heavy.fma(kilograms, new Vector3d(at.getX() + HALF, at.getY() + HALF, at.getZ() + HALF));
        }
        if (moves.isEmpty()) {
            StructureYard.release(level, piece.bounds);
            StructureYard.giveBack(level, piece.plot);
            return;
        }
        final StructureFrame frame = structure.frame();
        final Vector3d middle = mass > 0 ? heavy.div(mass) : new Vector3d(piece.bounds.minX() - piece.dx + HALF,
                piece.bounds.minY() + HALF, piece.bounds.minZ() - piece.dz + HALF);
        final PhysicsMotion going = pushed(structure, frame, goingAt(structure, frame, middle), moves, mass);
        // The blocks leaving are not blocks lost: what they leave beside them is still joined.
        structure.parting(true);
        try {
            BlockMover.move(level, level, moves, Rotation.NONE, false, false);
        } finally {
            structure.parting(false);
        }
        final MovingStructure parted = MovingStructures.make(level, piece.id, piece.plot, piece.bounds, moves.size(),
                structure.pivotX() + piece.dx, structure.pivotY(), structure.pivotZ() + piece.dz, structure.pose(),
                null);
        parted.move(going);
        StructureFooting.moved(structure, parted, moves);
        moveJoints(level, structure, parted, moves, piece.dx, piece.dz);
        moveHeld(structure, parted, moves, piece.dx, piece.dz);
        NeoForge.EVENT_BUS.post(new MovingStructureEvent.Parted(structure, List.of(parted)));
    }

    /*
     * The physics a piece goes on with: the speed its middle, {@code middle} of the yard, had on the structure: the
     * body's when the structure moves by its physics, else how its frame moved its last tick.
     */
    private static PhysicsMotion goingAt(final MovingStructure structure, final StructureFrame frame,
                                         final Vector3d middle) {
        final Vec3 at = new Vec3(frame.worldX(middle.x, middle.y, middle.z), frame.worldY(middle.x, middle.y, middle.z),
                frame.worldZ(middle.x, middle.y, middle.z));
        final StructurePhysics physics = StructureMounts.rootOf(structure).physics();
        if (physics != null && physics.active()) {
            final Vector3d velocity = physics.body().velocityAt(new Vector3d(at.x, at.y, at.z), new Vector3d());
            final Vector3d spin = physics.body().spin();
            return new PhysicsMotion(velocity.x, velocity.y, velocity.z, spin.x, spin.y, spin.z, List.of());
        }
        final Vec3 moved = StructureFooting.speedAt(structure, at).scale(TICKS);
        final double[] turned = structure.spin();
        return new PhysicsMotion(moved.x, moved.y, moved.z, turned[0] * TICKS, turned[1] * TICKS,
                turned[2] * TICKS, List.of());
    }

    /*
     * A piece parted by a decoupler goes on pushed away from what it parted from, by the decoupler's push over its
     * mass, and what it parted from, moving by its physics, the other way by the push over its own.
     */
    private static PhysicsMotion pushed(final MovingStructure structure, final StructureFrame frame,
                                        final PhysicsMotion going, final Map<BlockPos, BlockPos> moves,
                                        final double mass) {
        final Vector3d velocity = new Vector3d(going.vx(), going.vy(), going.vz());
        for (final StructureAboard.Push push : structure.aboard().pushes()) {
            final int sign = moves.containsKey(push.at().relative(push.way())) ? 1
                    : moves.containsKey(push.at().relative(push.way().getOpposite())) ? -1 : 0;
            if (sign == 0 || push.push() <= 0 || mass <= 0) {
                continue;
            }
            final Vector3d way = new Vector3d(frame.turnX(push.way().getStepX(), push.way().getStepY(),
                    push.way().getStepZ()), frame.turnY(push.way().getStepX(), push.way().getStepY(),
                    push.way().getStepZ()), frame.turnZ(push.way().getStepX(), push.way().getStepY(),
                    push.way().getStepZ())).mul(sign);
            velocity.fma(push.push() / mass, way);
            final StructurePhysics physics = StructureMounts.rootOf(structure).physics();
            if (physics != null && physics.active() && physics.mass() > mass) {
                physics.body().velocity().fma(-push.push() / (physics.mass() - mass), way);
                physics.wake();
            }
        }
        return new PhysicsMotion(velocity.x, velocity.y, velocity.z, going.sx(), going.sy(), going.sz(),
                going.warm());
    }

    /* The ends of joints tied to the blocks that went with the piece go with it. */
    private static void moveJoints(final ServerLevel level, final MovingStructure structure,
                                   final MovingStructure parted, final Map<BlockPos, BlockPos> moves, final int dx,
                                   final int dz) {
        if (StructureJoints.of(level, structure.id()).isEmpty()) {
            return;
        }
        StructureJoints.change(level, joint -> {
            if (!joint.touches(structure.id())) {
                return joint;
            }
            final JointEnd a = movedEnd(joint.a(), structure, parted, moves, dx, dz);
            final JointEnd b = movedEnd(joint.b(), structure, parted, moves, dx, dz);
            return a == joint.a() && b == joint.b() ? joint : joint.between(a, b);
        });
    }

    private static JointEnd movedEnd(final JointEnd end, final MovingStructure structure, final MovingStructure parted,
                                     final Map<BlockPos, BlockPos> moves, final int dx, final int dz) {
        if (!end.on(structure.id()) || !moves.containsKey(BlockPos.containing(end.place()))) {
            return end;
        }
        return end.onto(Optional.of(parted.id()), end.place().add(dx, 0, dz), end.axis(), end.normal());
    }

    /* What the structure held by a block that went with the piece is held by the piece now. */
    private static void moveHeld(final MovingStructure structure, final MovingStructure parted,
                                 final Map<BlockPos, BlockPos> moves, final int dx, final int dz) {
        for (final MovingStructure child : StructureMounts.children(structure)) {
            final StructureMount mount = child.mount();
            if (mount == null || mount.anchor().isEmpty() || !moves.containsKey(mount.anchor().get())) {
                continue;
            }
            StructureMounts.unfile(child);
            final StructurePose local = mount.local();
            child.mounted(new StructureMount(parted.id(), local.movedTo(local.x() + dx, local.y(), local.z() + dz),
                    Optional.of(mount.anchor().get().offset(dx, 0, dz))));
            StructureMounts.file(child);
        }
    }

    /*
     * A piece waiting for its plot to load: its name and plot, its blocks where they are, the box they take in the
     * plot, and how far they go there.
     */
    record Waiting(StructureUuid id, int plot, Set<Long> places, Box bounds, int dx, int dz, Set<Long> seeds) {

        Waiting {
            places = Set.copyOf(places);
            seeds = Set.copyOf(seeds);
        }
    }
}
