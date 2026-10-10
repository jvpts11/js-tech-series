/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.energy;

import dev.jstech.core.energy.internal.EnergyNetwork;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnergyNetworkTest {

    // Arbitrary deterministic positions; in production these are BlockPos.asLong().
    private static final long GEN_A = 1_000_000L;
    private static final long GEN_B = 2_000_000L;
    private static final long CONS_A = 3_000_000L;
    private static final long CONS_B = 4_000_000L;
    private static final long CABLE_1 = 10_000_000L;
    private static final long CABLE_2 = 11_000_000L;
    private static final long CABLE_3 = 12_000_000L;
    private static final long CONS_C = 5_000_000L;
    // Throughputs a cable can have in these tests.
    private static final long COPPER = 500L;
    private static final long HIGH_CAPACITY = 8_000L;
    private static final long UNLIMITED = Long.MAX_VALUE;

    @Test
    void emptyNetwork_returnsEmpty() {
        EnergyNetwork net = new EnergyNetwork();
        EnergyDistributionResult result = net.tickDistribute();
        assertEquals(0L, result.totalSupply());
        assertEquals(0L, result.totalDemand());
        assertEquals(0L, result.totalDelivered());
        assertTrue(result.perConsumerDelivered().isEmpty());
        assertTrue(result.perCableUsage().isEmpty());
    }

    @Test
    void generatorOnly_supplyPreservedNothingDistributed() {
        EnergyNetwork net = new EnergyNetwork();
        TestNode gen = TestNode.generator(1000L);
        net.addNode(GEN_A, gen);

        EnergyDistributionResult result = net.tickDistribute();

        assertEquals(1000L, result.totalSupply());
        assertEquals(0L, result.totalDemand());
        assertEquals(0L, result.totalDelivered());
        assertEquals(0L, gen.totalSupplied);
    }

    @Test
    void consumerOnly_demandUnsatisfied() {
        EnergyNetwork net = new EnergyNetwork();
        TestNode cons = TestNode.consumer(1000L);
        net.addNode(CONS_A, cons);

        EnergyDistributionResult result = net.tickDistribute();

        assertEquals(0L, result.totalSupply());
        assertEquals(1000L, result.totalDemand());
        assertEquals(0L, result.totalDelivered());
        assertEquals(1000L, result.unsatisfiedDemand());
        assertEquals(0L, cons.totalConsumed);
    }

    @Test
    void disconnectedNodes_noFlow() {
        EnergyNetwork net = new EnergyNetwork();
        net.addNode(GEN_A, TestNode.generator(1000L));
        net.addNode(CONS_A, TestNode.consumer(500L));
        // No connect(...).

        EnergyDistributionResult result = net.tickDistribute();

        assertEquals(0L, result.totalDelivered());
        assertEquals(500L, result.unsatisfiedDemand());
    }

    @Test
    void t3WithSufficientSupply_deliversFullDemand() {
        EnergyNetwork net = singlePathNetwork(
                1000L, 600L, HIGH_CAPACITY);

        EnergyDistributionResult result = net.tickDistribute();

        assertEquals(1000L, result.totalSupply());
        assertEquals(600L, result.totalDemand());
        assertEquals(600L, result.totalDelivered());
        assertEquals(Long.valueOf(600L), result.perConsumerDelivered().get(CONS_A));
        assertEquals(Long.valueOf(600L), result.perCableUsage().get(CABLE_1));
        assertEquals(0L, result.unsatisfiedDemand());
    }

    @Test
    void t1Cable_capsThroughputAt500() {
        EnergyNetwork net = singlePathNetwork(
                10000L, 10000L, COPPER);

        EnergyDistributionResult result = net.tickDistribute();

        assertEquals(500L, result.totalDelivered(),
                "T1 cable caps flow at 500 FE/t");
        assertEquals(Long.valueOf(500L), result.perCableUsage().get(CABLE_1));
        assertEquals(9500L, result.unsatisfiedDemand());
    }

    @Test
    void t7Cable_carriesUnlimitedThroughput() {
        EnergyNetwork net = singlePathNetwork(
                1_000_000L, 1_000_000L, UNLIMITED);

        EnergyDistributionResult result = net.tickDistribute();

        assertEquals(1_000_000L, result.totalDelivered());
        assertEquals(Long.valueOf(1_000_000L), result.perCableUsage().get(CABLE_1));
    }

    @Test
    void seriesCables_bottleneckIsTheMiddleCable() {
        // GEN -> T3 -> T1 -> T3 -> CONS, middle T1 caps at 500.
        EnergyNetwork net = new EnergyNetwork();
        net.addNode(GEN_A, TestNode.generator(1000L));
        net.addCable(CABLE_1, new TestCable(HIGH_CAPACITY));
        net.addCable(CABLE_2, new TestCable(COPPER));
        net.addCable(CABLE_3, new TestCable(HIGH_CAPACITY));
        net.addNode(CONS_A, TestNode.consumer(1000L));
        net.connect(GEN_A, CABLE_1);
        net.connect(CABLE_1, CABLE_2);
        net.connect(CABLE_2, CABLE_3);
        net.connect(CABLE_3, CONS_A);

        EnergyDistributionResult result = net.tickDistribute();

        assertEquals(500L, result.totalDelivered(),
                "Middle T1 cable bottlenecks the series");
        assertEquals(Long.valueOf(500L), result.perCableUsage().get(CABLE_1));
        assertEquals(Long.valueOf(500L), result.perCableUsage().get(CABLE_2));
        assertEquals(Long.valueOf(500L), result.perCableUsage().get(CABLE_3));
    }

    @Test
    void oneGeneratorTwoConsumers_splitsProportionallyWhenShortage() {
        // GEN(1000) -> T7 -> CONS_A(600), CONS_B(600) → each gets 500
        EnergyNetwork net = new EnergyNetwork();
        net.addNode(GEN_A, TestNode.generator(1000L));
        net.addCable(CABLE_1, new TestCable(UNLIMITED));
        net.addNode(CONS_A, TestNode.consumer(600L));
        net.addNode(CONS_B, TestNode.consumer(600L));
        net.connect(GEN_A, CABLE_1);
        net.connect(CABLE_1, CONS_A);
        net.connect(CABLE_1, CONS_B);

        EnergyDistributionResult result = net.tickDistribute();

        assertEquals(1000L, result.totalDelivered());
        assertEquals(Long.valueOf(500L), result.perConsumerDelivered().get(CONS_A));
        assertEquals(Long.valueOf(500L), result.perConsumerDelivered().get(CONS_B));
    }

    @Test
    void sharedBottleneckCable_neverExceedsItsThroughput() {
        /*
         * GEN(supply >> cap) -> ONE T1 cable (500 FE/t) -> two consumers each demanding far more than the cap.
         * The proportional split caps each consumer at the cable's full throughput independently, so before
         * the fix the single shared cable carried 2x its rating (energy created from nothing). The total
         * through the cable must stay within its rated throughput, and no more than that can be delivered.
         */
        EnergyNetwork net = new EnergyNetwork();
        net.addNode(GEN_A, TestNode.generator(100_000L));
        net.addCable(CABLE_1, new TestCable(COPPER));
        net.addNode(CONS_A, TestNode.consumer(100_000L));
        net.addNode(CONS_B, TestNode.consumer(100_000L));
        net.connect(GEN_A, CABLE_1);
        net.connect(CABLE_1, CONS_A);
        net.connect(CABLE_1, CONS_B);

        EnergyDistributionResult result = net.tickDistribute();

        final long cap = COPPER;
        final long usage = result.perCableUsage().getOrDefault(CABLE_1, 0L);
        assertTrue(usage <= cap,
                "shared cable usage " + usage + " must not exceed its throughput " + cap);
        assertTrue(result.totalDelivered() <= cap,
                "no more FE than the bottleneck cable can carry may be delivered in a tick");
    }

    @Test
    void clampedShare_isOfferedToReceiversWhoseWayIsStillFree() {
        // 100 FE; A and B want 100 each through one cable of 60; C wants 40 through a cable with no limit.
        EnergyNetwork net = new EnergyNetwork();
        net.addNode(GEN_A, TestNode.generator(100L));
        net.addCable(CABLE_1, new TestCable(60L));
        net.addCable(CABLE_2, new TestCable(UNLIMITED));
        net.addNode(CONS_A, TestNode.consumer(100L));
        net.addNode(CONS_B, TestNode.consumer(100L));
        net.addNode(CONS_C, TestNode.consumer(40L));
        net.connect(GEN_A, CABLE_1);
        net.connect(CABLE_1, CONS_A);
        net.connect(CABLE_1, CONS_B);
        net.connect(GEN_A, CABLE_2);
        net.connect(CABLE_2, CONS_C);

        EnergyDistributionResult result = net.tickDistribute();

        assertEquals(Long.valueOf(40L), result.perConsumerDelivered().get(CONS_C),
                "the free way gets what it asked for");
        assertEquals(100L, result.totalDelivered(), "nothing is left in the generator while a way can take it");
        assertEquals(Long.valueOf(60L), result.perCableUsage().get(CABLE_1));
    }

    @Test
    void oneGeneratorTwoConsumers_proportionalWithUnevenDemands() {
        EnergyNetwork net = new EnergyNetwork();
        net.addNode(GEN_A, TestNode.generator(1000L));
        net.addCable(CABLE_1, new TestCable(UNLIMITED));
        net.addNode(CONS_A, TestNode.consumer(300L));
        net.addNode(CONS_B, TestNode.consumer(900L));
        net.connect(GEN_A, CABLE_1);
        net.connect(CABLE_1, CONS_A);
        net.connect(CABLE_1, CONS_B);

        EnergyDistributionResult result = net.tickDistribute();

        assertEquals(1000L, result.totalDelivered());
        assertEquals(Long.valueOf(250L), result.perConsumerDelivered().get(CONS_A));
        assertEquals(Long.valueOf(750L), result.perConsumerDelivered().get(CONS_B));
    }

    @Test
    void demandBelowSupply_eachConsumerGetsFullDemand() {
        EnergyNetwork net = new EnergyNetwork();
        net.addNode(GEN_A, TestNode.generator(1000L));
        net.addCable(CABLE_1, new TestCable(UNLIMITED));
        net.addNode(CONS_A, TestNode.consumer(100L));
        net.addNode(CONS_B, TestNode.consumer(200L));
        net.connect(GEN_A, CABLE_1);
        net.connect(CABLE_1, CONS_A);
        net.connect(CABLE_1, CONS_B);

        EnergyDistributionResult result = net.tickDistribute();

        assertEquals(300L, result.totalDelivered());
        assertEquals(Long.valueOf(100L), result.perConsumerDelivered().get(CONS_A));
        assertEquals(Long.valueOf(200L), result.perConsumerDelivered().get(CONS_B));
        assertEquals(0L, result.unsatisfiedDemand());
    }

    @Test
    void twoGeneratorsOneConsumer_combinedSupplyMeetsDemand() {
        EnergyNetwork net = new EnergyNetwork();
        TestNode genA = TestNode.generator(500L);
        TestNode genB = TestNode.generator(500L);
        net.addNode(GEN_A, genA);
        net.addNode(GEN_B, genB);
        net.addCable(CABLE_1, new TestCable(UNLIMITED));
        net.addCable(CABLE_2, new TestCable(UNLIMITED));
        net.addNode(CONS_A, TestNode.consumer(1000L));
        net.connect(GEN_A, CABLE_1);
        net.connect(CABLE_1, CONS_A);
        net.connect(GEN_B, CABLE_2);
        net.connect(CABLE_2, CONS_A);

        EnergyDistributionResult result = net.tickDistribute();

        assertEquals(1000L, result.totalDelivered());
        assertEquals(Long.valueOf(1000L), result.perConsumerDelivered().get(CONS_A));
        assertEquals(500L, genA.totalSupplied);
        assertEquals(500L, genB.totalSupplied);
    }

    @Test
    void multipleGenerators_processedInDeterministicOrderByPosition() {
        // GEN_A at 1M and GEN_B at 2M, both with 1000 supply.
        EnergyNetwork net = new EnergyNetwork();
        TestNode genA = TestNode.generator(1000L);
        TestNode genB = TestNode.generator(1000L);
        net.addNode(GEN_A, genA);
        net.addNode(GEN_B, genB);
        net.addCable(CABLE_1, new TestCable(UNLIMITED));
        net.addCable(CABLE_2, new TestCable(UNLIMITED));
        net.addNode(CONS_A, TestNode.consumer(1500L));
        net.connect(GEN_A, CABLE_1);
        net.connect(CABLE_1, CONS_A);
        net.connect(GEN_B, CABLE_2);
        net.connect(CABLE_2, CONS_A);

        EnergyDistributionResult result = net.tickDistribute();

        assertEquals(1500L, result.totalDelivered());
        assertEquals(1000L, genA.totalSupplied, "GEN_A processed first: 1000");
        assertEquals(500L, genB.totalSupplied, "GEN_B picks up the remainder: 500");
    }

    @Test
    void removeNonExistentPosition_returnsFalse() {
        EnergyNetwork net = new EnergyNetwork();
        assertFalse(net.remove(GEN_A));
    }

    @Test
    void addNodeAtOccupiedPosition_throwsIllegalState() {
        EnergyNetwork net = new EnergyNetwork();
        net.addNode(GEN_A, TestNode.generator(100L));
        assertThrows(IllegalStateException.class, () ->
                net.addNode(GEN_A, TestNode.generator(200L)));
        assertThrows(IllegalStateException.class, () ->
                net.addCable(GEN_A, new TestCable(COPPER)));
    }

    @Test
    void connectNonMember_throwsIllegalArgument() {
        EnergyNetwork net = new EnergyNetwork();
        net.addNode(GEN_A, TestNode.generator(100L));
        assertThrows(IllegalArgumentException.class, () ->
                net.connect(GEN_A, CONS_A));
    }

    @Test
    void storageWithSupplyAndZeroDemand_actsAsGenerator() {
        EnergyNetwork net = new EnergyNetwork();
        TestNode storage = TestNode.storage(500L, 0L);
        net.addNode(GEN_A, storage);
        net.addCable(CABLE_1, new TestCable(UNLIMITED));
        net.addNode(CONS_A, TestNode.consumer(500L));
        net.connect(GEN_A, CABLE_1);
        net.connect(CABLE_1, CONS_A);

        EnergyDistributionResult result = net.tickDistribute();

        assertEquals(500L, result.totalDelivered());
        assertEquals(500L, storage.totalSupplied);
    }

    @Test
    void nodes_areNotifiedOfExactDistributedAmounts() {
        EnergyNetwork net = new EnergyNetwork();
        TestNode gen = TestNode.generator(1000L);
        TestNode cons = TestNode.consumer(500L);
        net.addNode(GEN_A, gen);
        net.addCable(CABLE_1, new TestCable(UNLIMITED));
        net.addNode(CONS_A, cons);
        net.connect(GEN_A, CABLE_1);
        net.connect(CABLE_1, CONS_A);

        net.tickDistribute();

        assertEquals(500L, gen.totalSupplied);
        assertEquals(500L, cons.totalConsumed);
    }

    @Test
    void lossyCable_sendsEnoughForTheDemandToArrive() {
        EnergyNetwork net = new EnergyNetwork();
        TestNode gen = TestNode.generator(1000L);
        TestNode cons = TestNode.consumer(500L);
        net.addNode(GEN_A, gen);
        net.addCable(CABLE_1, new LossyCable(Long.MAX_VALUE, 100));
        net.addNode(CONS_A, cons);
        net.connect(GEN_A, CABLE_1);
        net.connect(CABLE_1, CONS_A);

        EnergyDistributionResult result = net.tickDistribute();

        // A tenth is lost on the way: 556 sent brings 500, and 555 would bring only 499.
        assertEquals(500L, cons.totalConsumed);
        assertEquals(556L, gen.totalSupplied);
        assertEquals(56L, result.totalLost());
        assertEquals(gen.totalSupplied, result.totalDelivered() + result.totalLost());
    }

    @Test
    void lossyCables_addTheirLossesAlongTheWay() {
        EnergyNetwork net = new EnergyNetwork();
        TestNode gen = TestNode.generator(100L);
        TestNode cons = TestNode.consumer(1000L);
        net.addNode(GEN_A, gen);
        net.addCable(CABLE_1, new LossyCable(Long.MAX_VALUE, 100));
        net.addCable(CABLE_2, new LossyCable(Long.MAX_VALUE, 150));
        net.addNode(CONS_A, cons);
        net.connect(GEN_A, CABLE_1);
        net.connect(CABLE_1, CABLE_2);
        net.connect(CABLE_2, CONS_A);

        EnergyDistributionResult result = net.tickDistribute();

        assertEquals(75L, cons.totalConsumed);
        assertEquals(25L, result.totalLost());
    }

    @Test
    void route_takesTheWayThatLosesLeast() {
        EnergyNetwork net = new EnergyNetwork();
        TestNode cons = TestNode.consumer(100L);
        net.addNode(GEN_A, TestNode.generator(1000L));
        net.addCable(CABLE_1, new LossyCable(Long.MAX_VALUE, 200));
        net.addCable(CABLE_2, new LossyCable(Long.MAX_VALUE, 0));
        net.addCable(CABLE_3, new LossyCable(Long.MAX_VALUE, 0));
        net.addNode(CONS_A, cons);
        // The short way loses a fifth; the long way loses nothing.
        net.connect(GEN_A, CABLE_1);
        net.connect(CABLE_1, CONS_A);
        net.connect(GEN_A, CABLE_2);
        net.connect(CABLE_2, CABLE_3);
        net.connect(CABLE_3, CONS_A);

        EnergyDistributionResult result = net.tickDistribute();

        assertEquals(100L, cons.totalConsumed);
        assertEquals(0L, result.totalLost());
        assertFalse(result.perCableUsage().containsKey(CABLE_1));
        assertEquals(100L, result.perCableUsage().get(CABLE_3));
    }

    @Test
    void route_amongEquallyLossyWaysTakesTheShortest() {
        EnergyNetwork net = new EnergyNetwork();
        net.addNode(GEN_A, TestNode.generator(1000L));
        net.addCable(CABLE_1, new TestCable(UNLIMITED));
        net.addCable(CABLE_2, new TestCable(UNLIMITED));
        net.addCable(CABLE_3, new TestCable(UNLIMITED));
        net.addNode(CONS_A, TestNode.consumer(100L));
        net.connect(GEN_A, CABLE_2);
        net.connect(CABLE_2, CABLE_3);
        net.connect(CABLE_3, CONS_A);
        net.connect(GEN_A, CABLE_1);
        net.connect(CABLE_1, CONS_A);

        EnergyDistributionResult result = net.tickDistribute();

        assertEquals(100L, result.perCableUsage().get(CABLE_1));
        assertFalse(result.perCableUsage().containsKey(CABLE_3));
    }

    @Test
    void storage_takesWhatGeneratorsHaveLeftAfterTheConsumers() {
        EnergyNetwork net = new EnergyNetwork();
        TestNode gen = TestNode.generator(100L);
        TestNode cons = TestNode.consumer(60L);
        TestNode storage = TestNode.storage(0L, 1000L);
        net.addNode(GEN_A, gen);
        net.addCable(CABLE_1, new TestCable(UNLIMITED));
        net.addNode(CONS_A, cons);
        net.addNode(CONS_B, storage);
        net.connect(GEN_A, CABLE_1);
        net.connect(CABLE_1, CONS_A);
        net.connect(CABLE_1, CONS_B);

        net.tickDistribute();

        assertEquals(60L, cons.totalConsumed);
        assertEquals(40L, storage.totalConsumed);
        assertEquals(100L, gen.totalSupplied);
    }

    @Test
    void storage_feedsWhatConsumersStillWant() {
        EnergyNetwork net = new EnergyNetwork();
        TestNode gen = TestNode.generator(30L);
        TestNode cons = TestNode.consumer(100L);
        TestNode storage = TestNode.storage(500L, 1000L);
        net.addNode(GEN_A, gen);
        net.addCable(CABLE_1, new TestCable(UNLIMITED));
        net.addNode(CONS_A, cons);
        net.addNode(GEN_B, storage);
        net.connect(GEN_A, CABLE_1);
        net.connect(CABLE_1, CONS_A);
        net.connect(CABLE_1, GEN_B);

        net.tickDistribute();

        assertEquals(100L, cons.totalConsumed);
        assertEquals(70L, storage.totalSupplied);
        assertEquals(0L, storage.totalConsumed);
    }

    @Test
    void route_isKeptUntilTheShapeChanges() {
        EnergyNetwork net = singlePathNetwork(1000L, 100L, UNLIMITED);
        assertEquals(100L, net.tickDistribute().totalDelivered());

        net.remove(CABLE_1);

        assertEquals(0L, net.tickDistribute().totalDelivered());
    }

    private static EnergyNetwork singlePathNetwork(
            long supply, long demand, long throughput) {
        EnergyNetwork net = new EnergyNetwork();
        net.addNode(GEN_A, TestNode.generator(supply));
        net.addCable(CABLE_1, new TestCable(throughput));
        net.addNode(CONS_A, TestNode.consumer(demand));
        net.connect(GEN_A, CABLE_1);
        net.connect(CABLE_1, CONS_A);
        return net;
    }

    /**
     * Mutable test-only IEnergyNode.
     */
    private static final class TestNode implements IEnergyNode {
        private final EnergyNodeRole role;
        private long supply;
        private long demand;
        long totalSupplied;
        long totalConsumed;

        static TestNode generator(long supply) {
            return new TestNode(EnergyNodeRole.GENERATOR, supply, 0L);
        }

        static TestNode consumer(long demand) {
            return new TestNode(EnergyNodeRole.CONSUMER, 0L, demand);
        }

        static TestNode storage(long supply, long demand) {
            return new TestNode(EnergyNodeRole.STORAGE, supply, demand);
        }

        private TestNode(EnergyNodeRole role, long supply, long demand) {
            this.role = role;
            this.supply = supply;
            this.demand = demand;
        }

        @Override
        public EnergyNodeRole role() {
            return role;
        }

        @Override
        public long supply() {
            return role.canSupply() ? supply : 0L;
        }

        @Override
        public long demand() {
            return role.canConsume() ? demand : 0L;
        }

        @Override
        public void onSupplied(long amount) {
            totalSupplied += amount;
            supply -= amount;
        }

        @Override
        public void onConsumed(long amount) {
            totalConsumed += amount;
            demand -= amount;
        }
    }

    /**
     * Trivial test-only IEnergyCable.
     */
    private record TestCable(long maxThroughput) implements IEnergyCable {
    }

    /**
     * Test-only IEnergyCable that loses thousandths of what crosses it.
     */
    private record LossyCable(long maxThroughput, int loss) implements IEnergyCable {
    }
}
