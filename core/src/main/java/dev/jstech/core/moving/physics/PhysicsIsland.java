/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving.physics;

import dev.jstech.core.moving.IStructureBody;
import dev.jstech.core.moving.IStructureMotion;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.StructureFrame;
import dev.jstech.core.moving.StructureMounts;
import dev.jstech.core.moving.joint.JointEnd;
import dev.jstech.core.moving.joint.StructureJoint;
import dev.jstech.core.moving.joint.StructureJoints;
import dev.jstech.core.region.Box;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

/**
 * The structures moving by their physics that are joined to each other, worked as one in a tick: the same steps for
 * all, each pushed by its own forces, and their contacts and joints worked together, again and again, so a chain of
 * structures, a crane's hook and its load, a train of carts, holds as one. A structure joined to none is an island of
 * its own. The first of them whose turn comes in a tick works them all; the others take what came of it.
 */
final class PhysicsIsland {

    private final Map<MovingStructure, StructurePhysics> members = new LinkedHashMap<>();
    private final List<StructureJoint> joints = new ArrayList<>();
    private final Map<StructurePhysics, PhysicsMotion> motions = new IdentityHashMap<>();
    private final List<StructurePhysics> live = new ArrayList<>();
    private final StructurePhysics leader;
    private final PhysicsMotion leaderMotion;
    private List<StructureJoint> held = List.of();
    private long now;
    /* Whether a chunk it reaches is not loaded, so it waits where it is this tick and nothing is finished. */
    private boolean waiting;
    /* How long its last solving took, in nanoseconds. */
    private long cost;
    /* Whether any of its structures were taken in for touching another, not only for a joint. */
    private boolean stacked;

    /* The most structures worked as one; more joined are worked on their own. */
    private static final int MOST_MEMBERS = 64;
    /* The most steps a tick is worked in, and less of the tick than this left is none. */
    private static final int MOST_STEPS = 12;
    private static final double LEAST_STEP = 1.0e-6;
    /* How many rounds the joints are drawn back together in a step. */
    private static final int ROUNDS = 4;
    /* How many more times contacts are worked in a step for each structure of the island past the first, and most. */
    private static final int PASSES_PER_MEMBER = 4;
    private static final int MOST_PASSES = 40;
    /* The fewest steps a tick of structures resting on one another is cut into. */
    private static final int STACKED_STEPS = 4;
    /*
     * How far round its structures an island reads the world in a tick beyond how far they go in it, in blocks: past
     * the farthest a magnet aboard pulls from, with room to spare.
     */
    static final double READ_REACH = 24;

    private PhysicsIsland(final StructurePhysics leader, final PhysicsMotion leaderMotion) {
        this.leader = leader;
        this.leaderMotion = leaderMotion;
    }

    /** Works the island of {@code leader} for a tick, {@code motion} the leader's rule; gives the leader's step. */
    static IStructureMotion.Step step(final ServerLevel level, final StructurePhysics leader,
                                      final PhysicsMotion motion) {
        final PhysicsIsland island = prepare(level, leader, motion);
        if (island != null) {
            island.solve(level);
            island.finish(level);
        }
        return leaderStep(leader, motion);
    }

    /**
     * Readies the island of {@code leader} for a tick, on the server's thread: its members gathered and begun, its
     * winches reeled. None when there is nothing to work, each member's step kept already: asleep, or of no mass.
     */
    static @Nullable PhysicsIsland prepare(final ServerLevel level, final StructurePhysics leader,
                                           final PhysicsMotion motion) {
        final PhysicsIsland island = new PhysicsIsland(leader, motion);
        island.gather(level, leader);
        return island.ready(level) ? island : null;
    }

    /** The leader whose turn worked it. */
    StructurePhysics leader() {
        return leader;
    }

    /** How long its last solving took, in nanoseconds. */
    long cost() {
        return cost;
    }

    /**
     * The box of the world it may read this tick: its structures as they stand, grown by how far they may go in a
     * tick and by how far round them it reads.
     */
    AABB reach() {
        AABB box = null;
        for (final StructurePhysics member : members.values()) {
            final AABB own = member.reachThisTick().inflate(READ_REACH);
            box = box == null ? own : box.minmax(own);
        }
        return box == null ? new AABB(0, 0, 0, 0, 0, 0) : box;
    }

