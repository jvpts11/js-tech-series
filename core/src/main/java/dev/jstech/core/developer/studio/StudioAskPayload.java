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
 * What one of the studio's tools asks the server: a service by its name, with what it asks for.
 *
 * @param id      the question's number, which the answer carries back
 * @param service the service asked
 * @param args    what it asks for
 */
public record StudioAskPayload(int id, String service, CompoundTag args) implements CustomPacketPayload {

    /** The longest a service's name is carried. */
    public static final int LONGEST_NAME = 64;

    public static final CustomPacketPayload.Type<StudioAskPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "studio_ask"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StudioAskPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, StudioAskPayload::id,
            ByteBufCodecs.stringUtf8(LONGEST_NAME), StudioAskPayload::service,
            ByteBufCodecs.COMPOUND_TAG, StudioAskPayload::args,
            StudioAskPayload::new);

    @Override
    public CustomPacketPayload.Type<StudioAskPayload> type() {
        return TYPE;
    }
}
