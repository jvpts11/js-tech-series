/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.portable;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.operation.payload.teracoin.WalletStatePayload;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * A portable's Crypto Wallet, as the server answers it: whether the device reaches a network, its wallet's address,
 * balance and what waits to reach it, its latest movements, and what the last request did. The key stays on the server.
 *
 * @param device    the device it is about
 * @param connected whether the device reaches a network, without which its wallet cannot be seen or spent
 * @param address   the device's wallet's address, or empty before it has one
 * @param balance   what the wallet holds, in hundred-millionths of a TRC
 * @param pending   what waits for a block to reach it
 * @param moves     its latest movements, the newest first
 * @param note      what the last request did, one of the Crypto Wallet's notes
 */
public record PortableWalletPayload(UUID device, boolean connected, String address, long balance, long pending,
                                    List<WalletStatePayload.Move> moves, int note) implements CustomPacketPayload {

    /** The movements a device shows at most. */
    public static final int MOST_MOVES = 8;
    private static final int MOST_TEXT = 128;

    public static final CustomPacketPayload.Type<PortableWalletPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "portable_wallet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PortableWalletPayload> STREAM_CODEC = StreamCodec.of(
            PortableWalletPayload::write, PortableWalletPayload::read);

    public PortableWalletPayload {
        moves = List.copyOf(moves.size() > MOST_MOVES ? moves.subList(0, MOST_MOVES) : moves);
    }

    @Override
    public CustomPacketPayload.Type<PortableWalletPayload> type() {
        return TYPE;
    }

    private static String clip(final String text) {
        return text.length() > MOST_TEXT ? text.substring(0, MOST_TEXT) : text;
    }

    private static void write(final RegistryFriendlyByteBuf buf, final PortableWalletPayload payload) {
        UUIDUtil.STREAM_CODEC.encode(buf, payload.device);
        buf.writeBoolean(payload.connected);
        buf.writeUtf(clip(payload.address), MOST_TEXT);
        buf.writeVarLong(payload.balance);
        buf.writeVarLong(payload.pending);
        buf.writeVarInt(payload.moves.size());
        for (final WalletStatePayload.Move move : payload.moves) {
            buf.writeVarInt(move.kind());
            buf.writeVarLong(move.dayTime());
            buf.writeLong(move.amount());
            buf.writeUtf(clip(move.other()), MOST_TEXT);
            buf.writeUtf(clip(move.label()), MOST_TEXT);
            buf.writeVarInt(move.block());
            buf.writeVarInt(move.blocks() + 1);
        }
        buf.writeVarInt(payload.note);
    }

    private static PortableWalletPayload read(final RegistryFriendlyByteBuf buf) {
        final UUID device = UUIDUtil.STREAM_CODEC.decode(buf);
        final boolean connected = buf.readBoolean();
        final String address = buf.readUtf(MOST_TEXT);
        final long balance = buf.readVarLong();
        final long pending = buf.readVarLong();
        final int count = Math.min(MOST_MOVES, buf.readVarInt());
        final List<WalletStatePayload.Move> moves = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            moves.add(new WalletStatePayload.Move(buf.readVarInt(), buf.readVarLong(), buf.readLong(),
                    buf.readUtf(MOST_TEXT), buf.readUtf(MOST_TEXT), buf.readVarInt(), buf.readVarInt() - 1));
        }
        return new PortableWalletPayload(device, connected, address, balance, pending, moves, buf.readVarInt());
    }
}
