/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import dev.jstech.core.util.Loaded;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

/**
 * Moving real blocks together, whole: their states, their block entities with all they keep, turned as asked, from
 * one level to another or within one, as a structure is made from the world, set down on it, or taken to another
 * dimension. The blocks are lifted and laid down quietly, telling no neighbour and dropping nothing on the way, so a
 * torch on a wall moved with the wall does not fall off; the world's blocks round them are told once at the end.
 * Each block entity that keeps other blocks' places is told where they went ({@link IMovesWithStructures}); any other
 * has the places it saved follow the blocks they name ({@link StructurePlaces}); and every mod is told
 * ({@link StructureBlocksMovedEvent}).
 */
final class BlockMover {

    /* Lifting and laying blocks so: no neighbour told, no shape changed, nothing dropped, moved as a piston moves. */
    private static final int QUIETLY = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS
            | Block.UPDATE_MOVE_BY_PISTON;

    private BlockMover() {
    }

    /**
     * Moves each block of {@code moves}, by where it is in {@code from}, to where it goes in {@code to}, turned by
     * {@code rotation}. {@code fromWorld} and {@code toWorld} say which side is the world's land, whose neighbours
     * are told: the yard's blocks round a structure need no telling.
     */
    static BlockMove move(final ServerLevel from, final ServerLevel to, final Map<BlockPos, BlockPos> moves,
                          final Rotation rotation, final boolean fromWorld, final boolean toWorld) {
        final List<Lifted> lifted = new ArrayList<>(moves.size());
        for (final Map.Entry<BlockPos, BlockPos> each : moves.entrySet()) {
            final BlockPos at = each.getKey();
            final BlockState state = from.getBlockState(at);
            final BlockEntity entity = from.getBlockEntity(at);
            final CompoundTag kept = entity == null ? null : entity.saveWithFullMetadata(from.registryAccess());
            lifted.add(new Lifted(at.immutable(), each.getValue().immutable(), state, kept));
        }
        for (final Lifted block : lifted) {
            // Taken from its chunk, not through the level, which would read the comparators round it at once.
            if (block.kept != null) {
                from.getChunkAt(block.from).removeBlockEntity(block.from);
            }
            from.setBlock(block.from, Blocks.AIR.defaultBlockState(), QUIETLY);
        }
        final Map<BlockPos, BlockPos> moved = new LinkedHashMap<>();
        for (final Lifted block : lifted) {
            final BlockState turned = rotation == Rotation.NONE ? block.state : block.state.rotate(to, block.to,
                    rotation);
            to.setBlock(block.to, turned, QUIETLY);
            moved.put(block.from, block.to);
        }
        final BlockMove move = new BlockMove(from, to, moved, rotation);
        for (final Lifted block : lifted) {
            if (block.kept == null) {
                continue;
            }
            BlockEntity there = to.getBlockEntity(block.to);
            if (there != null) {
                there.loadWithComponents(block.kept, to.registryAccess());
                there.setChanged();
            } else {
                there = BlockEntity.loadStatic(block.to, to.getBlockState(block.to), block.kept,
                        to.registryAccess());
                if (there != null) {
                    to.setBlockEntity(there);
                }
            }
            // One that does not look after its places itself has those it saved follow the blocks they name.
            if (there != null && !(there instanceof IMovesWithStructures)) {
                final CompoundTag fixed = StructurePlaces.moved(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(
                        there.getType()), block.kept, move);
                if (fixed != null) {
                    there.loadWithComponents(fixed, to.registryAccess());
                    there.setChanged();
                }
            }
        }
        for (final Lifted block : lifted) {
            if (to.getBlockEntity(block.to) instanceof IMovesWithStructures told) {
                told.moved(block.from, move);
            }
        }
        NeoForge.EVENT_BUS.post(new StructureBlocksMovedEvent(move));
        if (fromWorld) {
            tellNeighbours(from, lifted, true);
        }
        if (toWorld) {
            tellNeighbours(to, lifted, false);
        }
        return move;
    }

    /*
     * Tells the world's blocks round the moved ones that they changed, each neighbour not moved itself, and lets a
     * block laid down on the world join to what is round it there, as a fence joins the fence beside it. A neighbour
     * in a chunk that is not loaded is not told: it would be loaded to be, and it hears of it as it loads.
     */
    private static void tellNeighbours(final ServerLevel level, final List<Lifted> lifted, final boolean left) {
        final Set<BlockPos> places = new HashSet<>();
        for (final Lifted block : lifted) {
            places.add(left ? block.from : block.to);
        }
        for (final BlockPos at : places) {
            BlockState state = level.getBlockState(at);
            if (!left && roundLoaded(level, at)) {
                final BlockState joined = Block.updateFromNeighbourShapes(state, level, at);
                if (joined != state) {
                    level.setBlock(at, joined, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                    state = joined;
                }
            }
            for (final Direction side : Direction.values()) {
                final BlockPos next = at.relative(side);
                if (places.contains(next) || !Loaded.at(level, next)) {
                    continue;
                }
                level.neighborShapeChanged(side.getOpposite(), state, next, at, Block.UPDATE_ALL, 512);
                level.neighborChanged(next, state.getBlock(), at);
            }
        }
    }

    /* Whether every block beside {@code at} is in a loaded chunk. */
    private static boolean roundLoaded(final ServerLevel level, final BlockPos at) {
        for (final Direction side : Direction.values()) {
            if (!Loaded.at(level, at.relative(side))) {
                return false;
            }
        }
        return true;
    }

    /* A block lifted: where it was, where it goes, its state, and what its block entity kept. */
    private record Lifted(BlockPos from, BlockPos to, BlockState state, @Nullable CompoundTag kept) {
    }
}
