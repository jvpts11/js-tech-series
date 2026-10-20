/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving.physics;

import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.StructureFrame;
import dev.jstech.core.moving.YardLayout;
import dev.jstech.core.moving.joint.JointEnd;
import dev.jstech.core.moving.joint.JointKind;
import dev.jstech.core.moving.joint.JointSignal;
import dev.jstech.core.moving.joint.StructureJoint;
import dev.jstech.core.moving.joint.StructureJoints;
import dev.jstech.core.util.Loaded;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3d;
import org.joml.Quaterniond;
import org.joml.Vector3d;

/**
 * The joints of the structures worked together in a tick ({@link PhysicsIsland}), as rows ({@link JointRows}): made
 * each step from where their ends are now, worked with the structures' contacts, and made again once the structures
 * have moved to draw them back together. Each joint's motor and winch is driven by its signal; a winch reels its rope
 * once a tick; a joint whose load is more than it holds breaks, and is taken away.
 */
final class JointWork {

    private final ConstraintRows rows = new ConstraintRows();
    private final JointRows make = new JointRows(rows);
    private final RigidBody ground = new RigidBody();
    /* The bodies of structures moved by a rule a joint holds, this tick, by the structure. */
    private final Map<MovingStructure, RigidBody> held = new HashMap<>();
    /* The rows each joint was made of in the last step: where they start and end in the rows. */
    private final List<int[]> spans = new ArrayList<>();
    private final List<StructureJoint> made = new ArrayList<>();
    /* The greatest load each joint took this tick, in newtons or newton metres, and those that broke. */
    private final Map<UUID, Double> loads = new HashMap<>();
    private final Set<UUID> broken = new HashSet<>();
    private final End first = new End();
    private final End second = new End();
    private final Vector3d scratch = new Vector3d();
    private final Matrix3d basis = new Matrix3d();

    /* A full signal. */
    private static final double FULL = 15.0;
    /* A rope reeled by less than this, in metres, is not written again. */
    private static final double REELED = 1.0e-4;
    /* Groups of rows a joint is made of, each keyed apart. */
    private static final int GROUPS = 4;

    JointWork() {
        ground.fixed();
    }

    /** Forgets every row and what it gave, for a structure set moving anew. */
    void reset() {
        rows.reset();
        loads.clear();
        broken.clear();
    }

    /** Reels the ropes of {@code joints} with winches by their signals, once a tick; gives the joints as they are. */
    List<StructureJoint> winch(final ServerLevel level, final List<StructureJoint> joints) {
        held.clear();
        loads.clear();
        broken.clear();
        List<StructureJoint> now = joints;
        for (int i = 0; i < joints.size(); i++) {
            final StructureJoint joint = joints.get(i);
            if (!(joint.kind() instanceof JointKind.Rope rope) || rope.winch().isEmpty()) {
                continue;
            }
            final JointKind.Winch winch = rope.winch().get();
            final double reeling = signal(level, joint, winch.out()) - signal(level, joint, winch.in());
            final double length = Math.max(winch.least(), Math.min(winch.most(),
                    rope.length() + winch.speed() * reeling / StructurePhysics.TICKS));
            if (Math.abs(length - rope.length()) < REELED) {
                continue;
            }
            final StructureJoint reeled = joint.holding(rope.reeled(length));
            StructureJoints.replace(level, reeled);
            if (now == joints) {
                now = new ArrayList<>(joints);
            }
            now.set(i, reeled);
        }
        return now;
    }

    /** Makes the rows of {@code joints} for a step of {@code seconds}, from where their ends are now. */
    void build(final ServerLevel level, final PhysicsIsland island, final List<StructureJoint> joints,
               final double seconds) {
        rows.clear();
        spans.clear();
        made.clear();
        for (final StructureJoint joint : joints) {
            if (broken.contains(joint.id()) || !first.resolve(level, island, joint.a(), this)
                    || !second.resolve(level, island, joint.b(), this)) {
                continue;
            }
            final int from = rows.rows().size();
            rowsOf(level, joint, seconds);
            spans.add(new int[] {from, rows.rows().size()});
            made.add(joint);
        }
    }

    /** Whether there are rows this step. */
    boolean any() {
        return !rows.isEmpty();
    }

