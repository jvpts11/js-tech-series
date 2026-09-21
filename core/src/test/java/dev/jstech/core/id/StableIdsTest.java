/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.id;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StableIdsTest {

    @Test
    void find_returnsTheConstantThatDeclaresTheId() {
        final StableIds<Gapped> ids = StableIds.of(Gapped.class);
        assertEquals(Gapped.FIRST, ids.find(0));
        assertEquals(Gapped.LATER, ids.find(5));
        assertEquals(Gapped.MIDDLE, ids.find(2));
    }

    @Test
    void find_returnsNullForAnIdNoConstantDeclares() {
        final StableIds<Gapped> ids = StableIds.of(Gapped.class);
        assertNull(ids.find(1));
        assertNull(ids.find(6));
        assertNull(ids.find(-1));
        assertNull(ids.find(1_000));
    }

    @Test
    void byId_fallsBackForAnIdNoConstantDeclares() {
        final StableIds<Gapped> ids = StableIds.of(Gapped.class);
        assertEquals(Gapped.MIDDLE, ids.byId(3, Gapped.MIDDLE));
        assertEquals(Gapped.LATER, ids.byId(5, Gapped.MIDDLE));
    }

    @Test
    void find_followsTheDeclaredIdNotTheDeclarationOrder() {
        // MIDDLE is declared last but carries 2, so reordering the constants never changes what a number reads back.
        final StableIds<Gapped> ids = StableIds.of(Gapped.class);
        assertEquals(Gapped.MIDDLE, ids.find(2));
    }

    @Test
    void of_rejectsAnIdTwoConstantsDeclare() {
        assertThrows(IllegalStateException.class, () -> StableIds.of(Twice.class));
    }

    @Test
    void of_rejectsAnIdThatDoesNotFitOneSignedByte() {
        assertThrows(IllegalStateException.class, () -> StableIds.of(TooHigh.class));
        assertThrows(IllegalStateException.class, () -> StableIds.of(Negative.class));
    }

    private enum Gapped implements IStableId {
        FIRST(0),
        LATER(5),
        MIDDLE(2);

        private final int id;

        Gapped(final int id) {
            this.id = id;
        }

        @Override
        public int id() {
            return id;
        }
    }

    private enum Twice implements IStableId {
        ONE,
        OTHER;

        @Override
        public int id() {
            return 1;
        }
    }

    private enum TooHigh implements IStableId {
        ONLY;

        @Override
        public int id() {
            return StableIds.MAX_ID + 1;
        }
    }

    private enum Negative implements IStableId {
        ONLY;

        @Override
        public int id() {
            return -1;
        }
    }
}
