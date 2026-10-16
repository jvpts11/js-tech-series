/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.world.edit;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * The blocks of a box of the world as they were at one moment, copied on the server's thread so work away from it can
 * read them freely: a large edit is planned from a snapshot while the world goes on. Reading outside the box, or
 * outside the world's heights, gives air.
 */
public final class RegionSnapshot {

    private final BoundingBox box;
    private final int minSection;
    private final Long2ObjectOpenHashMap<PalettedContainer<BlockState>[]> chunks;

    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    private RegionSnapshot(final BoundingBox box, final int minSection,
                           final Long2ObjectOpenHashMap<PalettedContainer<BlockState>[]> chunks) {
        this.box = box;
        this.minSection = minSection;
        this.chunks = chunks;
    }

    /** Copies the blocks of {@code box} in {@code level}, loading its chunks; on the server's thread. */
    @SuppressWarnings("unchecked")
    public static RegionSnapshot of(final ServerLevel level, final BoundingBox box) {
        final Long2ObjectOpenHashMap<PalettedContainer<BlockState>[]> chunks = new Long2ObjectOpenHashMap<>();
        for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) {
            for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) {
                final LevelChunk chunk = level.getChunk(cx, cz);
                final LevelChunkSection[] sections = chunk.getSections();
                final PalettedContainer<BlockState>[] copies = new PalettedContainer[sections.length];
                for (int i = 0; i < sections.length; i++) {
                    copies[i] = sections[i].getStates().copy();
                }
                chunks.put(ChunkPos.asLong(cx, cz), copies);
            }
        }
        return new RegionSnapshot(box, level.getMinSection(), chunks);
    }

    /** The box it holds. */
    public BoundingBox box() {
        return this.box;
    }

    /** The block at {@code x, y, z} as it was; air outside the box. Safe from any thread. */
    public BlockState get(final int x, final int y, final int z) {
        if (!this.box.isInside(x, y, z)) {
            return AIR;
        }
        final PalettedContainer<BlockState>[] sections = this.chunks.get(ChunkPos.asLong(x >> 4, z >> 4));
        final int index = (y >> 4) - this.minSection;
        if (sections == null || index < 0 || index >= sections.length) {
            return AIR;
        }
        return sections[index].get(x & 15, y & 15, z & 15);
    }
}
