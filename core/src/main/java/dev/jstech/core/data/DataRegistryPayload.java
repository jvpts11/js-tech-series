/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.data;

import dev.jstech.core.JsCore;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * The values of a synced {@link DataRegistry} the server read, sent to a player's game.
 *
 * @param registry the registry's id
 * @param entries  each value by its file id, as the registry's codec writes it
 */
public record DataRegistryPayload(ResourceLocation registry, CompoundTag entries) implements CustomPacketPayload {

    public static final Type<DataRegistryPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "data_registry"));
    public static final StreamCodec<ByteBuf, DataRegistryPayload> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, DataRegistryPayload::registry,
            ByteBufCodecs.COMPOUND_TAG, DataRegistryPayload::entries,
            DataRegistryPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
