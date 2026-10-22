/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import dev.jstech.core.region.Box;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Taking a moving structure to another dimension with everyone aboard, as a rocket reaching orbit: its blocks go whole
 * from this dimension's yard to that one's, the structure arrives at the place asked turned as it was, still moving by
 * its rule, and whatever stood on it or was inside it arrives where it was on it. It goes once the plot it takes in
 * that dimension's yard and the land it arrives at have loaded, a few ticks after it is sent, working on where it is
 * until then. A structure taller than the dimension it goes to has room for stays where it is.
 */
public final class StructureTravel {

    /* How far round its box, in blocks, what goes with a structure may stand: the reach it is gathered in, and one. */
    private static final double ABOARD_REACH = 3;

    private StructureTravel() {
    }

    /**
     * Sends {@code structure} to {@code to}, its pivot arriving at {@code at}, once the chunks it goes to have loaded
     * ({@link MovingStructure#departing()}); refused, saying why, when it cannot go. Within its own dimension it is
     * only moved there, at once.
     */
    public static StructureResult travel(final MovingStructure structure, final ServerLevel to, final Vec3 at) {
        if (!(structure.level() instanceof ServerLevel from) || structure.arriving() || !structure.ready()) {
            return StructureResult.refused(StructureTexts.NOT_READY.text());
        }
        if (structure.departing()) {
            return StructureResult.refused(StructureTexts.TRAVELLING.text());
        }
        if (from == to) {
            structure.teleport(structure.pose().movedTo(at.x, at.y, at.z));
            return StructureResult.done(structure, structure.blocks());
        }
        final Box bounds = structure.bounds();
        final int tall = bounds.maxY() - bounds.minY() + 1;
        if (tall > to.getHeight()) {
            return StructureResult.refused(StructureTexts.TOO_TALL.with(tall, to.dimension().location().toString(),
                    to.getHeight()));
        }
        final int dy = bounds.minY() < to.getMinBuildHeight() ? to.getMinBuildHeight() - bounds.minY()
                : bounds.maxY() >= to.getMaxBuildHeight() ? to.getMaxBuildHeight() - 1 - bounds.maxY() : 0;
        final int plot;
        try {
            plot = StructureYard.take(to, structure.id());
        } catch (final IllegalStateException full) {
            return StructureResult.refused(StructureTexts.YARD_FULL.text());
        }
        structure.depart(new StructureDeparture(to.dimension(), plot, dy, at.x, at.y, at.z));
        // What it holds goes with it, each arriving where it stands on it.
        final StructureFrame arriving = new StructureFrame(structure.pose().movedTo(at.x, at.y, at.z),
                structure.pivotX(), structure.pivotY(), structure.pivotZ());
        for (final MovingStructure child : StructureMounts.children(structure)) {
            final StructureMount mount = child.mount();
            if (mount != null && !child.departing()) {
                final StructurePose there = arriving.placed(mount.local());
                travel(child, to, new Vec3(there.x(), there.y(), there.z()));
            }
        }
        return StructureResult.done(structure, structure.blocks());
    }

    /* Keeps the chunks a structure on its way goes to loaded: its plot there, and the land it arrives at. */
    static void hold(final MovingStructure structure, final ServerLevel from, final StructureDeparture going) {
        final ServerLevel to = from.getServer().getLevel(going.to());
        if (to != null) {
            StructureYard.hold(to, arriving(structure, going));
            for (final ChunkPos chunk : landing(structure, going)) {
                StructureYard.holdChunk(to, chunk);
            }
        }
    }

    /* Lets the chunks a structure on its way held go, as it leaves the level before it went: it holds them again. */
    static void release(final MovingStructure structure, final ServerLevel from, final StructureDeparture going) {
        final ServerLevel to = from.getServer().getLevel(going.to());
        if (to != null) {
            StructureYard.release(to, arriving(structure, going));
            for (final ChunkPos chunk : landing(structure, going)) {
                StructureYard.releaseChunk(to, chunk);
            }
        }
    }

