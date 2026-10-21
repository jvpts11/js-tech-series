/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.item;

import dev.jstech.core.JsCore;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client to server: the mode the player chose on the ring of modes for the item in their main hand, by its id.
 *
 * @param mode the mode's id
 */
public record ItemModePayload(String mode) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ItemModePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "item_mode"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ItemModePayload> STREAM_CODEC =
            ByteBufCodecs.stringUtf8(ItemMode.LONGEST_ID).<RegistryFriendlyByteBuf>cast().map(ItemModePayload::new,
                    ItemModePayload::mode);

    @Override
    public CustomPacketPayload.Type<ItemModePayload> type() {
        return TYPE;
    }
}
