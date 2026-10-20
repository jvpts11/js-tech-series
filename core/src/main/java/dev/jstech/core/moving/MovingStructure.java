/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import com.mojang.serialization.Codec;
import dev.jstech.core.region.Box;
import dev.jstech.core.uuid.StructureUuid;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
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
    private @Nullable StructureArrival arrival;
    private @Nullable StructureDeparture departure;
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

    /** Sets the rule that moves it from the next tick, or null to bring it to rest where it is. */
    public void move(@Nullable final IStructureMotion next) {
        this.motion = next;
        this.pending = null;
        if (next == null) {
            settle();
        }
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
        if (now == null || now.readsWorld() || !ready) {
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
        final IStructureMotion now = motion;
        if (now == null) {
            if (previousFrame != frame) {
                previousFrame = frame;
            }
            return;
        }
        final IStructureMotion.Step step = pending != null ? pending
                : now.step(frame.pose(), new IStructureMotion.Context(frame, level));
        pending = null;
        motion = step.next();
        advance(step.pose());
        if (motion == null) {
            settle();
        }
    }

    /** Keeps its plot's chunks loaded, and files it with its level. */
    void hold(final ServerLevel level) {
        StructureYard.hold(level, bounds);
        held = true;
        LevelMovingStructures.of(level).add(this);
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
        final boolean was = !before.isAir();
        final boolean is = !after.isAir();
        if (was == is) {
            return;
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
            StructureSync.moved(this);
        }
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
