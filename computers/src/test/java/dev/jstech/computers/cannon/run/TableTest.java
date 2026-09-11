/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.cannon.run;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TableTest {

    private Values.Table table;

    @BeforeEach
    void setUp() {
        this.table = new Values.Table();
    }

    private List<Object> keys() {
        final List<Object> keys = new ArrayList<>();
        for (Object key = this.table.nextKey(null); key != null; key = this.table.nextKey(key)) {
            keys.add(key);
        }
        return keys;
    }

    @Test
    void put_keepsARunFromOneAsASequence() {
        for (long i = 1; i <= 5; i++) {
            this.table.put(i, "v" + i);
        }
        assertEquals(5, this.table.runLength());
        assertEquals(5L, this.table.length());
        assertEquals("v3", this.table.get(3L));
    }

    @Test
    void put_treatsAWholeRealAsTheWholeNumber() {
        this.table.put(1.0, "one");
        assertEquals("one", this.table.get(1L));
        this.table.put(-0.0, "zero");
        assertEquals("zero", this.table.get(0L));
        this.table.put(1.5, "half");
        assertEquals("half", this.table.get(1.5));
    }

    @Test
    void put_joinsKeysKeptApartOnceTheRunReachesThem() {
        this.table.put(3L, "c");
        this.table.put(2L, "b");
        assertEquals(0, this.table.runLength());
        this.table.put(1L, "a");
        assertEquals(3, this.table.runLength(), "2 and 3 moved into the run");
        assertEquals(3L, this.table.length());
    }

    @Test
    void length_findsABorderWhenTheEndOfTheRunIsEmptied() {
        for (long i = 1; i <= 4; i++) {
            this.table.put(i, i);
        }
        this.table.put(4L, null);
        assertEquals(3L, this.table.length());
    }

    @Test
    void nextKey_walksTheRunThenTheRestInTheOrderTheyCame() {
        this.table.put("b", 1L);
        this.table.put(1L, "x");
        this.table.put("a", 2L);
        assertEquals(List.of(1L, "b", "a"), this.keys());
    }

    @Test
    void nextKey_keepsItsFootingWhenKeysAreTakenOutOnTheWay() {
        this.table.put("a", 1L);
        this.table.put("b", 2L);
        this.table.put("c", 3L);
        final List<Object> seen = new ArrayList<>();
        for (Object key = this.table.nextKey(null); key != null; key = this.table.nextKey(key)) {
            seen.add(key);
            this.table.put(key, null);
        }
        assertEquals(List.of("a", "b", "c"), seen);
        assertNull(this.table.nextKey(null));
    }

    @Test
    void knows_saysWhetherAKeyIsThereEvenAfterItWasEmptied() {
        this.table.put("k", 1L);
        this.table.put("k", null);
        assertTrue(this.table.knows("k"));
        assertFalse(this.table.knows("other"));
    }
}
