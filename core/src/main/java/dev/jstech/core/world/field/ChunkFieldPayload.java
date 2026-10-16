/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.world.field;

import dev.jstech.core.JsCore;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * A field's values round a player, sent as they change: the chunks {@code radius} out each way from the centre, row by
 * row along x.
 *
 * @param field   the field
 * @param centreX the chunk in the middle
 * @param centreZ and along
 * @param radius  how many chunks out each way
 * @param amounts the values, {@code (2 * radius + 1)} squared of them
 */
public record ChunkFieldPayload(ResourceLocation field, int centreX, int centreZ, int radius, float[] amounts)
        implements CustomPacketPayload {

    public static final Type<ChunkFieldPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JsCore.MODID,
            "chunk_field"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ChunkFieldPayload> STREAM_CODEC = StreamCodec.of(
            ChunkFieldPayload::write, ChunkFieldPayload::read);

    /* The widest view a player is sent, so a payload is never larger than it may be. */
    private static final int WIDEST = 32;

    public ChunkFieldPayload {
        amounts = amounts.clone();
    }

    @Override
    public Type<ChunkFieldPayload> type() {
        return TYPE;
    }

    /** The values, a copy. */
    @Override
    public float[] amounts() {
        return this.amounts.clone();
    }

    /** The value at {@code dx, dz} chunks from the centre; 0 beyond the radius. */
    public float at(final int dx, final int dz) {
        if (Math.abs(dx) > this.radius || Math.abs(dz) > this.radius) {
            return 0;
        }
        return this.amounts[(dz + this.radius) * (this.radius * 2 + 1) + dx + this.radius];
    }

    private static void write(final RegistryFriendlyByteBuf buffer, final ChunkFieldPayload payload) {
        ResourceLocation.STREAM_CODEC.encode(buffer, payload.field);
        buffer.writeVarInt(payload.centreX);
        buffer.writeVarInt(payload.centreZ);
        buffer.writeVarInt(payload.radius);
        for (final float value : payload.amounts) {
            buffer.writeFloat(value);
        }
    }

    private static ChunkFieldPayload read(final RegistryFriendlyByteBuf buffer) {
        final ResourceLocation field = ResourceLocation.STREAM_CODEC.decode(buffer);
        final int x = buffer.readVarInt();
        final int z = buffer.readVarInt();
        final int radius = Math.clamp(buffer.readVarInt(), 0, WIDEST);
        final int side = radius * 2 + 1;
        final float[] amounts = new float[side * side];
        for (int i = 0; i < amounts.length; i++) {
            amounts[i] = buffer.readFloat();
        }
        return new ChunkFieldPayload(field, x, z, radius, amounts);
    }
}
