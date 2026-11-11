/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.teracoin;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;
import org.jetbrains.annotations.Nullable;

/**
 * Teracoin's one chain of blocks for the whole world, owned by nobody (computers/docs/design/teracoin.md): what every
 * address holds, the keys that spend from them, the movements of TRC, the blocks found, the difficulty and the
 * reward. Once a second the miners that hashed are weighed together: each one's chance of finding the block is its
 * hashrate over the difficulty. A block confirms the sends waiting for it; the difficulty is adjusted every ten blocks
 * to keep the target time, by four times at most either way and never below a floor that grows with the world's days;
 * the reward halves every so many blocks, for the whole server. Pure.
 */
public final class TeracoinChain {

    private final Map<String, Long> balances = new HashMap<>();
    private final Map<String, String> keys = new HashMap<>();
    private final List<LedgerEntry> ledger = new ArrayList<>();
    private final ArrayDeque<FoundBlock> blocks = new ArrayDeque<>();
    private TeracoinRules rules = TeracoinRules.DEFAULT;
    private int height;
    private long difficulty = TeracoinRules.START_DIFFICULTY;
    private long windowStart = -1L;
    private int halvingFrom;
    private long issued;
    private long burned;
    private long nextId = 1L;
    private long version;

    /** The movements of TRC kept, the oldest confirmed ones forgotten first. */
    public static final int LEDGER_KEPT = 4_096;
    /** The blocks kept for the windows that list them. */
    public static final int BLOCKS_KEPT = 256;

    /** The settings the chain follows from now on. */
    public void useRules(final TeracoinRules chosen) {
        rules = chosen;
    }

    public TeracoinRules rules() {
        return rules;
    }

    /** A number that grows with every change, so whoever keeps the chain knows when it has something to save. */
    public long version() {
        return version;
    }

    /** The blocks found so far. */
    public int height() {
        return height;
    }

    /** The difficulty a miner faces after {@code worldDays} days of the world: the chain's, or its floor. */
    public long difficulty(final long worldDays) {
        return Math.max(difficulty, rules.floor(worldDays));
    }

    /** What block {@code number} pays, in hundred-millionths of a TRC. */
    public long rewardAt(final int number) {
        if (!rules.halving()) {
            return rules.reward();
        }
        final int halvings = Math.max(0, number - 1 - halvingFrom) / rules.halvingBlocks();
        return halvings >= Long.SIZE - 1 ? 0L : rules.reward() >> halvings;
    }

    /** What the next block pays. */
    public long nextReward() {
        return rewardAt(height + 1);
    }

    /** The blocks until the first one that pays half, or -1 when the reward does not halve. */
    public int blocksToHalving() {
        if (!rules.halving()) {
            return -1;
        }
        return rules.halvingBlocks() - Math.floorMod(height - halvingFrom, rules.halvingBlocks()) + 1;
    }

    /** Puts the reward back to its first value: the halvings count again from this block. */
    public void resetHalving() {
        halvingFrom = height;
        version++;
    }

    /** The TRC every block has paid so far. */
    public long issued() {
        return issued;
    }

    /** The TRC paid to 3D printers, which left circulation. */
    public long burned() {
        return burned;
    }

    /** A new wallet: an address the chain has not seen, and the key that spends from it. */
    public Keys createWallet(final RandomGenerator random) {
        String address = TeracoinAddress.create(random);
        while (keys.containsKey(address)) {
            address = TeracoinAddress.create(random);
        }
        final String key = TeracoinAddress.key(random);
        keys.put(address, key);
        version++;
        return new Keys(address, key);
    }

    /** Whether {@code key} spends from {@code address}. */
    public boolean owns(final String address, final String key) {
        return !key.isEmpty() && key.equals(keys.get(address));
    }

    /** Whether the chain made {@code address}. */
    public boolean knows(final String address) {
        return keys.containsKey(address);
    }

    /** What {@code address} holds, confirmed, less what it sent that waits for a block. */
    public long balance(final String address) {
        return balances.getOrDefault(address, 0L);
    }

    /** What is on its way to {@code address}, waiting for a block. */
    public long pendingIn(final String address) {
        long sum = 0L;
        for (int i = ledger.size() - 1; i >= 0; i--) {
            final LedgerEntry entry = ledger.get(i);
            if (entry.pending() && entry.to().equals(address) && !entry.from().equals(address)) {
                sum += entry.amount();
            }
        }
        return sum;
    }

