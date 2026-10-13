/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.update;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Client to server: the UPDATE the window set up, for the item as the network holds it: the action by its number,
 * how many (smelting only), which enchanting offer from zero, and the name the anvil gives (empty keeps the item's).
 */
public record UpdateSubmitPayload(BlockPos monitorPos, BlockPos hostPos, ItemStack item, int action, long quantity,
                                  int offer, String name) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<UpdateSubmitPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "update_submit"));

    // Built by hand because the record has more components than StreamCodec.composite carries.
    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateSubmitPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, p) -> {
                        BlockPos.STREAM_CODEC.encode(buf, p.monitorPos());
                        BlockPos.STREAM_CODEC.encode(buf, p.hostPos());
                        ItemStack.STREAM_CODEC.encode(buf, p.item());
                        buf.writeVarInt(p.action());
                        buf.writeVarLong(p.quantity());
                        buf.writeVarInt(p.offer());
                        ByteBufCodecs.stringUtf8(UpdatePreviewRequestPayload.MAX_NAME).encode(buf, p.name());
                    },
                    buf -> new UpdateSubmitPayload(
                            BlockPos.STREAM_CODEC.decode(buf),
                            BlockPos.STREAM_CODEC.decode(buf),
                            ItemStack.STREAM_CODEC.decode(buf),
                            buf.readVarInt(),
                            buf.readVarLong(),
                            buf.readVarInt(),
                            ByteBufCodecs.stringUtf8(UpdatePreviewRequestPayload.MAX_NAME).decode(buf)));

    public UpdateSubmitPayload {
        name = name == null ? "" : name.length() > UpdatePreviewRequestPayload.MAX_NAME
                ? name.substring(0, UpdatePreviewRequestPayload.MAX_NAME) : name;
    }

    @Override
    public CustomPacketPayload.Type<UpdateSubmitPayload> type() {
        return TYPE;
    }
}
