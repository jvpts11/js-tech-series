/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.engine.prophet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProphetStatesTest {

    private final Map<String, Long> stock = new HashMap<>();
    private final AtomicInteger reads = new AtomicInteger();
    private ProphetStates states;

    @BeforeEach
    void setUp() {
        states = new ProphetStates();
    }

    @Test
    void evaluate_asksForWhatAStateLacks() {
        stock.put("steel", 300L);
        states.keep("steel", 512, IProphetStatement.UNBOUNDED);

        final List<ProphetStates.Reaction> reactions = states.evaluate(1, 0, this::held);

        assertEquals(List.of(new ProphetStates.Reaction(ProphetStates.Reaction.CRAFT, "steel", 212, "")), reactions);
        assertEquals(ProphetStates.Status.WORKING, states.keepOf("steel").status());
    }

    @Test
    void evaluate_countsWhatIsOnItsWay() {
        stock.put("steel", 300L);
        states.keep("steel", 512, IProphetStatement.UNBOUNDED);
        states.evaluate(1, 0, this::held);
        states.started("steel", 212);

        final List<ProphetStates.Reaction> again = states.evaluate(2, 1, this::held);

        assertTrue(again.isEmpty(), "what is on its way is not asked for twice: " + again);
        assertEquals(ProphetStates.Status.WORKING, states.keepOf("steel").status());
    }

    @Test
    void evaluate_holdsOnceTheWorkArrived() {
        stock.put("steel", 300L);
        states.keep("steel", 512, IProphetStatement.UNBOUNDED);
        states.evaluate(1, 0, this::held);
        states.started("steel", 212);
        stock.put("steel", 512L);
        states.settled("steel", 212, 1);

        assertTrue(states.evaluate(2, 1, this::held).isEmpty());
        assertEquals(ProphetStates.Status.HOLDING, states.keepOf("steel").status());
    }

    @Test
    void evaluate_countsWhatArrivedUntilTheCountCatchesUp() {
        stock.put("steel", 300L);
        states.keep("steel", 512, IProphetStatement.UNBOUNDED);
        states.evaluate(1, 0, this::held);
        states.started("steel", 212);
        // The work handed its steel over, and the network's count has not seen it yet.
        states.settled("steel", 212, 1);

        assertTrue(states.evaluate(1, 1, this::held).isEmpty(), "what just came is not asked for again");
        stock.put("steel", 512L);
        assertTrue(states.evaluate(2, 2, this::held).isEmpty());
        assertEquals(ProphetStates.Status.HOLDING, states.keepOf("steel").status());
    }

    @Test
    void evaluate_readsNothingWhenNothingChanged() {
        stock.put("steel", 600L);
        states.keep("steel", 512, IProphetStatement.UNBOUNDED);
        states.evaluate(1, 0, this::held);
        reads.set(0);

        states.evaluate(1, 1, this::held);

        assertEquals(0, reads.get(), "a network standing still costs one comparison");
    }

    @Test
    void evaluate_saysWhenALevelIsAboveItsBand() {
        stock.put("fuel", 1500L);
        states.keep("fuel", 500, 1000);

        assertTrue(states.evaluate(1, 0, this::held).isEmpty(), "nothing is thrown away");
        assertEquals(ProphetStates.Status.OVER, states.keepOf("fuel").status());
    }

    @Test
    void evaluate_cannotHoldUntilItIsToldToTryAgain() {
        stock.put("wire", 10L);
        states.keep("wire", 100, IProphetStatement.UNBOUNDED);
        states.evaluate(1, 0, this::held);
        states.cannotHold("wire");

        assertTrue(states.evaluate(2, 1, this::held).isEmpty());
        assertEquals(ProphetStates.Status.CANNOT_HOLD, states.keepOf("wire").status());
        states.retry();
        assertEquals(1, states.evaluate(2, 2, this::held).size(), "a retry asks again");
    }

    @Test
    void evaluate_firesAWatchOnceUntilItsConditionStops() {
        stock.put("redstone", 400L);
        final ProphetStates.WatchState watch = states.watch("redstone", IProphetStatement.Comparison.BELOW, 500,
                "CRAFT redstone TO 1000");

        assertEquals(1, states.evaluate(1, 0, this::held).size());
        stock.put("redstone", 450L);
        assertTrue(states.evaluate(2, 1, this::held).isEmpty(), "fired once, not again while it holds");
        stock.put("redstone", 900L);
        assertTrue(states.evaluate(3, 2, this::held).isEmpty());
        assertTrue(watch.armed(), "armed again once the condition stopped");
        stock.put("redstone", 100L);
        assertEquals(1, states.evaluate(4, 3, this::held).size());
        assertEquals(2, watch.fired());
    }

    @Test
    void keep_replacesTheStateOfTheSameItem() {
        states.keep("coal", 10, IProphetStatement.UNBOUNDED);
        states.keep("coal", 20, 40);

        assertEquals(1, states.keeps().size());
        assertEquals(20L, states.keepOf("coal").lower());
    }

    @Test
    void forget_letsAStateAndAWatchGo() {
        states.keep("coal", 10, IProphetStatement.UNBOUNDED);
        final ProphetStates.WatchState watch = states.watch("coal", IProphetStatement.Comparison.ABOVE, 5,
                "QUERY items");

        assertTrue(states.forget("coal"));
        assertTrue(states.forgetWatch(watch.number()));
        assertTrue(states.keeps().isEmpty() && states.watches().isEmpty());
    }

    @Test
    void samples_keepOnlyTheLatest() {
        states.keep("coal", 10, IProphetStatement.UNBOUNDED);
        for (int i = 0; i < ProphetStates.SAMPLES + 5; i++) {
            stock.put("coal", (long) i);
            states.evaluate(i, i, this::held);
        }

        assertEquals(ProphetStates.SAMPLES, states.keepOf("coal").samples().size());
    }

    private long held(final String item) {
        reads.incrementAndGet();
        return stock.getOrDefault(item, 0L);
    }
}
