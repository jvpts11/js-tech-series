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
 * What a Crypto Wallet asks of the computer it runs on: its wallets and the chain's state, a new wallet, a send, a
 * print or the wallet's Auto print. Every answer is the wallet's whole state ({@link WalletStatePayload}).
 *
 * @param hostPos the computer the wallet runs on
 * @param action  what is asked, one of the constants
 * @param wallet  the path of the wallet's file it is about, or empty for the computer's first
 * @param text    the address sent to, the new wallet's name, or the item printed
 * @param other   the label of a send, or the new wallet's folder
 * @param amount  the amount sent, the balance Auto print keeps, or 1 to copy a new wallet to a USB stick
 * @param target  the printer a print goes to
 * @param count   the copies printed
 */
public record WalletRequestPayload(BlockPos hostPos, int action, String wallet, String text, String other,
                                   long amount, long target, int count) implements CustomPacketPayload {

    /** Ask for the state of the wallet named. */
    public static final int LOOK = 0;
    /** Make a new wallet. */
    public static final int CREATE = 1;
    /** Send from the wallet named. */
    public static final int SEND = 2;
    /** Print an item on a 3D printer, paid from the wallet named. */
    public static final int PRINT = 3;
    /** Set the wallet's Auto print, or turn it off with no item. */
    public static final int AUTO_PRINT = 4;
    /** The letters of a path at most. */
    public static final int MOST_PATH = 256;
    /** The letters of a text or a label at most. */
    public static final int MOST_TEXT = 128;

    public static final CustomPacketPayload.Type<WalletRequestPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "wallet_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WalletRequestPayload> STREAM_CODEC = StreamCodec.of(
            WalletRequestPayload::write, WalletRequestPayload::read);

    public WalletRequestPayload {
        wallet = clip(wallet, MOST_PATH);
        text = clip(text, MOST_TEXT);
        other = clip(other, MOST_PATH);
    }

    /** A look at the wallet at {@code wallet}. */
    public static WalletRequestPayload look(final BlockPos hostPos, final String wallet) {
        return new WalletRequestPayload(hostPos, LOOK, wallet, "", "", 0L, 0L, 0);
    }

    @Override
    public CustomPacketPayload.Type<WalletRequestPayload> type() {
        return TYPE;
    }

    private static String clip(final String text, final int most) {
        return text.length() > most ? text.substring(0, most) : text;
    }

    private static void write(final RegistryFriendlyByteBuf buf, final WalletRequestPayload payload) {
        BlockPos.STREAM_CODEC.encode(buf, payload.hostPos);
        buf.writeVarInt(payload.action);
        buf.writeUtf(payload.wallet, MOST_PATH);
        buf.writeUtf(payload.text, MOST_TEXT);
        buf.writeUtf(payload.other, MOST_PATH);
        buf.writeVarLong(payload.amount);
        buf.writeLong(payload.target);
        buf.writeVarInt(payload.count);
    }

    private static WalletRequestPayload read(final RegistryFriendlyByteBuf buf) {
        return new WalletRequestPayload(BlockPos.STREAM_CODEC.decode(buf), buf.readVarInt(), buf.readUtf(MOST_PATH),
                buf.readUtf(MOST_TEXT), buf.readUtf(MOST_PATH), buf.readVarLong(), buf.readLong(), buf.readVarInt());
    }
}
