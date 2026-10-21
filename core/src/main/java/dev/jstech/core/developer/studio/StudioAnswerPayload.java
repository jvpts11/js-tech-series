/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer.studio;

import dev.jstech.core.JsCore;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * The server's answer to a question of one of the studio's tools.
 *
 * @param id     the question's number
 * @param answer what the service answered; {@link StudioServices#REFUSED} holds why, when it was refused
 */
public record StudioAnswerPayload(int id, CompoundTag answer) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<StudioAnswerPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "studio_answer"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StudioAnswerPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, StudioAnswerPayload::id,
                    ByteBufCodecs.TRUSTED_COMPOUND_TAG, StudioAnswerPayload::answer, StudioAnswerPayload::new);

    @Override
    public CustomPacketPayload.Type<StudioAnswerPayload> type() {
        return TYPE;
    }
}
