/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import com.mojang.serialization.Codec;
import dev.jstech.core.moving.physics.PhysicsMotion;
import dev.jstech.core.moving.physics.StructurePhysics;
import dev.jstech.core.region.Box;
import dev.jstech.core.uuid.StructureUuid;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/**
 * A moving structure on the server: the plot of the yard its real blocks are in, the box they fill there, the pivot
 * its pose stands at, the pose itself and the rule that moves it. Each tick it takes the rule's next pose, carries
 * whatever stands on it, pushes aside what it runs into, and tells the players who see it where it went; a structure
 * with no rule rests and costs nothing. It lives in its entity, saved with it, and is found through its level's
 * {@link LevelMovingStructures}.
 */
public final class MovingStructure implements IStructureBody {

    private final MovingStructureEntity entity;
    private final StructureUuid id;
    private final int plot;
    private final double[] velocity = new double[3];
    private final double[] spin = new double[3];
    private final StructureKeeping keeping = new StructureKeeping();
    private Box bounds;
    private int blocks;
    private double pivotX;
    private double pivotY;
    private double pivotZ;
    private StructureFrame frame;
    private StructureFrame previousFrame;
    private AABB worldBox;
    private @Nullable IStructureMotion motion;
    private IStructureMotion.@Nullable Step pending;
    /* Its physics, while it moves by them. */
    private @Nullable StructurePhysics physics;
    private @Nullable StructureArrival arrival;
    private @Nullable StructureDeparture departure;
    /* The structure holding it, and where it stands on it, or null. */
    private @Nullable StructureMount mount;
    /* The blocks beside those it lost, to look at for whether it is still one; the pieces waiting to part. */
    private final Set<Long> splitSeeds = new HashSet<>();
    private final List<StructureSplitting.Waiting> splitting = new ArrayList<>();
    /* Plots taken for pieces that were waiting when it was saved, to give back as it loads. */
    private final List<Integer> splitPlots = new ArrayList<>();
    /* What its blocks do aboard: its seats, clamps, decouplers and tools. */
    private final StructureAboard aboard = new StructureAboard();
    /* What it is told to do now, by its helm or its autopilot, and the autopilot flying it, if any. */
    private final StructureControls controls = new StructureControls();
    private @Nullable StructureAutopilot autopilot;
    /* What the autopilot has built up holding its height against its weight. */
    private final double[] autopilotHold = new double[1];
    private boolean parting;
    private boolean ready;
    private boolean held;
    private boolean departureHeld;
    private boolean emptied;

    private static final String ID = "structure";
    private static final String ARRIVAL = "arrival";
    private static final String DEPARTURE = "departure";
    private static final String PLOT = "plot";
    private static final String BOUNDS = "bounds";
    private static final String BLOCKS = "blocks";
    private static final String PIVOT = "pivot";
    private static final String POSE = "pose";
    private static final String MOTION = "motion";
    private static final String MOUNT = "mount";
    private static final String AUTOPILOT = "autopilot";
    private static final String SPLIT_SEEDS = "split_seeds";
    private static final String SPLIT_PLOTS = "split_plots";
    private static final Codec<double[]> PLACE = Codec.DOUBLE.listOf(3, 3).xmap(
            list -> new double[] {list.get(0), list.get(1), list.get(2)},
            place -> List.of(place[0], place[1], place[2]));

    MovingStructure(final MovingStructureEntity entity, final StructureUuid id, final int plot, final Box bounds,
                    final int blocks, final double pivotX, final double pivotY, final double pivotZ,
                    final StructurePose pose, @Nullable final IStructureMotion motion) {
        this.entity = entity;
        this.id = id;
        this.plot = plot;
        this.bounds = bounds;
        this.blocks = blocks;
        this.pivotX = pivotX;
        this.pivotY = pivotY;
        this.pivotZ = pivotZ;
        this.frame = new StructureFrame(pose, pivotX, pivotY, pivotZ);
        this.previousFrame = frame;
        this.worldBox = boxOf(frame, bounds);
        this.motion = motion;
    }

