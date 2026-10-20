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
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;

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

    /* The most poses kept, and the most ticks it goes on past the newest at its last speed. */
    private static final int KEPT = 32;
    private static final int GOES_ON = 10;

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

    /** The pose to draw it at, {@code partialTick} of the way from the last tick's to this tick's. */
    public StructurePose drawnAt(final float partialTick) {
        return StructurePose.between(previousFrame.pose(), frame.pose(), partialTick);
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

    /** The server told where it stood at a tick. */
    void told(final StructurePosePayload payload) {
        if (landed) {
            return;
        }
        final StructurePosePayload newest = told.peekLast();
        if (newest != null && payload.tick() <= newest.tick()) {
            return;
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

    /** One tick of the player's game: the clock goes on and the structure takes the pose of now, a little behind. */
    void tick() {
        if (!ready) {
            ready = loaded();
        }
        previousFrame = frame;
        if (landed || told.isEmpty()) {
            return;
        }
        clock++;
        final long at = clock - behind;
        final StructurePose pose = poseAt(at);
        frame = new StructureFrame(pose, pivotX, pivotY, pivotZ);
        worldBox = boxOf(frame);
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
