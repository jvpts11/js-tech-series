/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving.physics;

import dev.jstech.core.moving.IStructureMotion;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.StructureFrame;
import dev.jstech.core.moving.StructureMounts;
import dev.jstech.core.moving.joint.JointEnd;
import dev.jstech.core.moving.joint.StructureJoint;
import dev.jstech.core.moving.joint.StructureJoints;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/**
 * The structures moving by their physics that are joined to each other, worked as one in a tick: the same steps for
 * all, each pushed by its own forces, and their contacts and joints worked together, again and again, so a chain of
 * structures, a crane's hook and its load, a train of carts, holds as one. A structure joined to none is an island of
 * its own. The first of them whose turn comes in a tick works them all; the others take what came of it.
 */
final class PhysicsIsland {

    private final Map<MovingStructure, StructurePhysics> members = new LinkedHashMap<>();
    private final List<StructureJoint> joints = new ArrayList<>();

    /* The most structures worked as one; more joined are worked on their own. */
    private static final int MOST_MEMBERS = 64;
    /* The most steps a tick is worked in, and less of the tick than this left is none. */
    private static final int MOST_STEPS = 12;
    private static final double LEAST_STEP = 1.0e-6;
    /* How many rounds the joints are drawn back together in a step. */
    private static final int ROUNDS = 4;

    private PhysicsIsland() {
    }

    /** Works the island of {@code leader} for a tick, {@code motion} the leader's rule; gives the leader's step. */
    static IStructureMotion.Step step(final ServerLevel level, final StructurePhysics leader,
                                      final PhysicsMotion motion) {
        final PhysicsIsland island = new PhysicsIsland();
        island.gather(level, leader);
        return island.work(level, leader, motion);
    }

    /** The physics of this island whose body {@code structure} is part of, or null. */
    @Nullable StructurePhysics member(final MovingStructure structure) {
        final StructurePhysics direct = members.get(structure);
        if (direct != null) {
            return direct;
        }
        for (final StructurePhysics member : members.values()) {
            if (member.carries(structure)) {
                return member;
            }
        }
        return null;
    }

    /**
     * The physics whose body {@code structure} is part of: of this island, or of any structure moving by its
     * physics; null for one moved by a rule.
     */
    @Nullable StructurePhysics owner(final MovingStructure structure) {
        final StructurePhysics member = member(structure);
        if (member != null) {
            return member;
        }
        final StructurePhysics root = StructureMounts.rootOf(structure).physics();
        return root != null && root.carries(structure) ? root : null;
    }

    /** Where {@code structure} stands now: mid-tick for one of this island, else where its frame has it. */
    StructureFrame standing(final MovingStructure structure) {
        final StructurePhysics member = member(structure);
        final StructureFrame mid = member == null ? null : member.standingOf(structure);
        return mid != null ? mid : structure.frame();
    }

    /*
     * The structures joined to the leader, moving by their physics, one joint after another, and the joints that
     * hold any of them; at most so many.
     */
    private void gather(final ServerLevel level, final StructurePhysics leader) {
        members.put(leader.structure(), leader);
        final Map<StructureJoint, Boolean> seen = new IdentityHashMap<>();
        final Deque<MovingStructure> next = new ArrayDeque<>();
        next.add(leader.structure());
        while (!next.isEmpty()) {
            final MovingStructure at = next.poll();
            final List<MovingStructure> family = new ArrayList<>();
            family(at, family, 0);
            for (final MovingStructure part : family) {
                for (final StructureJoint joint : StructureJoints.of(level, part.id())) {
                    if (seen.put(joint, Boolean.TRUE) != null) {
                        continue;
                    }
                    joints.add(joint);
                    for (final JointEnd end : List.of(joint.a(), joint.b())) {
                        if (end.onWorld() || members.size() >= MOST_MEMBERS) {
                            continue;
                        }
                        final MovingStructure other = MovingStructures.byId(level, end.structure().get());
                        final MovingStructure root = other == null ? null : StructureMounts.rootOf(other);
                        // One just let go has its physics made here, before its own first turn.
                        final StructurePhysics physics = root != null && root.motion() instanceof PhysicsMotion
                                ? root.physicsMade() : null;
                        // One that already took its turn this tick, on its own, is held as it stands.
                        if (physics != null && root.ready() && !members.containsKey(root)
                                && !physics.tookTurn(level.getGameTime())) {
                            members.put(root, physics);
                            next.add(root);
                        }
                    }
                }
            }
        }
    }

    /* {@code structure} and every structure it holds, however deep, into {@code into}. */
    private static void family(final MovingStructure structure, final List<MovingStructure> into, final int depth) {
        into.add(structure);
        if (depth >= MOST_MEMBERS) {
            return;
        }
        for (final MovingStructure child : StructureMounts.children(structure)) {
            family(child, into, depth + 1);
        }
    }

