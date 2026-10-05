/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.motion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class MotionScopeTest {

    private static final MotionSpec BLINK = MotionSpec.of(MotionStyles.BLINK, 250, IEasing.LINEAR, "");
    private static final MotionSpec SLOW = MotionSpec.of(MotionStyles.BLINK, 530, IEasing.LINEAR, "");

    @Test
    void spec_isStillOutsideAnyScope() {
        assertSame(MotionSpec.NONE, MotionScope.spec(MotionKinds.CARET_BLINK));
    }

    @Test
    void within_readsTheScopesMotionsAndGivesBackTheOneBefore() {
        final AtomicReference<MotionSpec> inner = new AtomicReference<>();
        final AtomicReference<MotionSpec> outerAfter = new AtomicReference<>();
        MotionScope.within(kind -> BLINK, () -> {
            MotionScope.within(kind -> SLOW, () -> inner.set(MotionScope.spec(MotionKinds.CARET_BLINK)));
            outerAfter.set(MotionScope.spec(MotionKinds.CARET_BLINK));
        });
        assertEquals(SLOW, inner.get());
        assertEquals(BLINK, outerAfter.get());
        assertSame(MotionSpec.NONE, MotionScope.spec(MotionKinds.CARET_BLINK));
    }

    @Test
    void within_givesBackTheOneBeforeWhenTheDrawingFails() {
        assertThrows(IllegalStateException.class, () -> MotionScope.within(kind -> BLINK, () -> {
            throw new IllegalStateException("drawing failed");
        }));
        assertSame(MotionSpec.NONE, MotionScope.spec(MotionKinds.CARET_BLINK));
    }

    @Test
    void spec_ofAKindTheScopeDoesNotKnowIsStill() {
        final AtomicReference<MotionSpec> read = new AtomicReference<>();
        MotionScope.within(kind -> null, () -> read.set(MotionScope.spec(MotionKinds.BUSY)));
        assertSame(MotionSpec.NONE, read.get());
    }
}
