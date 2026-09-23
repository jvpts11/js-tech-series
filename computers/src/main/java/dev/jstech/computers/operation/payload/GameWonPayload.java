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
 * Client to server: a game on the computer at {@code hostPos} was won, on the named board.
 *
 * <p>The desktop games run on the screen that shows them, so the win is the screen's word. It earns an advancement
 * and nothing else, which is why the word is enough.
 */
public record GameWonPayload(BlockPos hostPos, String game, String board) implements CustomPacketPayload {

    public static final int NAME_LENGTH = 32;

    public static final CustomPacketPayload.Type<GameWonPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "game_won"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GameWonPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, GameWonPayload::hostPos,
                    ByteBufCodecs.stringUtf8(NAME_LENGTH), GameWonPayload::game,
                    ByteBufCodecs.stringUtf8(NAME_LENGTH), GameWonPayload::board,
                    GameWonPayload::new);

    public GameWonPayload {
        game = PayloadText.clip(game, NAME_LENGTH);
        board = PayloadText.clip(board, NAME_LENGTH);
    }

    @Override
    public CustomPacketPayload.Type<GameWonPayload> type() {
        return TYPE;
    }
}
