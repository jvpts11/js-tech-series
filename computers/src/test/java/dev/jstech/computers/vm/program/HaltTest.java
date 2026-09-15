/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class HaltTest {

    @Test
    void reasons_keepTheNumbersTheyAreWrittenDownBy() {
        final Map<Halt.Reason, Integer> written = new EnumMap<>(Halt.Reason.class);
        written.put(Halt.Reason.DIVIDE_BY_ZERO, 1);
        written.put(Halt.Reason.NO_OBJECT, 2);
        written.put(Halt.Reason.USE_AFTER_DISPOSE, 3);
        written.put(Halt.Reason.OUT_OF_MEMORY, 4);
        written.put(Halt.Reason.BAD_CAST, 5);
        written.put(Halt.Reason.OUT_OF_RANGE, 6);
        written.put(Halt.Reason.NO_SUCH_MEMBER, 7);
        written.put(Halt.Reason.NO_NETWORK, 8);
        written.put(Halt.Reason.NOT_LOCKED, 9);
        written.put(Halt.Reason.CANNOT_START, 10);
        written.put(Halt.Reason.REFUSED, 11);
        written.put(Halt.Reason.STACK_DEPTH, 12);
        written.put(Halt.Reason.FAULT, 13);

        for (final Halt.Reason reason : Halt.Reason.values()) {
            assertEquals(written.get(reason), reason.id(), reason::name);
        }
    }

    @Test
    void byId_findsEveryReasonByItsNumberAndAnUnknownOneAsAFault() {
        for (final Halt.Reason reason : Halt.Reason.values()) {
            assertSame(reason, Halt.Reason.byId(reason.id()));
        }
        assertSame(Halt.Reason.FAULT, Halt.Reason.byId(0));
        assertSame(Halt.Reason.FAULT, Halt.Reason.byId(99));
    }
}
