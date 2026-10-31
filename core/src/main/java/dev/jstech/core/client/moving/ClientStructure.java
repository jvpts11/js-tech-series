/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.moving;

import dev.jstech.core.moving.IStructureBody;
import dev.jstech.core.moving.MovingStructureEntity;
import dev.jstech.core.moving.SendRate;
import dev.jstech.core.moving.StructureFrame;
import dev.jstech.core.moving.StructurePose;
import dev.jstech.core.moving.StructurePosePayload;
import dev.jstech.core.moving.YardLayout;
import dev.jstech.core.region.Box;
import dev.jstech.core.uuid.StructureUuid;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/**
 * A moving structure as a player's game has it: the poses the server told, and the pose drawn between them a little
 * behind the newest, so the structure moves smoothly whatever the network does; past the newest pose it goes on at
 * the speed it last had, for a few ticks, until the next comes. Each tick of the game it takes one pose, the frame
 * colliding and carrying use; each frame it is drawn between the last two.
 */
public final class ClientStructure implements IStructureBody {

    private final MovingStructureEntity entity;
    private final Deque<StructurePosePayload> told = new ArrayDeque<>();
    private final StructureMeshes meshes;
    private StructureUuid id;
    private int plot;
    private Box bounds;
    private double pivotX;
    private double pivotY;
    private double pivotZ;
    private StructureFrame frame;
    private StructureFrame previousFrame;
    private AABB worldBox;
    private long clock = Long.MIN_VALUE;
    private int behind = 2;
    private boolean ready;
    private boolean landed;
    /* Held by another: the parent's entity, and where it stands on the parent this tick and the last. */
    private int parentEntity = -1;
    private @Nullable StructurePose local;
    private @Nullable StructurePose previousLocal;
    /* What stands on it, by entity: where its feet are in the yard and how it looks, this tick and the last. */
    private final Int2ObjectMap<double[]> riders = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectMap<double[]> previousRiders = new Int2ObjectOpenHashMap<>();

    /* The most poses kept, and the most ticks it goes on past the newest at its last speed. */
    private static final int KEPT = 32;
    private static final int GOES_ON = 10;
    /* How deep structures are held in structures, at most. */
    private static final int DEEPEST = 8;

