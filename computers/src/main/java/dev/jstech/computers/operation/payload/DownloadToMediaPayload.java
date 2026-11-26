/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import dev.jstech.computers.os.fs.FsPaths;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Client to server: serialize the selected recipes as {@code .craft} files into a folder of a disk the Crafting
 * Computer reads: its own disk, or a removable medium in a linked drive.
 *
 * <p>Each ref in {@code romIndices} names a recipe as the state numbers it; one that names nothing is skipped. The
 * file name is derived from the recipe (e.g. {@code diamond_sword.craft}).
 *
 * @param hostPos        the position of the Crafting Computer
 * @param mediaVolumeKey the disk: {@link CraftManagerStatePayload#SYSTEM_DISK} or a medium's {@code media:} key
 * @param folder         the folder on it the files go into, {@code ""} for its top
 * @param romIndices     the refs of the recipes to write
 */
public record DownloadToMediaPayload(
        BlockPos hostPos,
        String mediaVolumeKey,
        String folder,
        List<Integer> romIndices) implements CustomPacketPayload {

    /** The most patterns one payload writes: more go in payloads one after another. */
    public static final int MAX_INDICES = 50;

    public static final CustomPacketPayload.Type<DownloadToMediaPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath("jsc", "download_to_media"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DownloadToMediaPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, DownloadToMediaPayload::hostPos,
                    ByteBufCodecs.stringUtf8(64), DownloadToMediaPayload::mediaVolumeKey,
                    ByteBufCodecs.stringUtf8(FsPaths.MAX_PATH_LENGTH), DownloadToMediaPayload::folder,
                    ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(MAX_INDICES)), DownloadToMediaPayload::romIndices,
                    DownloadToMediaPayload::new);

    /* Copied on the way in, so what arrives from a client cannot change under whoever is acting on it. */
    public DownloadToMediaPayload {
        romIndices = List.copyOf(romIndices);
    }

    @Override
    public CustomPacketPayload.Type<DownloadToMediaPayload> type() {
        return TYPE;
    }
}
