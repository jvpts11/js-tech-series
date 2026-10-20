/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Arrays;
import java.util.stream.LongStream;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/*
 * The blocks a structure is made of that still stand where they were, to come into its plot once the plot's chunks
 * have loaded: where each stands, packed, and how far east and south it goes to its place in the plot. Nothing reads
 * or writes a chunk that is not loaded, so a structure made where the yard's chunks are not loaded yet waits for them
 * a few ticks, its blocks standing in the world as they stood; it is saved so, too.
 */
final class StructureArrival {

    private final long[] places;
    private final int dx;
    private final int dz;
    private final int fromChunkX;
    private final int fromChunkZ;
    private final int toChunkX;
    private final int toChunkZ;

    static final Codec<StructureArrival> CODEC = RecordCodecBuilder.create(arrival -> arrival.group(
            Codec.LONG_STREAM.xmap(LongStream::toArray, Arrays::stream).fieldOf("places")
                    .forGetter(StructureArrival::places),
            Codec.INT.fieldOf("dx").forGetter(StructureArrival::dx),
            Codec.INT.fieldOf("dz").forGetter(StructureArrival::dz)
    ).apply(arrival, StructureArrival::new));

    StructureArrival(final long[] places, final int dx, final int dz) {
        this.places = places.clone();
        this.dx = dx;
        this.dz = dz;
        int lowX = Integer.MAX_VALUE;
        int lowZ = Integer.MAX_VALUE;
        int highX = Integer.MIN_VALUE;
        int highZ = Integer.MIN_VALUE;
        // A block lifted reads the blocks beside it, so the chunks a block beyond them reaches are read too.
        for (final long place : this.places) {
            lowX = Math.min(lowX, (BlockPos.getX(place) - 1) >> 4);
            lowZ = Math.min(lowZ, (BlockPos.getZ(place) - 1) >> 4);
            highX = Math.max(highX, (BlockPos.getX(place) + 1) >> 4);
            highZ = Math.max(highZ, (BlockPos.getZ(place) + 1) >> 4);
        }
        this.fromChunkX = lowX;
        this.fromChunkZ = lowZ;
        this.toChunkX = highX;
        this.toChunkZ = highZ;
    }

    long[] places() {
        return places.clone();
    }

    int dx() {
        return dx;
    }

    int dz() {
        return dz;
    }

    /* Whether every chunk the blocks still stand in is loaded, and those beside them, so they can be lifted. */
    boolean standsLoaded(final ServerLevel level) {
        for (int x = fromChunkX; x <= toChunkX; x++) {
            for (int z = fromChunkZ; z <= toChunkZ; z++) {
                if (level.getChunkSource().getChunkNow(x, z) == null) {
                    return false;
                }
            }
        }
        return true;
    }
}
