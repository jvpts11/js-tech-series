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
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client: show the power-on self-test on this monitor, with the ticks it still has to run.
 *
 * <p>The machine keeps the time, not the screen, so a monitor opened halfway through a self-test is told
 * what is left of it rather than starting one over. The client plays the era-styled POST (memory count,
 * detected drives, "press DEL for setup") and answers with {@link PostCompletePayload} only when the player
 * asks for the firmware setup; the machine finishes the self-test itself and boots whoever is watching.
 */
public record OpenPostPayload(BlockPos host, BlockPos monitorPos, int firmwareKind, String name,
                              int remainingTicks, boolean halted, String complaint)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenPostPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "open_post"));

    /** How long a complaint may be: a line of a terminal, which is all one has ever been. */
    private static final int MOST_LETTERS = 80;

    /*
     * Written out by hand rather than composed: a composed codec takes six parts and this has seven, and the
     * seventh is the one that tells a player why their machine will not start.
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenPostPayload> STREAM_CODEC =
            StreamCodec.of(OpenPostPayload::encode, OpenPostPayload::decode);

    private static void encode(final RegistryFriendlyByteBuf buf, final OpenPostPayload payload) {
        buf.writeBlockPos(payload.host);
        buf.writeBlockPos(payload.monitorPos);
        buf.writeVarInt(payload.firmwareKind);
        buf.writeUtf(payload.name);
        buf.writeVarInt(payload.remainingTicks);
        buf.writeBoolean(payload.halted);
        buf.writeUtf(payload.complaint.length() > MOST_LETTERS
                ? payload.complaint.substring(0, MOST_LETTERS) : payload.complaint, MOST_LETTERS);
    }

    private static OpenPostPayload decode(final RegistryFriendlyByteBuf buf) {
        return new OpenPostPayload(buf.readBlockPos(), buf.readBlockPos(), buf.readVarInt(), buf.readUtf(),
                buf.readVarInt(), buf.readBoolean(), buf.readUtf(MOST_LETTERS));
    }

    @Override
    public CustomPacketPayload.Type<OpenPostPayload> type() {
        return TYPE;
    }
}
