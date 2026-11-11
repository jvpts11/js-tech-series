/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.portable;

import dev.jstech.computers.operation.payload.portable.PortableActionPayload;
import dev.jstech.computers.operation.payload.portable.PortableWalletPayload;
import dev.jstech.computers.operation.payload.teracoin.WalletStatePayload;
import dev.jstech.computers.teracoin.LedgerEntry;
import dev.jstech.computers.teracoin.TeracoinAddress;
import dev.jstech.computers.teracoin.TeracoinChain;
import dev.jstech.computers.teracoin.TeracoinState;
import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * A portable's Crypto Wallet (computers/docs/design/wireless.md, computers/docs/design/teracoin.md): one wallet kept on
 * the device itself, its balance, its address, sending and the latest movements. The device reaches the chain through
 * the network it is joined to; without one its wallet waits.
 */
public final class PortableWallet {

    private static final RandomGenerator RANDOM = RandomGenerator.getDefault();

    private PortableWallet() {
    }

    /** What the device's wallet was asked, answered with its state. */
    public static void act(final ServerPlayer player, final ServerLevel level, final PortableDevices devices,
                           final PortableState state, final boolean connected, final PortableActionPayload payload) {
        final TeracoinState teracoin = TeracoinState.of(level.getServer());
        final TeracoinChain chain = teracoin.chain();
        int note = WalletStatePayload.NOTE_NONE;
        if (connected && payload.action() == PortableActionPayload.Action.WALLET_CREATE
                && state.walletAddress().isEmpty()) {
            final TeracoinChain.Keys keys = chain.createWallet(RANDOM);
            devices.keepWallet(payload.device(), keys.address(), keys.key());
            teracoin.setDirty();
            note = WalletStatePayload.NOTE_CREATED;
        } else if (connected && payload.action() == PortableActionPayload.Action.WALLET_SEND
                && !state.walletAddress().isEmpty()) {
            note = send(level, teracoin, state, payload);
        }
        PacketDistributor.sendToPlayer(player, view(chain, state, connected, payload, note));
    }

    private static int send(final ServerLevel level, final TeracoinState teracoin, final PortableState state,
                            final PortableActionPayload payload) {
        final String to = payload.named().trim();
        if (!TeracoinAddress.valid(to)) {
            return WalletStatePayload.NOTE_BAD_ADDRESS;
        }
        final TeracoinChain.SendResult sent = teracoin.chain().send(state.walletAddress(), state.walletKey(), to,
                payload.amount(), "", level.getServer().overworld().getGameTime(),
                level.getServer().overworld().getDayTime());
        teracoin.setDirty();
        return switch (sent) {
            case SENT -> WalletStatePayload.NOTE_SENT;
            case NOT_YOURS -> WalletStatePayload.NOTE_NOT_YOURS;
            case BAD_ADDRESS -> WalletStatePayload.NOTE_BAD_ADDRESS;
            case NOTHING -> WalletStatePayload.NOTE_NOTHING;
            case TOO_MUCH -> WalletStatePayload.NOTE_TOO_MUCH;
        };
    }

    private static PortableWalletPayload view(final TeracoinChain chain, final PortableState state,
                                              final boolean connected, final PortableActionPayload payload,
                                              final int note) {
        final String address = state.walletAddress();
        if (address.isEmpty() || !connected) {
            return new PortableWalletPayload(payload.device(), connected, address, 0L, 0L, List.of(), note);
        }
        final List<WalletStatePayload.Move> moves = new ArrayList<>();
        for (final LedgerEntry entry : chain.history(address, PortableWalletPayload.MOST_MOVES)) {
            final String other = switch (entry.kind()) {
                case SEND -> entry.from().equals(address) ? entry.to() : entry.from();
                case PAYOUT, FEE -> entry.label();
                case REWARD, PRINT -> "";
            };
            final String label = entry.kind() == LedgerEntry.Kind.PRINT ? entry.label() : "";
            final int blocks = entry.pending() ? -1 : Math.max(1, chain.height() - entry.confirmedAt() + 1);
            // A device lists each movement by its block: the one it came from, or the one that confirmed it.
            final int block = entry.block() > 0 ? entry.block() : Math.max(0, entry.confirmedAt());
            moves.add(new WalletStatePayload.Move(entry.kind().id(), entry.dayTime(), entry.signedFor(address), other,
                    label, block, blocks));
        }
        return new PortableWalletPayload(payload.device(), true, address, chain.balance(address),
                chain.pendingIn(address), moves, note);
    }
}
