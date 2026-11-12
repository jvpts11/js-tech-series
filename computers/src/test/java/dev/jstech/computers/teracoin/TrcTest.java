/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.teracoin;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TrcTest {

    @Test
    void format_groupsThousandsAndShowsEightPlaces() {
        assertEquals("1,284.53120000", Trc.format(128_453_120_000L));
    }

    @Test
    void format_cutsThePlacesBeyondThoseAsked() {
        assertEquals("12.4053", Trc.format(1_240_539_999L, 4));
        assertEquals("12", Trc.format(1_240_539_999L, 0));
    }

    @Test
    void plain_dropsThePlacesAPriceDoesNotNeed() {
        assertEquals("10", Trc.plain(Trc.of(10)));
        assertEquals("241.43", Trc.plain(24_143_000_000L));
        assertEquals("1,200", Trc.plain(Trc.of(1200)));
        assertEquals("0.00000001", Trc.plain(1L));
        assertEquals("0", Trc.plain(0L));
    }

    @Test
    void signed_writesThePlusOfMoneyComingIn() {
        assertEquals("+41.43000000", Trc.signed(4_143_000_000L, 8));
        assertEquals("-100.00000000", Trc.signed(-Trc.of(100), 8));
    }

    @Test
    void parse_readsWholeAndFractionalAmounts() {
        assertEquals(Trc.of(100), Trc.parse("100"));
        assertEquals(Trc.of(1_000) + 50_000_000L, Trc.parse("1,000.5"));
        assertEquals(1L, Trc.parse("0.00000001"));
        assertEquals(50_000_000L, Trc.parse(".5"));
    }

    @Test
    void parse_refusesWhatIsNoAmount() {
        assertEquals(-1L, Trc.parse(""));
        assertEquals(-1L, Trc.parse("."));
        assertEquals(-1L, Trc.parse("-3"));
        assertEquals(-1L, Trc.parse("1.000000001"));
        assertEquals(-1L, Trc.parse("12a"));
        assertEquals(-1L, Trc.parse("1234567890123"));
        // Twelve digits and fewer whose units are past what a count holds wrapped round to some other amount.
        assertEquals(-1L, Trc.parse("99999999999"));
        assertEquals(-1L, Trc.parse("184467440738"));
        assertEquals(Trc.of(92_233_720_368L), Trc.parse("92233720368"));
    }
}
