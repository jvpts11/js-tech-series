/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client: what an open Crafting Interface window shows, sent when it opens and whenever it changes.
 *
 * @param containerId the window it is for
 * @param view        what it shows
 */
public record CraftingInterfaceStatePayload(int containerId, InterfaceView view) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<CraftingInterfaceStatePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "crafting_interface_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CraftingInterfaceStatePayload> STREAM_CODEC =
            StreamCodec.of((buf, payload) -> {
                buf.writeVarInt(payload.containerId());
                InterfaceView.write(buf, payload.view());
            }, buf -> new CraftingInterfaceStatePayload(buf.readVarInt(), InterfaceView.read(buf)));

    @Override
    public CustomPacketPayload.Type<CraftingInterfaceStatePayload> type() {
        return TYPE;
    }
}