    /** The structure an entity's save holds, or null when the save holds none it can read. */
    static @Nullable MovingStructure read(final MovingStructureEntity entity, final CompoundTag tag) {
        if (!tag.hasUUID(ID) || !tag.contains(BOUNDS, Tag.TAG_INT_ARRAY)) {
            return null;
        }
        final int[] box = tag.getIntArray(BOUNDS);
        final Optional<double[]> pivot = PLACE.parse(NbtOps.INSTANCE, tag.get(PIVOT)).result();
        final Optional<StructurePose> pose = StructureCodecs.POSE.parse(NbtOps.INSTANCE, tag.get(POSE)).result();
        if (box.length != 6 || pivot.isEmpty() || pose.isEmpty()) {
            return null;
        }
        final IStructureMotion motion = tag.contains(MOTION)
                ? StructureMotions.CODEC.parse(NbtOps.INSTANCE, tag.get(MOTION)).result().orElse(null) : null;
        final double[] at = pivot.get();
        final MovingStructure read = new MovingStructure(entity, new StructureUuid(tag.getUUID(ID)), tag.getInt(PLOT),
                new Box(box[0], box[1], box[2], box[3], box[4], box[5]), tag.getInt(BLOCKS), at[0], at[1], at[2],
                pose.get(), motion);
        if (tag.contains(ARRIVAL)) {
            read.arrival = StructureArrival.CODEC.parse(NbtOps.INSTANCE, tag.get(ARRIVAL)).result().orElse(null);
        }
        if (tag.contains(DEPARTURE)) {
            read.departure = StructureDeparture.CODEC.parse(NbtOps.INSTANCE, tag.get(DEPARTURE)).result()
                    .orElse(null);
        }
        if (tag.contains(MOUNT)) {
            read.mount = StructureMount.CODEC.parse(NbtOps.INSTANCE, tag.get(MOUNT)).result().orElse(null);
        }
        if (tag.contains(AUTOPILOT)) {
            read.autopilot = StructureAutopilot.CODEC.parse(NbtOps.INSTANCE, tag.get(AUTOPILOT)).result()
                    .orElse(null);
        }
        for (final long seed : tag.getLongArray(SPLIT_SEEDS)) {
            read.splitSeeds.add(seed);
        }
        for (final int plot : tag.getIntArray(SPLIT_PLOTS)) {
            read.splitPlots.add(plot);
        }
        return read;
    }

    /** Writes this structure into its entity's save. */
    void write(final CompoundTag tag) {
        tag.putUUID(ID, id.value());
        tag.putInt(PLOT, plot);
        tag.putIntArray(BOUNDS, new int[] {bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(),
                bounds.maxY(), bounds.maxZ()});
        tag.putInt(BLOCKS, blocks);
        PLACE.encodeStart(NbtOps.INSTANCE, new double[] {pivotX, pivotY, pivotZ}).result()
                .ifPresent(written -> tag.put(PIVOT, written));
        StructureCodecs.POSE.encodeStart(NbtOps.INSTANCE, frame.pose()).result()
                .ifPresent(written -> tag.put(POSE, written));
        if (motion != null) {
            StructureMotions.CODEC.encodeStart(NbtOps.INSTANCE, motion).result()
                    .ifPresent(written -> tag.put(MOTION, written));
        }
        if (arrival != null) {
            StructureArrival.CODEC.encodeStart(NbtOps.INSTANCE, arrival).result()
                    .ifPresent(written -> tag.put(ARRIVAL, written));
        }
        if (departure != null) {
            StructureDeparture.CODEC.encodeStart(NbtOps.INSTANCE, departure).result()
                    .ifPresent(written -> tag.put(DEPARTURE, written));
        }
        if (mount != null) {
            StructureMount.CODEC.encodeStart(NbtOps.INSTANCE, mount).result()
                    .ifPresent(written -> tag.put(MOUNT, written));
        }
        if (autopilot != null) {
            StructureAutopilot.CODEC.encodeStart(NbtOps.INSTANCE, autopilot).result()
                    .ifPresent(written -> tag.put(AUTOPILOT, written));
        }
        // A piece waiting to part is looked for again as it loads, and the plot taken for it given back.
        final Set<Long> seeds = new HashSet<>(splitSeeds);
        final List<Integer> plots = new ArrayList<>(splitPlots);
        for (final StructureSplitting.Waiting piece : splitting) {
            seeds.addAll(piece.seeds());
            plots.add(piece.plot());
        }
        if (!seeds.isEmpty()) {
            tag.putLongArray(SPLIT_SEEDS, seeds.stream().mapToLong(Long::longValue).toArray());
        }
        if (!plots.isEmpty()) {
            tag.putIntArray(SPLIT_PLOTS, plots.stream().mapToInt(Integer::intValue).toArray());
        }
    }

    @Override
    public StructureUuid id() {
        return id;
    }

    @Override
    public int plot() {
        return plot;
    }

    @Override
    public Level level() {
        return entity.level();
    }

