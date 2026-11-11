/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.teracoin;

import dev.jstech.computers.advancement.JscEvents;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * What Teracoin tells the advancements that the chain alone cannot: who made a wallet, a send to another player's,
 * a wallet holding its coins, and the reward halving. The chain keeps addresses, never players, so the maker of each
 * wallet is noted as it is made, and only those wallets count.
 */
public final class TeracoinAwards {

    /* What HODL asks a wallet to hold, and for how long, with nothing sent from it. */
    private static final long HODL_AMOUNT = Trc.of(1_000);
    private static final long HODL_TICKS = 7L * 24_000L;
    /* How many of a wallet's latest entries are read for something it sent. */
    private static final int ENTRIES_READ = 64;

    private TeracoinAwards() {
    }

    /** {@code player} made the wallet of {@code address}. */
    public static void walletMade(final ServerPlayer player, final String address) {
        TeracoinState.of(player.server).own(address, player.getUUID());
        JscEvents.award(player, JscEvents.WALLET_MADE);
    }

    /** {@code player} sent TRC from {@code from} to {@code to}: to another player's wallet, it is a gift. */
    public static void sent(final ServerPlayer player, final String from, final String to) {
        final TeracoinState state = TeracoinState.of(player.server);
        state.holdSince(from, null);
        final UUID owner = state.ownerOf(to);
        if (owner != null && !owner.equals(player.getUUID())) {
            JscEvents.award(player, JscEvents.TRC_SENT);
        }
    }

    /**
     * Looks at every wallet whose maker is known at game time {@code now}: one holding 1,000 TRC is timed from when it
     * first did, the time starting over at anything it sends or at dropping below, and after seven game days its
     * maker has held on. The chain's own clock calls it once a minute.
     */
    public static void lookAtHolders(final MinecraftServer server, final long now) {
        final TeracoinState state = TeracoinState.of(server);
        final TeracoinChain chain = state.chain();
        for (final Map.Entry<String, UUID> wallet : state.owners().entrySet()) {
            final String address = wallet.getKey();
            if (chain.balance(address) < HODL_AMOUNT) {
                state.holdSince(address, null);
                continue;
            }
            long since = state.heldSince(address) == null ? now : state.heldSince(address);
            for (final LedgerEntry entry : chain.history(address, ENTRIES_READ)) {
                if (entry.signedFor(address) < 0L && entry.tick() > since) {
                    since = entry.tick();
                }
            }
            state.holdSince(address, since);
            if (now - since >= HODL_TICKS) {
                JscEvents.award(server, wallet.getValue(), JscEvents.HODL, "");
            }
        }
    }

    /** A block was found at {@code height}: when its reward is half the one before, everybody here saw it halve. */
    static void found(final MinecraftServer server, final TeracoinChain chain, final int height) {
        if (height > 1 && chain.rewardAt(height) < chain.rewardAt(height - 1)) {
            for (final ServerPlayer player : server.getPlayerList().getPlayers()) {
                JscEvents.award(player, JscEvents.HALVING);
            }
        }
    }
}