    /**
     * Takes into {@code lens} every chunk it may read this tick: the world's within its reach, and the yard's of its
     * own structures and of every structure within its reach, whose blocks it meets.
     */
    void lend(final ServerLevel level, final PhysicsLens lens) {
        final AABB box = reach();
        lens.take(Mth.floor(box.minX) >> 4, Mth.floor(box.minZ) >> 4, Mth.floor(box.maxX) >> 4,
                Mth.floor(box.maxZ) >> 4);
        for (final StructurePhysics member : members.values()) {
            member.lendYard(lens);
        }
        for (final IStructureBody near : MovingStructures.near(level, box)) {
            final Box yard = near.bounds();
            lens.take((yard.minX() - 1) >> 4, (yard.minZ() - 1) >> 4, (yard.maxX() + 1) >> 4,
                    (yard.maxZ() + 1) >> 4);
        }
    }

    /**
     * The islands of {@code islands} in groups that lie apart: each group's reach meets no other's, so groups are
     * worked at once and the islands of one group in turn.
     */
    static List<List<PhysicsIsland>> apart(final List<PhysicsIsland> islands) {
        final List<List<PhysicsIsland>> groups = new ArrayList<>();
        final List<AABB> reaches = new ArrayList<>();
        for (final PhysicsIsland island : islands) {
            final List<PhysicsIsland> joined = new ArrayList<>(List.of(island));
            AABB together = island.reach();
            // A group grown by one it met may meet another it did not before: looked over again till none joins.
            boolean grew = true;
            while (grew) {
                grew = false;
                for (int i = groups.size() - 1; i >= 0; i--) {
                    if (reaches.get(i).intersects(together)) {
                        joined.addAll(groups.remove(i));
                        together = together.minmax(reaches.remove(i));
                        grew = true;
                    }
                }
            }
            groups.add(joined);
            reaches.add(together);
        }
        return groups;
    }

    /** The farthest any of its structures being worked may go over a step of {@code seconds}, as each looks ahead. */
    double widest(final double seconds) {
        double most = 0;
        for (final StructurePhysics member : live) {
            most = Math.max(most, member.margin(seconds));
        }
        return most;
    }

    /*
     * The farthest any of its structures has gone since the tick began. The structures near are found where they stood
     * as it began: one of the island looked for mid-tick may be as much further.
     */
    double goneMost() {
        double most = 0;
        for (final StructurePhysics member : live) {
            most = Math.max(most, member.goneThisTick());
        }
        return most;
    }

