/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.teracoin;

import dev.jstech.computers.JsComputers;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * What a Mining Panel asks of the network's pools: a look at one, or a change its owner makes from the pool's own
 * server. Every answer is the panel's whole state ({@link MiningPanelStatePayload}).
 *
 * @param hostPos the computer the panel runs on
 * @param action  what is asked, one of the constants
 * @param pool    which of the network's pools, from 0
 * @param text    the miner accepted or refused, the address the fee goes to, or the pool's new name
 * @param value   the fee, in thousandths, or 1 to take only the miners accepted
 */
public record MiningPanelRequestPayload(BlockPos hostPos, int action, int pool, String text, long value)
        implements CustomPacketPayload {

    public static final int LOOK = 0;
    public static final int SET_FEE = 1;
    public static final int SET_OWNER = 2;
    public static final int SET_ACCEPTED_ONLY = 3;
    public static final int ACCEPT = 4;
    public static final int REFUSE = 5;
    public static final int RENAME = 6;
    /** The letters of a text at most. */
    public static final int MOST_TEXT = 64;

    public static final CustomPacketPayload.Type<MiningPanelRequestPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "mining_panel_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MiningPanelRequestPayload> STREAM_CODEC =
            StreamCodec.of(MiningPanelRequestPayload::write, MiningPanelRequestPayload::read);

    public MiningPanelRequestPayload {
        text = text.length() > MOST_TEXT ? text.substring(0, MOST_TEXT) : text;
    }

    @Override
    public CustomPacketPayload.Type<MiningPanelRequestPayload> type() {
        return TYPE;
    }

    private static void write(final RegistryFriendlyByteBuf buf, final MiningPanelRequestPayload payload) {
        BlockPos.STREAM_CODEC.encode(buf, payload.hostPos);
        buf.writeVarInt(payload.action);
        buf.writeVarInt(payload.pool);
        buf.writeUtf(payload.text, MOST_TEXT);
        buf.writeVarLong(payload.value);
    }

    private static MiningPanelRequestPayload read(final RegistryFriendlyByteBuf buf) {
        return new MiningPanelRequestPayload(BlockPos.STREAM_CODEC.decode(buf), buf.readVarInt(), buf.readVarInt(),
                buf.readUtf(MOST_TEXT), buf.readVarLong());
    }
}
