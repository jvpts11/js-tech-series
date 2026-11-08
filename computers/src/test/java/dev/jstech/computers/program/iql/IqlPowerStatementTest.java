/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.iql;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class IqlPowerStatementTest {

    @Test
    void isPowerUpdate_isAnUpdateOfThePowerTableAlone() {
        assertTrue(IqlPowerStatement.isPowerUpdate("UPDATE power SET on = no"));
        assertTrue(IqlPowerStatement.isPowerUpdate("update POWER set on = yes"));
        assertFalse(IqlPowerStatement.isPowerUpdate("UPDATE iron_sword SET REPAIR"));
        assertFalse(IqlPowerStatement.isPowerUpdate("QUERY power"));
        assertFalse(IqlPowerStatement.isPowerUpdate(null));
    }

    @Test
    void parse_readsOnAndOffInEveryWord() {
        assertTrue(IqlPowerStatement.parse("UPDATE power SET on = yes").on());
        assertTrue(IqlPowerStatement.parse("UPDATE power SET on = TRUE").on());
        assertTrue(IqlPowerStatement.parse("UPDATE power SET on = on").on());
        assertFalse(IqlPowerStatement.parse("UPDATE power SET on = no").on());
        assertFalse(IqlPowerStatement.parse("UPDATE power SET on = false").on());
        assertFalse(IqlPowerStatement.parse("UPDATE power SET on = OFF").on());
    }

    @Test
    void parse_withNoWhereSwitchesEveryBay() {
        assertNull(IqlPowerStatement.parse("UPDATE power SET on = no").where());
    }

    @Test
    void parse_readsTheWhereAsTheTablesRowsAreRead() {
        final IIqlCondition where = IqlPowerStatement.parse(
                "UPDATE power SET on = no WHERE server = 'VAULT-01' AND watts > 100").where();
        assertNotNull(where);
        final Map<String, String> vault = Map.of("server", "VAULT-01", "watts", "1080");
        final Map<String, String> other = Map.of("server", "ARCHIVE", "watts", "1080");
        final Map<String, String> idle = Map.of("server", "VAULT-01", "watts", "20");
        assertTrue(where.matches(vault::get));
        assertFalse(where.matches(other::get));
        assertFalse(where.matches(idle::get));
    }

    @Test
    void parse_refusesAnythingButSettingOn() {
        assertThrows(IqlError.class, () -> IqlPowerStatement.parse("UPDATE power"));
        assertThrows(IqlError.class, () -> IqlPowerStatement.parse("UPDATE power SET watts = 10"));
        assertThrows(IqlError.class, () -> IqlPowerStatement.parse("UPDATE power SET on maybe"));
        assertThrows(IqlError.class, () -> IqlPowerStatement.parse("UPDATE power SET on = maybe"));
        assertThrows(IqlError.class, () -> IqlPowerStatement.parse("UPDATE power SET on = no LIMIT 2"));
        assertThrows(IqlError.class, () -> IqlPowerStatement.parse("UPDATE power SET on = no WHERE row = 1 junk"));
    }

    @Test
    void tryParse_givesThePowerStatementNotAnItemUpdate() {
        final IqlParseResult parsed = IqlParser.tryParse("UPDATE power SET on = no WHERE row = 3");
        assertTrue(parsed.ok() && parsed.isPower());
        assertNull(parsed.operation());
        assertFalse(IqlParser.tryParse("UPDATE power SET on = maybe").ok());
        assertFalse(IqlParser.tryParse("UPDATE iron_sword SET REPAIR").isPower());
    }
}
