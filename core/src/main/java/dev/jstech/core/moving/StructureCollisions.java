/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import dev.jstech.core.util.Loaded;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Entities against the blocks of moving structures, however the structures are turned: an entity's box, square to the
 * world, is seen from each structure near it as a turned box, and swept against the blocks there ({@link SweptBoxes})
 * until it touches, then slides along what it touched. What lands on a floor stays on it, never sliding down a
 * gently tilted deck; a step is climbed as on the ground; and an entity is told it stands on the structure, so the
 * structure carries it ({@link StructureFooting}). The game's own move goes first, against the world's blocks; this
 * takes what it allowed and holds it to the structures too ({@code EntityMixin}).
 */
public final class StructureCollisions {

    /* A surface whose normal points this far up is a floor: what stands on it stays put on it. */
    private static final double WALKABLE = 0.64;
    /* How far a box is kept off what it touched, so the next move does not start inside it. */
    private static final double KEEP_OFF = 1.0e-6;
    /* How many times a move slides along what it touches, at most. */
    private static final int SLIDES = 4;
    /* A move longer than this, in blocks, is gone in pieces, so no one piece reads a great many blocks. */
    private static final double LONGEST = 4;
    /* The most blocks one piece of a move looks at. */
    private static final int MOST_BLOCKS = 8192;
    /* Below this a move is no move. */
    private static final double TINY = 1.0e-9;
    /* How deep a pushed entity is pushed out at most a tick, in blocks; deeper it is let be, buried. */
    private static final double DEEPEST_PUSH = 1.5;
    /* The scratch of the sweeps on each thread: the server's and the player's game sweep at once. */
    private static final ThreadLocal<Scratch> SCRATCH = ThreadLocal.withInitial(Scratch::new);

    private StructureCollisions() {
    }

    /**
     * Holds an entity's move to the moving structures near it: {@code wanted} is the move it tried, {@code allowed}
     * what the world's blocks let it make. Gives back the move it makes.
     */
    public static Vec3 collide(final Entity entity, final Vec3 wanted, final Vec3 allowed) {
        if (!LevelMovingStructures.anyLoaded()) {
            return allowed;
        }
        final Level level = entity.level();
        final LevelMovingStructures all = LevelMovingStructures.of(level);
        if (all.isEmpty()) {
            return allowed;
        }
        final AABB box = entity.getBoundingBox();
        final List<IStructureBody> near = all.near(box.expandTowards(wanted).inflate(1));
        if (near.isEmpty()) {
            return allowed;
        }
        final Contacts contacts = new Contacts();
        Vec3 moved = sweepAll(entity, box, allowed, near, contacts);
        if (!same(moved, allowed)) {
            moved = world(entity, box, moved);
        }
        final float stepHeight = entity.maxUpStep();
        final boolean grounded = entity.onGround() || contacts.support != null || wanted.y < 0 && moved.y > wanted.y;
        if (stepHeight > 0 && contacts.sideways && grounded) {
            final Contacts stepped = new Contacts();
            final Vec3 step = step(entity, box, wanted, near, stepHeight, stepped);
            if (horizontal(step) > horizontal(moved) + 1.0e-7) {
                moved = step;
                contacts.support = stepped.support;
                contacts.supportBody = stepped.supportBody;
            }
        }
        if (contacts.support != null && contacts.supportBody != null) {
            StructureFooting.stood(entity, contacts.supportBody, contacts.support);
        }
        return moved;
    }