    /**
     * Sends {@code amount} from {@code from}, whose key is {@code key}, to {@code to}, with no fee: it leaves the
     * sender at once and reaches the receiver with the next block, or ten minutes later when no block comes.
     */
    public SendResult send(final String from, final String key, final String to, final long amount, final String label,
                           final long now, final long dayTime) {
        if (!owns(from, key)) {
            return SendResult.NOT_YOURS;
        }
        if (!TeracoinAddress.valid(to)) {
            return SendResult.BAD_ADDRESS;
        }
        if (amount <= 0L) {
            return SendResult.NOTHING;
        }
        if (amount > balance(from)) {
            return SendResult.TOO_MUCH;
        }
        balances.merge(from, -amount, Long::sum);
        record(new LedgerEntry(nextId++, LedgerEntry.Kind.SEND, from, to, amount, label, 0, now, dayTime,
                LedgerEntry.PENDING));
        return SendResult.SENT;
    }

    /**
     * Pays a 3D printer {@code amount} for {@code label} from {@code from}, whose key is {@code key}: at once, without
     * waiting for a block, and the TRC leave circulation. Whether it was paid.
     */
    public boolean pay(final String from, final String key, final long amount, final String label, final long now,
                       final long dayTime) {
        if (!owns(from, key) || amount <= 0L || amount > balance(from)) {
            return false;
        }
        balances.merge(from, -amount, Long::sum);
        burned += amount;
        record(new LedgerEntry(nextId++, LedgerEntry.Kind.PRINT, from, "", amount, label, 0, now, dayTime, height));
        return true;
    }

    /**
     * Weighs this second's miners: confirms the sends that waited ten minutes, then rolls each one's chance of the
     * block together; the block found, or null. A block found confirms what waited for it, may move the difficulty,
     * and still has to pay its finder ({@link #creditAlone}) or its pool ({@link #creditPool}).
     */
    @Nullable
    public FoundBlock resolve(final long now, final long dayTime, final long worldDays, final List<MinerOffer> offers,
                              final RandomGenerator random) {
        confirmDue(now);
        if (windowStart < 0L) {
            windowStart = now;
        }
        long total = 0L;
        for (final MinerOffer offer : offers) {
            total = saturatedAdd(total, Math.max(0L, offer.hashrate()));
        }
        if (total <= 0L) {
            return null;
        }
        final long faced = difficulty(worldDays);
        final double chance = Math.min(1.0, (double) total / (double) faced);
        if (random.nextDouble() >= chance) {
            return null;
        }
        final MinerOffer finder = pick(offers, total, random);
        height++;
        version++;
        confirmWaiting();
        final long reward = rewardAt(height);
        issued += reward;
        final FoundBlock found = new FoundBlock(height, now, dayTime, reward, faced, finder.machine(), finder.name(),
                finder.address(), finder.pool(), random.nextDouble() < rules.rareChance());
        blocks.addLast(found);
        while (blocks.size() > BLOCKS_KEPT) {
            blocks.removeFirst();
        }
        if (height % TeracoinRules.ADJUST_EVERY == 0) {
            adjust(now, worldDays);
        }
        return found;
    }

    /** Pays a block found by a miner alone to its address, confirmed with it. */
    public void creditAlone(final FoundBlock found) {
        balances.merge(found.address(), found.reward(), Long::sum);
        record(new LedgerEntry(nextId++, LedgerEntry.Kind.REWARD, "", found.address(), found.reward(), "",
                found.height(), found.tick(), found.dayTime(), found.height()));
    }

    /** Pays a block found for the pool {@code poolName} as its shares, each waiting for the next block. */
    public void creditPool(final FoundBlock found, final String poolName, final List<PoolShare> shares) {
        for (final PoolShare share : shares) {
            if (share.amount() <= 0L || !TeracoinAddress.valid(share.address())) {
                continue;
            }
            record(new LedgerEntry(nextId++, share.fee() ? LedgerEntry.Kind.FEE : LedgerEntry.Kind.PAYOUT, "",
                    share.address(), share.amount(), poolName, found.height(), found.tick(), found.dayTime(),
                    LedgerEntry.PENDING));
        }
    }

    /** The movements touching {@code address}, the newest first, at most {@code most}. */
    public List<LedgerEntry> history(final String address, final int most) {
        final List<LedgerEntry> out = new ArrayList<>();
        for (int i = ledger.size() - 1; i >= 0 && out.size() < most; i--) {
            if (ledger.get(i).touches(address)) {
                out.add(ledger.get(i));
            }
        }
        return out;
    }

    /** The blocks kept, the newest first, at most {@code most}. */
    public List<FoundBlock> latestBlocks(final int most) {
        final List<FoundBlock> out = new ArrayList<>();
        final Iterator<FoundBlock> newest = blocks.descendingIterator();
        while (newest.hasNext() && out.size() < most) {
            out.add(newest.next());
        }
        return out;
    }

