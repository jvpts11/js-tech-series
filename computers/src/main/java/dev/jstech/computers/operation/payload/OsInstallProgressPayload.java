/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client: a system is being copied onto a disk on this machine, and this much of it is left.
 *
 * <p>Sent when the copy starts and again whenever a monitor is opened on a machine in the middle of one, so a
 * player who walked away and came back sees how far it has got rather than a machine that looks idle. The copy
 * itself belongs to the machine: this only says where it is.
 *
 * @param targetLabel the disk it is going onto, read in the player's language
 */
public record OsInstallProgressPayload(BlockPos hostPos, BlockPos monitorPos, int firmwareKind, String osName,
                                       Text targetLabel, int ticksLeft,
                                       int ticksTotal) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OsInstallProgressPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "os_install_progress"));

    // Written by hand: composite() tops out at six pairs and this carries seven.
    public static final StreamCodec<RegistryFriendlyByteBuf, OsInstallProgressPayload> STREAM_CODEC =
            StreamCodec.of((buf, p) -> {
                buf.writeBlockPos(p.hostPos());
                buf.writeBlockPos(p.monitorPos());
                buf.writeVarInt(p.firmwareKind());
                buf.writeUtf(p.osName(), 64);
                TextCodecs.STREAM_CODEC.encode(buf, p.targetLabel());
                buf.writeVarInt(p.ticksLeft());
                buf.writeVarInt(p.ticksTotal());
            }, buf -> new OsInstallProgressPayload(buf.readBlockPos(), buf.readBlockPos(), buf.readVarInt(),
                    buf.readUtf(64), TextCodecs.STREAM_CODEC.decode(buf), buf.readVarInt(), buf.readVarInt()));

    @Override
    public CustomPacketPayload.Type<OsInstallProgressPayload> type() {
        return TYPE;
    }
}
