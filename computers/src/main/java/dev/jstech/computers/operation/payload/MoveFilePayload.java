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
 * Client to server: drag-and-drop or a cut and paste is moving the file or directory {@code srcPath} into the
 * directory {@code destDir} on the system disk of the computer at {@code hostPos}. The item keeps its name; a
 * directory move re-keys everything nested under it. {@code .dat} projections cannot be moved.
 *
 * @param replace whether a file of the same name already in {@code destDir} gives way to the one moved, which the
 *                player chose when asked; otherwise a name already taken there leaves the file where it was
 */
public record MoveFilePayload(BlockPos hostPos, String srcPath, String destDir, boolean replace)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<MoveFilePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "move_file"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MoveFilePayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, MoveFilePayload::hostPos,
                    PayloadCodecs.PATH, MoveFilePayload::srcPath,
                    PayloadCodecs.PATH, MoveFilePayload::destDir,
                    ByteBufCodecs.BOOL, MoveFilePayload::replace,
                    MoveFilePayload::new);

    /** A move that never takes the place of a file already there. */
    public MoveFilePayload(final BlockPos hostPos, final String srcPath, final String destDir) {
        this(hostPos, srcPath, destDir, false);
    }

    @Override
    public CustomPacketPayload.Type<MoveFilePayload> type() {
        return TYPE;
    }
}
