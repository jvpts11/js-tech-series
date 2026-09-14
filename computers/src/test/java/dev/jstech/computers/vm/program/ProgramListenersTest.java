/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import org.junit.jupiter.api.Test;

class ProgramListenersTest {

    private static Values.DelegateValue handler(final String type) {
        return new Values.DelegateValue(type, List.of());
    }

    @Test
    void chooseGateway_takesNoNameAsWhicheverTheMachineListsFirst() {
        final ProgramListeners listeners = new ProgramListeners();
        assertEquals("", listeners.gateway());

        listeners.chooseGateway("north");
        assertEquals("north", listeners.gateway());

        listeners.chooseGateway(null);
        assertEquals("", listeners.gateway());
    }

    @Test
    void hearMessages_replacesTheHandlerAndNullTakesItAway() {
        final ProgramListeners listeners = new ProgramListeners();
        final Values.DelegateValue second = handler("Second");
        assertNull(listeners.onMessage());

        listeners.hearMessages(handler("First"));
        listeners.hearMessages(second);
        assertSame(second, listeners.onMessage());

        listeners.hearMessages(null);
        assertNull(listeners.onMessage());
    }

    @Test
    void hearGateway_isKeptApartFromMessagesBetweenPrograms() {
        final ProgramListeners listeners = new ProgramListeners();
        final Values.DelegateValue gateway = handler("Gateway");

        listeners.hearGateway(gateway);
        assertSame(gateway, listeners.onGatewayMessage());
        assertNull(listeners.onMessage());

        listeners.hearGateway(null);
        assertNull(listeners.onGatewayMessage());
    }

    @Test
    void restore_bringsBackBothHandlersAndTheGatewayChosen() {
        final ProgramListeners listeners = new ProgramListeners();
        final Values.DelegateValue messages = handler("Messages");
        final Values.DelegateValue gateway = handler("Gateway");

        listeners.restore(messages, gateway, "north");

        assertSame(messages, listeners.onMessage());
        assertSame(gateway, listeners.onGatewayMessage());
        assertEquals("north", listeners.gateway());
    }
}