    @Override
    public StructureFrame frame() {
        return frame;
    }

    @Override
    public StructureFrame previousFrame() {
        return previousFrame;
    }

    @Override
    public Box bounds() {
        return bounds;
    }

    @Override
    public AABB worldBox() {
        return worldBox;
    }

    @Override
    public boolean ready() {
        return ready;
    }

    /** The entity it lives in. */
    public MovingStructureEntity entity() {
        return entity;
    }

    public StructurePose pose() {
        return frame.pose();
    }

    /** The rule moving it now, or null while it rests. */
    public @Nullable IStructureMotion motion() {
        return motion;
    }

    /** Whether it is at rest: no rule moves it, and it costs nothing a tick. */
    public boolean resting() {
        return motion == null;
    }

    /** How far its pivot moved in the last tick, in blocks, east, up and south. */
    public double[] velocity() {
        return velocity.clone();
    }

    /** How fast it turned in the last tick: the axis, of a length the angle in radians. */
    public double[] spin() {
        return spin.clone();
    }

    /** How many blocks it is made of. */
    public int blocks() {
        return blocks;
    }

    /**
     * Whether it is just made and its blocks still stand where they stood, until the chunks of its plot of the yard
     * have loaded: a few ticks, after which they are its own, in the yard, and it is {@link #ready()}.
     */
    public boolean arriving() {
        return arrival != null;
    }

    /** Whether it is on its way to another dimension, going once the chunks it goes to have loaded. */
    public boolean departing() {
        return departure != null;
    }

    /** The structure holding it and where it stands on it, or null for one held by none. */
    public @Nullable StructureMount mount() {
        return mount;
    }

    /**
     * Sets the rule that moves it from the next tick, or null to bring it to rest where it is. Held by another, a rule
     * moves it on its parent, in the parent's yard; let go by its physics, it is let go by its parent first.
     */
    public void move(@Nullable final IStructureMotion next) {
        if (next instanceof PhysicsMotion && mount != null) {
            StructureMounts.unmount(this, false);
        }
        this.motion = next;
        this.pending = null;
        if (!(next instanceof PhysicsMotion)) {
            physics = null;
            StructureSync.stopShowing(this);
        } else if (physics != null) {
            physics.wake();
        }
        if (next == null) {
            settle();
        }
    }

    /** Its physics while it moves by them ({@link PhysicsMotion}), or null. */
    public @Nullable StructurePhysics physics() {
        return motion instanceof PhysicsMotion ? physics : null;
    }

    /** Its physics, made as it starts to move by them. */
    public StructurePhysics physicsMade() {
        StructurePhysics made = physics;
        if (made == null) {
            made = StructurePhysics.of(this);
            physics = made;
        }
        return made;
    }

    /** Puts it at {@code pose} at once, carrying what stands on it there with it. */
    public void teleport(final StructurePose pose) {
        advance(pose);
        settle();
    }

    /**
     * Works out the next tick of a rule that does not read the world, away from the server's thread: the pose is
     * taken next tick on it. A rule that reads the world, or a structure at rest, does nothing here.
     */
    void prepare() {
        final IStructureMotion now = motion;
        if (now == null || now.readsWorld() || !ready || mount != null) {
            pending = null;
            return;
        }
        pending = now.step(frame.pose(), new IStructureMotion.Context(frame, null));
    }

    /**
     * One tick on the server's thread: the structure waits for its blocks to load, a structure just made for the
     * chunks of its plot so its blocks can come into it, one on its way to another dimension for the chunks it goes
     * to; then it takes its rule's step.
     */
    void tick() {
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        if (!held) {
            hold(level);
        }
        if (arrival != null) {
            if (!StructureYard.loaded(level, bounds) || !arrival.standsLoaded(level)) {
                return;
            }
            arrive(level);
        }
        if (!ready) {
            ready = StructureYard.loaded(level, bounds);
            if (!ready) {
                return;
            }
            MovingStructures.loaded(this);
        }
        if (emptied) {
            MovingStructures.remove(this, false);
            return;
        }
        if (!splitPlots.isEmpty()) {
            for (final int plot : splitPlots) {
                StructureYard.giveBack(level, plot);
            }
            splitPlots.clear();
        }
        aboard.tick(level, this);
        if (!splitSeeds.isEmpty() || !splitting.isEmpty()) {
            StructureSplitting.look(level, this, splitSeeds, splitting);
        }
        if (departure != null) {
            if (!departureHeld) {
                StructureTravel.hold(this, level, departure);
                departureHeld = true;
            }
            if (StructureTravel.go(this, level, departure)) {
                return;
            }
        }
        keeping.tick(level, bounds, worldBox);
        if (mount != null) {
            // Its parent moves it, and turns it by its rule, in the parent's own tick.
            return;
        }
        if (autopilot != null && !controls.piloted(level.getGameTime())) {
            final StructureAutopilot next = autopilot.steer(level, this, autopilotHold);
            if (next != autopilot) {
                fly(next);
            }
        }
        stepOwn(level);
        StructureMounts.drive(level, this, 0);
    }

