/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.item;

import dev.jstech.core.JsCore;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * The mode key was pressed: the server moves the item in the player's main hand on to its next mode, or back one.
 *
 * @param backwards whether it goes back one
 */
public record ItemModePayload(boolean backwards) implements CustomPacketPayload {

    public static final Type<ItemModePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "item_mode"));
    public static final StreamCodec<ByteBuf, ItemModePayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, ItemModePayload::backwards, ItemModePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