    /*
     * Takes a structure on its way to where it goes, once the chunks it goes to have loaded; whether it went. One sent
     * to a dimension that is no more stays.
     */
    static boolean go(final MovingStructure structure, final ServerLevel from, final StructureDeparture going) {
        final ServerLevel to = from.getServer().getLevel(going.to());
        if (to == null) {
            structure.depart(null);
            return false;
        }
        final Box arriving = arriving(structure, going);
        final List<ChunkPos> landing = landing(structure, going);
        if (!StructureYard.loaded(to, arriving)) {
            return false;
        }
        for (final ChunkPos chunk : landing) {
            if (!entitiesReady(to, chunk)) {
                return false;
            }
        }
        final Box bounds = structure.bounds();
        final int dx = arriving.minX() - bounds.minX();
        final int dy = going.dy();
        final int dz = arriving.minZ() - bounds.minZ();
        final List<Aboard> aboard = aboard(structure, from);
        final Map<BlockPos, BlockPos> moves = new LinkedHashMap<>();
        final BlockPos.MutableBlockPos block = new BlockPos.MutableBlockPos();
        for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
            for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                for (int y = bounds.minY(); y <= bounds.maxY(); y++) {
                    block.set(x, y, z);
                    if (!from.getBlockState(block).isAir()) {
                        moves.put(block.immutable(), block.offset(dx, dy, dz));
                    }
                }
            }
        }
        final IStructureMotion motion = structure.motion();
        final StructureMount mount = structure.mount();
        structure.move(null);
        BlockMover.move(from, to, moves, Rotation.NONE, false, false);
        final StructurePose arrived = structure.pose().movedTo(going.x(), going.y(), going.z());
        final MovingStructure there = MovingStructures.make(to, structure.id(), going.plot(), arriving, moves.size(),
                structure.pivotX() + dx, structure.pivotY() + dy, structure.pivotZ() + dz, arrived, null);
        if (mount != null) {
            StructureMounts.unfile(there);
            there.mounted(mount);
            StructureMounts.file(there);
        }
        there.move(motion);
        structure.handMakerTo(there);
        // What it holds stands where it stood on it, here and there, its blocks now in another plot.
        StructureMounts.shift(from, structure.id(), dx, dy, dz);
        StructureMounts.shift(to, structure.id(), dx, dy, dz);
        structure.depart(null);
        MovingStructures.remove(structure, true);
        // The land stays loaded a while so the structure ticks there and holds its plot, then goes as a portal's.
        for (final ChunkPos chunk : landing) {
            StructureYard.holdLanding(to, chunk);
            StructureYard.releaseChunk(to, chunk);
        }
        for (final Aboard one : aboard) {
            one.arrive(there, dx, dy, dz);
        }
        return true;
    }

    /*
     * The chunks of the world a structure on its way arrives in: those its blocks fill there, and those a few blocks
     * round, where what stands on it arrives. An entity put in a chunk whose own entities have not loaded yet is lost
     * as they load, so every one of them is waited for.
     */
    private static List<ChunkPos> landing(final MovingStructure structure, final StructureDeparture going) {
        final Box arriving = arriving(structure, going);
        final Box bounds = structure.bounds();
        final StructurePose arrived = structure.pose().movedTo(going.x(), going.y(), going.z());
        final StructureFrame frame = new StructureFrame(arrived, structure.pivotX() + arriving.minX() - bounds.minX(),
                structure.pivotY() + going.dy(), structure.pivotZ() + arriving.minZ() - bounds.minZ());
        final AABB box = MovingStructure.boxOf(frame, arriving).inflate(ABOARD_REACH);
        final List<ChunkPos> chunks = new ArrayList<>();
        for (int x = Mth.floor(box.minX) >> 4; x <= Mth.floor(box.maxX) >> 4; x++) {
            for (int z = Mth.floor(box.minZ) >> 4; z <= Mth.floor(box.maxZ) >> 4; z++) {
                chunks.add(new ChunkPos(x, z));
            }
        }
        return chunks;
    }

    /*
     * Whether a chunk is loaded now with its own entities, and ticks them: a chunk's entities load after its blocks,
     * and one put there before they have is hidden, or lost as they load.
     */
    private static boolean entitiesReady(final ServerLevel level, final ChunkPos chunk) {
        return level.getChunkSource().getChunkNow(chunk.x, chunk.z) != null && level.areEntitiesLoaded(chunk.toLong())
                && level.isPositionEntityTicking(chunk.getWorldPosition());
    }

    /* The box a structure on its way fills in the yard it goes to: its own, in the plot it takes there. */
    private static Box arriving(final MovingStructure structure, final StructureDeparture going) {
        final Box bounds = structure.bounds();
        final int dx = YardLayout.centreX(going.plot()) - YardLayout.centreX(structure.plot());
        final int dz = YardLayout.centreZ(going.plot()) - YardLayout.centreZ(structure.plot());
        return new Box(bounds.minX() + dx, bounds.minY() + going.dy(), bounds.minZ() + dz, bounds.maxX() + dx,
                bounds.maxY() + going.dy(), bounds.maxZ() + dz);
    }

    /* What stands on the structure or is inside its box, and where each is on it. */
    private static List<Aboard> aboard(final MovingStructure structure, final ServerLevel level) {
        final List<Aboard> found = new ArrayList<>();
        final StructureFrame frame = structure.frame();
        for (final Entity entity : level.getEntities((Entity) null, structure.worldBox().inflate(2), any -> true)) {
            if (entity instanceof MovingStructureEntity || entity.isPassenger()) {
                continue;
            }
            final boolean on = StructureFooting.standingOn(entity) == structure;
            if (!on && !structure.worldBox().contains(entity.position())) {
                continue;
            }
            found.add(new Aboard(entity, frame.yardX(entity.getX(), entity.getY(), entity.getZ()),
                    frame.yardY(entity.getX(), entity.getY(), entity.getZ()),
                    frame.yardZ(entity.getX(), entity.getY(), entity.getZ()),
                    (float) Mth.wrapDegrees(entity.getYRot() - frame.pose().yawDegrees())));
        }
        return found;
    }

    /* One aboard: where it is on the structure, in the yard, and how it looks from the structure's forward. */
    private record Aboard(Entity entity, double x, double y, double z, float yaw) {

        void arrive(final MovingStructure structure, final int dx, final int dy, final int dz) {
            if (!(structure.level() instanceof ServerLevel level)) {
                return;
            }
            final StructureFrame frame = structure.frame();
            final double yx = x + dx;
            final double yy = y + dy;
            final double yz = z + dz;
            final double wx = frame.worldX(yx, yy, yz);
            final double wy = frame.worldY(yx, yy, yz);
            final double wz = frame.worldZ(yx, yy, yz);
            final float turned = (float) (frame.pose().yawDegrees() + yaw);
            if (entity instanceof ServerPlayer player) {
                player.teleportTo(level, wx, wy, wz, turned, player.getXRot());
            } else {
                entity.teleportTo(level, wx, wy, wz, Set.of(), turned, entity.getXRot());
            }
        }
    }
}
