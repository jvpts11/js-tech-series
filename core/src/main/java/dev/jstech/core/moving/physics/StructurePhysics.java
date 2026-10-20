/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving.physics;

import dev.jstech.core.JsCore;
import dev.jstech.core.diagnostic.Diagnostics;
import dev.jstech.core.moving.IStructureBody;
import dev.jstech.core.moving.IStructureMotion;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.StructureFrame;
import dev.jstech.core.moving.StructurePose;
import dev.jstech.core.region.Box;
import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix3d;
import org.joml.Quaterniond;
import org.joml.Vector3d;

/**
 * A structure moving by its physics, as a body of real mass: its blocks' mass, centre and inertia, kept as blocks
 * are set on it and broken off it; the forces on it each tick, its weight, engines, wings, drag, floating, balloons,
 * levitation and magnets ({@link StructureForces}); its wheels ({@link StructureWheels}); and what it meets, the world
 * and the other structures ({@link StructureContacts}). A tick is worked in as many steps as keep it from going more
 * than half a block in one, so a fast structure meets what it meets. A structure left still a second falls asleep and
 * costs next to nothing, looking about twice a second, and wakes the moment a block of it changes, it is pushed, or
 * what holds it goes.
 */
public final class StructurePhysics {

    private final MovingStructure structure;
    private final StructureParts parts = new StructureParts();
    private final RigidBody body = new RigidBody();
    private final ForceSum forces = new ForceSum();
    private final StructureForces gather = new StructureForces();
    private final StructureContacts contacts = new StructureContacts();
    private final StructureWheels wheels = new StructureWheels();
    private final StructureBalloons balloons = new StructureBalloons();
    private final StructureHeating heating = new StructureHeating();
    private final Matrix3d inertia = new Matrix3d();
    /* Its centre of mass in the yard, as last worked out. */
    private final Vector3d centre = new Vector3d();
    private final Vector3d scratch = new Vector3d();
    private final Vector3d wheelForce = new Vector3d();
    private final Vector3d startVelocity = new Vector3d();
    private final Vector3d startSpin = new Vector3d();
    private final Quaterniond turn = new Quaterniond();
    /* Where it stood when it last began to be still. */
    private final Vector3d anchor = new Vector3d();
    private final Quaterniond anchorTurn = new Quaterniond();
    private boolean filled;
    private boolean asleep;
    /* Whether it waited last tick, the dimension's physics having taken its share. */
    private boolean waited;
    private int still;
    private long woke;
    private List<PhysicsMotion.WarmRoom> warmSaved = List.of();

    /** Ticks in a second. */
    public static final double TICKS = 20;
    /** The name a structure's tick of physics is timed under, in the Core's diagnostics. */
    public static final ResourceLocation TIMED = ResourceLocation.fromNamespaceAndPath(JsCore.MODID,
            "structure_physics");
    /* The most steps a tick is worked in, and the furthest a structure goes in one, in blocks. */
    private static final int MOST_STEPS = 12;
    private static final double STEP_REACH = 0.5;
    /* Less of the tick than this left is none. */
    private static final double LEAST_STEP = 1.0e-6;
    /* The most a stiff hold swings it in a step, in radians of its swing, for the step to hold steady. */
    private static final double STEADY_SWING = 0.5;
    /* How far ahead of time contacts are looked for beyond what a step goes. */
    private static final double MARGIN = 0.05;
    /* A structure asleep pushed faster than these, in metres and radians a second, wakes. */
    private static final double SLEEP_SPEED = 0.05;
    private static final double SLEEP_SPIN = 0.05;
    /*
     * A structure slower than these, never further than these from where it stood, a second on end, falls asleep;
     * one going faster wakes those asleep near it.
     */
    private static final double STILL_SPEED = 0.25;
    private static final double STILL_SPIN = 0.25;
    private static final double STILL_DRIFT = 0.01;
    private static final double STILL_TURN = 0.005;
    private static final int SLEEP_AFTER = 20;
    /* How often an asleep structure looks about, in ticks. */
    private static final int LOOK_ABOUT = 10;
    /* A bounce under a strong pull is told from resting by more than twice what the pull gives in a step. */
    private static final double SETTLES_LEAST = 1.0;
    /* How near a structure that moves wakes one asleep, in blocks. */
    private static final double WAKE_REACH = 0.5;

