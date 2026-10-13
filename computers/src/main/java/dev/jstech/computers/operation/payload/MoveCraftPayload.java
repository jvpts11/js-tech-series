/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client to server: the Crafting Manager moves a recipe the Crafting Computer keeps to another place, an interface's
 * pattern to another interface, say.
 *
 * @param hostPos the position of the Crafting Computer
 * @param ref     the recipe, as the state names it
 * @param place   the place it goes to, as the state numbers it
 */
public record MoveCraftPayload(BlockPos hostPos, int ref, int place) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<MoveCraftPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "move_craft"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MoveCraftPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, MoveCraftPayload::hostPos,
                    ByteBufCodecs.VAR_INT, MoveCraftPayload::ref,
                    ByteBufCodecs.VAR_INT, MoveCraftPayload::place,
                    MoveCraftPayload::new);

    @Override
    public CustomPacketPayload.Type<MoveCraftPayload> type() {
        return TYPE;
    }
}
