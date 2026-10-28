/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.network;

import dev.jstech.space.JsSpace;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * One ring of the far map of the ground round a player on the way up, as the world's own generator makes it: what the
 * player sees beyond the land that is loaded, out towards the horizon, each pixel's biome and the height of its ground.
 * The client paints it in the blocks' own colours.
 *
 * @param ring     which ring it is, the finest first
 * @param centreX  the block the map's middle stands over, east
 * @param centreZ  and south
 * @param blocks   how many blocks wide each pixel is
 * @param size     how many pixels it is each way
 * @param sea      the height of the world's sea, which only the server knows
 * @param biomes   its biomes
 * @param pixels   each pixel's biome in {@code biomes}, row by row from the north-west
 * @param heights  each pixel's ground, in {@link #HEIGHT_STEP}s of blocks over {@link #LOWEST}
 */
public record FarMapPayload(int ring, int centreX, int centreZ, int blocks, int size, int sea,
                            List<ResourceLocation> biomes, byte[] pixels, byte[] heights)
        implements CustomPacketPayload {

    public static final Type<FarMapPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "far_map"));
    /** The lowest ground a pixel can hold, and the blocks each step of its height is. */
    public static final int LOWEST = -64;
    public static final int HEIGHT_STEP = 2;
    /* The most biomes a ring holds. */
    private static final int MAX_BIOMES = 256;
    public static final StreamCodec<RegistryFriendlyByteBuf, FarMapPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, map) -> map.write(buffer), FarMapPayload::read);

    public FarMapPayload {
        biomes = List.copyOf(biomes);
        pixels = pixels.clone();
        heights = heights.clone();
    }

    /** The height of a ground, in blocks, as a pixel holds it. */
    public static byte height(final double blocks) {
        return (byte) Math.max(0, Math.min(255, Math.round((blocks - LOWEST) / HEIGHT_STEP)));
    }

    /** The height a pixel holds, in blocks. */
    public static int blocks(final byte height) {
        return (height & 0xFF) * HEIGHT_STEP + LOWEST;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static FarMapPayload read(final RegistryFriendlyByteBuf buffer) {
        return new FarMapPayload(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                buffer.readVarInt(), buffer.readVarInt(),
                ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_BIOMES)).decode(buffer),
                buffer.readByteArray(), buffer.readByteArray());
    }

    private void write(final RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(this.ring);
        buffer.writeVarInt(this.centreX);
        buffer.writeVarInt(this.centreZ);
        buffer.writeVarInt(this.blocks);
        buffer.writeVarInt(this.size);
        buffer.writeVarInt(this.sea);
        ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_BIOMES)).encode(buffer, this.biomes);
        buffer.writeByteArray(this.pixels);
        buffer.writeByteArray(this.heights);
    }
}
