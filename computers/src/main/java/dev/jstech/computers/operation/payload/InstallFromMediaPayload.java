/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import dev.jstech.computers.os.media.MediaVolume;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client to server: install the program carried by the PROGRAM_INSTALL medium on the volume {@code readerPos} and
 * {@code bay} name onto the computer at {@code hostPos}. The server verifies the drive is one of the computer's,
 * reads the program id, and adds it to the computer's installed set.
 *
 * @param hostPos   the computer
 * @param readerPos the drive's packed position, or the computer's own for a drive on its front
 * @param bay       which of its volumes, as {@link MediaVolume#bay()} counts them
 */
public record InstallFromMediaPayload(BlockPos hostPos, long readerPos, int bay) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<InstallFromMediaPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "install_from_media"));

    public static final StreamCodec<RegistryFriendlyByteBuf, InstallFromMediaPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, InstallFromMediaPayload::hostPos,
                    ByteBufCodecs.VAR_LONG, InstallFromMediaPayload::readerPos,
                    ByteBufCodecs.VAR_INT, InstallFromMediaPayload::bay,
                    InstallFromMediaPayload::new);

    /** The volume the medium is on. */
    public MediaVolume volume() {
        return new MediaVolume(readerPos, bay);
    }

    @Override
    public CustomPacketPayload.Type<InstallFromMediaPayload> type() {
        return TYPE;
    }
}