    /** The chain as it is saved. */
    public Saved saved() {
        return new Saved(height, difficulty, windowStart, halvingFrom, issued, burned, nextId, balances, keys, ledger,
                List.copyOf(blocks));
    }

    /** Takes up the chain as it was saved. */
    public void restore(final Saved saved) {
        balances.clear();
        balances.putAll(saved.balances());
        keys.clear();
        keys.putAll(saved.keys());
        ledger.clear();
        ledger.addAll(saved.ledger());
        blocks.clear();
        blocks.addAll(saved.blocks());
        height = Math.max(0, saved.height());
        difficulty = Math.max(1L, saved.difficulty());
        windowStart = saved.windowStart();
        halvingFrom = Math.max(0, Math.min(height, saved.halvingFrom()));
        issued = Math.max(0L, saved.issued());
        burned = Math.max(0L, saved.burned());
        nextId = Math.max(1L, saved.nextId());
    }

    private void record(final LedgerEntry entry) {
        ledger.add(entry);
        version++;
        int drop = 0;
        while (ledger.size() - drop > LEDGER_KEPT && !ledger.get(drop).pending()) {
            drop++;
        }
        if (drop > 0) {
            ledger.subList(0, drop).clear();
        }
    }

    /* Confirms every movement waiting for a block, with the block just found. */
    private void confirmWaiting() {
        for (int i = 0; i < ledger.size(); i++) {
            if (ledger.get(i).pending()) {
                confirm(i);
            }
        }
    }

    /* Confirms the movements that waited ten minutes for a block that did not come. */
    private void confirmDue(final long now) {
        for (int i = ledger.size() - 1; i >= 0; i--) {
            final LedgerEntry entry = ledger.get(i);
            if (entry.pending() && now - entry.tick() >= TeracoinRules.CONFIRM_TICKS) {
                confirm(i);
            }
        }
    }

    private void confirm(final int index) {
        final LedgerEntry entry = ledger.get(index);
        ledger.set(index, entry.confirmed(height));
        version++;
        if (!entry.to().isEmpty()) {
            balances.merge(entry.to(), entry.amount(), Long::sum);
        }
    }

    /* The difficulty times what ten blocks should have taken over what they took, by four at most either way. */
    private void adjust(final long now, final long worldDays) {
        final double expected = (double) TeracoinRules.ADJUST_EVERY * rules.targetTicks();
        final double took = Math.max(1L, now - windowStart);
        final double step = Math.max(1.0 / TeracoinRules.MOST_STEP, Math.min(TeracoinRules.MOST_STEP, expected / took));
        final double moved = Math.min((double) Long.MAX_VALUE / 8.0, difficulty(worldDays) * step);
        difficulty = Math.max(rules.floor(worldDays), Math.round(moved));
        windowStart = now;
    }

    private static MinerOffer pick(final List<MinerOffer> offers, final long total, final RandomGenerator random) {
        final double at = random.nextDouble() * (double) total;
        double reached = 0.0;
        MinerOffer last = offers.get(0);
        for (final MinerOffer offer : offers) {
            if (offer.hashrate() <= 0L) {
                continue;
            }
            last = offer;
            reached += (double) offer.hashrate();
            if (at < reached) {
                return offer;
            }
        }
        return last;
    }

    private static long saturatedAdd(final long a, final long b) {
        final long sum = a + b;
        return sum < a ? Long.MAX_VALUE : sum;
    }

    /** A wallet's address and the key that spends from it. */
    public record Keys(String address, String key) {
    }

    /** One address's part of a block a pool found: its share of the pool's work, or the owner's fee. */
    public record PoolShare(String address, long amount, boolean fee) {
    }

    /** How a send went. */
    public enum SendResult {
        SENT,
        /** The wallet's key does not spend from its address. */
        NOT_YOURS,
        /** The address it was sent to is not written as an address is. */
        BAD_ADDRESS,
        /** Nothing was sent. */
        NOTHING,
        /** The wallet does not hold that much. */
        TOO_MUCH
    }

    /** The chain as it is saved: its numbers, every address's balance and key, its movements and its blocks. */
    public record Saved(int height, long difficulty, long windowStart, int halvingFrom, long issued, long burned,
                        long nextId, Map<String, Long> balances, Map<String, String> keys, List<LedgerEntry> ledger,
                        List<FoundBlock> blocks) {

        public Saved {
            balances = Map.copyOf(balances);
            keys = Map.copyOf(keys);
            ledger = List.copyOf(ledger);
            blocks = List.copyOf(blocks);
        }
    }
}