    /** Readies the rows, each from its push of the step before. */
    void begin() {
        rows.begin();
    }

    /** Works the rows once, the {@code index}th time. */
    void round(final int index) {
        rows.round(index);
    }

    /** Keeps what the rows gave, and the load each joint took over the step of {@code seconds}. */
    void end(final double seconds) {
        rows.end();
        for (int i = 0; i < made.size(); i++) {
            final StructureJoint joint = made.get(i);
            if (joint.breaksAt().isEmpty()) {
                continue;
            }
            double sum = 0;
            for (int row = spans.get(i)[0]; row < spans.get(i)[1]; row++) {
                final double impulse = rows.rows().get(row).impulse();
                sum += impulse * impulse;
            }
            final double load = Math.sqrt(sum) / seconds;
            loads.merge(joint.id(), load, Math::max);
            if (load > joint.breaksAt().get()) {
                broken.add(joint.id());
            }
        }
    }

    /** Draws the bodies back together where they stand now, the rows of {@code joints} made again there. */
    void project(final ServerLevel level, final PhysicsIsland island, final List<StructureJoint> joints,
                 final double seconds, final int rounds) {
        build(level, island, joints, seconds);
        rows.project(rounds);
    }

    /** Takes away each joint that broke this tick. */
    void breakWhatBroke(final ServerLevel level) {
        for (final UUID id : broken) {
            StructureJoints.remove(level, id);
        }
        broken.clear();
    }

    /** The greatest load the joint {@code id} took this tick, in newtons or newton metres; 0 for one not worked. */
    double load(final UUID id) {
        return loads.getOrDefault(id, 0.0);
    }

    /* The rows of a joint between its two ends, as resolved. */
    private void rowsOf(final ServerLevel level, final StructureJoint joint, final double seconds) {
        final long key = joint.key() * GROUPS;
        final RigidBody a = first.body;
        final RigidBody b = second.body;
        switch (joint.kind()) {
            case JointKind.Hinge hinge -> {
                make.point(a, first.point, b, second.point, key);
                make.align(a, first.axis, b, second.axis, key + 1);
                final double angle = JointRows.angle(first.axis, first.normal, flat(second.normal, first.axis));
                final double least = hinge.limits().map(JointKind.Limits::least).orElse(Double.NEGATIVE_INFINITY);
                final double most = hinge.limits().map(JointKind.Limits::most).orElse(Double.POSITIVE_INFINITY);
                final double speed = hinge.drive().map(drive -> speedOf(level, joint, drive)).orElse(0.0);
                final double torque = hinge.drive().map(JointKind.Drive::force).orElse(0.0);
                make.turn(a, b, first.axis, angle, least, most, speed, torque, seconds, key + 2);
            }
            case JointKind.Slider slider -> {
                make.onLine(a, first.point, first.axis, b, second.point, key);
                make.lock(a, first.frame(basis), b, second.frame(basis), key + 1);
                final double least = slider.limits().map(JointKind.Limits::least).orElse(Double.NEGATIVE_INFINITY);
                final double most = slider.limits().map(JointKind.Limits::most).orElse(Double.POSITIVE_INFINITY);
                final double speed = slider.drive().map(drive -> speedOf(level, joint, drive)).orElse(0.0);
                final double force = slider.drive().map(JointKind.Drive::force).orElse(0.0);
                make.slide(a, first.point, first.axis, b, second.point, least, most, speed, force, seconds, key + 2);
            }
            case JointKind.Spring spring -> make.distance(a, first.point, b, second.point, spring.rest(),
                    JointRows.Distance.SPRING, spring.stiffness(), spring.damping(), seconds, key);
            case JointKind.Rope rope -> make.distance(a, first.point, b, second.point, rope.length(),
                    JointRows.Distance.AT_MOST, 0, 0, seconds, key);
            case JointKind.Bar bar -> make.distance(a, first.point, b, second.point, bar.length(),
                    JointRows.Distance.EXACTLY, 0, 0, seconds, key);
            case JointKind.Ball ball -> make.point(a, first.point, b, second.point, key);
            case JointKind.Weld weld -> {
                make.point(a, first.point, b, second.point, key);
                make.lock(a, first.frame(basis), b, second.frame(basis), key + 1);
            }
        }
    }

