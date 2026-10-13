/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.bus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.tier.HardwareEra;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BusScriptTest {

    @Test
    void address_putsTheNameAfterTheScheme() {
        assertEquals("bus://Ore in", BusScript.address("Ore in"));
    }

    @Test
    void routerAddress_putsTheNameAfterTheRouterScheme() {
        assertEquals("router://North", BusScript.routerAddress("North"));
    }

    @Test
    void routerIql_writesAnEmptyFilterAsTakingAnything() {
        assertEquals(List.of("SET ROUTER 'North' FILTER NONE"),
                BusScript.routerIql(named(BusSettings.fresh(HardwareEra.STANDARD), "North")));
        assertEquals(List.of("craftRouter(\"North\").Any();"),
                BusScript.routerSigma(named(BusSettings.fresh(HardwareEra.STANDARD), "North")));
    }

    @Test
    void routerIql_writesItsFilterAndNoneOfAMoversSettings() {
        final BusSettings router = new BusSettings("North", HardwareEra.STANDARD,
                List.of("item|minecraft:gravel", "item|minecraft:sand", "", "", ""), false, 16, 64,
                List.of(0, 0, 0, 0, 0), List.of(0, 0, 0, 0, 0), 5, List.of(), List.of(), false, true, true, Map.of());

        assertEquals(List.of("SET ROUTER 'North' FILTER ONLY gravel, sand"), BusScript.routerIql(router));
        assertEquals(List.of("craftRouter(\"North\").Only(\"gravel, sand\");"), BusScript.routerSigma(router));
    }

    @Test
    void iql_writesAFreshBusAsItsModeAlone() {
        assertEquals(List.of("SET BUS 'Ore in' MODE CONTINUOUS"),
                BusScript.iql(named(BusSettings.fresh(HardwareEra.LEGACY), "Ore in")));
    }

    @Test
    void iql_writesTheLegacyFilterAndQuantities() {
        final BusSettings legacy = legacy(List.of("item|minecraft:iron_ore", "item|minecraft:coal", "", "", ""),
                false, 16, 64);

        assertEquals(List.of("SET BUS 'Ore in' FILTER ONLY iron_ore, coal", "SET BUS 'Ore in' KEEP 16 MAX 64",
                "SET BUS 'Ore in' MODE CONTINUOUS"), BusScript.iql(legacy));
    }

    @Test
    void iql_writesAllButWhenTheFilterExcludes() {
        final BusSettings legacy = legacy(List.of("item|minecraft:dirt", "", "", "", ""), true, 0, 0);

        assertEquals("SET BUS 'Ore in' FILTER ALL BUT dirt", BusScript.iql(legacy).get(0));
    }

    @Test
    void iql_writesWhatTheEraCannotBeSetToNot() {
        final BusSettings vintage = with(BusSettings.fresh(HardwareEra.VINTAGE), "Ore in", 5,
                List.of(BusCondition.after("Coal in")));

        assertEquals(List.of("SET BUS 'Ore in' MODE CONTINUOUS"), BusScript.iql(vintage));
    }

    @Test
    void iql_writesTheStandardsPriorityAndConditions() {
        final BusSettings standard = with(BusSettings.fresh(HardwareEra.STANDARD), "Ore in", 5,
                List.of(BusCondition.stock("minecraft:iron_ore", 512), BusCondition.hours(18, 6),
                        BusCondition.after("Coal in")));
        final List<String> lines = BusScript.iql(standard);

        assertTrue(lines.contains("SET BUS 'Ore in' PRIORITY 5"));
        assertTrue(lines.contains("SET BUS 'Ore in' WHEN STOCK iron_ore < 512"));
        assertTrue(lines.contains("CREATE JOB night_shift AS SET BUS 'Ore in' ON WHEN TIME BETWEEN 18:00 AND 06:00"));
        assertTrue(lines.contains("SET BUS 'Ore in' AFTER BUS 'Coal in'"));
    }

    @Test
    void iql_writesTheTransitionsQuantitiesItemByItem() {
        final BusSettings transition = new BusSettings("Ore in", HardwareEra.TRANSITION,
                List.of("item|minecraft:iron_ore", "item|minecraft:log", "", "", ""), false, 0, 0,
                List.of(16, 8, 0, 0, 0), List.of(64, 0, 0, 0, 0), 0, List.of(), List.of(), false, true, false,
                Map.of());

        assertEquals(List.of("SET BUS 'Ore in' FILTER ONLY iron_ore, log",
                "SET BUS 'Ore in' KEEP iron_ore 16 MAX iron_ore 64", "SET BUS 'Ore in' KEEP log 8",
                "SET BUS 'Ore in' MODE CONTINUOUS"), BusScript.iql(transition));
    }

    @Test
    void iql_writesTheAdvancedTagsAndLooseMatch() {
        final BusSettings advanced = new BusSettings("Ore in", HardwareEra.ADVANCED, List.of("", "", "", "", ""),
                false, 0, 0, List.of(0, 0, 0, 0, 0), List.of(0, 0, 0, 0, 0), 0,
                List.of(BusCondition.stock("#c:ores", 4096)), List.of("c:ores", "c:raw_materials"), true, true, false,
                Map.of());
        final List<String> lines = BusScript.iql(advanced);

        assertTrue(lines.contains("SET BUS 'Ore in' FILTER TAG c:ores, c:raw_materials"));
        assertTrue(lines.contains("SET BUS 'Ore in' MATCH FUZZY"));
        assertTrue(lines.contains("SET BUS 'Ore in' WHEN STOCK TAG c:ores < 4096"));
    }

    @Test
    void iql_doublesAQuoteInTheName() {
        assertEquals("SET BUS 'Joe''s bus' MODE CONTINUOUS",
                BusScript.iql(named(BusSettings.fresh(HardwareEra.LEGACY), "Joe's bus")).get(0));
    }

    @Test
    void iql_writesAnOffBusAndItsDemand() {
        final BusSettings vintage = new BusSettings("Ore in", HardwareEra.VINTAGE, List.of("", "", "", "", ""),
                false, 0, 0, List.of(0, 0, 0, 0, 0), List.of(0, 0, 0, 0, 0), 0, List.of(), List.of(), false, false,
                true, Map.of());

        assertEquals(List.of("SET BUS 'Ore in' OFF", "SET BUS 'Ore in' MODE ON DEMAND"), BusScript.iql(vintage));
    }

    @Test
    void sigma_chainsASingleCallOnTheBus() {
        final BusSettings legacy = legacy(List.of("", "", "", "", ""), false, 16, 64);

        assertEquals(List.of("bus(\"Ore in\").Keep(16).Max(64);"), BusScript.sigma(legacy));
    }

    @Test
    void sigma_holdsTheBusInAVariableForSeveralCalls() {
        final BusSettings standard = with(BusSettings.fresh(HardwareEra.STANDARD), "Ore in", 5,
                List.of(BusCondition.after("Coal in")));

        assertEquals(List.of("Bus b = bus(\"Ore in\");", "b.Priority(5);", "b.After(bus(\"Coal in\"));"),
                BusScript.sigma(standard));
    }

    @Test
    void sigma_writesAFreshBusAsSwitchedOn() {
        assertEquals(List.of("bus(\"Ore in\").On();"),
                BusScript.sigma(named(BusSettings.fresh(HardwareEra.VINTAGE), "Ore in")));
    }

    @Test
    void sigma_escapesAQuoteInTheName() {
        final List<String> lines = BusScript.sigma(named(BusSettings.fresh(HardwareEra.VINTAGE), "a\"b"));

        assertEquals("bus(\"a\\\"b\").On();", lines.get(0));
    }

    @Test
    void iql_writesAnExternalBusesAccessAndNoMode() {
        final BusSettings external = new BusSettings("Chest wall", HardwareEra.STANDARD,
                List.of("item|minecraft:log", "", "", "", ""), false, 0, 0, List.of(0, 0, 0, 0, 0),
                List.of(0, 0, 0, 0, 0), 2, List.of(), List.of(), false, true, false, Map.of(), true,
                BusSettings.READ_ONLY);

        assertEquals(List.of("SET BUS 'Chest wall' FILTER ONLY log", "SET BUS 'Chest wall' ACCESS READ ONLY",
                "SET BUS 'Chest wall' PRIORITY 2"), BusScript.iql(external));
        assertEquals(List.of("Bus b = bus(\"Chest wall\");", "b.Only(\"log\");", "b.ReadOnly();", "b.Priority(2);"),
                BusScript.sigma(external));
    }

    @Test
    void iql_writesNothingForAVintageExternalBus() {
        final BusSettings vintage = BusSettings.freshExternal(HardwareEra.VINTAGE);

        assertTrue(BusScript.iql(vintage).isEmpty());
        assertTrue(BusScript.sigma(vintage).isEmpty());
    }

    @Test
    void shortId_dropsTheVanillaNamespaceAndNamesOtherKinds() {
        assertEquals("iron_ore", BusScript.shortId("item|minecraft:iron_ore"));
        assertEquals("create:brass_ingot", BusScript.shortId("item|create:brass_ingot"));
        assertEquals("FLUID water", BusScript.shortId("fluid|minecraft:water"));
        assertFalse(BusScript.shortId("chemical|mekanism:oxygen").isEmpty());
    }

    @Test
    void hour_padsTheHour() {
        assertEquals("06:00", BusScript.hour(6));
        assertEquals("18:00", BusScript.hour(18));
    }

    private static BusSettings legacy(final List<String> filter, final boolean exclude, final int keep,
                                      final int max) {
        return new BusSettings("Ore in", HardwareEra.LEGACY, filter, exclude, keep, max, List.of(0, 0, 0, 0, 0),
                List.of(0, 0, 0, 0, 0), 0, List.of(), List.of(), false, true, false, Map.of());
    }

    private static BusSettings named(final BusSettings settings, final String name) {
        return new BusSettings(name, settings.era(), settings.filter(), settings.exclude(), settings.keep(),
                settings.max(), settings.itemKeep(), settings.itemMax(), settings.priority(), settings.conditions(),
                settings.tags(), settings.fuzzy(), settings.powered(), settings.onDemand(), settings.setBy());
    }

    private static BusSettings with(final BusSettings settings, final String name, final int priority,
                                    final List<BusCondition> conditions) {
        return new BusSettings(name, settings.era(), settings.filter(), settings.exclude(), settings.keep(),
                settings.max(), settings.itemKeep(), settings.itemMax(), priority, conditions, settings.tags(),
                settings.fuzzy(), settings.powered(), settings.onDemand(),
                Map.of(BusSettings.CONDITIONS, "Night shift"));
    }
}
