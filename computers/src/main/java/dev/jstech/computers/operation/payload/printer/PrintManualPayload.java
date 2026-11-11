/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.printer;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * A computer's help asks for the book it shows to be printed on the machine's Large Scale Printer.
 *
 * @param hostPos the computer
 * @param manual  the manual shown, by its id
 */
public record PrintManualPayload(BlockPos hostPos, String manual) implements CustomPacketPayload {

    /** The longest manual id that travels. */
    public static final int MAX_MANUAL = 64;

    public static final CustomPacketPayload.Type<PrintManualPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "print_manual"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PrintManualPayload> STREAM_CODEC =
            StreamCodec.composite(BlockPos.STREAM_CODEC, PrintManualPayload::hostPos,
                    ByteBufCodecs.stringUtf8(MAX_MANUAL), PrintManualPayload::manual, PrintManualPayload::new);

    @Override
    public CustomPacketPayload.Type<PrintManualPayload> type() {
        return TYPE;
    }
}