    /*
     * Held by another, one tick: turned by its own rule on its parent, in the parent's yard, then put where the
     * parent, standing at {@code parent}, has it; then what it holds after it.
     */
    void followParent(final ServerLevel level, final StructureFrame parent, final int depth) {
        final StructureMount held = mount;
        if (held == null || !ready) {
            return;
        }
        StructurePose local = held.local();
        final IStructureMotion now = motion;
        if (now != null && !now.readsWorld()) {
            final IStructureMotion.Step step = now.step(local, new IStructureMotion.Context(new StructureFrame(local,
                    pivotX, pivotY, pivotZ), null));
            local = step.pose();
            motion = step.next();
        }
        if (local != held.local()) {
            mount = held.at(local);
        }
        final StructurePose world = parent.placed(local);
        if (world.equals(frame.pose())) {
            holdStill();
        } else {
            advance(world);
        }
        StructureMounts.drive(level, this, depth + 1);
    }

    /** What its blocks do aboard: its seats, clamps, decouplers and tools. */
    public StructureAboard aboard() {
        return aboard;
    }

    /** What it is told to do now, by its helm or its autopilot; what it holds takes its parent's. */
    public StructureControls controls() {
        return controls;
    }

    /** The autopilot flying it, or null. */
    public @Nullable StructureAutopilot autopilot() {
        return autopilot;
    }

    /** Sets the autopilot flying it from the next tick, or null for none; a pilot at its helm still steers. */
    public void fly(@Nullable final StructureAutopilot pilot) {
        this.autopilot = pilot;
        if (pilot == null) {
            controls.release();
        }
    }

