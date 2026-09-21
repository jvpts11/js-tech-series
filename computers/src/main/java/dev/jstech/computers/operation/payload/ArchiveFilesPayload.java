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

import java.util.List;

/**
 * Client to server: pack {@code paths} on the machine at {@code hostPos} into the archive at
 * {@code archivePath}.
 *
 * <p>The packing happens on the server because the disk's free space is the server's to judge and the
 * saving has to be real: an archive written by a client could claim any weight it liked.
 *
 * <p>With {@code removeOriginals} the files that went in are deleted once the archive is safely written,
 * which is the whole point of archiving on a disk that is short of room. It is refused rather than done
 * halfway: nothing is deleted unless the archive was written.
 */
public record ArchiveFilesPayload(BlockPos hostPos, String archivePath, List<String> paths,
                                  boolean removeOriginals) implements CustomPacketPayload {

    public ArchiveFilesPayload {
        paths = List.copyOf(paths);
    }

    /** How many files one request may name, which is what the archive format holds anyway. */
    public static final int MAX_PATHS = 512;

    public static final CustomPacketPayload.Type<ArchiveFilesPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "archive_files"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ArchiveFilesPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, ArchiveFilesPayload::hostPos,
                    ByteBufCodecs.stringUtf8(160), ArchiveFilesPayload::archivePath,
                    ByteBufCodecs.stringUtf8(160).apply(ByteBufCodecs.list(MAX_PATHS)),
                    ArchiveFilesPayload::paths,
                    ByteBufCodecs.BOOL, ArchiveFilesPayload::removeOriginals,
                    ArchiveFilesPayload::new);

    @Override
    public CustomPacketPayload.Type<ArchiveFilesPayload> type() {
        return TYPE;
    }
}