    /* {@code way} with its part along {@code axis} taken away, of unit length: the way turned about the axis. */
    private Vector3d flat(final Vector3d way, final Vector3d axis) {
        scratch.set(way).fma(-way.dot(axis), axis);
        return scratch.lengthSquared() < 1.0e-12 ? scratch.set(way) : scratch.normalize();
    }

    /* How fast a motor drives now: its speed, as strong as its signal says, less its reverse's. */
    private static double speedOf(final ServerLevel level, final StructureJoint joint, final JointKind.Drive drive) {
        if (drive.signal().isEmpty() && drive.reverse().isEmpty()) {
            return drive.speed();
        }
        return drive.speed() * (signal(level, joint, drive.signal()) - signal(level, joint, drive.reverse()));
    }

    /* How strong a joint's signal is now, from 0 to 1; none read as 0. */
    private static double signal(final ServerLevel level, final StructureJoint joint,
                                 final Optional<JointSignal> signal) {
        if (signal.isEmpty()) {
            return 0;
        }
        final JointSignal at = signal.get();
        final BlockPos pos = at.block();
        final JointEnd end = joint.end(at.end());
        if (end.structure().isPresent() != YardLayout.isYardBlock(pos.getX()) || !Loaded.at(level, pos)) {
            return 0;
        }
        final BlockState state = level.getBlockState(pos);
        return BlockPhysics.signal(level, pos, BlockPhysics.wayOf(state), at.from()) / FULL;
    }

    /* The body of a structure moved by a rule, held as its rule moves it, made once a tick. */
    private RigidBody heldBody(final MovingStructure structure) {
        return held.computeIfAbsent(structure, StructureContacts::held);
    }

    /* One end of a joint as it stands now: its body, its point and ways in the world. */
    private static final class End {

        private final Vector3d point = new Vector3d();
        private final Vector3d axis = new Vector3d();
        private final Vector3d normal = new Vector3d();
        private final Vector3d across = new Vector3d();
        private final Quaterniond turn = new Quaterniond();
        private RigidBody body;

        /*
         * Finds where {@code end} is now: on a structure of the island, as its body stands mid-tick; on one moved by
         * a rule, as its frame stands; on the world, where it is. False for an end on a structure not loaded.
         */
        boolean resolve(final ServerLevel level, final PhysicsIsland island, final JointEnd end,
                        final JointWork work) {
            final Vec3 place = end.place();
            final Vec3 way = end.axis();
            final Vec3 side = end.normal();
            if (end.onWorld()) {
                body = work.ground;
                point.set(place.x, place.y, place.z);
                axis.set(way.x, way.y, way.z);
                normal.set(side.x, side.y, side.z);
                return true;
            }
            final MovingStructure structure = MovingStructures.byId(level, end.structure().get());
            if (structure == null || !structure.ready()) {
                return false;
            }
            final StructurePhysics member = island.member(structure);
            if (member != null) {
                // Its own end or one of what it holds: the point where the body that carries it has it now.
                body = member.body();
                member.pieceWorldOf(structure, place.x, place.y, place.z, across.set(way.x, way.y, way.z), point,
                        axis);
                member.pieceWorldOf(structure, place.x, place.y, place.z, across.set(side.x, side.y, side.z),
                        point, normal);
                return true;
            }
            final StructureFrame frame = structure.frame();
            body = work.heldBody(structure);
            point.set(frame.worldX(place.x, place.y, place.z), frame.worldY(place.x, place.y, place.z),
                    frame.worldZ(place.x, place.y, place.z));
            axis.set(frame.turnX(way.x, way.y, way.z), frame.turnY(way.x, way.y, way.z),
                    frame.turnZ(way.x, way.y, way.z));
            normal.set(frame.turnX(side.x, side.y, side.z), frame.turnY(side.x, side.y, side.z),
                    frame.turnZ(side.x, side.y, side.z));
            return true;
        }

        /* The world's turn of the joint's frame at this end: its axis, its normal and the way across both. */
        Quaterniond frame(final Matrix3d scratch) {
            axis.cross(normal, across);
            scratch.set(axis.x, axis.y, axis.z, normal.x, normal.y, normal.z, across.x, across.y, across.z);
            return turn.setFromNormalized(scratch);
        }
    }
}
