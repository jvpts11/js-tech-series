/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving.physics;

import dev.jstech.core.multiblock.FormedPartBlock;
import dev.jstech.core.region.Box;
import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3d;

/**
 * What a structure is made of, for its physics, kept as its blocks change, each change costing the same whatever its
 * size: its mass, centre and inertia; its faces that meet the air, by the way they look, for its drag; its blocks at
 * its surface, which meet the world; the volume each block displaces, for its floating; and its blocks that give a
 * force, its engines, wings, wheels and the rest. All of it in the yard's places, on the server's thread.
 */
public final class StructureParts {

    private final MassSum mass = new MassSum();
    private final LongSet filled = new LongOpenHashSet();
    private final Long2DoubleMap masses = new Long2DoubleOpenHashMap();
    private final Long2DoubleMap displaced = new Long2DoubleOpenHashMap();
    /* How many of each block's faces meet the air; a block with any, that collides, is at the surface. */
    private final Long2IntMap open = new Long2IntOpenHashMap();
    private final LongSet collides = new LongOpenHashSet();
    private final LongSet surface = new LongOpenHashSet();
    /* The boxes of each block that collides, in its own block's places, from 0 to 1, and how well it grips. */
    private final Long2ObjectMap<AABB[]> shapes = new Long2ObjectOpenHashMap<>();
    private final Long2DoubleMap grips = new Long2DoubleOpenHashMap();
    private final Long2ObjectMap<Part> parts = new Long2ObjectOpenHashMap<>();
    /* The faces meeting the air, by the way they look: their area, and the sums of their middles times their area. */
    private final Map<Direction, double[]> faces = facesNone();
    /* The air its hull holds, worked out again a while after its blocks change, and when it last was. */
    private final LongSet hull = new LongOpenHashSet();
    private boolean hullStale = true;
    private long hullAt = -HULL_AGAIN;
    private boolean changed;

    /* A block's side, in metres. */
    private static final double SIDE = 1.0;
    private static final double HALF = 0.5;
    /* The boxes of a whole block, shared by every whole block; never written. */
    private static final AABB[] WHOLE = {new AABB(0, 0, 0, 1, 1, 1)};
    private static final AABB[] NO_BOXES = {};
    /* The game's own slipperiness of most blocks. */
    private static final double PLAIN_SLIPPERINESS = 0.6;
    /* How long after its blocks change the air its hull holds is worked out again, at most, in ticks. */
    private static final long HULL_AGAIN = 20;

