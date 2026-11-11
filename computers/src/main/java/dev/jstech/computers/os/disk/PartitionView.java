/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.disk;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The mark a partition's view carries (see {@link DiskPartitions#view}): which partition of its disk it is, how big,
 * and its file system, so what reads the view as a disk reads the partition's room and format rather than the disk's.
 * A view is made for one operation and written back; it is never put anywhere a player can take it.
 *
 * @param number the partition's number
 * @param sizeMb its size, in megabytes
 * @param fs     its file system's name
 */
public record PartitionView(int number, long sizeMb, String fs) {

    public static final Codec<PartitionView> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.INT.fieldOf("number").forGetter(PartitionView::number),
            Codec.LONG.fieldOf("size").forGetter(PartitionView::sizeMb),
            Codec.STRING.fieldOf("fs").forGetter(PartitionView::fs)
    ).apply(inst, PartitionView::new));

    public static final StreamCodec<ByteBuf, PartitionView> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PartitionView::number,
            ByteBufCodecs.VAR_LONG, PartitionView::sizeMb,
            ByteBufCodecs.STRING_UTF8, PartitionView::fs,
            PartitionView::new);
}
