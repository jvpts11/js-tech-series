/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.tool;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A region of blocks a building tool copied, kept in the tool: the blocks it holds, each kind once, the size of the
 * region, which of them stands at each place in it, and which way the player faced copying it, so it is pasted turned
 * as the player has turned since. Only the blocks are copied, never what a block keeps inside (a chest's items, a
 * sign's words), and the air in it is left as it is where it is pasted.
 *
 * @param palette each kind of block it holds once, air first
 * @param size    the size of the region, each way
 * @param blocks  for each place in the region, x fastest then z then y, which of the palette stands there
 * @param facing  which way the player faced copying it
 */
public record CopiedRegion(List<BlockState> palette, Vec3i size, int[] blocks, Direction facing) {

    public static final Codec<CopiedRegion> CODEC = RecordCodecBuilder.create(region -> region.group(
            BlockState.CODEC.listOf().fieldOf("palette").forGetter(CopiedRegion::palette),
            Vec3i.CODEC.fieldOf("size").forGetter(CopiedRegion::size),
            Codec.INT_STREAM.xmap(stream -> stream.toArray(), Arrays::stream).fieldOf("blocks")
                    .forGetter(CopiedRegion::blocks),
            Direction.CODEC.fieldOf("facing").forGetter(CopiedRegion::facing))
            .apply(region, CopiedRegion::new));
    public static final StreamCodec<ByteBuf, CopiedRegion> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    public CopiedRegion {
        palette = List.copyOf(palette);
        Objects.requireNonNull(size, "size");
        if (!facing.getAxis().isHorizontal()) {
            throw new IllegalArgumentException("a copy faces a way across the ground, not " + facing);
        }
        if (palette.isEmpty() || !palette.get(0).isAir()) {
            throw new IllegalArgumentException("a copy's palette starts with air");
        }
        if (blocks.length != volume(size)) {
            throw new IllegalArgumentException("a copy of " + size + " holds " + volume(size) + " places, not "
                    + blocks.length);
        }
        for (final int index : blocks) {
            if (index < 0 || index >= palette.size()) {
                throw new IllegalArgumentException("a place of a copy names no block of its palette: " + index);
            }
        }
        blocks = blocks.clone();
    }

    /** How many places a region of {@code size} has. */
    public static long volume(final Vec3i size) {
        return (long) size.getX() * size.getY() * size.getZ();
    }

    /** The block at {@code x}, {@code y}, {@code z} of the region, from its lowest corner. */
    public BlockState at(final int x, final int y, final int z) {
        return this.palette.get(this.blocks[index(x, y, z)]);
    }

    /** How many blocks it holds that are not air. */
    public int count() {
        int count = 0;
        for (final int index : this.blocks) {
            if (index != 0) {
                count++;
            }
        }
        return count;
    }

    @Override
    public int[] blocks() {
        return this.blocks.clone();
    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof CopiedRegion region && this.palette.equals(region.palette)
                && this.size.equals(region.size) && Arrays.equals(this.blocks, region.blocks)
                && this.facing == region.facing;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.palette, this.size, Arrays.hashCode(this.blocks), this.facing);
    }

    /** The place of {@code x}, {@code y}, {@code z} among the blocks. */
    public int index(final int x, final int y, final int z) {
        return (y * this.size.getZ() + z) * this.size.getX() + x;
    }

    /** Air, the first of every palette. */
    public static BlockState air() {
        return Blocks.AIR.defaultBlockState();
    }
}
