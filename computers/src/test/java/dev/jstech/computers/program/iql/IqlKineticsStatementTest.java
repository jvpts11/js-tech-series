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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class IqlKineticsStatementTest {

    @Test
    void isKineticsUpdate_isAnUpdateOfTheKineticsTableAlone() {
        assertTrue(IqlKineticsStatement.isKineticsUpdate("UPDATE create_kinetics SET target_speed = 64"));
        assertTrue(IqlKineticsStatement.isKineticsUpdate("update CREATE_KINETICS set target_speed = 64"));
        assertFalse(IqlKineticsStatement.isKineticsUpdate("UPDATE power SET on = no"));
        assertFalse(IqlKineticsStatement.isKineticsUpdate("QUERY create_kinetics"));
        assertFalse(IqlKineticsStatement.isKineticsUpdate(null));
    }

    @Test
    void parse_readsTheSpeedEitherWay() {
        assertEquals(64.0, IqlKineticsStatement.parse("UPDATE create_kinetics SET target_speed = 64").speed());
        assertEquals(-32.0, IqlKineticsStatement.parse("UPDATE create_kinetics SET target_speed = -32").speed());
        assertEquals(12.5, IqlKineticsStatement.parse("UPDATE create_kinetics SET target_speed = 12.5").speed());
    }

    @Test
    void parse_withNoWhereSetsEveryController() {
        assertNull(IqlKineticsStatement.parse("UPDATE create_kinetics SET target_speed = 64").where());
    }

    @Test
    void parse_readsTheWhereAsTheTablesRowsAreRead() {
        final IIqlCondition where = IqlKineticsStatement.parse(
                "UPDATE create_kinetics SET target_speed = 64 WHERE block = 'Rotation Speed Controller'"
                        + " AND stress < 100").where();
        assertNotNull(where);
        final Map<String, String> calm = Map.of("block", "Rotation Speed Controller", "stress", "40");
        final Map<String, String> loaded = Map.of("block", "Rotation Speed Controller", "stress", "400");
        final Map<String, String> motor = Map.of("block", "Creative Motor", "stress", "40");
        assertTrue(where.matches(calm::get));
        assertFalse(where.matches(loaded::get));
        assertFalse(where.matches(motor::get));
    }

    @Test
    void parse_refusesAnythingButSettingTheTargetSpeed() {
        assertThrows(IqlError.class, () -> IqlKineticsStatement.parse("UPDATE create_kinetics"));
        assertThrows(IqlError.class, () -> IqlKineticsStatement.parse("UPDATE create_kinetics SET speed = 64"));
        assertThrows(IqlError.class, () -> IqlKineticsStatement.parse("UPDATE create_kinetics SET target_speed 64"));
        assertThrows(IqlError.class, () -> IqlKineticsStatement.parse("UPDATE create_kinetics SET target_speed = -"));
        assertThrows(IqlError.class,
                () -> IqlKineticsStatement.parse("UPDATE create_kinetics SET target_speed = fast"));
        assertThrows(IqlError.class,
                () -> IqlKineticsStatement.parse("UPDATE create_kinetics SET target_speed = 64 LIMIT 2"));
        assertThrows(IqlError.class, () -> IqlKineticsStatement.parse(
                "UPDATE create_kinetics SET target_speed = 64 WHERE speed = 1 junk"));
        assertThrows(IqlError.class, () -> IqlKineticsStatement.parse("UPDATE create_kinetics SET target_speed = NaN"));
        assertThrows(IqlError.class,
                () -> IqlKineticsStatement.parse("UPDATE create_kinetics SET target_speed = -Infinity"));
    }

    @Test
    void tryParse_givesTheKineticsStatementNotAnItemUpdate() {
        final IqlParseResult parsed = IqlParser.tryParse("UPDATE create_kinetics SET target_speed = 64");
        assertTrue(parsed.ok() && parsed.isKinetics());
        assertNull(parsed.operation());
        assertFalse(IqlParser.tryParse("UPDATE create_kinetics SET target_speed = fast").ok());
        assertFalse(IqlParser.tryParse("UPDATE iron_sword SET REPAIR").isKinetics());
    }
}
