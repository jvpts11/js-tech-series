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
 * Client to server: eject the medium in the drive at {@code readerPos}, or the disk in one tray of a Dock Station
 * there, which must be linked to the computer at {@code hostPos}. What comes out goes to the player's inventory, or
 * onto the drive when the inventory is full, the same as sneak-clicking the drive in the world.
 *
 * @param hostPos   the computer
 * @param readerPos the drive's packed position
 * @param bay       the dock's tray, or {@link MediaVolume#MEDIUM} for the drive's own medium
 */
public record EjectMediaPayload(BlockPos hostPos, long readerPos, int bay) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<EjectMediaPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "eject_media"));

    public static final StreamCodec<RegistryFriendlyByteBuf, EjectMediaPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, EjectMediaPayload::hostPos,
                    ByteBufCodecs.VAR_LONG, EjectMediaPayload::readerPos,
                    ByteBufCodecs.INT, EjectMediaPayload::bay,
                    EjectMediaPayload::new);

    /** Ejects the drive's own medium. */
    public EjectMediaPayload(final BlockPos hostPos, final long readerPos) {
        this(hostPos, readerPos, MediaVolume.MEDIUM);
    }

    /** Ejects what a volume's key names: a drive's medium or a docked disk. */
    public static EjectMediaPayload of(final BlockPos hostPos, final MediaVolume volume) {
        return new EjectMediaPayload(hostPos, volume.readerPos(), volume.bay());
    }

    @Override
    public CustomPacketPayload.Type<EjectMediaPayload> type() {
        return TYPE;
    }
}
