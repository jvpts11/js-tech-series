/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ResourceChangesTest {

    private static final long QUIET = 750;

    private ResourceChanges changes;

    @BeforeEach
    void setUp() {
        changes = new ResourceChanges(QUIET);
    }

    @Test
    void take_givesNothingWhileNothingChanged() {
        assertEquals(0, changes.take(10_000));
    }

    @Test
    void take_waitsUntilTheFilesStopChanging() {
        changes.changed(1_000);
        changes.changed(1_500);

        assertEquals(0, changes.take(2_000));
        assertEquals(2, changes.take(2_250));
    }

    @Test
    void take_handsEachChangeOverOnce() {
        changes.changed(1_000);

        assertEquals(1, changes.take(5_000));
        assertEquals(0, changes.take(9_000));
    }

    @Test
    void changed_afterATakeStartsTheWaitAgain() {
        changes.changed(1_000);
        changes.take(5_000);
        changes.changed(6_000);

        assertEquals(0, changes.take(6_500));
        assertEquals(1, changes.take(6_750));
    }
}
