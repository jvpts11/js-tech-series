/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.overhead;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * One end of an overhead wire: the block it is hung from and which of its insulators.
 *
 * @param pos    the block
 * @param anchor the insulator's number among the block's
 */
public record WireEnd(BlockPos pos, int anchor) {

    public static final Codec<WireEnd> CODEC = RecordCodecBuilder.create(end -> end.group(
            BlockPos.CODEC.fieldOf("pos").forGetter(WireEnd::pos),
            Codec.intRange(0, Integer.MAX_VALUE).fieldOf("anchor").forGetter(WireEnd::anchor)
    ).apply(end, WireEnd::new));

    public static final StreamCodec<ByteBuf, WireEnd> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, WireEnd::pos,
            ByteBufCodecs.VAR_INT, WireEnd::anchor,
            WireEnd::new);

    public WireEnd {
        pos = pos.immutable();
        if (anchor < 0) {
            throw new IllegalArgumentException("an insulator's number counts from 0, not " + anchor);
        }
    }
}
