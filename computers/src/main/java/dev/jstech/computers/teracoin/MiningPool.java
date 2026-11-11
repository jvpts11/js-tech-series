/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.teracoin;

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A Mining Pool, the service a rack server runs (computers/docs/design/teracoin.md). It mines with its miners'
 * hashrate together, and when it finds a block shares the reward in proportion to the work each address gave since
 * its last block (the hashrate times the seconds mined), less its fee, set by its owner from 0 to 5% (1% by default)
 * and paid to the owner's address. Its owner may take only the miners they accept, whose requests wait in its
 * settings. Pure.
 */
public final class MiningPool {

    private final Set<String> accepted = new LinkedHashSet<>();
    private final Map<String, Request> requests = new LinkedHashMap<>();
    private final Map<String, Long> work = new LinkedHashMap<>();
    private final Map<String, Member> members = new LinkedHashMap<>();
    private final ArrayDeque<FoundBlock> found = new ArrayDeque<>();
    /* The pool's hashrate over the last hour, in twelve slices of five minutes: the sums and the seconds counted. */
    private final long[] hourSums = new long[HOUR_SLICES];
    private final int[] hourSeconds = new int[HOUR_SLICES];
    private String name = "";
    private String owner = "";
    private int feePermille = DEFAULT_FEE;
    private boolean acceptedOnly;
    private long roundStart = -1L;
    private int foundCount;
    private long lastSlice = -1L;
    private long secondSeen = -1L;

    /** The fee a new pool takes, in thousandths of a block: 1%. */
    public static final int DEFAULT_FEE = 10;
    /** The most fee a pool may take: 5%. */
    public static final int MOST_FEE = 50;
    /** The requests to join that wait at most; more are not taken until some are answered. */
    public static final int MOST_REQUESTS = 16;
    /** The blocks found that are kept. */
    public static final int FOUND_KEPT = 64;
    /** The letters of a pool's name at most. */
    public static final int NAME_MOST = 24;
    /** How long a miner not heard from still counts as mining for the pool. */
    public static final long SEEN_TICKS = 60L;
    /** The slices of the last hour a panel draws, five minutes each. */
    public static final int HOUR_SLICES = 12;
    /* A slice's ticks: five minutes. */
    private static final long SLICE_TICKS = 6_000L;

    /** The pool's name, as its miners and its panel show it; empty until its owner names it. */
    public String name() {
        return name;
    }

    public void rename(final String chosen) {
        final String trimmed = chosen.trim();
        name = trimmed.length() > NAME_MOST ? trimmed.substring(0, NAME_MOST) : trimmed;
    }

    /** The address its fee is paid to, or empty for a pool that takes none. */
    public String owner() {
        return owner;
    }

    public void setOwner(final String address) {
        owner = TeracoinAddress.valid(address) ? address : "";
    }

    /** Its fee, in thousandths of a block. */
    public int feePermille() {
        return feePermille;
    }

    public void setFee(final int permille) {
        feePermille = Math.max(0, Math.min(MOST_FEE, permille));
    }

    /** Whether it takes only the miners its owner accepted. */
    public boolean acceptedOnly() {
        return acceptedOnly;
    }

    public void setAcceptedOnly(final boolean only) {
        acceptedOnly = only;
    }

    /**
     * Whether the miner of {@code offer} may mine for it; one it may not take waits among its requests, as it offered
     * itself.
     */
    public boolean admits(final MinerOffer offer) {
        if (!acceptedOnly || accepted.contains(offer.machine())) {
            return true;
        }
        if (requests.containsKey(offer.machine()) || requests.size() < MOST_REQUESTS) {
            requests.put(offer.machine(), new Request(offer.machine(), offer.name(), offer.kind(), offer.address(),
                    offer.hashrate()));
        }
        return false;
    }

    /** Takes the miner on {@code machine}, which asked to join. */
    public void accept(final String machine) {
        requests.remove(machine);
        accepted.add(machine);
    }

    /** Turns down the request of the miner on {@code machine}. */
    public void refuse(final String machine) {
        requests.remove(machine);
    }

    /** Lets the miner on {@code machine} go: it asks again to join. */
    public void dismiss(final String machine) {
        accepted.remove(machine);
        members.remove(machine);
    }

