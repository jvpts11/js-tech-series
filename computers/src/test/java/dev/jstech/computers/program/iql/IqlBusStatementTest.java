/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.iql;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.bus.BusCondition;
import dev.jstech.computers.bus.BusScript;
import dev.jstech.computers.bus.BusSettings;
import dev.jstech.core.tier.HardwareEra;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class IqlBusStatementTest {

    @Test
    void parse_readsTheNameInSingleQuotes() {
        final IqlBusStatement statement = IqlBusStatement.parse("SET BUS 'Joe''s bus' OFF");

        assertEquals("Joe's bus", statement.bus());
        assertEquals(new IqlBusStatement.Power(false), statement.change());
    }

    @Test
    void parse_readsTheFilterBothWaysAndItsTags() {
        assertEquals(new IqlBusStatement.Filter(false, List.of("iron_ore", "coal")),
                change("SET BUS 'Ore in' FILTER ONLY iron_ore, coal"));
        assertEquals(new IqlBusStatement.Filter(true, List.of("dirt")), change("SET BUS x FILTER ALL BUT dirt"));
        assertEquals(new IqlBusStatement.Tags(List.of("c:ores", "c:logs")),
                change("SET BUS x FILTER TAG c:ores, c:logs"));
        assertEquals(new IqlBusStatement.Filter(false, List.of("FLUID water", "log")),
                change("SET BUS x FILTER ONLY FLUID water, log"));
    }

    @Test
    void parse_readsTheQuantitiesForTheBusAndForAnItem() {
        assertEquals(new IqlBusStatement.Quantities(16, 64), change("SET BUS x KEEP 16 MAX 64"));
        assertEquals(new IqlBusStatement.Quantities(IqlBusStatement.UNCHANGED, 8), change("SET BUS x MAX 8"));
        assertEquals(new IqlBusStatement.ItemQuantities("iron_ore", 16, 64),
                change("SET BUS x KEEP iron_ore 16 MAX iron_ore 64"));
        assertEquals(new IqlBusStatement.ItemQuantities("log", IqlBusStatement.UNCHANGED, 8),
                change("SET BUS x MAX log 8"));
    }

    @Test
    void parse_readsTheConditions() {
        assertEquals(new IqlBusStatement.Stock("iron_ore", 512), change("SET BUS x WHEN STOCK iron_ore < 512"));
        assertEquals(new IqlBusStatement.Stock("#c:ores", 4096), change("SET BUS x WHEN STOCK TAG c:ores < 4096"));
        assertEquals(new IqlBusStatement.Hours(18, 6), change("SET BUS x WHEN TIME BETWEEN 18:00 AND 06:00"));
        assertEquals(new IqlBusStatement.After("Coal in"), change("SET BUS x AFTER BUS 'Coal in'"));
    }

    @Test
    void parse_readsModePriorityMatchAndAccess() {
        assertEquals(new IqlBusStatement.Mode(true), change("SET BUS x MODE ON DEMAND"));
        assertEquals(new IqlBusStatement.Priority(-2), change("SET BUS x PRIORITY -2"));
        assertEquals(new IqlBusStatement.Match(true), change("SET BUS x MATCH FUZZY"));
        assertEquals(new IqlBusStatement.Access(BusSettings.WRITE_ONLY), change("SET BUS x ACCESS WRITE ONLY"));
        assertEquals(new IqlBusStatement.Access(BusSettings.READ_WRITE), change("SET BUS x ACCESS READ AND WRITE"));
    }

    @Test
    void parse_refusesWhatABusHasNot() {
        assertThrows(IqlError.class, () -> IqlBusStatement.parse("SET BUS x COLOUR red"));
        assertThrows(IqlError.class, () -> IqlBusStatement.parse("SET BUS x"));
        assertThrows(IqlError.class, () -> IqlBusStatement.parse("SET BUS x WHEN TIME BETWEEN 25:00 AND 06:00"));
        assertThrows(IqlError.class, () -> IqlBusStatement.parse("SET BUS x KEEP 16 trailing"));
    }

    @Test
    void tryParse_givesASetBusStatementItsOwnResult() {
        final IqlParseResult result = IqlParser.tryParse("SET BUS 'Ore in' PRIORITY 5");

        assertTrue(result.ok());
        assertTrue(result.isBus());
        assertFalse(result.isDefinition());
        assertFalse(IqlParser.tryParse("SET BUS 'Ore in' NOTHING").ok());
    }

    @Test
    void hoursOf_readsTheTimeOfAJobThatSwitchesABusOn() {
        assertArrayEquals(new int[] {18, 6}, IqlBusStatement.hoursOf("TIME BETWEEN 18:00 AND 06:00"));
        assertNull(IqlBusStatement.hoursOf("qty(iron_ore) < 64"));
    }

    @Test
    void everyLineTheSoftwareTabWritesReadsBack() {
        final BusSettings standard = new BusSettings("Ore in", HardwareEra.STANDARD,
                List.of("item|minecraft:iron_ore", "item|minecraft:coal", "", "", ""), true, 16, 64,
                List.of(0, 0, 0, 0, 0), List.of(0, 0, 0, 0, 0), 5, List.of(BusCondition.stock("minecraft:iron_ore",
                512), BusCondition.hours(18, 6), BusCondition.after("Coal in")), List.of(), false, false, true,
                Map.of(BusSettings.CONDITIONS, "Night shift"));
        final BusSettings advanced = new BusSettings("Ore in", HardwareEra.ADVANCED, List.of("", "", "", "", ""),
                false, 0, 0, List.of(0, 0, 0, 0, 0), List.of(0, 0, 0, 0, 0), 0,
                List.of(BusCondition.stock("#c:ores", 4096)), List.of("c:ores"), true, true, false, Map.of());
        final BusSettings transition = new BusSettings("Ore in", HardwareEra.TRANSITION,
                List.of("item|minecraft:iron_ore", "", "", "", ""), false, 0, 0, List.of(16, 0, 0, 0, 0),
                List.of(64, 0, 0, 0, 0), 0, List.of(), List.of(), false, true, false, Map.of());
        final BusSettings external = new BusSettings("Chest wall", HardwareEra.STANDARD,
                List.of("item|minecraft:log", "", "", "", ""), false, 0, 0, List.of(0, 0, 0, 0, 0),
                List.of(0, 0, 0, 0, 0), 2, List.of(), List.of(), false, true, false, Map.of(), true,
                BusSettings.READ_ONLY);
        for (final BusSettings settings : List.of(standard, advanced, transition, external)) {
            for (final String line : BusScript.iql(settings)) {
                if (line.startsWith("CREATE JOB")) {
                    final String trigger = line.substring(line.indexOf(" WHEN ") + " WHEN ".length());
                    assertArrayEquals(new int[] {18, 6}, IqlBusStatement.hoursOf(trigger), line);
                } else {
                    assertTrue(IqlParser.tryParse(line).isBus(), line + ": " + IqlParser.tryParse(line).error());
                }
            }
        }
    }

    private static IqlBusStatement.Change change(final String statement) {
        return IqlBusStatement.parse(statement).change();
    }

    @Test
    void parse_refusesCountsThatDoNotFitAnIntOrAreNegative() {
        assertThrows(IqlError.class, () -> IqlBusStatement.parse("SET BUS x MAX 4294967297"));
        assertThrows(IqlError.class, () -> IqlBusStatement.parse("SET BUS x KEEP -1"));
        assertThrows(IqlError.class, () -> IqlBusStatement.parse("SET BUS x PRIORITY -1"));
    }
}