    /*
     * Its box of blocks taken in to the blocks it has now, after pieces of it parted: the chunks of its plot it no
     * longer fills are let go, and who sees it is told.
     */
    void shrinkToBlocks(final ServerLevel level) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
            for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                for (int y = bounds.minY(); y <= bounds.maxY(); y++) {
                    if (!level.getBlockState(at.set(x, y, z)).isAir()) {
                        minX = Math.min(minX, x);
                        minY = Math.min(minY, y);
                        minZ = Math.min(minZ, z);
                        maxX = Math.max(maxX, x);
                        maxY = Math.max(maxY, y);
                        maxZ = Math.max(maxZ, z);
                    }
                }
            }
        }
        if (minX > maxX) {
            return;
        }
        final Box smaller = new Box(minX, minY, minZ, maxX, maxY, maxZ);
        if (smaller.equals(bounds)) {
            return;
        }
        if (held) {
            // Let go first and held again in the same tick: the chunks it still fills never unload.
            StructureYard.release(level, bounds);
            StructureYard.hold(level, smaller);
        }
        bounds = smaller;
        worldBox = boxOf(frame, bounds);
        LevelMovingStructures.of(level).moved(this);
        entity.boundsChanged();
    }

    /* Looks at once for the pieces its lost blocks left, a decoupler having just fired. */
    void lookForPieces(final ServerLevel level) {
        StructureSplitting.look(level, this, splitSeeds, splitting);
    }

    /* Whether blocks are leaving it whole, for a piece of it to go its own way: those are not blocks lost. */
    void parting(final boolean leaving) {
        this.parting = leaving;
    }

    /* Sets what holds it, or null to be held by none; it is filed again by whoever calls. */
    void mounted(@Nullable final StructureMount held) {
        this.mount = held;
        if (held != null && motion instanceof PhysicsMotion) {
            motion = null;
            physics = null;
            StructureSync.stopShowing(this);
        }
    }

    /** Keeps its plot's chunks loaded, and files it with its level. */
    void hold(final ServerLevel level) {
        StructureYard.hold(level, bounds);
        held = true;
        LevelMovingStructures.of(level).add(this);
        StructureMounts.file(this);
        if (!ready && arrival == null && StructureYard.loaded(level, bounds)) {
            ready = true;
            MovingStructures.loaded(this);
        }
    }

    /*
     * Its blocks still stand in the world, to come into its plot once its chunks have loaded: now, when they and the
     * plot's are loaded already, or at a later tick.
     */
    void arriving(final StructureArrival coming) {
        this.arrival = coming;
        if (entity.level() instanceof ServerLevel level && StructureYard.loaded(level, bounds)
                && coming.standsLoaded(level)) {
            arrive(level);
            hold(level);
        }
    }

    /* Where it is on its way to, or null. */
    @Nullable StructureDeparture departure() {
        return departure;
    }

    /* Sets it on its way to another dimension, or, with null, keeps it where it is. */
    void depart(@Nullable final StructureDeparture going) {
        this.departure = going;
        this.departureHeld = false;
    }

    /** Lets go of its plot's chunks, and takes it out of its level's files, as its entity leaves the level. */
    void letGo(final ServerLevel level) {
        if (held) {
            StructureYard.release(level, bounds);
            held = false;
        }
        if (departure != null && departureHeld) {
            StructureTravel.release(this, level, departure);
            departureHeld = false;
        }
        ready = false;
        keeping.letGo(level);
        LevelMovingStructures.of(level).remove(id);
        StructureMounts.unfile(this);
        StructureSync.forget(this);
    }

    /** Whether it keeps the world's chunks under it loaded, for a chunk loader aboard. */
    public boolean keepsItsPlace() {
        return keeping.keeping();
    }

    /**
     * A block of its plot changed: the box of its blocks grows to hold a block set down past its edge, and a
     * structure whose last block went is taken away at its next tick.
     */
    void blockChanged(final ServerLevel level, final BlockPos at, final BlockState before, final BlockState after) {
        if (physics != null) {
            physics.blockChanged(level, at, before, after);
        }
        if (mount != null) {
            // Held by a structure moving by its physics, its blocks are part of the body that moves them all.
            final StructurePhysics carrying = StructureMounts.rootOf(this).physics();
            if (carrying != null) {
                carrying.pieceChanged(this, level, at, before, after);
            }
        }
        aboard.changed(at, after);
        final boolean was = !before.isAir();
        final boolean is = !after.isAir();
        if (was == is) {
            return;
        }
        StructureMounts.anchorChanged(this, at, !is);
        if (was && !parting) {
            // The blocks beside one lost are looked at next tick: they may no longer be joined.
            for (final Direction side : Direction.values()) {
                splitSeeds.add(BlockPos.asLong(at.getX() + side.getStepX(), at.getY() + side.getStepY(),
                        at.getZ() + side.getStepZ()));
            }
        }
        if (is) {
            blocks++;
            if (!bounds.contains(at.getX(), at.getY(), at.getZ())) {
                final Box grown = new Box(Math.min(bounds.minX(), at.getX()), Math.min(bounds.minY(), at.getY()),
                        Math.min(bounds.minZ(), at.getZ()), Math.max(bounds.maxX(), at.getX()),
                        Math.max(bounds.maxY(), at.getY()), Math.max(bounds.maxZ(), at.getZ()));
                final Box smaller = bounds;
                bounds = grown;
                if (held) {
                    StructureYard.hold(level, bounds);
                }
                worldBox = boxOf(frame, bounds);
                LevelMovingStructures.of(level).moved(this);
                entity.boundsChanged();
                StructureSync.grew(this, smaller);
            }
        } else {
            blocks = Math.max(0, blocks - 1);
            emptied = blocks == 0;
        }
    }

    /** Moves its pivot to the yard's place {@code (x, y, z)}, keeping every block where it is in the world. */
    public void setPivot(final double x, final double y, final double z) {
        final double wx = frame.worldX(x, y, z);
        final double wy = frame.worldY(x, y, z);
        final double wz = frame.worldZ(x, y, z);
        this.pivotX = x;
        this.pivotY = y;
        this.pivotZ = z;
        final StructurePose pose = frame.pose().movedTo(wx, wy, wz);
        this.frame = new StructureFrame(pose, x, y, z);
        this.previousFrame = frame;
        entity.pivotChanged();
        entity.follow(frame);
    }

    public double pivotX() {
        return pivotX;
    }

    public double pivotY() {
        return pivotY;
    }

    public double pivotZ() {
        return pivotZ;
    }

    /*
     * Its blocks come into its plot from where they stood: those still standing there that may be moved, so a block
     * broken since it was made, or one that must stay put set there since, stays; a structure none of whose blocks are
     * left is taken away.
     */
    private void arrive(final ServerLevel level) {
        final StructureArrival coming = arrival;
        arrival = null;
        if (coming == null) {
            return;
        }
        final Map<BlockPos, BlockPos> moves = new LinkedHashMap<>();
        for (final long place : coming.places()) {
            final BlockPos at = BlockPos.of(place);
            final BlockState state = level.getBlockState(at);
            if (!state.isAir() && !state.is(StructureAssembly.STAYS_PUT)) {
                moves.put(at, at.offset(coming.dx(), 0, coming.dz()));
            }
        }
        if (!moves.isEmpty()) {
            StructureSync.arriving(this, level, moves.keySet());
            BlockMover.move(level, level, moves, Rotation.NONE, true, false);
        }
        // Counted after: each block laid in its plot was also heard of as a block set on the structure.
        blocks = moves.size();
        emptied = moves.isEmpty();
    }

    /* One tick by its own rule, held by none. */
    private void stepOwn(final ServerLevel level) {
        final IStructureMotion now = motion;
        if (now == null) {
            if (previousFrame != frame) {
                previousFrame = frame;
            }
            return;
        }
        final IStructureMotion.Step step = pending != null ? pending
                : now.step(frame.pose(), new IStructureMotion.Context(frame, level, this));
        pending = null;
        motion = step.next();
        if (step.pose() == frame.pose() && motion != null) {
            // Held still this tick, as a structure asleep is: nothing is carried, pushed or told.
            holdStill();
            return;
        }
        advance(step.pose());
        if (motion == null) {
            settle();
        }
    }

    /* Takes the pose: carries what stands on it, pushes aside what it runs into, tells who sees it. */
    private void advance(final StructurePose next) {
        final StructurePose was = frame.pose();
        previousFrame = frame;
        frame = new StructureFrame(next, pivotX, pivotY, pivotZ);
        velocity[0] = next.x() - was.x();
        velocity[1] = next.y() - was.y();
        velocity[2] = next.z() - was.z();
        spinBetween(was, next, spin);
        worldBox = boxOf(frame, bounds);
        entity.follow(frame);
        if (entity.level() instanceof ServerLevel level) {
            LevelMovingStructures.of(level).moved(this);
            StructureFooting.carry(this);
            StructureCollisions.pushAside(this);
            StructurePhysics.wakeAround(this);
            StructureSync.moved(this);
        }
    }

    /* Stands still a tick under its rule: it stood where it stands, and goes nowhere. */
    private void holdStill() {
        if (previousFrame == frame && velocity[0] == 0 && velocity[1] == 0 && velocity[2] == 0) {
            return;
        }
        settle();
    }

    /* Comes to rest: nothing moves it, it stood where it stands, and who sees it is told once. */
    private void settle() {
        previousFrame = frame;
        velocity[0] = 0;
        velocity[1] = 0;
        velocity[2] = 0;
        spin[0] = 0;
        spin[1] = 0;
        spin[2] = 0;
        if (entity.level() instanceof ServerLevel) {
            StructureSync.moved(this);
        }
    }

    /* The box of the world a structure's blocks fill, turned as its frame stands. */
    static AABB boxOf(final StructureFrame frame, final Box bounds) {
        final double[] box = frame.worldBox(bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX() + 1,
                bounds.maxY() + 1, bounds.maxZ() + 1);
        return new AABB(box[0], box[1], box[2], box[3], box[4], box[5]);
    }

    /* The turn from one pose to the next, as an axis whose length is the angle, in radians. */
    static void spinBetween(final StructurePose from, final StructurePose to, final double[] out) {
        final double w = to.qw() * from.qw() + to.qx() * from.qx() + to.qy() * from.qy() + to.qz() * from.qz();
        final double x = -to.qw() * from.qx() + to.qx() * from.qw() - to.qy() * from.qz() + to.qz() * from.qy();
        final double y = -to.qw() * from.qy() + to.qx() * from.qz() + to.qy() * from.qw() - to.qz() * from.qx();
        final double z = -to.qw() * from.qz() - to.qx() * from.qy() + to.qy() * from.qx() + to.qz() * from.qw();
        final double sign = w < 0 ? -1 : 1;
        final double half = Math.sqrt(x * x + y * y + z * z);
        if (half < 1.0e-12) {
            out[0] = 0;
            out[1] = 0;
            out[2] = 0;
            return;
        }
        final double angle = 2 * Math.atan2(half, Math.abs(w));
        out[0] = sign * x / half * angle;
        out[1] = sign * y / half * angle;
        out[2] = sign * z / half * angle;
    }
}
