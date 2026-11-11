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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TeracoinChainTest {

    private TeracoinChain chain;

    private static final long START = TeracoinRules.START_DIFFICULTY;

    @BeforeEach
    void setUp() {
        chain = new TeracoinChain();
    }

    @Test
    void createWallet_givesAKeyThatSpendsOnlyItsAddress() {
        final TeracoinChain.Keys one = chain.createWallet(new Random(1));
        final TeracoinChain.Keys two = chain.createWallet(new Random(2));

        assertTrue(chain.owns(one.address(), one.key()));
        assertFalse(chain.owns(one.address(), two.key()));
        assertFalse(chain.owns(one.address(), ""));
    }

    @Test
    void creditAlone_paysTheFinderAtOnce() {
        final TeracoinChain.Keys miner = chain.createWallet(new Random(1));

        mine(miner.address(), 100L);

        assertEquals(Trc.of(50), chain.balance(miner.address()));
        assertEquals(1, chain.height());
    }

    @Test
    void send_leavesTheSenderAtOnceAndReachesWithTheNextBlock() {
        final TeracoinChain.Keys alice = chain.createWallet(new Random(1));
        final TeracoinChain.Keys bob = chain.createWallet(new Random(2));
        mine(alice.address(), 100L);

        final TeracoinChain.SendResult sent = chain.send(alice.address(), alice.key(), bob.address(), Trc.of(20),
                "Bob", 200L, 0L);

        assertEquals(TeracoinChain.SendResult.SENT, sent);
        assertEquals(Trc.of(30), chain.balance(alice.address()));
        assertEquals(0L, chain.balance(bob.address()));
        assertEquals(Trc.of(20), chain.pendingIn(bob.address()));
        mine(alice.address(), 300L);
        assertEquals(Trc.of(20), chain.balance(bob.address()));
        assertEquals(0L, chain.pendingIn(bob.address()));
    }

    @Test
    void send_confirmsAfterTenMinutesWithoutABlock() {
        final TeracoinChain.Keys alice = chain.createWallet(new Random(1));
        final TeracoinChain.Keys bob = chain.createWallet(new Random(2));
        mine(alice.address(), 100L);
        chain.send(alice.address(), alice.key(), bob.address(), Trc.of(5), "", 200L, 0L);

        chain.resolve(200L + TeracoinRules.CONFIRM_TICKS - 1L, 0L, 0L, List.of(), new Fixed(0.0));
        assertEquals(0L, chain.balance(bob.address()));
        chain.resolve(200L + TeracoinRules.CONFIRM_TICKS, 0L, 0L, List.of(), new Fixed(0.0));

        assertEquals(Trc.of(5), chain.balance(bob.address()));
        assertEquals(1, chain.height());
    }

    @Test
    void send_refusesAWrongKeyAnUnwrittenAddressAndMoreThanHeld() {
        final TeracoinChain.Keys alice = chain.createWallet(new Random(1));
        final TeracoinChain.Keys bob = chain.createWallet(new Random(2));
        mine(alice.address(), 100L);

        assertEquals(TeracoinChain.SendResult.NOT_YOURS,
                chain.send(alice.address(), bob.key(), bob.address(), 1L, "", 0L, 0L));
        assertEquals(TeracoinChain.SendResult.BAD_ADDRESS,
                chain.send(alice.address(), alice.key(), "trc1qnot", 1L, "", 0L, 0L));
        assertEquals(TeracoinChain.SendResult.TOO_MUCH,
                chain.send(alice.address(), alice.key(), bob.address(), Trc.of(51), "", 0L, 0L));
        assertEquals(TeracoinChain.SendResult.NOTHING,
                chain.send(alice.address(), alice.key(), bob.address(), 0L, "", 0L, 0L));
        assertEquals(Trc.of(50), chain.balance(alice.address()));
    }

    @Test
    void pay_takesThePriceAtOnceAndBurnsIt() {
        final TeracoinChain.Keys alice = chain.createWallet(new Random(1));
        mine(alice.address(), 100L);

        assertTrue(chain.pay(alice.address(), alice.key(), Trc.of(10), "Diamond", 200L, 0L));
        assertFalse(chain.pay(alice.address(), alice.key(), Trc.of(41), "Diamond", 200L, 0L));

        assertEquals(Trc.of(40), chain.balance(alice.address()));
        assertEquals(Trc.of(10), chain.burned());
        assertFalse(chain.history(alice.address(), 1).get(0).pending());
    }

    @Test
    void resolve_findsNothingWithoutHashrate() {
        assertNull(chain.resolve(0L, 0L, 0L, List.of(), new Fixed(0.0)));
        assertNull(chain.resolve(20L, 0L, 0L, List.of(offer("a", "trc1q", 0L)), new Fixed(0.0)));
    }

    @Test
    void resolve_findsOnlyAsOftenAsTheHashrateOverTheDifficulty() {
        final List<MinerOffer> half = List.of(offer("rig", address(1), START / 2));

        assertNull(chain.resolve(0L, 0L, 0L, half, new Fixed(0.6)));
        assertNotNull(chain.resolve(20L, 0L, 0L, half, new Fixed(0.4)));
    }

    @Test
    void resolve_givesTheBlockToTheMinerWhoseWorkItLandsOn() {
        final List<MinerOffer> two = List.of(offer("slow", address(1), START / 4), offer("fast", address(2),
                START * 3 / 4));

        final FoundBlock found = chain.resolve(0L, 0L, 0L, two, new Fixed(0.5));

        assertNotNull(found);
        assertEquals("fast", found.machine());
        assertEquals(address(2), found.address());
    }

    @Test
    void rewardAt_halvesEveryHalvingAndResetPutsItBack() {
        chain.useRules(new TeracoinRules(TeracoinRules.DEFAULT.targetTicks(), Trc.of(50), true, 3, 100, 0.0));
        for (int i = 0; i < 3; i++) {
            mine(address(1), i * 100L);
        }

        assertEquals(Trc.of(50), chain.rewardAt(3));
        assertEquals(Trc.of(25), chain.nextReward());
        assertEquals(4, chain.blocksToHalving());
        chain.resetHalving();
        assertEquals(Trc.of(50), chain.nextReward());
    }

    @Test
    void resolve_quadruplesTheDifficultyAtMostAfterTenQuickBlocks() {
        for (int i = 0; i < TeracoinRules.ADJUST_EVERY; i++) {
            mine(address(1), 1L + i);
        }

        assertEquals(START * 4, chain.difficulty(0L));
    }

    @Test
    void resolve_neverLowersTheDifficultyBelowItsFloor() {
        final long slow = TeracoinRules.DEFAULT.targetTicks() * 100L;
        for (int i = 0; i < TeracoinRules.ADJUST_EVERY; i++) {
            chain.resolve(i * slow, 0L, 50L, List.of(offer("rig", address(1), Long.MAX_VALUE / 4)), new Fixed(0.0));
        }

        assertEquals(TeracoinRules.DEFAULT.floor(50L), chain.difficulty(50L));
        assertEquals(START + START / 2, chain.difficulty(50L));
    }

    @Test
    void creditPool_paysEachShareWithTheNextBlock() {
        final TeracoinChain.Keys owner = chain.createWallet(new Random(1));
        final FoundBlock found = chain.resolve(0L, 0L, 0L, List.of(offer("rig", address(2), START)), new Fixed(0.0));
        assertNotNull(found);

        chain.creditPool(found, "Hilltop Pool", List.of(new TeracoinChain.PoolShare(owner.address(), Trc.of(1), true),
                new TeracoinChain.PoolShare(address(2), Trc.of(49), false)));

        assertEquals(0L, chain.balance(address(2)));
        assertEquals(Trc.of(49), chain.pendingIn(address(2)));
        mine(address(3), 50L);
        assertEquals(Trc.of(49), chain.balance(address(2)));
        assertEquals(Trc.of(1), chain.balance(owner.address()));
    }

    @Test
    void restore_takesUpTheChainAsSaved() {
        final TeracoinChain.Keys alice = chain.createWallet(new Random(1));
        mine(alice.address(), 100L);
        chain.send(alice.address(), alice.key(), address(2), Trc.of(3), "", 120L, 0L);
        final TeracoinChain copy = new TeracoinChain();

        copy.restore(chain.saved());

        assertEquals(chain.height(), copy.height());
        assertEquals(Trc.of(47), copy.balance(alice.address()));
        assertEquals(Trc.of(3), copy.pendingIn(address(2)));
        assertTrue(copy.owns(alice.address(), alice.key()));
    }

    private void mine(final String address, final long now) {
        final FoundBlock found = chain.resolve(now, 0L, 0L, List.of(offer("rig", address, Long.MAX_VALUE / 4)),
                new Fixed(0.0));
        assertNotNull(found);
        chain.creditAlone(found);
    }

    private static MinerOffer offer(final String machine, final String address, final long hashrate) {
        return new MinerOffer(machine, machine, MinerKinds.machine("personal_computer"), address, hashrate, "");
    }

    private static String address(final int seed) {
        return TeracoinAddress.create(new Random(seed * 31L));
    }

    /** A random that always draws the same fraction. */
    private static final class Fixed implements RandomGenerator {

        private final double value;

        private Fixed(final double value) {
            this.value = value;
        }

        @Override
        public long nextLong() {
            return 0L;
        }

        @Override
        public double nextDouble() {
            return value;
        }
    }
}
