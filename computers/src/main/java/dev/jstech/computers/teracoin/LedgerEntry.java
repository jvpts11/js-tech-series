/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.teracoin;

import dev.jstech.core.id.IStableId;
import dev.jstech.core.id.IStableName;

/**
 * One movement of TRC on the chain: who it left and who it reached, how much, and the block that confirmed it, or none
 * yet. A send, a pool's payout and its fee wait for the next block, or ten minutes when none comes; a block's reward
 * to a miner alone and a payment to a 3D printer are confirmed at once. Pure.
 *
 * @param id          its number on the chain, never reused
 * @param kind        what moved the TRC
 * @param from        the address it left, or empty for a block's reward and a pool's payout
 * @param to          the address it reached, or empty for a payment to a printer, whose TRC leave circulation
 * @param amount      how much, in hundred-millionths of a TRC
 * @param label       what the sender called it, the pool that paid it or the item printed
 * @param block       the block it came from, for a reward, a payout and a fee; 0 otherwise
 * @param tick        when it was made, in game ticks
 * @param dayTime     the world's time of day then, for the hour and the day it was made
 * @param confirmedAt the chain's height when it was confirmed, or {@link #PENDING}
 */
public record LedgerEntry(long id, Kind kind, String from, String to, long amount, String label, int block, long tick,
                          long dayTime, int confirmedAt) {

    /** The height of an entry not confirmed yet. */
    public static final int PENDING = -1;

    /** Whether it waits for a block. */
    public boolean pending() {
        return confirmedAt == PENDING;
    }

    /** It confirmed at {@code height}. */
    public LedgerEntry confirmed(final int height) {
        return new LedgerEntry(id, kind, from, to, amount, label, block, tick, dayTime, height);
    }

    /** Whether {@code address} sent it or received it. */
    public boolean touches(final String address) {
        return from.equals(address) || to.equals(address);
    }

    /** What it did to {@code address}'s balance: what it brought in, or minus what it took out. */
    public long signedFor(final String address) {
        if (from.equals(address) && to.equals(address)) {
            return 0L;
        }
        return to.equals(address) ? amount : -amount;
    }

    /** What moved the TRC, each kind with the number and the name it travels and is saved by. */
    public enum Kind implements IStableId, IStableName {
        /** A wallet sent it to an address. */
        SEND(0, "send"),
        /** A block found by a miner alone paid it. */
        REWARD(1, "reward"),
        /** A pool paid a miner's share of a block it found. */
        PAYOUT(2, "payout"),
        /** A pool paid its owner its fee from a block it found. */
        FEE(3, "fee"),
        /** A wallet paid a 3D printer for an item; the TRC left circulation. */
        PRINT(4, "print");

        private final int id;
        private final String serializedName;

        Kind(final int id, final String serializedName) {
            this.id = id;
            this.serializedName = serializedName;
        }

        @Override
        public int id() {
            return id;
        }

        @Override
        public String serializedName() {
            return serializedName;
        }
    }
}
