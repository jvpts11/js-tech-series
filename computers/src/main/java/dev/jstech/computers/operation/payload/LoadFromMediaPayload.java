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
 * Client to server: load one or more {@code .craft} files from a removable medium into a Crafting Computer: a bench
 * recipe into a card's ROM, a processing or pipeline recipe into a Crafting Interface it drives.
 *
 * <p>When {@code allMissing} is true the server loads every {@code .craft} file on the medium that the computer does
 * not keep yet; {@code fileNames} is ignored in that case. Otherwise the server loads exactly the files listed.
 *
 * @param hostPos        the position of the Crafting Computer
 * @param mediaVolumeKey the {@code media:<readerPos>} key identifying the medium
 * @param fileNames      the {@code .craft} file names to load (ignored when {@code allMissing})
 * @param allMissing     when true, load all files the computer does not keep yet instead of the explicit list
 * @param place          the place to load into, as the state numbers them; -1 for the first with room
 */
public record LoadFromMediaPayload(BlockPos hostPos, String mediaVolumeKey, List<String> fileNames,
                                   boolean allMissing, int place) implements CustomPacketPayload {

    private static final int MAX_FILES = 64;

    public static final CustomPacketPayload.Type<LoadFromMediaPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "load_from_media"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LoadFromMediaPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, LoadFromMediaPayload::hostPos,
                    ByteBufCodecs.stringUtf8(64), LoadFromMediaPayload::mediaVolumeKey,
                    ByteBufCodecs.stringUtf8(FsPaths.MAX_NAME_LENGTH).apply(ByteBufCodecs.list(MAX_FILES)),
                    LoadFromMediaPayload::fileNames,
                    ByteBufCodecs.BOOL, LoadFromMediaPayload::allMissing,
                    ByteBufCodecs.VAR_INT, LoadFromMediaPayload::place,
                    LoadFromMediaPayload::new);

    /* Copied on the way in, so what arrives from a client cannot change under whoever is acting on it. */
    public LoadFromMediaPayload {
        fileNames = List.copyOf(fileNames);
    }

    @Override
    public CustomPacketPayload.Type<LoadFromMediaPayload> type() {
        return TYPE;
    }
}