    private StructurePhysics(final MovingStructure moved) {
        this.structure = moved;
    }

    /** The physics of {@code structure}, made as it starts to move by them. */
    public static StructurePhysics of(final MovingStructure structure) {
        return new StructurePhysics(structure);
    }

    /**
     * Wakes every structure asleep near {@code moved}, which just moved, by its rule or its physics: one resting on it
     * or against it meets it again at once, carried, pushed or let fall.
     */
    public static void wakeAround(final MovingStructure moved) {
        if (!(moved.level() instanceof ServerLevel level)) {
            return;
        }
        // Settling as slowly as a structure falls asleep at, it wakes none: two settling side by side would keep
        // waking each other.
        final double[] going = moved.velocity();
        final double[] turning = moved.spin();
        if (Math.sqrt(going[0] * going[0] + going[1] * going[1] + going[2] * going[2]) * TICKS < STILL_SPEED
                && Math.sqrt(turning[0] * turning[0] + turning[1] * turning[1] + turning[2] * turning[2]) * TICKS
                < STILL_SPIN) {
            return;
        }
        for (final IStructureBody near : MovingStructures.near(level, moved.worldBox().inflate(WAKE_REACH))) {
            if (near != moved && near instanceof MovingStructure them) {
                final StructurePhysics physics = them.physics();
                if (physics != null && physics.asleep()) {
                    physics.wake();
                }
            }
        }
    }

    /** What it is made of. */
    public StructureParts parts() {
        return parts;
    }

    /** Its body as it moves, its centre of mass in the world. */
    public RigidBody body() {
        return body;
    }

    /** The forces on it in the last tick, by their kind. */
    public ForceSum forces() {
        return forces;
    }

    /** Whether it is asleep, still and costing next to nothing. */
    public boolean asleep() {
        return asleep;
    }

    /** Whether it moves by its physics now: it has taken in its blocks. */
    public boolean active() {
        return filled;
    }

    /** Wakes it: something about it changed. */
    public void wake() {
        asleep = false;
        still = 0;
    }

    /** A block of it changed: its mass and shape follow, and it wakes. */
    public void blockChanged(final ServerLevel level, final BlockPos pos, final BlockState before,
                             final BlockState after) {
        if (filled) {
            parts.change(level, pos, before, after);
            wake();
        }
    }

    /** Lends its last contacts with the world to {@code into}, for its body against {@code ground} there. */
    void lendWorld(final ContactSolver into, final RigidBody theirs, final RigidBody ground) {
        contacts.lendWorld(into, theirs, ground);
    }

    /** Readies its body for the pushes of what meets it now, as it is turned now. */
    public void prepareBody() {
        parts.mass().inertia(inertia);
        body.prepare(parts.mass().mass(), inertia);
    }

    /**
     * One tick of it under {@code motion}, the rule it moves by: where it goes, and the rule that goes on from there,
     * holding how fast it goes for a restart.
     */
    public IStructureMotion.Step step(final ServerLevel level, final PhysicsMotion motion) {
        final PhysicsBudget budget = PhysicsBudget.of(level);
        if (budget.spent() && !waited) {
            // The dimension's physics took its share of this tick: it waits, as it is, and goes the next whatever.
            waited = true;
            return new IStructureMotion.Step(structure.frame().pose(), motion);
        }
        waited = false;
        final long start = System.nanoTime();
        try {
            return stepTimed(level, motion);
        } finally {
            budget.spend(System.nanoTime() - start);
            Diagnostics.record(TIMED, start);
        }
    }

