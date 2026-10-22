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
import dev.jstech.core.moving.ForeignMovers;
import dev.jstech.core.moving.IStructureBody;
import dev.jstech.core.moving.IStructureMotion;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.StructureControls;
import dev.jstech.core.moving.StructureFrame;
import dev.jstech.core.moving.StructureMount;
import dev.jstech.core.moving.StructureMounts;
import dev.jstech.core.moving.StructurePose;
import dev.jstech.core.moving.StructureSync;
import dev.jstech.core.region.Box;
import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3d;
import org.joml.Quaterniond;
import org.joml.Vector3d;

/**
 * A structure moving by its physics, as a body of real mass: its blocks' mass, centre and inertia, kept as blocks
 * are set on it and broken off it; the forces on it each tick, its weight, engines, wings, drag, floating, balloons,
 * levitation and magnets ({@link StructureForces}); its wheels ({@link StructureWheels}); what it meets, the world
 * and the other structures ({@link StructureContacts}); and the joints that hold it ({@link JointWork}), with which the
 * structures it is joined to move with it as one ({@link PhysicsIsland}). What it holds, its turrets and the shuttles
 * docked to it ({@link StructureMounts}), is part of its body: their mass, their engines, their wheels and their blocks
 * meeting the world all its own. A tick is worked in as many steps as keep it from going more than half a block in
 * one, so a fast structure meets what it meets. A structure left still a second falls asleep and costs next to
 * nothing, looking about twice a second, and wakes the moment a block of it changes, it is pushed, or what holds it
 * goes.
 */
public final class StructurePhysics {

    private final MovingStructure structure;
    private final Piece own;
    /* What it holds, by the structure held, and each one's place in its yard. */
    private final Map<MovingStructure, Piece> held = new IdentityHashMap<>();
    private final RigidBody body = new RigidBody();
    private final ForceSum forces = new ForceSum();
    private final JointWork joints = new JointWork();
    private final CompoundMass compound = new CompoundMass();
    private final Matrix3d inertia = new Matrix3d();
    private final Matrix3d pieceInertia = new Matrix3d();
    private final Matrix3d turnMatrix = new Matrix3d();
    /* Its centre of mass in the yard, as last worked out. */
    private final Vector3d centre = new Vector3d();
    private final Vector3d scratch = new Vector3d();
    private final Vector3d wheelForce = new Vector3d();
    private final Vector3d startVelocity = new Vector3d();
    private final Vector3d startSpin = new Vector3d();
    private final Vector3d startPosition = new Vector3d();
    private final Quaterniond startTurn = new Quaterniond();
    private final Quaterniond turn = new Quaterniond();
    /* Where it stood when it last began to be still. */
    private final Vector3d anchor = new Vector3d();
    private final Quaterniond anchorTurn = new Quaterniond();
    private double mass;
    private boolean filled;
    private boolean asleep;
    /* Whether it waited last tick, the dimension's physics having taken its share. */
    private boolean waited;
    private int still;
    private long woke;
    private List<PhysicsMotion.WarmRoom> warmSaved = List.of();
    private Map<Long, Double> warmGiven = Map.of();
    /* The tick worked: where it stands in it, how many steps it took, and whether its balloons warmed. */
    private @Nullable StructureFrame at;
    private int taken;
    private boolean warming;
    /* The tick it was last worked in, with the structures it is joined to, and what came of it. */
    private long workedAt = Long.MIN_VALUE;
    private long askedAt = Long.MIN_VALUE;
    /* What its last tick's work cost, in nanoseconds. */
    private long lastCost;
    private IStructureMotion.Step worked;
    /* Pushes given from outside since its last tick, each a push in newton seconds and the world's place it is at. */
    private final List<Vector3d[]> pushes = new ArrayList<>();
    /* What holds it from outside, the physics tool's grip or a mod's, or null for nothing. */
    private @Nullable Hold hold;

    /** Ticks in a second. */
    public static final double TICKS = 20;
    /** The name a structure's tick of physics is timed under, in the Core's diagnostics. */
    public static final ResourceLocation TIMED = ResourceLocation.fromNamespaceAndPath(JsCore.MODID,
            "structure_physics");
    /* How far ahead of time contacts are looked for beyond what a step goes. */
    private static final double MARGIN = 0.05;
    /* The furthest a structure goes in a step, in blocks. */
    private static final double STEP_REACH = 0.5;
    /* The most a stiff hold swings it in a step, in radians of its swing, for the step to hold steady. */
    private static final double STEADY_SWING = 0.5;
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
    /* A centre that moved less than this, in blocks, has not moved. */
    private static final double SAME_CENTRE = 1.0e-9;
    /* An engine or a wheel looking within this of a way pushes along it, as one answers a control. */
    private static final double ANSWERS = 0.7;
    /* How fast a held structure swings to where it is held, in radians a second: about a second to come there. */
    private static final double HOLD_SWING = 4.0;
    private static final double HALF = 0.5;

