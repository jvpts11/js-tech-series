/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.teracoin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MiningPoolTest {

    private MiningPool pool;

    private static final String OWNER = TeracoinAddress.create(new Random(1));
    private static final String ALICE = TeracoinAddress.create(new Random(2));
    private static final String BOB = TeracoinAddress.create(new Random(3));

    @BeforeEach
    void setUp() {
        pool = new MiningPool();
    }

    @Test
    void split_takesTheFeeThenSharesByWork() {
        pool.setOwner(OWNER);
        pool.work(offer("rig-1", ALICE, 300L), 0L);
        pool.work(offer("rig-2", BOB, 100L), 0L);

        final List<TeracoinChain.PoolShare> shares = pool.split(block(Trc.of(50)));

        assertEquals(new TeracoinChain.PoolShare(OWNER, Trc.of(50) / 100, true), shares.get(0));
        assertEquals(Trc.of(50) - Trc.of(50) / 100, total(shares) - shares.get(0).amount());
        assertEquals(3L * amountOf(shares, BOB), amountOf(shares, ALICE), 3L);
    }

    @Test
    void split_placesEveryHundredMillionth() {
        pool.work(offer("a", ALICE, 1L), 0L);
        pool.work(offer("b", BOB, 1L), 0L);
        pool.work(offer("c", OWNER, 1L), 0L);

        assertEquals(100L, total(pool.split(block(100L))));
    }

    @Test
    void split_startsANewRound() {
        pool.work(offer("rig-1", ALICE, 300L), 0L);

        pool.split(block(Trc.of(50)));

        assertTrue(pool.roundShares().isEmpty());
        assertEquals(1, pool.found().size());
    }

    @Test
    void split_givesTheFinderTheBlockWhenNoWorkIsCounted() {
        final List<TeracoinChain.PoolShare> shares = pool.split(block(Trc.of(50)));

        assertEquals(List.of(new TeracoinChain.PoolShare(ALICE, Trc.of(50), false)), shares);
    }

    @Test
    void admits_holdsTheMinersNotAcceptedAsRequests() {
        pool.setAcceptedOnly(true);
        final MinerOffer rig = offer("rig-1", ALICE, 300L);

        assertFalse(pool.admits(rig));
        assertEquals(List.of(new MiningPool.Request("rig-1", "RIG-1", rig.kind(), ALICE, 300L)), pool.requests());
        pool.accept("rig-1");
        assertTrue(pool.admits(rig));
        assertTrue(pool.requests().isEmpty());
    }

    @Test
    void accept_takesOnlyAMinerThatAsked() {
        pool.setAcceptedOnly(true);

        pool.accept("never-asked");

        assertFalse(pool.admits(offer("never-asked", ALICE, 300L)), "a name nobody sent in is not taken");
    }

    @Test
    void lastHour_averagesEachFiveMinutesOfWork() {
        pool.work(offer("rig-1", ALICE, 300L), 0L);
        pool.work(offer("rig-2", BOB, 100L), 0L);
        pool.work(offer("rig-1", ALICE, 300L), 20L);

        final long[] hour = pool.lastHour(20L);

        assertEquals(MiningPool.HOUR_SLICES, hour.length);
        assertEquals(350L, hour[MiningPool.HOUR_SLICES - 1]);
        assertEquals(0L, hour[0]);
    }

    @Test
    void setFee_keepsItBetweenNothingAndFivePercent() {
        pool.setFee(80);
        assertEquals(MiningPool.MOST_FEE, pool.feePermille());
        pool.setFee(-3);
        assertEquals(0, pool.feePermille());
    }

    @Test
    void hashrate_countsOnlyTheMinersHeardFromLately() {
        pool.work(offer("rig-1", ALICE, 300L), 0L);
        pool.work(offer("rig-2", BOB, 100L), 100L);

        assertEquals(100L, pool.hashrate(100L + MiningPool.SEEN_TICKS));
    }

    @Test
    void restore_takesUpThePoolAsSaved() {
        pool.rename("Hilltop Pool");
        pool.setOwner(OWNER);
        pool.setFee(25);
        pool.work(offer("rig-1", ALICE, 300L), 0L);
        final MiningPool copy = new MiningPool();

        copy.restore(pool.saved());

        assertEquals("Hilltop Pool", copy.name());
        assertEquals(25, copy.feePermille());
        assertEquals(pool.roundShares(), copy.roundShares());
    }

    private static MinerOffer offer(final String machine, final String address, final long hashrate) {
        return new MinerOffer(machine, machine.toUpperCase(), MinerKinds.rig("standard", 6), address, hashrate,
                "pool");
    }

    private static FoundBlock block(final long reward) {
        return new FoundBlock(1, 0L, 0L, reward, TeracoinRules.START_DIFFICULTY, "rig-1", "RIG-1", ALICE, "pool",
                false);
    }

    private static long total(final List<TeracoinChain.PoolShare> shares) {
        long sum = 0L;
        for (final TeracoinChain.PoolShare share : shares) {
            sum += share.amount();
        }
        return sum;
    }

    private static long amountOf(final List<TeracoinChain.PoolShare> shares, final String address) {
        long sum = 0L;
        for (final TeracoinChain.PoolShare share : shares) {
            if (share.address().equals(address) && !share.fee()) {
                sum += share.amount();
            }
        }
        return sum;
    }
}
