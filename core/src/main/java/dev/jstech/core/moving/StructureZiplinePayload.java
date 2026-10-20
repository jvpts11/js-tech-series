/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import dev.jstech.core.JsCore;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client to server: the player takes hold of a rope, to ride it down as a zipline, at a share of it from its first
 * end; the server holds them there if the rope is within their reach.
 *
 * @param joint the rope's joint
 * @param along where along it, from 0 at its first end to 1 at its second
 */
public record StructureZiplinePayload(UUID joint, float along) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<StructureZiplinePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "structure_zipline"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StructureZiplinePayload> STREAM_CODEC =
            StreamCodec.composite(
                    UUIDUtil.STREAM_CODEC, StructureZiplinePayload::joint,
                    ByteBufCodecs.FLOAT, StructureZiplinePayload::along,
                    StructureZiplinePayload::new);

    @Override
    public CustomPacketPayload.Type<StructureZiplinePayload> type() {
        return TYPE;
    }
}
