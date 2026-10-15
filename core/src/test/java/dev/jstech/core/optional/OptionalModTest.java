/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class OptionalModTest {

    @Test
    void ifLoaded_startsTheAdapterWhenTheModIsThere() {
        final AtomicInteger started = new AtomicInteger();
        final OptionalMod mod = OptionalMod.of("other", id -> true);

        assertTrue(mod.ifLoaded(() -> started::incrementAndGet));
        assertEquals(1, started.get());
    }

    @Test
    void ifLoaded_neverAsksForTheAdapterWhenTheModIsAbsent() {
        final AtomicInteger asked = new AtomicInteger();
        final OptionalMod mod = OptionalMod.of("other", id -> false);

        assertFalse(mod.ifLoaded(() -> {
            asked.incrementAndGet();
            return () -> { };
        }));
        assertEquals(0, asked.get(), "the adapter, and every class it names, is never reached");
    }

    @Test
    void isLoaded_asksAboutTheModItNames() {
        final OptionalMod mod = OptionalMod.of("other", "other"::equals);

        assertTrue(mod.isLoaded());
        assertEquals("other", mod.modid());
        assertFalse(OptionalMod.of("else", "other"::equals).isLoaded());
    }
}