    /** Takes in every block of {@code bounds} of the yard, as the structure starts to move by its physics. */
    public void fill(final Level level, final Box bounds) {
        final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
            for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                for (int y = bounds.minY(); y <= bounds.maxY(); y++) {
                    at.set(x, y, z);
                    final BlockState state = level.getBlockState(at);
                    if (!state.isAir()) {
                        add(level, at.immutable(), state);
                    }
                }
            }
        }
        changed = true;
    }

    /** The block at {@code pos} of the yard changed from {@code before} to {@code after}. */
    public void change(final Level level, final BlockPos pos, final BlockState before, final BlockState after) {
        if (filled.contains(pos.asLong())) {
            remove(pos);
        }
        if (!after.isAir()) {
            add(level, pos, after);
        }
        changed = true;
        hullStale = true;
    }

    /** Whether it changed since this was last asked, and so the structure is to wake. */
    public boolean takeChanged() {
        final boolean was = changed;
        changed = false;
        return was;
    }

    /** Its mass, centre and inertia, in the yard's places. */
    public MassSum mass() {
        return mass;
    }

    /** Whether a block of it is at the yard's {@code pos}. */
    public boolean filled(final long pos) {
        return filled.contains(pos);
    }

    /** Its blocks that collide and meet the world, by their place in the yard. */
    public LongSet surface() {
        return surface;
    }

    /** The volume the block at {@code pos} displaces in a liquid, by its place in the yard. */
    public Long2DoubleMap displaced() {
        return displaced;
    }

    /**
     * The boxes the block at the yard's {@code pos} collides by, in its own block's places; none when it does not. The
     * array is shared: it is read, never written.
     */
    public AABB[] shape(final long pos) {
        final AABB[] boxes = shapes.get(pos);
        return boxes == null ? NO_BOXES : boxes;
    }

    /**
     * The air its hull holds, by its place in the yard, for its floating: each block of air with its blocks below it
     * and on its four sides, as the inside of a boat is, so a ship of iron floats by the air in it as a real one does.
     * Worked out again at most once a second while its blocks change; {@code now} is the game's tick.
     */
    public LongSet hull(final Box bounds, final long now) {
        if (hullStale && now - hullAt >= HULL_AGAIN) {
            hullOf(bounds);
            hullStale = false;
            hullAt = now;
        }
        return hull;
    }

    /** Whether the block at the yard's {@code pos} is of it and collides as a whole block: a face of it is covered. */
    public boolean whole(final long pos) {
        return shapes.get(pos) == WHOLE;
    }

    /** How well the block at the yard's {@code pos} grips against sliding, as {@link ContactMath#grip(double)}. */
    public double grip(final long pos) {
        return grips.containsKey(pos) ? grips.get(pos) : ContactMath.grip(PLAIN_SLIPPERINESS);
    }

    /** Its blocks that give a force. */
    public Collection<Part> parts() {
        return parts.values();
    }

    /** The part at the yard's {@code pos}, or null for a block that gives no force. */
    public Part partAt(final long pos) {
        return parts.get(pos);
    }

    /** The area, in square metres, of its faces meeting the air looking {@code way}, by how much each drags. */
    public double faceArea(final Direction way) {
        return faces.get(way)[0];
    }

    /** The middle, in the yard's places, of its faces meeting the air looking {@code way}, into {@code into}. */
    public Vector3d faceMiddle(final Direction way, final Vector3d into) {
        final double[] face = faces.get(way);
        final double area = face[0];
        if (area <= 0) {
            return into.set(0, 0, 0);
        }
        return into.set(face[1] / area, face[2] / area, face[3] / area);
    }

    /** The boxes of {@code shape}, a whole block's shared: read, never written. */
    public static AABB[] boxesOf(final VoxelShape shape) {
        if (shape == Shapes.block()) {
            return WHOLE;
        }
        return shape.toAabbs().toArray(new AABB[0]);
    }

    /*
     * Takes in the block {@code state} at {@code pos}. A block of a formed structure merged into its model weighs,
     * floats, drags, grips and pushes as the block it was, an engine merged into a rocket's model still thrusting; it
     * collides as it stands, as its part of the model.
     */
    private void add(final Level level, final BlockPos pos, final BlockState state) {
        final long key = pos.asLong();
        final BlockState was = FormedPartBlock.originalAt(level, pos, state);
        final double kilograms = BlockPhysics.massOf(level, pos, was);
        mass.add(pos.getX() + HALF, pos.getY() + HALF, pos.getZ() + HALF, kilograms, SIDE);
        masses.put(key, kilograms);
        filled.add(key);
        final double displacement = BlockPhysics.displacementOf(level, pos, was);
        if (displacement > 0) {
            displaced.put(key, displacement);
        }
        final BlockPhysics physics = BlockPhysics.of(was);
        final double drag = physics.drag().orElse(1.0);
        for (final Direction way : Direction.values()) {
            final long beside = BlockPos.offset(key, way);
            if (filled.contains(beside)) {
                // The block beside no longer meets the air on its side towards this one.
                closeFace(beside, way.getOpposite(), dragOf(beside));
            } else {
                openFace(key, way, drag);
            }
        }
        // A wheel meets the ground by its spring, not as a block.
        final VoxelShape shape = state.getCollisionShape(level, pos);
        if (!shape.isEmpty() && physics.wheel().isEmpty()) {
            collides.add(key);
            shapes.put(key, boxesOf(shape));
            grips.put(key, ContactMath.grip(was.getFriction(level, pos, null)));
            if (open.get(key) > 0) {
                surface.add(key);
            }
        }
        if (physics.thrust().isPresent() || physics.wing().isPresent() || physics.levitation().isPresent()
                || physics.magnet().isPresent() || physics.wheel().isPresent() || physics.burner().isPresent()) {
            parts.put(key, new Part(pos, was, physics, BlockPhysics.wayOf(was)));
        }
        buriedAgain(key);
    }

    private void remove(final BlockPos pos) {
        final long key = pos.asLong();
        final double kilograms = masses.remove(key);
        mass.remove(pos.getX() + HALF, pos.getY() + HALF, pos.getZ() + HALF, kilograms, SIDE);
        displaced.remove(key);
        final Part part = parts.remove(key);
        final double drag = part != null ? part.physics().drag().orElse(1.0) : dragOf(key);
        for (final Direction way : Direction.values()) {
            final long beside = BlockPos.offset(key, way);
            if (filled.contains(beside)) {
                openFace(beside, way.getOpposite(), dragOf(beside));
                if (collides.contains(beside)) {
                    surface.add(beside);
                }
            } else {
                closeFace(key, way, drag);
            }
        }
        filled.remove(key);
        open.remove(key);
        surface.remove(key);
        collides.remove(key);
        shapes.remove(key);
        grips.remove(key);
    }

    /*
     * The air its hull holds: each block of air that has a block of its own that collides somewhere below it, and
     * somewhere along each of its four sides, in the same row.
     */
    private void hullOf(final Box bounds) {
        hull.clear();
        final int minX = bounds.minX();
        final int minY = bounds.minY();
        final int minZ = bounds.minZ();
        final int sizeX = bounds.maxX() - minX + 1;
        final int sizeY = bounds.maxY() - minY + 1;
        final int sizeZ = bounds.maxZ() - minZ + 1;
        final int[] floor = filledWith(sizeX * sizeZ, Integer.MAX_VALUE);
        final int[] rowLeast = filledWith(sizeY * sizeZ, Integer.MAX_VALUE);
        final int[] rowMost = filledWith(sizeY * sizeZ, Integer.MIN_VALUE);
        final int[] lineLeast = filledWith(sizeX * sizeY, Integer.MAX_VALUE);
        final int[] lineMost = filledWith(sizeX * sizeY, Integer.MIN_VALUE);
        for (final long key : collides) {
            final int x = BlockPos.getX(key) - minX;
            final int y = BlockPos.getY(key) - minY;
            final int z = BlockPos.getZ(key) - minZ;
            if (x < 0 || y < 0 || z < 0 || x >= sizeX || y >= sizeY || z >= sizeZ) {
                continue;
            }
            floor[x * sizeZ + z] = Math.min(floor[x * sizeZ + z], y);
            rowLeast[y * sizeZ + z] = Math.min(rowLeast[y * sizeZ + z], x);
            rowMost[y * sizeZ + z] = Math.max(rowMost[y * sizeZ + z], x);
            lineLeast[x * sizeY + y] = Math.min(lineLeast[x * sizeY + y], z);
            lineMost[x * sizeY + y] = Math.max(lineMost[x * sizeY + y], z);
        }
        for (int x = 0; x < sizeX; x++) {
            for (int z = 0; z < sizeZ; z++) {
                if (floor[x * sizeZ + z] == Integer.MAX_VALUE) {
                    continue;
                }
                for (int y = floor[x * sizeZ + z] + 1; y < sizeY; y++) {
                    final long key = BlockPos.asLong(minX + x, minY + y, minZ + z);
                    if (filled.contains(key)) {
                        continue;
                    }
                    if (rowLeast[y * sizeZ + z] < x && x < rowMost[y * sizeZ + z]
                            && lineLeast[x * sizeY + y] < z && z < lineMost[x * sizeY + y]) {
                        hull.add(key);
                    }
                }
            }
        }
    }

    private static int[] filledWith(final int length, final int value) {
        final int[] array = new int[length];
        Arrays.fill(array, value);
        return array;
    }

    /* The blocks beside one just set may no longer meet the world, all their faces now against blocks. */
    private void buriedAgain(final long key) {
        for (final Direction way : Direction.values()) {
            final long beside = BlockPos.offset(key, way);
            if (filled.contains(beside) && open.get(beside) == 0) {
                surface.remove(beside);
            }
        }
    }

    private double dragOf(final long key) {
        final Part part = parts.get(key);
        return part != null ? part.physics().drag().orElse(1.0) : 1.0;
    }

    private void openFace(final long key, final Direction way, final double drag) {
        open.put(key, open.get(key) + 1);
        face(key, way, drag);
    }

    private void closeFace(final long key, final Direction way, final double drag) {
        open.put(key, Math.max(0, open.get(key) - 1));
        face(key, way, -drag);
    }

    /* Adds, or takes away, the face of the block at {@code key} looking {@code way}, of {@code area}. */
    private void face(final long key, final Direction way, final double area) {
        final double[] face = faces.get(way);
        face[0] += area;
        face[1] += area * (BlockPos.getX(key) + HALF + HALF * way.getStepX());
        face[2] += area * (BlockPos.getY(key) + HALF + HALF * way.getStepY());
        face[3] += area * (BlockPos.getZ(key) + HALF + HALF * way.getStepZ());
    }

    /* No faces yet, each way's: its area, and the sum of its middles times their area, east, up and south. */
    private static Map<Direction, double[]> facesNone() {
        final Map<Direction, double[]> none = new EnumMap<>(Direction.class);
        for (final Direction way : Direction.values()) {
            none.put(way, new double[4]);
        }
        return none;
    }

    /**
     * A block that gives a force.
     *
     * @param pos     its place in the yard
     * @param state   what it is
     * @param physics the forces it gives
     * @param way     the way it looks
     */
    public record Part(BlockPos pos, BlockState state, BlockPhysics physics, Direction way) {
    }
}