    /* The tick it is worked in. */
    long now() {
        return now;
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
            touching(level, at, next);
        }
    }

    /*
     * The structures moving by their physics, awake, that {@code at}, one of the island, may meet this tick: worked
     * with it, so a stack of them holds as one, each held up by what is under it. One asleep is not taken in: met
     * gently it holds as the ground does, and met hard it wakes and takes its own turn. One just let go is taken in
     * before its own first turn, and so is the island's own: a stack let go at once is worked as one from its first
     * tick, never each of it alone, falling a tick on what has gone already.
     */
    private void touching(final ServerLevel level, final MovingStructure at, final Deque<MovingStructure> next) {
        final StructurePhysics own = members.get(at);
        if (own == null) {
            return;
        }
        for (final IStructureBody near : MovingStructures.near(level, own.reachThisTick())) {
            if (members.size() >= MOST_MEMBERS) {
                return;
            }
            if (!(near instanceof MovingStructure them) || !them.ready()) {
                continue;
            }
            final MovingStructure root = StructureMounts.rootOf(them);
            if (members.containsKey(root) || !root.ready() || !(root.motion() instanceof PhysicsMotion)) {
                continue;
            }
            final StructurePhysics physics = root.physicsMade();
            if (!physics.asleep() && !physics.tookTurn(level.getGameTime())) {
                members.put(root, physics);
                next.add(root);
                stacked = true;
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

    /*
     * The start of the island's tick, on the server's thread: each member readied, those of no mass given their step
     * at once, and its winches reeled; false when there is nothing to work, every member asleep or of no mass.
     */
    private boolean ready(final ServerLevel level) {
        now = level.getGameTime();
        for (final StructurePhysics member : members.values()) {
            member.taken(now);
        }
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
            return false;
        }
        upward(level);
        held = joints.isEmpty() ? joints : leader.joints().winch(level, joints);
        return true;
    }

    /*
     * Orders its structures being worked from the lowest to the highest against the pull where the leader stands: a
     * stack is held up from the ground through each, so its rounds go up it and back down, as one solver of all their
     * contacts would, not in the order they were gathered in, which leaves a tall stack swaying.
     */
    private void upward(final ServerLevel level) {
        if (live.size() < 2) {
            return;
        }
        final Vector3d pull = PhysicsWorld.gravity(level, leader.body().position(), new Vector3d());
        live.sort(Comparator.comparingDouble(member -> -member.body().position().dot(pull)));
    }

    /*
     * The island stepped through the tick, its members together, reading the world and changing nothing of it: on
     * the server's thread, or beside it through a lens. A chunk one of them reaches not loaded, they wait where they
     * stood, as they went.
     */
    void solve(final ServerLevel level) {
        final long start = System.nanoTime();
        try {
            steps(level);
        } finally {
            cost = System.nanoTime() - start;
        }
    }

    /*
     * The end of the island's tick, on the server's thread: what broke broken, and each member finished, its step
     * kept for its turn.
     */
    void finish(final ServerLevel level) {
        if (waiting) {
            return;
        }
        leader.joints().breakWhatBroke(level);
        // Its structures fall asleep together, once each of them has been still long enough, or none does.
        boolean quiet = true;
        for (final StructurePhysics member : live) {
            quiet &= member.finish(level);
        }
        for (final StructurePhysics member : live) {
            member.worked(now, member.rest(level, quiet, motions.get(member)));
        }
    }

    /*
     * Waits the island where it stood this tick, going as it went: a chunk it reaches is not loaded, or its work
     * beside the server's thread failed.
     */
    void hold() {
        waiting = true;
        for (final StructurePhysics each : live) {
            each.restore();
            each.worked(now, new IStructureMotion.Step(each.structure().frame().pose(), motions.get(each)));
        }
    }

    /* The steps of the tick. */
    private void steps(final ServerLevel level) {
        final JointWork work = leader.joints();
        double left = 1 / StructurePhysics.TICKS;
        int taken = 0;
        while (left > LEAST_STEP && taken < MOST_STEPS) {
            final int stepsLeft = MOST_STEPS - taken;
            // Structures resting on one another are worked in shorter steps, each sinking into the next less before
            // it is held up, so a stack settles still rather than shaking.
            double step = stacked ? Math.min(left, 1 / StructurePhysics.TICKS / STACKED_STEPS) : left;
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
                    hold();
                    return;
                }
            }
            holdTogether(level, settles, work, step);
            for (final StructurePhysics member : live) {
                member.drift(step);
            }
            if (work.any()) {
                work.project(level, this, held, step, ROUNDS);
            }
            left -= step;
            taken++;
        }
    }

    /*
     * Holds the island's contacts and joints over a step: each structure's contacts and the joints readied, then
     * worked a round each by turns, as often as the most a structure's contacts ask, so they agree.
     */
    private void holdTogether(final ServerLevel level, final double[] settles, final JointWork work,
                              final double step) {
        if (!held.isEmpty()) {
            work.build(level, this, held, step);
        }
        int times = 0;
        for (int i = 0; i < live.size(); i++) {
            times = Math.max(times, live.get(i).readyContacts(step, settles[i]));
        }
        /*
         * Every contact is readied as the bodies come before any starts from its last push: started one structure
         * after another, each would read the bodies as those before pushed them, and a stack at rest would read itself
         * closing as fast as every crate's start pushed it, a blow at every step.
         */
        for (final StructurePhysics member : live) {
            member.warmContacts();
        }
        // A push travels one structure a pass: a stack of them needs a pass more for each, to hold its top up.
        if (stacked) {
            times = Math.max(times, Math.min(MOST_PASSES, times + PASSES_PER_MEMBER * (live.size() - 1)));
        }
        if (!work.any() && live.size() == 1) {
            live.get(0).iterateContacts(times);
        } else {
            work.begin();
            for (int round = 0; round < times; round++) {
                // Up the island and back down by turns, as each structure's own contacts are worked there and back.
                for (int i = 0; i < live.size(); i++) {
                    live.get(round % 2 == 0 ? i : live.size() - 1 - i).contactsRound(round);
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