    /**
     * Whether the world's box {@code box} overlaps a block of a moving structure: what the game asks to know whether
     * a place is free, for a sneaking player at an edge, a mob finding room, a block placed.
     */
    public static boolean overlaps(final Level level, @Nullable final Entity entity, final AABB box) {
        if (!LevelMovingStructures.anyLoaded()) {
            return false;
        }
        final LevelMovingStructures all = LevelMovingStructures.of(level);
        if (all.isEmpty()) {
            return false;
        }
        for (final IStructureBody body : all.near(box)) {
            if (!body.ready()) {
                continue;
            }
            final Scratch scratch = SCRATCH.get();
            if (gather(level, entity, body.frame(), box, scratch) == 0) {
                continue;
            }
            turn(body.frame(), box, scratch.turned);
            for (int i = 0; i < scratch.count; i++) {
                final double[] b = scratch.boxes;
                final SweptBoxes.Contact touch = SweptBoxes.sweep(scratch.turned, 0, 0, 0, b[i * 6], b[i * 6 + 1],
                        b[i * 6 + 2], b[i * 6 + 3], b[i * 6 + 4], b[i * 6 + 5]);
                if (touch != null && touch.overlapping()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Pushes out of a structure's blocks what it moved into: an entity a wall swept over, one a rising floor came up
     * under. A player's own game pushes them, so the server leaves players be; an entity buried deep is let be.
     */
    static void pushAside(final MovingStructure structure) {
        if (!(structure.level() instanceof ServerLevel level)) {
            return;
        }
        final List<Entity> touching = level.getEntities((Entity) null, structure.worldBox().inflate(0.1),
                StructureCollisions::pushable);
        for (final Entity entity : touching) {
            pushOut(entity, structure);
        }
    }

    /** Pushes {@code entity} out of {@code body}'s blocks it is inside, on whichever side of the game it is. */
    public static void pushOut(final Entity entity, final IStructureBody body) {
        final Level level = entity.level();
        final Scratch scratch = SCRATCH.get();
        for (int round = 0; round < 3; round++) {
            final AABB box = entity.getBoundingBox();
            if (gather(level, entity, body.frame(), box, scratch) == 0) {
                return;
            }
            turn(body.frame(), box, scratch.turned);
            SweptBoxes.Contact deepest = null;
            int deepestBox = -1;
            for (int i = 0; i < scratch.count; i++) {
                final double[] b = scratch.boxes;
                final SweptBoxes.Contact touch = SweptBoxes.sweep(scratch.turned, 0, 0, 0, b[i * 6], b[i * 6 + 1],
                        b[i * 6 + 2], b[i * 6 + 3], b[i * 6 + 4], b[i * 6 + 5]);
                if (touch != null && touch.overlapping() && (deepest == null || touch.depth() > deepest.depth())) {
                    deepest = touch;
                    deepestBox = i;
                }
            }
            if (deepest == null || deepest.depth() > DEEPEST_PUSH) {
                return;
            }
            final StructureFrame frame = body.frame();
            final double push = deepest.depth() + KEEP_OFF;
            final double nx = frame.turnX(deepest.nx(), deepest.ny(), deepest.nz());
            final double ny = frame.turnY(deepest.nx(), deepest.ny(), deepest.nz());
            final double nz = frame.turnZ(deepest.nx(), deepest.ny(), deepest.nz());
            entity.setPos(entity.getX() + nx * push, entity.getY() + ny * push, entity.getZ() + nz * push);
            if (ny >= WALKABLE) {
                StructureFooting.stood(entity, body, scratch.block(deepestBox));
            }
        }
    }

    /* Sweeps a move against every structure near, in pieces no longer than LONGEST, stopping when one is held. */
    private static Vec3 sweepAll(final Entity entity, final AABB box, final Vec3 motion,
                                 final List<IStructureBody> near, final Contacts contacts) {
        final double length = motion.length();
        if (length < TINY) {
            return motion;
        }
        final int pieces = (int) Math.ceil(length / LONGEST);
        final Vec3 piece = motion.scale(1.0 / pieces);
        Vec3 total = Vec3.ZERO;
        AABB at = box;
        for (int i = 0; i < pieces; i++) {
            Vec3 m = piece;
            for (final IStructureBody body : near) {
                if (body.ready()) {
                    m = sweepOne(entity, at, m, body, contacts);
                }
            }
            total = total.add(m);
            at = at.move(m);
            if (!same(m, piece)) {
                break;
            }
        }
        return total;
    }

    /* Sweeps a move against one structure's blocks, in the structure's own places. */
    private static Vec3 sweepOne(final Entity entity, final AABB box, final Vec3 motion, final IStructureBody body,
                                 final Contacts contacts) {
        final StructureFrame frame = body.frame();
        final Scratch scratch = SCRATCH.get();
        if (gather(entity.level(), entity, frame, box.expandTowards(motion), scratch) == 0) {
            return motion;
        }
        final SweptBoxes.Turned turned = scratch.turned;
        turn(frame, box, turned);
        double lx = frame.unturnX(motion.x, motion.y, motion.z);
        double ly = frame.unturnY(motion.x, motion.y, motion.z);
        double lz = frame.unturnZ(motion.x, motion.y, motion.z);
        double tx = 0;
        double ty = 0;
        double tz = 0;
        final double startX = turned.centreX();
        final double startY = turned.centreY();
        final double startZ = turned.centreZ();
        for (int slide = 0; slide < SLIDES; slide++) {
            if (lx * lx + ly * ly + lz * lz < TINY * TINY) {
                lx = 0;
                ly = 0;
                lz = 0;
                break;
            }
            turned.moveTo(startX + tx, startY + ty, startZ + tz);
            SweptBoxes.Contact first = null;
            int firstBox = -1;
            for (int i = 0; i < scratch.count; i++) {
                final double[] b = scratch.boxes;
                final SweptBoxes.Contact touch = SweptBoxes.sweep(turned, lx, ly, lz, b[i * 6], b[i * 6 + 1],
                        b[i * 6 + 2], b[i * 6 + 3], b[i * 6 + 4], b[i * 6 + 5]);
                if (touch == null) {
                    continue;
                }
                if (touch.overlapping()) {
                    if (lx * touch.nx() + ly * touch.ny() + lz * touch.nz() >= 0) {
                        // Inside it and moving out of it, or along it: it holds nothing back.
                        continue;
                    }
                    // Inside it and moving deeper: held at once.
                    first = touch;
                    firstBox = i;
                    break;
                }
                if (first == null || touch.time() < first.time()) {
                    first = touch;
                    firstBox = i;
                }
            }
            if (first == null) {
                tx += lx;
                ty += ly;
                tz += lz;
                lx = 0;
                ly = 0;
                lz = 0;
                break;
            }
            final double t = first.time();
            final double nx = first.nx();
            final double ny = first.ny();
            final double nz = first.nz();
            if (!first.overlapping()) {
                tx += lx * t + nx * KEEP_OFF;
                ty += ly * t + ny * KEEP_OFF;
                tz += lz * t + nz * KEEP_OFF;
            }
            double rx = lx * (1 - t);
            double ry = ly * (1 - t);
            double rz = lz * (1 - t);
            final double wy = frame.turnY(nx, ny, nz);
            if (wy >= WALKABLE) {
                contacts.support = scratch.block(firstBox);
                contacts.supportBody = body;
                double wrx = frame.turnX(rx, ry, rz);
                double wry = frame.turnY(rx, ry, rz);
                double wrz = frame.turnZ(rx, ry, rz);
                if (wry < 0) {
                    wry = 0;
                }
                final double wnx = frame.turnX(nx, ny, nz);
                final double wnz = frame.turnZ(nx, ny, nz);
                final double into = wrx * wnx + wry * wy + wrz * wnz;
                if (into < 0) {
                    wrx -= into * wnx;
                    wry -= into * wy;
                    wrz -= into * wnz;
                }
                rx = frame.unturnX(wrx, wry, wrz);
                ry = frame.unturnY(wrx, wry, wrz);
                rz = frame.unturnZ(wrx, wry, wrz);
            } else {
                final double into = rx * nx + ry * ny + rz * nz;
                if (into < 0) {
                    rx -= into * nx;
                    ry -= into * ny;
                    rz -= into * nz;
                }
                if (Math.abs(wy) < WALKABLE) {
                    contacts.sideways = true;
                }
            }
            lx = rx;
            ly = ry;
            lz = rz;
        }
        return new Vec3(frame.turnX(tx, ty, tz), frame.turnY(tx, ty, tz), frame.turnZ(tx, ty, tz));
    }

    /* Climbing a step, as the game does on the ground: up, across, and down onto it. */
    private static Vec3 step(final Entity entity, final AABB box, final Vec3 wanted, final List<IStructureBody> near,
                             final float height, final Contacts contacts) {
        final Contacts ignored = new Contacts();
        Vec3 up = world(entity, box, new Vec3(0, height, 0));
        up = sweepAll(entity, box, up, near, ignored);
        final AABB raised = box.move(up);
        Vec3 across = world(entity, raised, new Vec3(wanted.x, 0, wanted.z));
        across = sweepAll(entity, raised, across, near, ignored);
        final AABB over = raised.move(across);
        Vec3 down = world(entity, over, new Vec3(0, -up.y + Math.min(0, wanted.y), 0));
        down = sweepAll(entity, over, down, near, contacts);
        return up.add(across).add(down);
    }

    /* What the world's blocks, and the entities one collides with, let a box move by. */
    private static Vec3 world(final Entity entity, final AABB box, final Vec3 motion) {
        final Level level = entity.level();
        return Entity.collideBoundingBox(entity, motion, box, level,
                level.getEntityCollisions(entity, box.expandTowards(motion)));
    }

    /*
     * Gathers the boxes of the blocks of a structure that the world's box {@code area} may meet, in the yard's
     * places; gives back how many.
     */
    private static int gather(final Level level, @Nullable final Entity entity, final StructureFrame frame,
                              final AABB area, final Scratch scratch) {
        scratch.count = 0;
        final double[] yard = frame.yardBox(area.minX, area.minY, area.minZ, area.maxX, area.maxY, area.maxZ);
        final int minX = (int) Math.floor(yard[0] - 1.0e-3);
        final int minY = Math.max(level.getMinBuildHeight(), (int) Math.floor(yard[1] - 1.0e-3));
        final int minZ = (int) Math.floor(yard[2] - 1.0e-3);
        final int maxX = (int) Math.floor(yard[3] + 1.0e-3);
        final int maxY = Math.min(level.getMaxBuildHeight() - 1, (int) Math.floor(yard[4] + 1.0e-3));
        final int maxZ = (int) Math.floor(yard[5] + 1.0e-3);
        final long blocks = (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        if (maxY < minY || blocks > MOST_BLOCKS) {
            return 0;
        }
        final CollisionContext context = entity == null ? CollisionContext.empty() : CollisionContext.of(entity);
        final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (!Loaded.chunk(level, x >> 4, z >> 4)) {
                    continue;
                }
                for (int y = minY; y <= maxY; y++) {
                    at.set(x, y, z);
                    final BlockState state = level.getBlockState(at);
                    if (state.isAir()) {
                        continue;
                    }
                    final VoxelShape shape = state.getCollisionShape(level, at, context);
                    if (shape.isEmpty()) {
                        continue;
                    }
                    if (shape == Shapes.block()) {
                        scratch.add(x, y, z, 0, 0, 0, 1, 1, 1);
                        continue;
                    }
                    for (final AABB part : shape.toAabbs()) {
                        scratch.add(x, y, z, part.minX, part.minY, part.minZ, part.maxX, part.maxY, part.maxZ);
                    }
                }
            }
        }
        return scratch.count;
    }

    /* Sets {@code turned} to the world's box {@code box} as a structure sees it: turned, in the yard's places. */
    private static void turn(final StructureFrame frame, final AABB box, final SweptBoxes.Turned turned) {
        final double cx = (box.minX + box.maxX) / 2;
        final double cy = (box.minY + box.maxY) / 2;
        final double cz = (box.minZ + box.maxZ) / 2;
        final double[] rows = SCRATCH.get().rows;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                rows[row * 3 + column] = frame.rotation(row, column);
            }
        }
        turned.set(frame.yardX(cx, cy, cz), frame.yardY(cx, cy, cz), frame.yardZ(cx, cy, cz),
                (box.maxX - box.minX) / 2, (box.maxY - box.minY) / 2, (box.maxZ - box.minZ) / 2, rows);
    }

    private static boolean pushable(final Entity entity) {
        return !(entity instanceof MovingStructureEntity) && !(entity instanceof Player) && !entity.noPhysics
                && !entity.isSpectator() && !entity.isPassenger();
    }

    private static boolean same(final Vec3 a, final Vec3 b) {
        return Math.abs(a.x - b.x) < TINY && Math.abs(a.y - b.y) < TINY && Math.abs(a.z - b.z) < TINY;
    }

    private static double horizontal(final Vec3 motion) {
        return motion.x * motion.x + motion.z * motion.z;
    }

    /* What a sweep found: the floor stood on, and whether a wall held the move sideways. */
    private static final class Contacts {
        private @Nullable BlockPos support;
        private @Nullable IStructureBody supportBody;
        private boolean sideways;
    }

    /* The boxes of the blocks a sweep looks at, and the turned box, kept per thread so a sweep makes no garbage. */
    private static final class Scratch {

        private final SweptBoxes.Turned turned = new SweptBoxes.Turned();
        private final double[] rows = new double[9];
        private double[] boxes = new double[6 * 64];
        private int[] places = new int[3 * 64];
        private int count;

        void add(final int x, final int y, final int z, final double minX, final double minY, final double minZ,
                 final double maxX, final double maxY, final double maxZ) {
            if (count * 6 == boxes.length) {
                boxes = Arrays.copyOf(boxes, boxes.length * 2);
                places = Arrays.copyOf(places, places.length * 2);
            }
            final int at = count * 6;
            boxes[at] = x + minX;
            boxes[at + 1] = y + minY;
            boxes[at + 2] = z + minZ;
            boxes[at + 3] = x + maxX;
            boxes[at + 4] = y + maxY;
            boxes[at + 5] = z + maxZ;
            places[count * 3] = x;
            places[count * 3 + 1] = y;
            places[count * 3 + 2] = z;
            count++;
        }

        BlockPos block(final int box) {
            return new BlockPos(places[box * 3], places[box * 3 + 1], places[box * 3 + 2]);
        }
    }
}
