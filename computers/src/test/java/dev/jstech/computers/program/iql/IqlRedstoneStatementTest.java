/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.iql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class IqlRedstoneStatementTest {

    @Test
    void parse_readsInAndOutWithItsStrength() {
        assertEquals(new IqlRedstoneStatement("Gate", false, 0), IqlRedstoneStatement.parse("SET REDSTONE 'Gate' IN"));
        assertEquals(new IqlRedstoneStatement("Gate", true, 15),
                IqlRedstoneStatement.parse("SET REDSTONE 'Gate' OUT 15"));
        assertEquals(new IqlRedstoneStatement("Gate", true, 0), IqlRedstoneStatement.parse("SET REDSTONE Gate OUT 0"));
    }

    @Test
    void parse_takesItsWordsInAnyLetters() {
        assertEquals(new IqlRedstoneStatement("Back door", true, 7),
                IqlRedstoneStatement.parse("set redstone 'Back door' out 7"));
    }

    @Test
    void parse_readsANameWithAQuoteInIt() {
        assertEquals("Joe's gate", IqlRedstoneStatement.parse("SET REDSTONE 'Joe''s gate' IN").target());
    }

    @Test
    void parse_refusesAMissingNameOrMode() {
        assertThrows(IqlError.class, () -> IqlRedstoneStatement.parse("SET REDSTONE"));
        assertThrows(IqlError.class, () -> IqlRedstoneStatement.parse("SET REDSTONE 'Gate'"));
        assertThrows(IqlError.class, () -> IqlRedstoneStatement.parse("SET REDSTONE 'Gate' SIDEWAYS"));
        assertThrows(IqlError.class, () -> IqlRedstoneStatement.parse("SET REDSTONE 'Gate' OUT"));
    }

    @Test
    void parse_refusesAStrengthRedstoneDoesNotHave() {
        final IqlError tooStrong =
                assertThrows(IqlError.class, () -> IqlRedstoneStatement.parse("SET REDSTONE 'Gate' OUT 16"));
        assertTrue(tooStrong.getMessage().contains("16"), tooStrong.getMessage());
        assertThrows(IqlError.class, () -> IqlRedstoneStatement.parse("SET REDSTONE 'Gate' OUT high"));
        assertThrows(IqlError.class, () -> IqlRedstoneStatement.parse("SET REDSTONE 'Gate' OUT 1.5"));
    }

    @Test
    void parse_refusesWhatComesAfterTheStatement() {
        assertThrows(IqlError.class, () -> IqlRedstoneStatement.parse("SET REDSTONE 'Gate' IN 4"));
        assertThrows(IqlError.class, () -> IqlRedstoneStatement.parse("SET REDSTONE 'Gate' OUT 4 NOW"));
    }

    @Test
    void isSetRedstone_tellsItFromOtherStatements() {
        assertTrue(IqlRedstoneStatement.isSetRedstone("SET REDSTONE 'Gate' IN"));
        assertTrue(IqlRedstoneStatement.isSetRedstone("set redstone"));
        assertFalse(IqlRedstoneStatement.isSetRedstone("SET BUS 'Gate' ON"));
        assertFalse(IqlRedstoneStatement.isSetRedstone("SELECT 5 iron_ingot"));
        assertFalse(IqlRedstoneStatement.isSetRedstone(null));
    }

    @Test
    void tryParse_givesARedstoneResultOrAnError() {
        final IqlParseResult set = IqlParser.tryParse("SET REDSTONE 'Gate' OUT 3");
        assertTrue(set.ok());
        assertTrue(set.isRedstone());
        assertFalse(set.isRead());
        assertNull(set.operation());
        assertEquals(new IqlRedstoneStatement("Gate", true, 3), set.redstone());

        final IqlParseResult wrong = IqlParser.tryParse("SET REDSTONE 'Gate' OUT 99");
        assertFalse(wrong.ok());
    }

    @Test
    void isRead_isTrueOnlyForAQueryOrACount() {
        assertTrue(IqlParser.tryParse("QUERY items").isRead());
        assertFalse(IqlParser.tryParse("SELECT 64 iron_ingot").isRead());
        assertFalse(IqlParser.tryParse("SET BUS 'Ore in' ON").isRead());
    }
}
