/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * The questions a Gateway puts across, and the rules an answer has to keep to.
 *
 * <p>These are the rules that decide whether an answer from another mod's computer can reach a program
 * of ours, so they are checked here rather than only where they happen to be called from.
 */
class GatewayRpcBrokerTest {

    private static final int COMPUTER = 3;
    private static final GatewayRpcBroker.Waiting PROGRAM = new GatewayRpcBroker.Waiting.ByProgram(11);

    @Test
    void submit_namesEveryQuestionSomethingOfItsOwn() {
        final GatewayRpcBroker one = new GatewayRpcBroker();
        final GatewayRpcBroker other = new GatewayRpcBroker();
        final GatewayRpcBroker.Pending first = one.submit(COMPUTER, PROGRAM, "read", 100L);
        final GatewayRpcBroker.Pending second = other.submit(COMPUTER, PROGRAM, "read", 100L);
        assertNotNull(first);
        assertNotNull(second);
        assertNotEquals(first.id(), second.id(),
                "two Gateways that both start counting would hand out the same name; these do not");
        assertEquals(GatewayRpcBroker.Landing.UNKNOWN, one.landing(second.id(), COMPUTER, "x"),
                "and one Gateway's answer is nothing to the other");
    }

    @Test
    void landing_refusesAnAnswerFromAComputerThatWasNotAsked() {
        final GatewayRpcBroker broker = new GatewayRpcBroker();
        final GatewayRpcBroker.Pending question = broker.submit(COMPUTER, PROGRAM, "read", 100L);
        assertEquals(GatewayRpcBroker.Landing.WRONG_COMPUTER, broker.landing(question.id(), 9, "mine now"));
        assertEquals(GatewayRpcBroker.Landing.DELIVERED, broker.landing(question.id(), COMPUTER, "the file"),
                "and the one that was asked is still able to answer it");
    }

    @Test
    void landing_takesTheQuestionOffTheListOnce() {
        final GatewayRpcBroker broker = new GatewayRpcBroker();
        final GatewayRpcBroker.Pending question = broker.submit(COMPUTER, PROGRAM, "read", 100L);
        assertEquals(GatewayRpcBroker.Landing.DELIVERED, broker.landing(question.id(), COMPUTER, "once"));
        assertEquals(GatewayRpcBroker.Landing.UNKNOWN, broker.landing(question.id(), COMPUTER, "twice"));
        assertEquals(0, broker.inFlight());
    }

    @Test
    void expired_letsGoOfWhatWillNeverBeAnsweredAndNothingElse() {
        final GatewayRpcBroker broker = new GatewayRpcBroker();
        final GatewayRpcBroker.Pending soon = broker.submit(COMPUTER, PROGRAM, "read", 10L);
        broker.submit(COMPUTER, PROGRAM, "list", 100L);
        assertEquals(List.of(), broker.expired(9L), "nothing is given up on before its time");
        assertEquals(List.of(soon), broker.expired(10L));
        assertEquals(1, broker.inFlight(), "the other one is still waiting");
    }

    @Test
    void forget_dropsEveryQuestionPutToAComputerThatIsGone() {
        final GatewayRpcBroker broker = new GatewayRpcBroker();
        broker.submit(COMPUTER, PROGRAM, "read", 100L);
        broker.submit(COMPUTER, PROGRAM, "list", 100L);
        final GatewayRpcBroker.Pending elsewhere = broker.submit(9, PROGRAM, "read", 100L);
        assertEquals(2, broker.forget(COMPUTER).size());
        assertEquals(1, broker.inFlight());
        assertNotNull(broker.waiting(elsewhere.id()));
    }

    @Test
    void submit_stopsAProgramFillingTheMachineWithQuestions() {
        final GatewayRpcBroker broker = new GatewayRpcBroker();
        for (int i = 0; i < 64; i++) {
            assertNotNull(broker.submit(COMPUTER, PROGRAM, "read", 100L), "question " + i);
        }
        assertNull(broker.submit(COMPUTER, PROGRAM, "read", 100L), "and no more than that at once");
    }

    @Test
    void limits_refuseAnAnswerThatSaysTooMuch() {
        assertNull(GatewayLimits.refuse("a reasonable answer"));
        assertNull(GatewayLimits.refuse(Map.of(1.0, "a", 2.0, "b")));
        assertTrue(GatewayLimits.refuse("x".repeat(GatewayLimits.TEXT + 1)).contains("longer than"),
                "a piece of text longer than the limit");
        final Map<Object, Object> many = new LinkedHashMap<>();
        for (int i = 1; i <= GatewayLimits.ITEMS + 1; i++) {
            many.put((double) i, "x");
        }
        assertTrue(GatewayLimits.refuse(many).contains("more than"), "more things than the limit");
    }

    @Test
    void limits_refuseAnAnswerThatGoesRoundForEver() {
        final Map<Object, Object> table = new LinkedHashMap<>();
        table.put("self", table);
        assertNotNull(GatewayLimits.refuse(table),
                "a table that holds itself is refused rather than followed round");
    }
}