    private StructurePhysics(final MovingStructure moved) {
        this.structure = moved;
        this.own = new Piece(moved);
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
                final StructurePhysics physics = StructureMounts.rootOf(them).physics();
                if (physics != null && physics.asleep()) {
                    physics.wake();
                }
            }
        }
    }

    /** The structure moved. */
    public MovingStructure structure() {
        return structure;
    }

    /** What it is made of, its own blocks. */
    public StructureParts parts() {
        return own.parts;
    }

    /** Its body as it moves, its centre of mass in the world. */
    public RigidBody body() {
        return body;
    }

    /** The forces on it in the last tick, by their kind. */
    public ForceSum forces() {
        return forces;
    }

    /** Its whole mass, what it holds with it, in kilograms, as last worked out. */
    public double mass() {
        return mass;
    }

    /**
     * How fast its engines and driving wheels speed it up along {@code way}, a way of the world, at full: each engine
     * pushing along it and each wheel rolling along it, as nearly as each does, over its whole mass, in metres a
     * second a second; nought for none. What an autopilot works its controls by, so a heavy ship and a light car are
     * flown alike.
     */
    public double authority(final Vec3 way) {
        if (mass <= 0) {
            return 0;
        }
        final StructureFrame frame = structure.frame();
        double newtons = pushAlong(own, frame, way);
        for (final Piece piece : held.values()) {
            newtons += pushAlong(piece, new StructureFrame(frame.placed(piece.inRoot), piece.structure.pivotX(),
                    piece.structure.pivotY(), piece.structure.pivotZ()), way);
        }
        return newtons / mass;
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

    /**
     * Pushes it by {@code impulse}, in newton seconds, at the world's place {@code at}, as it next steps: its speed and
     * its spin change at once, as at a blow, and it wakes.
     */
    public void push(final Vector3d impulse, final Vector3d at) {
        pushes.add(new Vector3d[] {new Vector3d(impulse), new Vector3d(at)});
        wake();
    }

    /**
     * Holds its point at the yard's place {@code point} towards the world's place {@code target}, as a stiff spring
     * damped to stop it swinging, bearing its weight, with at most {@code most} newtons, until the game's tick
     * {@code until}: the physics tool's grip, renewed each tick it holds. It wakes.
     */
    public void hold(final Vector3d point, final Vector3d target, final double most, final long until) {
        hold = new Hold(new Vector3d(point), new Vector3d(target), most, until);
        wake();
    }

    /** Lets go of what held it from outside. */
    public void letGo() {
        hold = null;
    }

    /** Whether something holds it from outside now. */
    public boolean heldFromOutside() {
        return hold != null;
    }

    /** A block of it changed: its mass and shape follow, and it wakes. */
    public void blockChanged(final ServerLevel level, final BlockPos pos, final BlockState before,
                             final BlockState after) {
        if (filled) {
            own.parts.change(level, pos, before, after);
            wake();
        }
    }

    /** A block of {@code piece}, which it holds, changed: the whole body's mass and shape follow, and it wakes. */
    public void pieceChanged(final MovingStructure piece, final ServerLevel level, final BlockPos pos,
                             final BlockState before, final BlockState after) {
        final Piece part = held.get(piece);
        if (part != null && part.filled) {
            part.parts.change(level, pos, before, after);
            wake();
        }
    }

    /** Readies its body for the pushes of what meets it now, as it is turned now. */
    public void prepareBody() {
        if (!held.isEmpty() || mass > 0) {
            body.prepare(mass, inertia);
            return;
        }
        own.parts.mass().inertia(inertia);
        body.prepare(own.parts.mass().mass(), inertia);
    }

    /**
     * One tick of it under {@code motion}, the rule it moves by: where it goes, and the rule that goes on from there,
     * holding how fast it goes for a restart. A structure joined to others moving by their physics is worked with
     * them, all in the tick of the first of them to come.
     */
    public IStructureMotion.Step step(final ServerLevel level, final PhysicsMotion motion) {
        if (workedAt == level.getGameTime() && worked != null) {
            return worked;
        }
        askedAt = level.getGameTime();
        final PhysicsBudget budget = PhysicsBudget.of(level);
        if (budget.spent() && !waited) {
            // The dimension's physics took its share of this tick: it waits, as it is, and goes the next whatever.
            waited = true;
            return new IStructureMotion.Step(structure.frame().pose(), motion);
        }
        waited = false;
        final long start = System.nanoTime();
        try {
            return PhysicsIsland.step(level, this, motion);
        } finally {
            lastCost = System.nanoTime() - start;
            budget.spend(lastCost);
            Diagnostics.record(TIMED, start);
        }
    }

    /*
     * Readies its island for this tick, to be worked beside the server's thread, from the start of the dimension's
     * tick: none when it took its turn already, waits on the dimension's budget (the islands readied before it this
     * tick expected to cost {@code expected} nanoseconds, as they did the last), or has nothing to work.
     */
    @Nullable PhysicsIsland readyBeside(final ServerLevel level, final PhysicsMotion motion, final long expected) {
        final long now = level.getGameTime();
        if (tookTurn(now)) {
            return null;
        }
        askedAt = now;
        if (PhysicsBudget.of(level).spentWith(expected) && !waited) {
            waited = true;
            worked(now, new IStructureMotion.Step(structure.frame().pose(), motion));
            return null;
        }
        waited = false;
        return PhysicsIsland.prepare(level, this, motion);
    }

    /* What its last tick's work cost, in nanoseconds: what its next is expected to. */
    long lastCost() {
        return lastCost;
    }

    /* Keeps what its tick's work beside the server's thread cost, in nanoseconds, against the dimension's budget. */
    void spentBeside(final ServerLevel level, final long nanos) {
        lastCost = nanos;
        PhysicsBudget.of(level).spend(nanos);
        Diagnostics.record(TIMED, System.nanoTime() - nanos);
    }

    /* It is worked in an island this tick, readied already: no other island takes it in. */
    void taken(final long now) {
        askedAt = now;
    }

    /*
     * The box of the world its structure, and what it holds, may fill this tick: where they stand, grown by how far
     * its going and its turning may carry them in a tick.
     */
    AABB reachThisTick() {
        final double going = (body.velocity().length() + body.spin().length() * reach()) / TICKS + 1;
        AABB box = structure.worldBox();
        for (final Piece piece : held.values()) {
            box = box.minmax(piece.structure.worldBox());
        }
        return box.inflate(going);
    }

    /* Takes into {@code lens} the yard's chunks its blocks and those of what it holds stand in, and those beside. */
    void lendYard(final PhysicsLens lens) {
        lendYard(lens, structure.bounds());
        for (final Piece piece : held.values()) {
            lendYard(lens, piece.structure.bounds());
        }
    }

    private static void lendYard(final PhysicsLens lens, final Box yard) {
        lens.take((yard.minX() - 1) >> 4, (yard.minZ() - 1) >> 4, (yard.maxX() + 1) >> 4, (yard.maxZ() + 1) >> 4);
    }

    /* Lends its last contacts with the world to {@code into}, for its body against {@code ground} there. */
    void lendWorld(final ContactSolver into, final RigidBody theirs, final RigidBody ground) {
        own.contacts.lendWorld(into, theirs, ground);
        for (final Piece piece : held.values()) {
            piece.contacts.lendWorld(into, theirs, ground);
        }
    }

    /* Whether {@code piece} is part of its body: itself, or a structure it holds. */
    boolean carries(final MovingStructure piece) {
        return piece == structure || held.containsKey(piece);
    }

    /* The blocks of {@code piece}, part of its body; its own for one not part of it. */
    StructureParts partsOf(final MovingStructure piece) {
        final Piece part = held.get(piece);
        return part == null ? own.parts : part.parts;
    }

    /* Where {@code piece}, part of its body, stands mid-tick; null for one not part of it. */
    @Nullable StructureFrame standingOf(final MovingStructure piece) {
        if (piece == structure) {
            return at;
        }
        final Piece part = held.get(piece);
        return part == null ? null : part.at;
    }

    /* Its joints' rows and what they gave, worked with the structures it is joined to. */
    JointWork joints() {
        return joints;
    }

    /* Where it stands in the tick being worked. */
    @Nullable StructureFrame standing() {
        return at;
    }

    /* Keeps what came of the tick it was worked in, {@code now}, for its own turn in that tick to give back. */
    void worked(final long now, final IStructureMotion.Step step) {
        workedAt = now;
        worked = step;
    }

    /* What came of the tick it was last worked in, or null. */
    IStructureMotion.Step workedStep() {
        return worked;
    }

    /* Whether it took its turn this tick already, worked or waiting, and is not to be worked again now. */
    boolean tookTurn(final long now) {
        return askedAt == now || workedAt == now;
    }

    /*
     * Readies it for a tick under {@code motion}: its blocks taken in the first time, what it holds taken in as it
     * comes, its centre found again if they changed, and its body where its frame stands. False for a structure of no
     * mass, which stays where it is.
     */
    boolean begin(final ServerLevel level, final PhysicsMotion motion) {
        final StructureFrame frame = structure.frame();
        if (!filled) {
            own.fill(level);
            filled = true;
            body.velocity().set(motion.vx(), motion.vy(), motion.vz());
            body.spin().set(motion.sx(), motion.sy(), motion.sz());
            warmGiven = motion.warmRooms();
            own.balloons.warm(warmGiven);
            warmSaved = motion.warm();
            own.contacts.reset();
            joints.reset();
        }
        final boolean holding = takeHeld(level);
        if (own.parts.mass().empty() && held.isEmpty()) {
            return false;
        }
        final boolean changed = own.parts.takeChanged() | holding;
        if (changed || !held.isEmpty()) {
            weigh(frame, changed);
        } else if (mass <= 0) {
            weigh(frame, true);
        }
        follow(frame);
        if (mass > 0) {
            for (final Vector3d[] given : pushes) {
                body.push(given[0], given[1], mass, inertia);
            }
            pushes.clear();
        }
        startVelocity.set(body.velocity());
        startSpin.set(body.spin());
        startPosition.set(body.position());
        startTurn.set(body.orientation());
        wheelForce.zero();
        warming = false;
        at = frame;
        placePieces();
        taken = 0;
        return true;
    }

    /*
     * Whether it sleeps on this tick, nothing having pushed it and it not looking about: it costs nothing, and holds
     * still where it is. Another mod's moving structure going in its reach keeps it awake, as nothing of that mod
     * tells it when it comes.
     */
    boolean dozing(final ServerLevel level) {
        return asleep && body.velocity().length() < SLEEP_SPEED && body.spin().length() < SLEEP_SPIN
                && (level.getGameTime() - woke) % LOOK_ABOUT != 0
                && !ForeignMovers.goingIn(level, structure.worldBox().inflate(WAKE_REACH));
    }

    /* Holds it still where it is, asleep. */
    void holdStill() {
        body.velocity().zero();
        body.spin().zero();
    }

    /*
     * The longest step of the {@code left} of the tick in which it goes no more than half a block, its edges too;
     * never so short that the rest of the tick takes more than the {@code steps} it has left.
     */
    double byGoing(final double left, final int steps) {
        final double going = body.velocity().length() + body.spin().length() * reach();
        final double step = going > 0 ? left / Math.max(1, Math.ceil(going * left / STEP_REACH)) : left;
        return Math.max(step, left / Math.max(1, steps));
    }

    /*
     * The longest step in which the stiffest hold on it, a liquid's surface, as last gathered, swings it no more than
     * half a radian, so it holds steady; never so short that the {@code left} of the tick takes more than the
     * {@code steps} it has left.
     */
    double byFloating(final double left, final int steps) {
        double floating = own.gather.floating();
        for (final Piece piece : held.values()) {
            floating += piece.gather.floating();
        }
        final double swinging = mass > 0 ? Math.sqrt(floating / mass) : 0;
        if (swinging <= 0) {
            return Double.MAX_VALUE;
        }
        return Math.max(STEADY_SWING / swinging, left / Math.max(1, steps));
    }

    /* Where it stands at the start of the next step: its frame as its body stands now, and what it holds on it. */
    void nextStep() {
        if (taken > 0) {
            at = frameOf(at);
            placePieces();
        }
    }

    /* Sums the forces on it over a step of {@code seconds}, where it stands, and on what it holds. */
    void gatherForces(final ServerLevel level, final double seconds) {
        forces.clear(body.position());
        final StructureControls told = structure.controls();
        own.gather.gather(level, at, own.parts, structure.bounds(), body, seconds, forces, told);
        for (final Piece piece : held.values()) {
            piece.gather.gather(level, piece.at, piece.parts, piece.structure.bounds(), body, seconds, forces, told);
        }
        final Hold holding = hold;
        if (holding != null) {
            if (level.getGameTime() > holding.until()) {
                hold = null;
            } else {
                pullTowards(holding);
            }
        }
    }

    /*
     * Its balloons, then its push over a step of {@code seconds}, and its wheels; how fast a blow must close to
     * bounce.
     */
    double push(final ServerLevel level, final double seconds, final PhysicsIsland island) {
        warming |= own.balloons.gather(level, at, own.parts, structure.bounds(), own.gather.gravity(), seconds,
                forces);
        for (final Piece piece : held.values()) {
            warming |= piece.balloons.gather(level, piece.at, piece.parts, piece.structure.bounds(),
                    own.gather.gravity(), seconds, forces);
        }
        // A blow bounces only when it closes faster than the forces on it would close it in two such steps.
        final double settles = Math.max(SETTLES_LEAST, 2 * forces.force().length() / mass * seconds);
        body.prepare(mass, inertia);
        body.accelerate(forces.force(), forces.torque(), mass, inertia, seconds);
        final StructureControls told = structure.controls();
        own.wheels.step(level, structure, at, own.parts, body, seconds, wheelForce, island, told);
        for (final Piece piece : held.values()) {
            piece.wheels.step(level, piece.structure, piece.at, piece.parts, body, seconds, wheelForce, island,
                    told);
        }
        return settles;
    }

    /*
     * Finds what it and what it holds meet over a step of {@code seconds}, the structures of {@code island} standing
     * where they stand mid-tick; false while a chunk one of them reaches is not loaded.
     */
    boolean findContacts(final ServerLevel level, final double seconds, final PhysicsIsland island) {
        final double margin = (body.velocity().length() + body.spin().length() * reach()) * seconds + MARGIN;
        if (!own.contacts.find(level, structure, at, own.parts, body, margin, island)) {
            return false;
        }
        for (final Piece piece : held.values()) {
            if (!piece.contacts.find(level, piece.structure, piece.at, piece.parts, body, margin, island)) {
                return false;
            }
        }
        return true;
    }

    /* Readies its contacts for a step, giving how many times they are to be worked. */
    int beginContacts(final double seconds, final double settles) {
        int times = own.contacts.begin(seconds, settles);
        for (final Piece piece : held.values()) {
            times = Math.max(times, piece.contacts.begin(seconds, settles));
        }
        return times;
    }

    /* Works its contacts {@code times} times. */
    void iterateContacts(final int times) {
        if (held.isEmpty()) {
            own.contacts.iterate(times);
            return;
        }
        for (int round = 0; round < times; round++) {
            contactsRound(round);
        }
    }

    /* Works its contacts once, the {@code index}th time. */
    void contactsRound(final int index) {
        own.contacts.round(index);
        for (final Piece piece : held.values()) {
            piece.contacts.round(index);
        }
    }

    /* Keeps what its contacts gave, and the blows they took. */
    void endContacts() {
        own.contacts.end();
        for (final Piece piece : held.values()) {
            piece.contacts.end();
        }
    }

    /* Goes and turns over a step of {@code seconds} as fast as it now goes and turns. */
    void drift(final double seconds) {
        body.drift(inertia, seconds);
        body.prepare(mass, inertia);
        taken++;
    }

    /* Puts it back where it stood at the start of the tick, going as it went: what it reaches is not loaded. */
    void restore() {
        body.position().set(startPosition);
        body.orientation().set(startTurn);
        body.velocity().set(startVelocity);
        body.spin().set(startSpin);
    }

    /*
     * The end of the tick: its blows break what broke, its faces heat, what it shows of itself going is told, it
     * falls asleep or not, and where it goes.
     */
    IStructureMotion.Step finish(final ServerLevel level, final PhysicsMotion motion) {
        forces.note(ForceSum.ForceKind.WHEELS, wheelForce.div(Math.max(1, taken)));
        own.contacts.breakWhatBroke(level);
        final StructureFrame after = frameOf(at);
        own.heating.heat(level, after, own.parts, body, 1 / TICKS);
        StructureSync.effects(structure, StructureShowing.of(level, structure.entity().getId(), after, own.gather,
                own.heating, body));
        boolean burning = !own.gather.engines().isEmpty();
        for (final Piece piece : held.values()) {
            piece.contacts.breakWhatBroke(level);
            final StructureFrame placed = new StructureFrame(after.placed(piece.inRoot), piece.structure.pivotX(),
                    piece.structure.pivotY(), piece.structure.pivotZ());
            piece.heating.heat(level, placed, piece.parts, body, 1 / TICKS);
            StructureSync.effects(piece.structure, StructureShowing.of(level, piece.structure.entity().getId(),
                    placed, piece.gather, piece.heating, body));
            burning |= !piece.gather.engines().isEmpty();
        }
        // An engine pushing keeps it awake, as warming balloons do, whether it lifts it or not: asleep, it would
        // stop showing its flame, and a signal grown stronger would not wake it.
        settle(level, warming || burning);
        if (asleep) {
            return new IStructureMotion.Step(structure.frame().pose(), saved(motion, warming));
        }
        return new IStructureMotion.Step(after.pose(), saved(motion, warming));
    }

    /* Where its point at the yard's place {@code (x, y, z)} is in the world, as its body stands now. */
    Vector3d worldOf(final double x, final double y, final double z, final Vector3d into) {
        into.set(x - centre.x, y - centre.y, z - centre.z);
        body.orientation().transform(into);
        return into.add(body.position());
    }

    /*
     * Where the point of {@code piece}, part of its body, at that piece's yard place {@code (x, y, z)} is in the
     * world, as its body stands now; and the world's way of that piece's way {@code way}, into {@code turned}.
     */
    void pieceWorldOf(final MovingStructure piece, final double x, final double y, final double z,
                      final Vector3d way, final Vector3d into, final Vector3d turned) {
        final Piece part = held.get(piece);
        if (part == null) {
            worldOf(x, y, z, into);
            body.orientation().transform(turned.set(way));
            return;
        }
        final StructureFrame inYard = new StructureFrame(part.inRoot, piece.pivotX(), piece.pivotY(),
                piece.pivotZ());
        worldOf(inYard.worldX(x, y, z), inYard.worldY(x, y, z), inYard.worldZ(x, y, z), into);
        turned.set(inYard.turnX(way.x, way.y, way.z), inYard.turnY(way.x, way.y, way.z),
                inYard.turnZ(way.x, way.y, way.z));
        body.orientation().transform(turned);
    }

    /*
     * Takes in what it holds now, however deep, each where it stands on it in its yard, and lets go of what it no
     * longer holds; whether that changed.
     */
    private boolean takeHeld(final ServerLevel level) {
        final Map<MovingStructure, StructurePose> now = new IdentityHashMap<>();
        gatherHeld(structure, null, now, 0);
        boolean changed = held.keySet().retainAll(now.keySet());
        for (final Map.Entry<MovingStructure, StructurePose> each : now.entrySet()) {
            Piece piece = held.get(each.getKey());
            if (piece == null) {
                piece = new Piece(each.getKey());
                piece.fill(level);
                piece.balloons.warm(warmGiven);
                held.put(each.getKey(), piece);
                changed = true;
            }
            if (!each.getValue().equals(piece.inRoot)) {
                piece.inRoot = each.getValue();
                changed = true;
            }
        }
        for (final Piece piece : held.values()) {
            changed |= piece.parts.takeChanged();
        }
        return changed;
    }

    /* Gathers what {@code holder}, standing at {@code inRoot} in the root's yard, holds, into {@code into}. */
    private static void gatherHeld(final MovingStructure holder, @Nullable final StructurePose inRoot,
                                   final Map<MovingStructure, StructurePose> into, final int depth) {
        if (depth >= 8) {
            return;
        }
        for (final MovingStructure child : StructureMounts.children(holder)) {
            final StructureMount mount = child.mount();
            if (mount == null || !child.ready() || into.containsKey(child)) {
                continue;
            }
            final StructurePose place = inRoot == null ? mount.local()
                    : new StructureFrame(inRoot, holder.pivotX(), holder.pivotY(), holder.pivotZ())
                    .placed(mount.local());
            into.put(child, place);
            gatherHeld(child, place, into, depth + 1);
        }
    }

    /* Puts each structure it holds where it stands on it now, for this step. */
    private void placePieces() {
        for (final Piece piece : held.values()) {
            piece.at = new StructureFrame(at.placed(piece.inRoot), piece.structure.pivotX(),
                    piece.structure.pivotY(), piece.structure.pivotZ());
        }
    }

    /*
     * Its whole mass, centre and inertia, of its own blocks and of what it holds, in its yard; its centre moved there
     * when it moved, going as that point of it went, so nothing it does jumps.
     */
    private void weigh(final StructureFrame frame, final boolean changed) {
        compound.clear();
        own.parts.mass().inertia(pieceInertia);
        compound.add(own.parts.mass().mass(), own.parts.mass().centre(scratch), pieceInertia);
        for (final Piece piece : held.values()) {
            final StructureFrame inYard = new StructureFrame(piece.inRoot, piece.structure.pivotX(),
                    piece.structure.pivotY(), piece.structure.pivotZ());
            final Vector3d middle = piece.parts.mass().centre(new Vector3d());
            final Vector3d placed = new Vector3d(inYard.worldX(middle.x, middle.y, middle.z),
                    inYard.worldY(middle.x, middle.y, middle.z), inYard.worldZ(middle.x, middle.y, middle.z));
            piece.parts.mass().inertia(pieceInertia);
            for (int row = 0; row < 3; row++) {
                for (int column = 0; column < 3; column++) {
                    turnMatrix.set(column, row, inYard.rotation(row, column));
                }
            }
            // R I R^T: the piece's inertia in its own yard's axes, in the holder's.
            final Matrix3d turned = new Matrix3d(turnMatrix).mul(pieceInertia).mul(new Matrix3d(turnMatrix)
                    .transpose());
            compound.add(piece.parts.mass().mass(), placed, turned);
        }
        mass = compound.mass();
        compound.inertia(inertia);
        final Vector3d moved = compound.centre(new Vector3d());
        if (moved.distance(centre) > SAME_CENTRE || changed) {
            recentre(frame, moved);
        }
    }

    /*
     * Falls asleep after a second still, nothing warming or burning: slow, and never more than a hair from where it
     * stood a second ago, so a cart swaying on its springs in the wind, by a few millimetres, sleeps; or stays awake.
     */
    private void settle(final ServerLevel level, final boolean working) {
        final boolean slow = body.velocity().length() < STILL_SPEED && body.spin().length() < STILL_SPIN;
        final boolean near = body.position().distance(anchor) < STILL_DRIFT && turnFromAnchor() < STILL_TURN;
        if (!working && slow && near) {
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

    /* The rule that goes on, holding how fast it goes and its balloons' warm air, its own and what it holds. */
    private PhysicsMotion saved(final PhysicsMotion was, final boolean warmingNow) {
        int rooms = own.balloons.warm().size();
        for (final Piece piece : held.values()) {
            rooms += piece.balloons.warm().size();
        }
        if (warmingNow || warmSaved.size() != rooms) {
            final Map<Long, Double> all = new HashMap<>();
            for (final Long2DoubleMap.Entry room : own.balloons.warm().long2DoubleEntrySet()) {
                all.put(room.getLongKey(), room.getDoubleValue());
            }
            for (final Piece piece : held.values()) {
                for (final Long2DoubleMap.Entry room : piece.balloons.warm().long2DoubleEntrySet()) {
                    all.put(room.getLongKey(), room.getDoubleValue());
                }
            }
            final List<PhysicsMotion.WarmRoom> listed = new ArrayList<>();
            all.forEach((room, kelvin) -> listed.add(new PhysicsMotion.WarmRoom(room, kelvin)));
            warmSaved = List.copyOf(listed);
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
     * Its blocks changed, and its centre of mass with them, to {@code moved}: its body's centre is the new one, going
     * as that point of it went, so nothing it does jumps.
     */
    private void recentre(final StructureFrame frame, final Vector3d moved) {
        final Vector3d world = FrameMath.world(frame, moved, new Vector3d());
        if (filled && centre.lengthSquared() > 0) {
            follow(frame);
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
     * The pull of what holds it from outside: a spring from its held point to the place it is held towards, as stiff
     * as swings it there in about a second and damped so it comes to rest there, bearing its weight, no harder than the
     * hold allows; its spinning held back alike, so a structure held does not twirl.
     */
    private void pullTowards(final Hold holding) {
        if (mass <= 0) {
            return;
        }
        final Vector3d point = worldOf(holding.point().x, holding.point().y, holding.point().z, new Vector3d());
        final Vector3d going = body.velocityAt(point, new Vector3d());
        final double stiffness = mass * HOLD_SWING * HOLD_SWING;
        final Vector3d pull = new Vector3d(holding.target()).sub(point).mul(stiffness)
                .fma(-2 * mass * HOLD_SWING, going).fma(-mass, own.gather.gravity());
        if (pull.length() > holding.most()) {
            pull.mul(holding.most() / pull.length());
        }
        forces.add(ForceSum.ForceKind.HELD, pull, point);
        final double reach = Math.max(1, reach() * HALF);
        final Vector3d turning = new Vector3d(body.spin()).mul(-2 * HOLD_SWING * mass * reach * reach);
        if (turning.length() > holding.most() * reach) {
            turning.mul(holding.most() * reach / turning.length());
        }
        forces.addTurning(turning);
    }

    /*
     * How hard the engines and driving wheels of {@code piece}, standing at {@code frame}, push along {@code way} at
     * full, in newtons: an engine looking along it, a wheel rolling along it either way.
     */
    private static double pushAlong(final Piece piece, final StructureFrame frame, final Vec3 way) {
        double newtons = 0;
        final Vector3d turned = new Vector3d();
        for (final StructureParts.Part part : piece.parts.parts()) {
            final BlockPhysics physics = part.physics();
            if (physics.thrust().isEmpty() && physics.wheel().isEmpty()) {
                continue;
            }
            FrameMath.turned(part.way(), frame, turned);
            final double along = turned.x * way.x + turned.y * way.y + turned.z * way.z;
            if (physics.thrust().isPresent() && along > ANSWERS) {
                newtons += physics.thrust().get().newtons() * along;
            }
            if (physics.wheel().isPresent() && Math.abs(along) > ANSWERS) {
                newtons += physics.wheel().get().drive() * Math.abs(along);
            }
        }
        return newtons;
    }

    /* How far its furthest block, or that of what it holds, is from its centre, for how fast its turning carries it. */
    private double reach() {
        double most = reachOf(structure.bounds(), null, structure);
        for (final Piece piece : held.values()) {
            most = Math.max(most, reachOf(piece.structure.bounds(), piece.inRoot, piece.structure));
        }
        return most;
    }

    /* How far the furthest corner of {@code bounds}, standing at {@code inRoot} in its yard, is from its centre. */
    private double reachOf(final Box bounds, @Nullable final StructurePose inRoot, final MovingStructure piece) {
        final double cx = (bounds.minX() + bounds.maxX() + 1) * HALF;
        final double cy = (bounds.minY() + bounds.maxY() + 1) * HALF;
        final double cz = (bounds.minZ() + bounds.maxZ() + 1) * HALF;
        final double half = Math.sqrt(Math.pow(bounds.maxX() + 1 - bounds.minX(), 2)
                + Math.pow(bounds.maxY() + 1 - bounds.minY(), 2) + Math.pow(bounds.maxZ() + 1 - bounds.minZ(), 2))
                * HALF;
        if (inRoot == null) {
            return Math.sqrt(Math.pow(cx - centre.x, 2) + Math.pow(cy - centre.y, 2) + Math.pow(cz - centre.z, 2))
                    + half;
        }
        final StructureFrame inYard = new StructureFrame(inRoot, piece.pivotX(), piece.pivotY(), piece.pivotZ());
        return Math.sqrt(Math.pow(inYard.worldX(cx, cy, cz) - centre.x, 2)
                + Math.pow(inYard.worldY(cx, cy, cz) - centre.y, 2)
                + Math.pow(inYard.worldZ(cx, cy, cz) - centre.z, 2)) + half;
    }

    /*
     * A structure that is part of the body: itself, or one it holds. Each has its own blocks, its own forces, wheels,
     * balloons and contacts, all pushing the one body; one held stands at its place on it, {@code inRoot}.
     */
    private static final class Piece {

        private final MovingStructure structure;
        private final StructureParts parts = new StructureParts();
        private final StructureForces gather = new StructureForces();
        private final StructureContacts contacts = new StructureContacts();
        private final StructureWheels wheels = new StructureWheels();
        private final StructureBalloons balloons = new StructureBalloons();
        private final StructureHeating heating = new StructureHeating();
        private StructurePose inRoot;
        private StructureFrame at;
        private boolean filled;

        private Piece(final MovingStructure structure) {
            this.structure = structure;
        }

        /* Takes in its blocks. */
        private void fill(final ServerLevel level) {
            parts.fill(level, structure.bounds());
            filled = true;
        }
    }

    /*
     * A hold from outside: the point held, a place of the yard; the place it is held towards, of the world; the most it
     * pulls with, in newtons; and the last tick it holds.
     */
    private record Hold(Vector3d point, Vector3d target, double most, long until) {
    }
}