    ClientStructure(final MovingStructureEntity entity) {
        this.entity = entity;
        this.meshes = new StructureMeshes(this);
        this.id = entity.structureId() != null ? entity.structureId() : StructureUuid.random();
        this.plot = entity.plot();
        this.bounds = entity.bounds();
        final double[] pivot = entity.pivot();
        this.pivotX = pivot[0];
        this.pivotY = pivot[1];
        this.pivotZ = pivot[2];
        this.frame = new StructureFrame(StructurePose.at(entity.getX(), entity.getY(), entity.getZ()), pivotX, pivotY,
                pivotZ);
        this.previousFrame = frame;
        this.worldBox = boxOf(frame);
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

    /* While the player's aim is found for a frame, the structure as that frame draws it. */
    @Override
    public StructureFrame aimFrame() {
        final float partial = StructureAim.aimingAt();
        return Float.isNaN(partial) ? frame : new StructureFrame(drawnAt(partial), pivotX, pivotY, pivotZ);
    }

    @Override
    public Box bounds() {
        return bounds;
    }

    @Override
    public AABB worldBox() {
        return worldBox;
    }

    /**
     * The box it may be drawn in this tick, for whether it is seen: where it was the tick before and where it is,
     * drawn between them, and a block more each way, for a block entity's model standing out of its block.
     */
    public AABB drawnBox() {
        return worldBox.minmax(boxOf(previousFrame)).inflate(1);
    }

    @Override
    public boolean ready() {
        return ready;
    }

    public MovingStructureEntity entity() {
        return entity;
    }

    /** The drawing of its blocks. */
    public StructureMeshes meshes() {
        return meshes;
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

    /**
     * The pose to draw it at, {@code partialTick} of the way from the last tick's to this tick's. One held by another
     * is drawn where its parent is drawn, at its place on it then, so the two never part between ticks.
     */
    public StructurePose drawnAt(final float partialTick) {
        final ClientStructure holding = parent();
        if (holding != null && local != null && previousLocal != null) {
            final StructureFrame drawn = new StructureFrame(holding.drawnAt(partialTick), holding.pivotX(),
                    holding.pivotY(), holding.pivotZ());
            return drawn.placed(StructurePose.between(previousLocal, local, partialTick));
        }
        return StructurePose.between(previousFrame.pose(), frame.pose(), partialTick);
    }

    /** The structure holding it, as the player's game has it, or null. */
    public @Nullable ClientStructure parent() {
        return parentEntity < 0 ? null : ClientMovingStructures.byEntity(parentEntity);
    }

    /** How many structures hold it, one inside another: none for one held by none. */
    int depth() {
        int depth = 0;
        ClientStructure at = parent();
        while (at != null && depth < DEEPEST) {
            depth++;
            at = at.parent();
        }
        return depth;
    }

    /** Whether it is being set down: it stands where it lands, and is told nothing more. */
    public boolean landed() {
        return landed;
    }

    /**
     * The server is setting it down at {@code grid}: it stands there from now on, its blocks drawn as they are, until
     * the land is drawn with them ({@link StructureHandOffs}).
     */
    void land(final StructurePose grid) {
        landed = true;
        told.clear();
        frame = new StructureFrame(grid, pivotX, pivotY, pivotZ);
        previousFrame = frame;
        worldBox = boxOf(frame);
        meshes.freeze();
    }

    /**
     * The server told where it stood at a tick. Told twice for one tick, made and turned in it, the later word is
     * where it stood.
     */
    void told(final StructurePosePayload payload) {
        if (landed) {
            return;
        }
        StructurePosePayload newest = told.peekLast();
        if (newest != null && payload.tick() < newest.tick()) {
            return;
        }
        if (newest != null && payload.tick() == newest.tick()) {
            told.removeLast();
            newest = told.peekLast();
        }
        if (newest != null) {
            behind = (int) Math.max(2, Math.min(SendRate.LONGEST + 1, payload.tick() - newest.tick() + 1));
        }
        told.addLast(payload);
        while (told.size() > KEPT) {
            told.removeFirst();
        }
        if (clock == Long.MIN_VALUE || payload.tick() > clock || clock - payload.tick() > GOES_ON * 2L) {
            clock = payload.tick();
        }
        if (clock == payload.tick() && told.size() == 1) {
            frame = new StructureFrame(payload.pose(), pivotX, pivotY, pivotZ);
            previousFrame = frame;
            worldBox = boxOf(frame);
        }
    }

    /**
     * The server changed its plot, its pivot or its box of blocks. The game hands those over one at a time, so a box
     * whose corners are not yet both in the structure's plot is not taken until they are.
     */
    void changed() {
        if (landed) {
            return;
        }
        final StructureUuid known = entity.structureId();
        if (known != null) {
            this.id = known;
        }
        this.plot = entity.plot();
        final Box told = entity.bounds();
        if (plot >= 0 && YardLayout.plotOfBlock(told.minX(), told.minZ()) == plot
                && YardLayout.plotOfBlock(told.maxX(), told.maxZ()) == plot) {
            this.bounds = told;
        }
        final double[] pivot = entity.pivot();
        if (pivot[0] != pivotX || pivot[1] != pivotY || pivot[2] != pivotZ) {
            this.pivotX = pivot[0];
            this.pivotY = pivot[1];
            this.pivotZ = pivot[2];
            this.frame = new StructureFrame(frame.pose(), pivotX, pivotY, pivotZ);
            this.previousFrame = frame;
        }
        this.worldBox = boxOf(frame);
        meshes.boundsChanged();
    }

    /**
     * One tick of the player's game: the clock goes on and the structure takes the pose of now, a little behind; one
     * held by another takes its place on its parent, which took its own pose first.
     */
    void tick() {
        if (!ready) {
            ready = loaded();
        }
        previousFrame = frame;
        previousLocal = local;
        if (landed || told.isEmpty()) {
            return;
        }
        clock++;
        final long at = clock - behind;
        final StructurePosePayload newest = told.peekLast();
        parentEntity = newest.parent();
        final ClientStructure holding = parent();
        final StructurePose pose;
        if (holding != null) {
            local = localAt(at, holding.frame());
            if (previousLocal == null) {
                previousLocal = local;
            }
            pose = holding.frame().placed(local);
        } else {
            local = null;
            previousLocal = null;
            pose = poseAt(at);
        }
        frame = new StructureFrame(pose, pivotX, pivotY, pivotZ);
        worldBox = boxOf(frame);
        previousRiders.clear();
        previousRiders.putAll(riders);
        riders.clear();
        // At rest, what stands on it is where the game has it; going, where the server had it on it then.
        if (!frame.pose().equals(previousFrame.pose())) {
            ridersAt(at);
        }
        while (told.size() > 2) {
            final Iterator<StructurePosePayload> oldest = told.iterator();
            oldest.next();
            if (oldest.next().tick() > at) {
                break;
            }
            told.removeFirst();
        }
    }

    /* The pose at the server's tick {@code at}: between the two told round it, or carried on past the newest. */
    private StructurePose poseAt(final long at) {
        StructurePosePayload before = null;
        StructurePosePayload after = null;
        for (final StructurePosePayload one : told) {
            if (one.tick() <= at) {
                before = one;
            } else {
                after = one;
                break;
            }
        }
        if (before == null) {
            return told.peekFirst().pose();
        }
        if (after != null) {
            final double t = (double) (at - before.tick()) / (after.tick() - before.tick());
            return StructurePose.between(before.pose(), after.pose(), t);
        }
        final long past = Math.min(GOES_ON, at - before.tick());
        final StructurePose pose = before.pose();
        final float[] speed = before.velocity();
        final float[] spin = before.spin();
        final double angle = Math.sqrt(spin[0] * spin[0] + spin[1] * spin[1] + spin[2] * spin[2]);
        final StructurePose moved = pose.movedTo(pose.x() + speed[0] * past, pose.y() + speed[1] * past,
                pose.z() + speed[2] * past);
        return angle < 1.0e-9 ? moved : moved.turnedBy(spin[0], spin[1], spin[2], angle * past);
    }

    /**
     * What stands on it this tick, by entity: where its feet are in the yard, {@code {x, y, z}}, and how it looks from
     * its forward, in degrees; none while it is at rest.
     */
    public Int2ObjectMap<double[]> riders() {
        return riders;
    }

    /** What stood on it the tick before, as {@link #riders()}. */
    public Int2ObjectMap<double[]> previousRiders() {
        return previousRiders;
    }

    /* What stood on it at the server's tick {@code at}, each between the two told round it, or the newest. */
    private void ridersAt(final long at) {
        StructurePosePayload before = null;
        StructurePosePayload after = null;
        for (final StructurePosePayload one : told) {
            if (one.tick() <= at) {
                before = one;
            } else {
                after = one;
                break;
            }
        }
        if (before == null) {
            return;
        }
        final double t = after == null ? 0 : (double) (at - before.tick()) / (after.tick() - before.tick());
        for (final StructurePosePayload.Rider rider : before.riders()) {
            double x = rider.x();
            double y = rider.y();
            double z = rider.z();
            double yaw = rider.yaw();
            if (after != null) {
                for (final StructurePosePayload.Rider later : after.riders()) {
                    if (later.entity() == rider.entity()) {
                        x += (later.x() - x) * t;
                        y += (later.y() - y) * t;
                        z += (later.z() - z) * t;
                        yaw += Mth.wrapDegrees(later.yaw() - yaw) * t;
                        break;
                    }
                }
            }
            riders.put(rider.entity(), new double[] {pivotX + x, pivotY + y, pivotZ + z, yaw});
        }
    }

    /*
     * Where it stood on its parent, standing at {@code holder} now, at the server's tick {@code at}: between the two
     * told round it, or the newest. Told it then before it was held, where it stood in the world is taken on the
     * parent where it stands, so one just taken hold of does not jump for the ticks it is drawn behind.
     */
    private StructurePose localAt(final long at, final StructureFrame holder) {
        StructurePosePayload before = null;
        StructurePosePayload after = null;
        for (final StructurePosePayload one : told) {
            if (one.tick() <= at) {
                before = one;
            } else {
                after = one;
                break;
            }
        }
        if (before == null) {
            before = told.peekFirst();
        }
        if (!before.held()) {
            return holder.local(before.pose());
        }
        if (after != null && after != before && after.held()) {
            final double t = (double) (at - before.tick()) / (after.tick() - before.tick());
            return StructurePose.between(before.local(), after.local(), t);
        }
        return before.local();
    }

    /** Whether the server has told the box of its blocks, both corners in its plot of the yard. */
    boolean known() {
        return plot >= 0 && YardLayout.plotOfBlock(bounds.minX(), bounds.minZ()) == plot
                && YardLayout.plotOfBlock(bounds.maxX(), bounds.maxZ()) == plot;
    }

    /* Whether every chunk of the yard holding its blocks has reached the player's game. */
    private boolean loaded() {
        if (!known()) {
            return false;
        }
        for (int x = bounds.minX() >> 4; x <= bounds.maxX() >> 4; x++) {
            for (int z = bounds.minZ() >> 4; z <= bounds.maxZ() >> 4; z++) {
                if (entity.level().getChunkSource().getChunk(x, z, ChunkStatus.FULL, false) == null) {
                    return false;
                }
            }
        }
        return true;
    }

    private AABB boxOf(final StructureFrame at) {
        final double[] box = at.worldBox(bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX() + 1,
                bounds.maxY() + 1, bounds.maxZ() + 1);
        return new AABB(box[0], box[1], box[2], box[3], box[4], box[5]);
    }
}