    /* One tick of it, timed by {@link #step}. */
    private IStructureMotion.Step stepTimed(final ServerLevel level, final PhysicsMotion motion) {
        final StructureFrame frame = structure.frame();
        if (!filled) {
            parts.fill(level, structure.bounds());
            filled = true;
            body.velocity().set(motion.vx(), motion.vy(), motion.vz());
            body.spin().set(motion.sx(), motion.sy(), motion.sz());
            balloons.warm(motion.warmRooms());
            warmSaved = motion.warm();
            contacts.reset();
        }
        if (parts.mass().empty()) {
            return new IStructureMotion.Step(frame.pose(), motion);
        }
        if (parts.takeChanged()) {
            recentre(frame);
        }
        follow(frame);
        if (asleep) {
            // Pushed by another, it wakes; else it looks about now and then, and sleeps on if nothing moves it.
            if (body.velocity().length() < SLEEP_SPEED && body.spin().length() < SLEEP_SPIN
                    && (level.getGameTime() - woke) % LOOK_ABOUT != 0) {
                body.velocity().zero();
                body.spin().zero();
                return new IStructureMotion.Step(frame.pose(), motion);
            }
        }
        final double seconds = 1 / TICKS;
        final Box bounds = structure.bounds();
        final double mass = parts.mass().mass();
        parts.mass().inertia(inertia);
        final double reach = reach(bounds);
        startVelocity.set(body.velocity());
        startSpin.set(body.spin());
        final Vector3d startPosition = new Vector3d(body.position());
        final Quaterniond startTurn = new Quaterniond(body.orientation());
        wheelForce.zero();
        boolean warming = false;
        StructureFrame at = frame;
        double left = seconds;
        int taken = 0;
        while (left > LEAST_STEP && taken < MOST_STEPS) {
            if (taken > 0) {
                at = frameOf(at);
            }
            // The forces are worked each step, and the step made as short as the stiffest of them needs: a light
            // boat's floating, known only once it touches the water, is too stiff to be held a whole tick.
            double step = Math.min(left, byGoing(reach, left, MOST_STEPS - taken));
            forces.clear(body.position());
            gather.gather(level, at, parts, bounds, body, step, forces);
            final double steady = byFloating(mass, left, MOST_STEPS - taken);
            if (steady < step) {
                step = steady;
                forces.clear(body.position());
                gather.gather(level, at, parts, bounds, body, step, forces);
            }
            warming |= balloons.gather(level, at, parts, bounds, gather.gravity(), step, forces);
            // A blow bounces only when it closes faster than the forces on it would close it in two such steps.
            final double settles = Math.max(SETTLES_LEAST, 2 * forces.force().length() / mass * step);
            body.prepare(mass, inertia);
            body.accelerate(forces.force(), forces.torque(), mass, inertia, step);
            wheels.step(level, structure, at, parts, body, step, wheelForce);
            final double margin = (body.velocity().length() + body.spin().length() * reach) * step + MARGIN;
            if (!contacts.step(level, structure, at, parts, body, margin, step, settles)) {
                // A chunk it reaches is not loaded: it waits where it is, as it went.
                body.position().set(startPosition);
                body.orientation().set(startTurn);
                body.velocity().set(startVelocity);
                body.spin().set(startSpin);
                return new IStructureMotion.Step(frame.pose(), motion);
            }
            body.drift(inertia, step);
            left -= step;
            taken++;
        }
        forces.note(ForceSum.ForceKind.WHEELS, wheelForce.div(taken));
        contacts.breakWhatBroke(level);
        final StructureFrame after = frameOf(at);
        heating.heat(level, after, parts, body, seconds);
        settle(level, warming);
        if (asleep) {
            return new IStructureMotion.Step(frame.pose(), saved(motion, warming));
        }
        return new IStructureMotion.Step(after.pose(), saved(motion, warming));
    }

    /*
     * Falls asleep after a second still, nothing warming: slow, and never more than a hair from where it stood a
     * second ago, so a cart swaying on its springs in the wind, by a few millimetres, sleeps; or stays awake.
     */
    private void settle(final ServerLevel level, final boolean warming) {
        final boolean slow = body.velocity().length() < STILL_SPEED && body.spin().length() < STILL_SPIN;
        final boolean near = body.position().distance(anchor) < STILL_DRIFT && turnFromAnchor() < STILL_TURN;
        if (!warming && slow && near) {
            still++;
            if (still >= SLEEP_AFTER && !asleep) {
                asleep = true;
                woke = level.getGameTime();
                body.velocity().zero();
                body.spin().zero();
            }
        } else {
            still = 0;
            asleep = false;
            anchor.set(body.position());
            anchorTurn.set(body.orientation());
        }
    }

    /* How far it is turned from where it stood when it last began to be still, in radians. */
    private double turnFromAnchor() {
        final double dot = Math.abs(anchorTurn.x * body.orientation().x + anchorTurn.y * body.orientation().y
                + anchorTurn.z * body.orientation().z + anchorTurn.w * body.orientation().w);
        return 2 * Math.acos(Math.min(1, dot));
    }

