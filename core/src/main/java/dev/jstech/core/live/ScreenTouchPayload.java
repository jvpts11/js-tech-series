/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.live;

import dev.jstech.core.JsCore;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * A press on a screen in the world, from the player's game: the block, the face and the pixel.
 *
 * @param pos  the block
 * @param face the face the screen is on
 * @param x    the pixel across
 * @param y    the pixel down
 */
public record ScreenTouchPayload(BlockPos pos, Direction face, int x, int y) implements CustomPacketPayload {

    public static final Type<ScreenTouchPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JsCore.MODID,
            "screen_touch"));
    public static final StreamCodec<ByteBuf, ScreenTouchPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ScreenTouchPayload::pos,
            Direction.STREAM_CODEC, ScreenTouchPayload::face,
            ByteBufCodecs.VAR_INT, ScreenTouchPayload::x,
            ByteBufCodecs.VAR_INT, ScreenTouchPayload::y,
            ScreenTouchPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