    /* One tick of the island: each readied, stepped together, and finished; the leader's step given. */
    private IStructureMotion.Step work(final ServerLevel level, final StructurePhysics leader,
                                       final PhysicsMotion leaderMotion) {
        final long now = level.getGameTime();
        final Map<StructurePhysics, PhysicsMotion> motions = new IdentityHashMap<>();
        final List<StructurePhysics> live = new ArrayList<>();
        for (final StructurePhysics member : members.values()) {
            final PhysicsMotion motion = member == leader ? leaderMotion
                    : member.structure().motion() instanceof PhysicsMotion own ? own : PhysicsMotion.AT_REST;
            motions.put(member, motion);
            if (member.begin(level, motion)) {
                live.add(member);
            } else {
                member.worked(now, new IStructureMotion.Step(member.structure().frame().pose(), motion));
            }
        }
        boolean dozing = true;
        for (final StructurePhysics member : live) {
            dozing &= member.dozing(level);
        }
        if (dozing) {
            for (final StructurePhysics member : live) {
                member.holdStill();
                member.worked(now, new IStructureMotion.Step(member.structure().frame().pose(), motions.get(member)));
            }
            return leaderStep(leader, leaderMotion);
        }
        final JointWork work = leader.joints();
        final List<StructureJoint> held = joints.isEmpty() ? joints : work.winch(level, joints);
        double left = 1 / StructurePhysics.TICKS;
        int taken = 0;
        while (left > LEAST_STEP && taken < MOST_STEPS) {
            final int stepsLeft = MOST_STEPS - taken;
            double step = left;
            for (final StructurePhysics member : live) {
                member.nextStep();
                step = Math.min(step, member.byGoing(left, stepsLeft));
            }
            // The forces are worked each step, and the step made as short as the stiffest of them needs: a light
            // boat's floating, known only once it touches the water, is too stiff to be held a whole tick.
            for (final StructurePhysics member : live) {
                member.gatherForces(level, step);
            }
            double steady = Double.MAX_VALUE;
            for (final StructurePhysics member : live) {
                steady = Math.min(steady, member.byFloating(left, stepsLeft));
            }
            if (steady < step) {
                step = steady;
                for (final StructurePhysics member : live) {
                    member.gatherForces(level, step);
                }
            }
            final double[] settles = new double[live.size()];
            for (int i = 0; i < live.size(); i++) {
                settles[i] = live.get(i).push(level, step, this);
            }
            for (final StructurePhysics member : live) {
                if (!member.findContacts(level, step, this)) {
                    // A chunk one of them reaches is not loaded: they wait where they are, as they went.
                    for (final StructurePhysics each : live) {
                        each.restore();
                        each.worked(now, new IStructureMotion.Step(each.structure().frame().pose(),
                                motions.get(each)));
                    }
                    return leaderStep(leader, leaderMotion);
                }
            }
            solve(level, live, settles, work, held, step);
            for (final StructurePhysics member : live) {
                member.drift(step);
            }
            if (work.any()) {
                work.project(level, this, held, step, ROUNDS);
            }
            left -= step;
            taken++;
        }
        work.breakWhatBroke(level);
        for (final StructurePhysics member : live) {
            member.worked(now, member.finish(level, motions.get(member)));
        }
        return leaderStep(leader, leaderMotion);
    }

    /*
     * Holds the island's contacts and joints over a step: each structure's contacts and the joints readied, then
     * worked a round each by turns, as often as the most a structure's contacts ask, so they agree.
     */
    private void solve(final ServerLevel level, final List<StructurePhysics> live, final double[] settles,
                       final JointWork work, final List<StructureJoint> held, final double step) {
        if (!held.isEmpty()) {
            work.build(level, this, held, step);
        }
        int times = 0;
        for (int i = 0; i < live.size(); i++) {
            times = Math.max(times, live.get(i).beginContacts(step, settles[i]));
        }
        if (!work.any() && live.size() == 1) {
            live.get(0).iterateContacts(times);
        } else {
            work.begin();
            for (int round = 0; round < times; round++) {
                for (final StructurePhysics member : live) {
                    member.contactsRound(round);
                }
                work.round(round);
            }
            work.end(step);
        }
        for (final StructurePhysics member : live) {
            member.endContacts();
        }
    }

    private static IStructureMotion.Step leaderStep(final StructurePhysics leader, final PhysicsMotion motion) {
        final IStructureMotion.Step step = leader.workedStep();
        return step != null ? step : new IStructureMotion.Step(leader.structure().frame().pose(), motion);
    }
}