    /* The rule that goes on, holding how fast it goes and its balloons' warm air, for a restart. */
    private PhysicsMotion saved(final PhysicsMotion was, final boolean warming) {
        if (warming || warmSaved.size() != balloons.warm().size()) {
            final List<PhysicsMotion.WarmRoom> rooms = new ArrayList<>();
            for (final Long2DoubleMap.Entry room : balloons.warm().long2DoubleEntrySet()) {
                rooms.add(new PhysicsMotion.WarmRoom(room.getLongKey(), room.getDoubleValue()));
            }
            warmSaved = List.copyOf(rooms);
        }
        final Vector3d velocity = body.velocity();
        final Vector3d spin = body.spin();
        if (velocity.x == was.vx() && velocity.y == was.vy() && velocity.z == was.vz() && spin.x == was.sx()
                && spin.y == was.sy() && spin.z == was.sz() && warmSaved == was.warm()) {
            return was;
        }
        return new PhysicsMotion(velocity.x, velocity.y, velocity.z, spin.x, spin.y, spin.z, warmSaved);
    }

    /* Its body stands where its frame does: its centre where its frame has it, turned as it is. */
    private void follow(final StructureFrame frame) {
        final StructurePose pose = frame.pose();
        body.orientation().set(pose.qx(), pose.qy(), pose.qz(), pose.qw());
        FrameMath.world(frame, centre, body.position());
    }

    /*
     * Its blocks changed, and its centre of mass with them: its body's centre is the new one, going as that point of
     * it went, so nothing it does jumps.
     */
    private void recentre(final StructureFrame frame) {
        final Vector3d moved = parts.mass().centre(new Vector3d());
        final Vector3d world = FrameMath.world(frame, moved, new Vector3d());
        if (filled && centre.lengthSquared() > 0) {
            body.velocityAt(world, scratch);
            body.velocity().set(scratch);
        }
        centre.set(moved);
        wake();
    }

    /* Its frame as its body stands now: its pivot where its centre puts it. */
    private StructureFrame frameOf(final StructureFrame was) {
        turn.set(body.orientation());
        scratch.set(was.pivotX() - centre.x, was.pivotY() - centre.y, was.pivotZ() - centre.z);
        turn.transform(scratch);
        scratch.add(body.position());
        final StructurePose pose = new StructurePose(scratch.x, scratch.y, scratch.z, turn.x, turn.y, turn.z,
                turn.w);
        return new StructureFrame(pose, was.pivotX(), was.pivotY(), was.pivotZ());
    }

    /*
     * The longest step of the {@code left} of the tick in which it goes no more than half a block, its edges too;
     * never so short that the rest of the tick takes more than the {@code steps} it has left.
     */
    private double byGoing(final double reach, final double left, final int steps) {
        final double going = body.velocity().length() + body.spin().length() * reach;
        final double step = going > 0 ? left / Math.max(1, Math.ceil(going * left / STEP_REACH)) : left;
        return Math.max(step, left / Math.max(1, steps));
    }

    /*
     * The longest step in which the stiffest hold on it, a liquid's surface, as just gathered, swings it no more than
     * half a radian, so it holds steady; never so short that the {@code left} of the tick takes more than the
     * {@code steps} it has left.
     */
    private double byFloating(final double mass, final double left, final int steps) {
        final double swinging = mass > 0 ? Math.sqrt(gather.floating() / mass) : 0;
        if (swinging <= 0) {
            return Double.MAX_VALUE;
        }
        return Math.max(STEADY_SWING / swinging, left / Math.max(1, steps));
    }

    /* How far its furthest block is from its centre, for how fast its turning carries its edge. */
    private double reach(final Box bounds) {
        final double x = Math.max(Math.abs(bounds.minX() - centre.x), Math.abs(bounds.maxX() + 1 - centre.x));
        final double y = Math.max(Math.abs(bounds.minY() - centre.y), Math.abs(bounds.maxY() + 1 - centre.y));
        final double z = Math.max(Math.abs(bounds.minZ() - centre.z), Math.abs(bounds.maxZ() + 1 - centre.z));
        return Math.sqrt(x * x + y * y + z * z);
    }
}
