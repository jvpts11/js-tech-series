/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

import dev.jstech.core.JsCore;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * A player tore a page out of a manual open on their screen: the server gives them a sheet with a copy of it.
 *
 * @param sheet which page, of which manual
 */
public record TearPayload(TornSheets.Sheet sheet) implements CustomPacketPayload {

    public static final Type<TearPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JsCore.MODID,
            "tear_sheet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TearPayload> STREAM_CODEC =
            TornSheets.Sheet.STREAM_CODEC.map(TearPayload::new, TearPayload::sheet);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
