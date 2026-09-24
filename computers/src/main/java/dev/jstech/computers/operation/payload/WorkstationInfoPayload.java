/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import dev.jstech.computers.os.WorkstationFacts;
import dev.jstech.core.text.TextCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server to client: what a workstation is, for CDE's Workstation Info, read off the machine as it stands. */
public record WorkstationInfoPayload(BlockPos hostPos, WorkstationFacts facts) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<WorkstationInfoPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "workstation_info"));

    /*
     * Written out by hand: the facts are thirteen things, more than a composite takes. Every text is cut to its
     * cap rather than refused, since a cap on writeUtf drops the connection and these names are the player's.
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, WorkstationInfoPayload> STREAM_CODEC =
            StreamCodec.of(WorkstationInfoPayload::encode, WorkstationInfoPayload::decode);

    /** The longest any one fact may be; a network's id is thirty-six letters and a system's name fewer. */
    private static final int MAX_TEXT = 64;

    @Override
    public CustomPacketPayload.Type<WorkstationInfoPayload> type() {
        return TYPE;
    }

    private static void encode(final RegistryFriendlyByteBuf buf, final WorkstationInfoPayload p) {
        final WorkstationFacts f = p.facts();
        buf.writeBlockPos(p.hostPos());
        for (final String text : new String[] {f.userName(), f.hostName(), f.network(), f.system(),
            f.architecture(), f.windowSystem()}) {
            buf.writeUtf(text.length() <= MAX_TEXT ? text : text.substring(0, MAX_TEXT), MAX_TEXT);
        }
        TextCodecs.STREAM_CODEC.encode(buf, f.processor());
        buf.writeVarInt(f.processorMhz());
        buf.writeVarLong(f.memoryMb());
        buf.writeVarLong(f.memoryUsedMb());
        buf.writeVarLong(f.videoMb());
        buf.writeVarLong(f.diskMb());
        buf.writeVarLong(f.diskUsedMb());
    }

    private static WorkstationInfoPayload decode(final RegistryFriendlyByteBuf buf) {
        final BlockPos host = buf.readBlockPos();
        return new WorkstationInfoPayload(host, new WorkstationFacts(buf.readUtf(MAX_TEXT), buf.readUtf(MAX_TEXT),
                buf.readUtf(MAX_TEXT), buf.readUtf(MAX_TEXT), buf.readUtf(MAX_TEXT), buf.readUtf(MAX_TEXT),
                TextCodecs.STREAM_CODEC.decode(buf), buf.readVarInt(), buf.readVarLong(), buf.readVarLong(),
                buf.readVarLong(), buf.readVarLong(), buf.readVarLong()));
    }
}