    /** The requests to join that wait, the oldest first. */
    public List<Request> requests() {
        return new ArrayList<>(requests.values());
    }

    /** The machines its owner accepted. */
    public List<String> accepted() {
        return List.copyOf(accepted);
    }

    /** Counts a second of {@code offer}'s work toward the round, at {@code now}. */
    public void work(final MinerOffer offer, final long now) {
        if (roundStart < 0L) {
            roundStart = now;
        }
        work.merge(offer.address(), Math.max(0L, offer.hashrate()), (a, b) -> a + b < a ? Long.MAX_VALUE : a + b);
        members.put(offer.machine(), new Member(offer.machine(), offer.name(), offer.kind(), offer.address(),
                offer.hashrate(), now));
        slice(now);
        final int at = (int) Math.floorMod(now / SLICE_TICKS, (long) HOUR_SLICES);
        hourSums[at] += Math.max(0L, offer.hashrate());
        if (secondSeen != now) {
            secondSeen = now;
            hourSeconds[at]++;
        }
    }

    /** Its hashrate in each five minutes of the last hour, the oldest first; 0 where it did not mine. */
    public long[] lastHour(final long now) {
        slice(now);
        final long[] out = new long[HOUR_SLICES];
        final long current = now / SLICE_TICKS;
        for (int i = 0; i < HOUR_SLICES; i++) {
            final int at = (int) Math.floorMod(current - (HOUR_SLICES - 1 - i), (long) HOUR_SLICES);
            out[i] = hourSeconds[at] == 0 ? 0L : hourSums[at] / hourSeconds[at];
        }
        return out;
    }

    /** The blocks it has found since it began. */
    public int foundCount() {
        return foundCount;
    }

    /** The miners that mined for it lately, the fastest first. */
    public List<Member> members(final long now) {
        forgetQuiet(now);
        final List<Member> out = new ArrayList<>(members.values());
        out.sort(Comparator.comparingLong(Member::hashrate).reversed());
        return out;
    }

    /** Its hashrate: its miners' together. */
    public long hashrate(final long now) {
        long sum = 0L;
        for (final Member member : members(now)) {
            sum += member.hashrate();
        }
        return sum;
    }

    /** Each address's part of the round's work so far, in thousandths, the largest first. */
    public List<RoundShare> roundShares() {
        long total = 0L;
        for (final long hashes : work.values()) {
            total += hashes;
        }
        final List<RoundShare> out = new ArrayList<>();
        if (total <= 0L) {
            return out;
        }
        for (final Map.Entry<String, Long> entry : work.entrySet()) {
            out.add(new RoundShare(entry.getKey(), (int) (entry.getValue() * 1000.0 / total)));
        }
        out.sort(Comparator.comparingInt(RoundShare::permille).reversed());
        return out;
    }

    /** When its round began, the tick of its last block or of its first work; -1 before either. */
    public long roundStart() {
        return roundStart;
    }

    /**
     * Shares {@code block}, found for it, among its round's addresses by their work, its fee to its owner first; the
     * shares add up to the block's reward exactly, the hundred-millionths a division leaves going to the largest
     * remainders. The round starts again.
     */
    public List<TeracoinChain.PoolShare> split(final FoundBlock block) {
        final List<TeracoinChain.PoolShare> out = new ArrayList<>();
        final long fee = owner.isEmpty() ? 0L : block.reward() * feePermille / 1000L;
        if (fee > 0L) {
            out.add(new TeracoinChain.PoolShare(owner, fee, true));
        }
        final long rest = block.reward() - fee;
        long total = 0L;
        for (final long hashes : work.values()) {
            total += hashes;
        }
        if (total <= 0L) {
            out.add(new TeracoinChain.PoolShare(block.address(), rest, false));
        } else {
            out.addAll(shares(rest, total));
        }
        found.addLast(block);
        foundCount++;
        while (found.size() > FOUND_KEPT) {
            found.removeFirst();
        }
        work.clear();
        roundStart = block.tick();
        return out;
    }

    /** The blocks it found, the newest first. */
    public List<FoundBlock> found() {
        final List<FoundBlock> out = new ArrayList<>();
        final Iterator<FoundBlock> newest = found.descendingIterator();
        while (newest.hasNext()) {
            out.add(newest.next());
        }
        return out;
    }

