/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import dev.jstech.core.region.Box;
import dev.jstech.core.util.Loaded;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Setting a moving structure down on the world's grid: its heading turned to the nearest quarter turn, its place to
 * the nearest block, and every block laid where it lands, turned with it, block entities whole. A structure leaning
 * more than half a quarter turn is refused, since the game's blocks only turn about the up axis; so is one that would
 * land on blocks that are in the way, or out of the world's height.
 */
public final class StructureDisassembly {

    /* A structure whose up leans further from the world's up than half a quarter turn cannot be set down. */
    private static final double UPRIGHT = Math.cos(Math.PI / 4);

    private StructureDisassembly() {
    }

    /**
     * Sets {@code structure} down where it is, aligned to the grid, and takes it away. One just made whose blocks still
     * stand where they stood is only undone; one whose blocks are not loaded, or that is on its way to another
     * dimension, is refused, and so is one that would land where the land is not loaded.
     */
    public static StructureResult setDown(final MovingStructure structure) {
        if (!(structure.level() instanceof ServerLevel level)) {
            return StructureResult.refused(StructureTexts.NOTHING.text());
        }
        if (structure.arriving()) {
            final int blocks = structure.blocks();
            MovingStructures.remove(structure, false);
            return StructureResult.done(null, blocks);
        }
        if (!structure.ready()) {
            return StructureResult.refused(StructureTexts.NOT_READY.text());
        }
        if (structure.departing()) {
            return StructureResult.refused(StructureTexts.TRAVELLING.text());
        }
        final StructureFrame frame = structure.frame();
        if (frame.turnY(0, 1, 0) < UPRIGHT) {
            return StructureResult.refused(StructureTexts.TILTED.text());
        }
        final int quarters = Math.floorMod((int) Math.round(-frame.pose().yawDegrees() / 90.0), 4);
        final StructurePose snapped = StructurePose.turned(frame.pose().x(), frame.pose().y(), frame.pose().z(), 0, 1,
                0, quarters * Math.PI / 2);
        final Box bounds = structure.bounds();
        final StructureFrame turned = new StructureFrame(snapped, structure.pivotX(), structure.pivotY(),
                structure.pivotZ());
        final double cx = bounds.minX() + 0.5;
        final double cy = bounds.minY() + 0.5;
        final double cz = bounds.minZ() + 0.5;
        final double wx = turned.worldX(cx, cy, cz);
        final double wy = turned.worldY(cx, cy, cz);
        final double wz = turned.worldZ(cx, cy, cz);
        final StructurePose grid = snapped.movedTo(snapped.x() + Math.floor(wx) + 0.5 - wx,
                snapped.y() + Math.floor(wy) + 0.5 - wy, snapped.z() + Math.floor(wz) + 0.5 - wz);
        final StructureFrame landing = new StructureFrame(grid, structure.pivotX(), structure.pivotY(),
                structure.pivotZ());
        final Map<BlockPos, BlockPos> moves = new LinkedHashMap<>();
        final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
            for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                for (int y = bounds.minY(); y <= bounds.maxY(); y++) {
                    at.set(x, y, z);
                    if (level.getBlockState(at).isAir()) {
                        continue;
                    }
                    final BlockPos to = BlockPos.containing(landing.worldX(x + 0.5, y + 0.5, z + 0.5),
                            landing.worldY(x + 0.5, y + 0.5, z + 0.5), landing.worldZ(x + 0.5, y + 0.5, z + 0.5));
                    if (level.isOutsideBuildHeight(to)) {
                        return StructureResult.refused(StructureTexts.OUT_OF_WORLD.text());
                    }
                    if (!Loaded.at(level, to)) {
                        return StructureResult.refused(StructureTexts.LAND_NOT_LOADED.text());
                    }
                    final BlockState there = level.getBlockState(to);
                    if (!there.isAir() && !there.canBeReplaced()) {
                        return StructureResult.refused(StructureTexts.BLOCKED.with(
                                BuiltInRegistries.BLOCK.getKey(there.getBlock()).toString(), to.toShortString()));
                    }
                    moves.put(at.immutable(), to);
                }
            }
        }
        // A block laid reads the blocks beside it, so the land a block beyond where they land must be loaded too.
        if (!roundLoaded(level, moves.values())) {
            return StructureResult.refused(StructureTexts.LAND_NOT_LOADED.text());
        }
        final Rotation rotation = switch (quarters) {
            case 1 -> Rotation.COUNTERCLOCKWISE_90;
            case 2 -> Rotation.CLOCKWISE_180;
            case 3 -> Rotation.CLOCKWISE_90;
            default -> Rotation.NONE;
        };
        // What it holds is set down with it, each where it stands; one that cannot be stays where it is, at rest.
        for (final MovingStructure child : StructureMounts.children(structure)) {
            StructureMounts.unmount(child, false);
            setDown(child);
        }
        structure.move(null);
        StructureSync.landing(structure, level, grid, moves.values());
        BlockMover.move(level, level, moves, rotation, false, true);
        MovingStructures.remove(structure, false);
        return StructureResult.done(null, moves.size());
    }

    /* Whether the chunks of the places, and of a block beyond them each way, are loaded now. */
    private static boolean roundLoaded(final ServerLevel level, final Collection<BlockPos> places) {
        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (final BlockPos at : places) {
            minX = Math.min(minX, at.getX());
            minZ = Math.min(minZ, at.getZ());
            maxX = Math.max(maxX, at.getX());
            maxZ = Math.max(maxZ, at.getZ());
        }
        return places.isEmpty() || Loaded.area(level, minX - 1, minZ - 1, maxX + 1, maxZ + 1);
    }
}
