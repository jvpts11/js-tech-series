/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.crash;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client to server: a key was pressed on the console of that machine's crashed system, which a kernel that offers to
 * stop its automatic reboot takes as the order to stop it, and once stopped as the order to reboot.
 */
public record CrashKeyPayload(BlockPos host) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<CrashKeyPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "crash_key"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CrashKeyPayload> STREAM_CODEC =
            StreamCodec.of((buf, p) -> buf.writeBlockPos(p.host()), buf -> new CrashKeyPayload(buf.readBlockPos()));

    @Override
    public CustomPacketPayload.Type<CrashKeyPayload> type() {
        return TYPE;
    }
}
