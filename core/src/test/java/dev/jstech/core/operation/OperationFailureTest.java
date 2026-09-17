/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.operation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.util.Utf8Text;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class OperationFailureTest {

    @Test
    void none_hasNothingToSay() {
        assertFalse(OperationFailure.NONE.isPresent());
        assertTrue(OperationFailure.NONE.arguments().isEmpty());
    }

    @Test
    void of_keepsTheKeyAndItsArguments() {
        final OperationFailure why = OperationFailure.of("jsc.x", "Iron Ingot", "3");
        assertTrue(why.isPresent());
        assertEquals("jsc.x", why.key());
        assertEquals(List.of("Iron Ingot", "3"), why.arguments());
    }

    @Test
    void of_takesNoArgumentsAtAll() {
        assertEquals(List.of(), OperationFailure.of("jsc.x").arguments());
    }

    /*
     * A reason is built where something has already gone wrong, so it cuts rather than throws: an exception
     * here would replace the failure with one about the failure's own message, and the first would be lost.
     */
    @Test
    void newFailure_cutsAKeyTooLongToTravel() {
        final String tooLong = "k".repeat(OperationFailure.MAX_KEY_BYTES + 50);
        assertEquals(OperationFailure.MAX_KEY_BYTES,
                Utf8Text.byteLength(new OperationFailure(tooLong, List.of()).key()));
    }

    @Test
    void newFailure_cutsAnArgumentTooLongToTravel() {
        final String tooLong = "n".repeat(OperationFailure.MAX_ARGUMENT_BYTES + 50);
        final OperationFailure why = OperationFailure.of("jsc.x", tooLong);
        assertEquals(OperationFailure.MAX_ARGUMENT_BYTES, Utf8Text.byteLength(why.arguments().get(0)));
    }

    @Test
    void newFailure_dropsArgumentsPastWhatTravels() {
        final String[] many = new String[OperationFailure.MAX_ARGUMENTS + 5];
        Arrays.fill(many, "x");
        assertEquals(OperationFailure.MAX_ARGUMENTS, OperationFailure.of("jsc.x", many).arguments().size());
    }

    @Test
    void newFailure_readsNothingAsAnEmptyArgument() {
        assertEquals(List.of(""), OperationFailure.of("jsc.x", (String) null).arguments());
    }

    @Test
    void newFailure_refusesNoKeyAndNoArgumentsAtAll() {
        assertThrows(NullPointerException.class, () -> new OperationFailure(null, List.of()));
        assertThrows(NullPointerException.class, () -> new OperationFailure("jsc.x", null));
    }

    /* What a record hands out cannot be what somebody else goes on writing to. */
    @Test
    void newFailure_doesNotShareTheListItWasBuiltFrom() {
        final List<String> mine = new ArrayList<>(List.of("one"));
        final OperationFailure why = new OperationFailure("jsc.x", mine);
        mine.add("two");
        assertEquals(List.of("one"), why.arguments());
        assertThrows(UnsupportedOperationException.class, () -> why.arguments().add("three"));
    }
}
