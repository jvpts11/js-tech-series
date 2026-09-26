/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import dev.jstech.computers.blockentity.SpeakerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * The name typed into a speaker's screen, sent as it is typed.
 *
 * @param speakerPos the speaker being named
 * @param name       the name asked for, empty to give it back its default
 */
public record RenameSpeakerPayload(BlockPos speakerPos, String name) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RenameSpeakerPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "rename_speaker"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RenameSpeakerPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, RenameSpeakerPayload::speakerPos,
                    ByteBufCodecs.stringUtf8(SpeakerBlockEntity.MAX_NAME), RenameSpeakerPayload::name,
                    RenameSpeakerPayload::new);

    @Override
    public CustomPacketPayload.Type<RenameSpeakerPayload> type() {
        return TYPE;
    }
}
