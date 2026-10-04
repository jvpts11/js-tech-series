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

class ProphetStatementTest {

    @Test
    void parse_keepAtLeastHasNoUpperEnd() {
        final ProphetStatement.Keep keep = assertInstanceOf(ProphetStatement.Keep.class,
                ProphetStatement.parse("KEEP steel_ingot >= 10000;"));

        assertEquals("steel_ingot", keep.item());
        assertEquals(10000L, keep.lower());
        assertEquals(ProphetStatement.UNBOUNDED, keep.upper());
    }

    @Test
    void parse_keepBetweenHasBothEnds() {
        final ProphetStatement.Keep keep = assertInstanceOf(ProphetStatement.Keep.class,
                ProphetStatement.parse("keep uranium_fuel between 500 and 1000"));

        assertEquals(500L, keep.lower());
        assertEquals(1000L, keep.upper());
    }

    @Test
    void parse_aBandUpsideDownIsMalformed() {
        assertEquals(new ProphetStatement.Malformed(ProphetStatement.BAD_KEEP),
                ProphetStatement.parse("KEEP coal BETWEEN 900 AND 100"));
        assertEquals(new ProphetStatement.Malformed(ProphetStatement.BAD_KEEP),
                ProphetStatement.parse("KEEP coal > lots"));
    }

    @Test
    void parse_watchKeepsItsActionAsWritten() {
        final ProphetStatement.Watch watch = assertInstanceOf(ProphetStatement.Watch.class,
                ProphetStatement.parse("WATCH redstone < 500 DO CRAFT redstone TO 1000"));

        assertEquals("redstone", watch.item());
        assertEquals(ProphetStatement.Comparison.BELOW, watch.comparison());
        assertEquals(500L, watch.threshold());
        assertEquals("CRAFT redstone TO 1000", watch.action());
    }

    @Test
    void parse_aWatchWithNoActionIsMalformed() {
        assertEquals(new ProphetStatement.Malformed(ProphetStatement.BAD_WATCH),
                ProphetStatement.parse("WATCH redstone < 500 DO"));
        assertEquals(new ProphetStatement.Malformed(ProphetStatement.BAD_WATCH),
                ProphetStatement.parse("WATCH redstone about 500 DO CRAFT 4 redstone"));
    }

    @Test
    void parse_forgetTakesAnItemOrAWatchsNumber() {
        assertEquals(new ProphetStatement.Forget("coal"), ProphetStatement.parse("FORGET coal"));
        assertEquals(new ProphetStatement.ForgetWatch(2), ProphetStatement.parse("FORGET WATCH 2"));
        assertEquals(new ProphetStatement.Malformed(ProphetStatement.BAD_FORGET),
                ProphetStatement.parse("FORGET WATCH none"));
    }

    @Test
    void parse_showStatesListsThem() {
        assertInstanceOf(ProphetStatement.ShowStates.class, ProphetStatement.parse("SHOW STATES"));
    }

    @Test
    void parse_theLanguagesCoreIsLeftAlone() {
        assertNull(ProphetStatement.parse("CRAFT 64 torch"));
        assertNull(ProphetStatement.parse("SHOW items"));
        assertNull(ProphetStatement.parse(""));
    }

    @Test
    void comparison_testsTheLevelAgainstItsThreshold() {
        assertTrue(ProphetStatement.Comparison.BELOW.test(4, 5));
        assertTrue(ProphetStatement.Comparison.AT_MOST.test(5, 5));
        assertTrue(ProphetStatement.Comparison.ABOVE.test(6, 5));
        assertTrue(ProphetStatement.Comparison.AT_LEAST.test(5, 5));
    }
}
