/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.engine.prophet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class IProphetStatementTest {

    @Test
    void parse_keepAtLeastHasNoUpperEnd() {
        final IProphetStatement.Keep keep = assertInstanceOf(IProphetStatement.Keep.class,
                IProphetStatement.parse("KEEP steel_ingot >= 10000;"));

        assertEquals("steel_ingot", keep.item());
        assertEquals(10000L, keep.lower());
        assertEquals(IProphetStatement.UNBOUNDED, keep.upper());
    }

    @Test
    void parse_keepBetweenHasBothEnds() {
        final IProphetStatement.Keep keep = assertInstanceOf(IProphetStatement.Keep.class,
                IProphetStatement.parse("keep uranium_fuel between 500 and 1000"));

        assertEquals(500L, keep.lower());
        assertEquals(1000L, keep.upper());
    }

    @Test
    void parse_aBandUpsideDownIsMalformed() {
        assertEquals(new IProphetStatement.Malformed(IProphetStatement.BAD_KEEP),
                IProphetStatement.parse("KEEP coal BETWEEN 900 AND 100"));
        assertEquals(new IProphetStatement.Malformed(IProphetStatement.BAD_KEEP),
                IProphetStatement.parse("KEEP coal > lots"));
    }

    @Test
    void parse_watchKeepsItsActionAsWritten() {
        final IProphetStatement.Watch watch = assertInstanceOf(IProphetStatement.Watch.class,
                IProphetStatement.parse("WATCH redstone < 500 DO CRAFT redstone TO 1000"));

        assertEquals("redstone", watch.item());
        assertEquals(IProphetStatement.Comparison.BELOW, watch.comparison());
        assertEquals(500L, watch.threshold());
        assertEquals("CRAFT redstone TO 1000", watch.action());
    }

    @Test
    void parse_aWatchWithNoActionIsMalformed() {
        assertEquals(new IProphetStatement.Malformed(IProphetStatement.BAD_WATCH),
                IProphetStatement.parse("WATCH redstone < 500 DO"));
        assertEquals(new IProphetStatement.Malformed(IProphetStatement.BAD_WATCH),
                IProphetStatement.parse("WATCH redstone about 500 DO CRAFT 4 redstone"));
    }

    @Test
    void parse_forgetTakesAnItemOrAWatchsNumber() {
        assertEquals(new IProphetStatement.Forget("coal"), IProphetStatement.parse("FORGET coal"));
        assertEquals(new IProphetStatement.ForgetWatch(2), IProphetStatement.parse("FORGET WATCH 2"));
        assertEquals(new IProphetStatement.Malformed(IProphetStatement.BAD_FORGET),
                IProphetStatement.parse("FORGET WATCH none"));
    }

    @Test
    void parse_showStatesListsThem() {
        assertInstanceOf(IProphetStatement.ShowStates.class, IProphetStatement.parse("SHOW STATES"));
    }

    @Test
    void parse_theLanguagesCoreIsLeftAlone() {
        assertNull(IProphetStatement.parse("CRAFT 64 torch"));
        assertNull(IProphetStatement.parse("SHOW items"));
        assertNull(IProphetStatement.parse(""));
    }

    @Test
    void comparison_testsTheLevelAgainstItsThreshold() {
        assertTrue(IProphetStatement.Comparison.BELOW.test(4, 5));
        assertTrue(IProphetStatement.Comparison.AT_MOST.test(5, 5));
        assertTrue(IProphetStatement.Comparison.ABOVE.test(6, 5));
        assertTrue(IProphetStatement.Comparison.AT_LEAST.test(5, 5));
    }
}