    /** The pool as it is saved: its settings, its requests and accepted miners, its round and its blocks. */
    public Saved saved() {
        return new Saved(name, owner, feePermille, acceptedOnly, roundStart, List.copyOf(accepted), requests(), work,
                List.copyOf(found), foundCount);
    }

    /** Takes up the pool as it was saved. */
    public void restore(final Saved saved) {
        rename(saved.name());
        setOwner(saved.owner());
        setFee(saved.feePermille());
        acceptedOnly = saved.acceptedOnly();
        roundStart = saved.roundStart();
        accepted.clear();
        accepted.addAll(saved.accepted());
        requests.clear();
        for (final Request request : saved.requests()) {
            requests.put(request.machine(), request);
        }
        work.clear();
        work.putAll(saved.work());
        found.clear();
        found.addAll(saved.found());
        foundCount = Math.max(saved.foundCount(), found.size());
        members.clear();
        lastSlice = -1L;
    }

    /** Forgets everything: its service was taken off its server. */
    public void clear() {
        restore(new Saved("", "", DEFAULT_FEE, false, -1L, List.of(), List.of(), Map.of(), List.of(), 0));
    }

    private void forgetQuiet(final long now) {
        members.values().removeIf(member -> now - member.seen() > SEEN_TICKS);
    }

    /* Empties the slices the clock passed since the last time: what was counted there is an hour old. */
    private void slice(final long now) {
        final long current = now / SLICE_TICKS;
        if (lastSlice < 0L || current - lastSlice >= HOUR_SLICES) {
            Arrays.fill(hourSums, 0L);
            Arrays.fill(hourSeconds, 0);
        } else {
            for (long s = lastSlice + 1; s <= current; s++) {
                final int at = (int) Math.floorMod(s, (long) HOUR_SLICES);
                hourSums[at] = 0L;
                hourSeconds[at] = 0;
            }
        }
        lastSlice = Math.max(lastSlice, current);
    }

    /* The rest of a block shared by work, every hundred-millionth placed: the largest remainders take what is left. */
    private List<TeracoinChain.PoolShare> shares(final long rest, final long total) {
        final List<TeracoinChain.PoolShare> out = new ArrayList<>();
        final List<long[]> remainders = new ArrayList<>();
        final BigInteger whole = BigInteger.valueOf(rest);
        final BigInteger of = BigInteger.valueOf(total);
        long given = 0L;
        final List<String> addresses = new ArrayList<>(work.keySet());
        for (int i = 0; i < addresses.size(); i++) {
            final BigInteger[] split = whole.multiply(BigInteger.valueOf(work.get(addresses.get(i))))
                    .divideAndRemainder(of);
            final long part = split[0].longValue();
            given += part;
            remainders.add(new long[] {i, split[1].longValue(), part});
        }
        remainders.sort(Comparator.comparingLong((long[] r) -> r[1]).reversed().thenComparingLong(r -> r[0]));
        long left = rest - given;
        for (final long[] remainder : remainders) {
            if (left > 0L) {
                remainder[2]++;
                left--;
            }
        }
        remainders.sort(Comparator.comparingLong(r -> r[0]));
        for (final long[] remainder : remainders) {
            out.add(new TeracoinChain.PoolShare(addresses.get((int) remainder[0]), remainder[2], false));
        }
        return out;
    }

    /** A miner that mined for it lately: its machine, name and kind, the address it pays, its hashrate and when. */
    public record Member(String machine, String name, String kind, String address, long hashrate, long seen) {
    }

    /** A miner asking to join: its machine, name and kind, the address it pays and its hashrate. */
    public record Request(String machine, String name, String kind, String address, long hashrate) {
    }

    /** An address's part of the round's work, in thousandths. */
    public record RoundShare(String address, int permille) {
    }

    /** The pool as it is saved. */
    public record Saved(String name, String owner, int feePermille, boolean acceptedOnly, long roundStart,
                        List<String> accepted, List<Request> requests, Map<String, Long> work,
                        List<FoundBlock> found, int foundCount) {

        public Saved {
            accepted = List.copyOf(accepted);
            requests = List.copyOf(requests);
            work = Map.copyOf(work);
            found = List.copyOf(found);
        }
    }
}
