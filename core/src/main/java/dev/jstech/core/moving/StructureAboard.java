/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import dev.jstech.core.moving.physics.BlockPhysics;
import dev.jstech.core.region.Box;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

/**
 * The blocks of a moving structure that do something aboard ({@link BlockAboard}): its seats and helms, its docking
 * clamps, its decouplers and its tools, found once as it loads and kept as its blocks change. Each tick a decoupler
 * whose signal comes on fires, and the structure parts there; a clamp whose signal is on holds the clamp of another
 * structure met face to face, and lets it go when its signal goes off; a tool works the world as the structure passes
 * ({@link StructureTools}).
 */
public final class StructureAboard {

    private final Map<BlockPos, BlockAboard> blocks = new HashMap<>();
    /* The last signal seen at each clamp and decoupler, for when it comes on and goes off. */
    private final Map<BlockPos, Integer> signals = new HashMap<>();
    /* The pushes of the decouplers fired, for the pieces they part when those go. */
    private final List<Push> pushes = new ArrayList<>();
    private final StructureTools tools = new StructureTools();
    private boolean filled;

    /* How often a clamp whose signal is on looks for a clamp to hold, in ticks. */
    private static final int CLAMP_LOOK = 5;
    private static final int FULL_SIGNAL = 15;

    /** Its blocks that do something aboard, by their place in the yard. */
    public Map<BlockPos, BlockAboard> blocks() {
        return Collections.unmodifiableMap(blocks);
    }

    /* Finds its blocks that do something aboard, every one of {@code bounds}, a section only where one may be. */
    void fill(final ServerLevel level, final Box bounds) {
        blocks.clear();
        final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int chunkX = bounds.minX() >> 4; chunkX <= bounds.maxX() >> 4; chunkX++) {
            for (int chunkZ = bounds.minZ() >> 4; chunkZ <= bounds.maxZ() >> 4; chunkZ++) {
                final LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) {
                    continue;
                }
                for (int sectionY = bounds.minY() >> 4; sectionY <= bounds.maxY() >> 4; sectionY++) {
                    final int index = chunk.getSectionIndexFromSectionY(sectionY);
                    if (index < 0 || index >= chunk.getSectionsCount()) {
                        continue;
                    }
                    final LevelChunkSection section = chunk.getSection(index);
                    if (section.hasOnlyAir() || !section.maybeHas(state -> BlockAboard.of(state).any())) {
                        continue;
                    }
                    for (int x = 0; x < 16; x++) {
                        for (int y = 0; y < 16; y++) {
                            for (int z = 0; z < 16; z++) {
                                final BlockState state = section.getBlockState(x, y, z);
                                final BlockAboard aboard = BlockAboard.of(state);
                                if (aboard.any()) {
                                    at.set((chunkX << 4) + x, (sectionY << 4) + y, (chunkZ << 4) + z);
                                    blocks.put(at.immutable(), aboard);
                                }
                            }
                        }
                    }
                }
            }
        }
        filled = true;
    }

    /* The block at {@code pos} became {@code after}. */
    void changed(final BlockPos pos, final BlockState after) {
        if (!filled) {
            return;
        }
        final BlockAboard aboard = BlockAboard.of(after);
        if (aboard.any()) {
            blocks.put(pos.immutable(), aboard);
        } else {
            blocks.remove(pos);
            signals.remove(pos);
        }
    }

    /* One tick: its decouplers, its clamps and its tools. */
    void tick(final ServerLevel level, final MovingStructure structure) {
        if (!filled) {
            fill(level, structure.bounds());
        }
        if (blocks.isEmpty()) {
            return;
        }
        final List<BlockPos> fired = new ArrayList<>();
        for (final Map.Entry<BlockPos, BlockAboard> each : List.copyOf(blocks.entrySet())) {
            final BlockPos pos = each.getKey();
            final BlockAboard aboard = each.getValue();
            final BlockState state = level.getBlockState(pos);
            if (aboard.decoupler().isPresent()) {
                if (cameOn(level, pos, state, aboard.decoupler().get().signal())) {
                    fired.add(pos);
                }
            }
            if (aboard.clamp().isPresent()) {
                clamp(level, structure, pos, state, aboard.clamp().get());
            }
            if (aboard.tool().isPresent()) {
                tools.work(level, structure, pos, state, aboard.tool().get());
            }
        }
        for (final BlockPos pos : fired) {
            fire(level, structure, pos);
        }
    }

    /* The pushes of the decouplers fired, still to push the pieces they part. */
    List<Push> pushes() {
        return pushes;
    }

    /* Forgets the pushes, every piece they parted gone. */
    void clearPushes() {
        pushes.clear();
    }

    /* A decoupler fires: it is gone, the structure looks for its pieces at once, and they are pushed apart. */
    private void fire(final ServerLevel level, final MovingStructure structure, final BlockPos pos) {
        final BlockState state = level.getBlockState(pos);
        final BlockAboard aboard = BlockAboard.of(state);
        if (aboard.decoupler().isEmpty()) {
            return;
        }
        pushes.add(new Push(pos.immutable(), BlockPhysics.wayOf(state), aboard.decoupler().get().push()));
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        structure.lookForPieces(level);
    }

    /*
     * A clamp: with its signal on, it looks now and then for another structure's clamp met face to face, and holds it;
     * as its signal goes off, it lets go what it holds.
     */
    private void clamp(final ServerLevel level, final MovingStructure structure, final BlockPos pos,
                       final BlockState state, final BlockAboard.Clamp clamp) {
        // An autopilot docking closes the clamps as a signal would.
        final boolean docking = StructureMounts.rootOf(structure).controls().docking(level.getGameTime());
        final int signal = docking ? FULL_SIGNAL
                : BlockPhysics.signal(level, pos, BlockPhysics.wayOf(state), clamp.signal());
        final int before = signals.getOrDefault(pos, 0);
        signals.put(pos.immutable(), signal);
        if (signal > 0 && (before == 0 || level.getGameTime() % CLAMP_LOOK == 0)) {
            StructureDocking.closeClamp(level, structure, pos, state, clamp);
        } else if (signal == 0 && before > 0) {
            StructureDocking.openClamp(structure, pos, state);
        }
    }

    /* Whether the signal at {@code pos}, from {@code side}, came on this tick. */
    private boolean cameOn(final ServerLevel level, final BlockPos pos, final BlockState state,
                           final BlockPhysics.SignalSide side) {
        final int signal = BlockPhysics.signal(level, pos, BlockPhysics.wayOf(state), side);
        final int before = signals.getOrDefault(pos, 0);
        signals.put(pos.immutable(), signal);
        return signal > 0 && before == 0;
    }

    /**
     * A decoupler fired: where it was, the way it looked, and how hard it pushes the side it looked to and the side
     * behind it apart.
     *
     * @param at   where it was, in the yard
     * @param way  the way it looked
     * @param push its push, in newton seconds
     */
    record Push(BlockPos at, Direction way, double push) {
    }
}
