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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.crafting.InterfaceScript;
import java.util.List;
import org.junit.jupiter.api.Test;

class IqlCraftingStatementTest {

    @Test
    void isCrafting_knowsItsStatementsAndNoOthers() {
        assertTrue(IqlCraftingStatement.isCrafting("SET INTERFACE 'Kiln A' EXCLUSIVE ON"));
        assertTrue(IqlCraftingStatement.isCrafting("set router North filter none"));
        assertTrue(IqlCraftingStatement.isCrafting("PAUSE INTERFACE 'Kiln A'"));
        assertTrue(IqlCraftingStatement.isCrafting("RESUME INTERFACE 'Kiln A'"));
        assertTrue(IqlCraftingStatement.isCrafting("RENAME ROUTER North TO South"));
        assertFalse(IqlCraftingStatement.isCrafting("SET BUS 'Ore in' OFF"));
        assertFalse(IqlCraftingStatement.isCrafting("PAUSE ROUTER North"));
        assertFalse(IqlCraftingStatement.isCrafting("SET"));
        assertFalse(IqlCraftingStatement.isCrafting(null));
    }

    @Test
    void parse_readsTheInterfaceSettings() {
        assertEquals(new IqlCraftingStatement(IqlCraftingStatement.Part.INTERFACE, "Joe's kiln",
                        new IqlCraftingStatement.Exclusive(true)),
                IqlCraftingStatement.parse("SET INTERFACE 'Joe''s kiln' EXCLUSIVE ON"));
        assertEquals(new IqlCraftingStatement.Exclusive(false), change("SET INTERFACE x EXCLUSIVE OFF"));
        assertEquals(new IqlCraftingStatement.MaxJobs(4), change("SET INTERFACE x MAX JOBS 4"));
        assertEquals(new IqlCraftingStatement.MaxJobs(0), change("SET INTERFACE x MAX JOBS AUTO"));
        assertEquals(new IqlCraftingStatement.Paused(true), change("PAUSE INTERFACE x"));
        assertEquals(new IqlCraftingStatement.Paused(false), change("RESUME INTERFACE x"));
    }

    @Test
    void parse_readsARouteToARouterAndBackToTheFilters() {
        assertEquals(new IqlCraftingStatement.Route("Coarse dirt", "gravel", "North"),
                change("SET INTERFACE 'Mixer' ROUTE 'Coarse dirt' INPUT gravel TO ROUTER 'North'"));
        assertEquals(new IqlCraftingStatement.Route("Coarse dirt", "gravel", null),
                change("SET INTERFACE 'Mixer' ROUTE 'Coarse dirt' INPUT gravel AUTO"));
    }

    @Test
    void parse_readsTheRenames() {
        final IqlCraftingStatement router = IqlCraftingStatement.parse("RENAME ROUTER North TO 'Gravel in'");

        assertEquals(IqlCraftingStatement.Part.ROUTER, router.part());
        assertEquals("North", router.name());
        assertEquals(new IqlCraftingStatement.Rename("Gravel in"), router.change());
        assertEquals(new IqlCraftingStatement.Rename("Kiln B"), change("RENAME INTERFACE 'Kiln A' TO 'Kiln B'"));
    }

    @Test
    void parse_readsARoutersSettingAsABusWritesIt() {
        assertEquals(new IqlCraftingStatement.RouterSetting(new IqlBusStatement.Filter(false, List.of("gravel"))),
                change("SET ROUTER 'North' FILTER ONLY gravel"));
        assertEquals(new IqlCraftingStatement.RouterSetting(new IqlBusStatement.Filter(false, List.of())),
                change("SET ROUTER 'North' FILTER NONE"));
        assertEquals(new IqlCraftingStatement.RouterSetting(new IqlBusStatement.Match(true)),
                change("SET ROUTER 'North' MATCH FUZZY"));
    }

    @Test
    void parse_refusesWhatIsNotOneOfItsStatements() {
        assertThrows(IqlError.class, () -> IqlCraftingStatement.parse("SET INTERFACE"));
        assertThrows(IqlError.class, () -> IqlCraftingStatement.parse("SET INTERFACE x"));
        assertThrows(IqlError.class, () -> IqlCraftingStatement.parse("SET INTERFACE x SPEED 4"));
        assertThrows(IqlError.class, () -> IqlCraftingStatement.parse("SET INTERFACE x MAX JOBS many"));
        assertThrows(IqlError.class, () -> IqlCraftingStatement.parse("SET INTERFACE x EXCLUSIVE ON OFF"));
        assertThrows(IqlError.class, () -> IqlCraftingStatement.parse("RENAME ROUTER North South"));
        assertThrows(IqlError.class, () -> IqlCraftingStatement.parse("SET ROUTER North SPEED 4"));
        assertThrows(IqlError.class, () -> IqlCraftingStatement.parse("SET INTERFACE x ROUTE p INPUT gravel TO"));
    }

    @Test
    void parse_readsBackWhatTheInterfacesWindowWrites() {
        final List<String> lines = InterfaceScript.iql("Kiln 'A'", true, 3, true);

        assertEquals(new IqlCraftingStatement.Exclusive(true), change(lines.get(0)));
        assertEquals(new IqlCraftingStatement.MaxJobs(3), change(lines.get(1)));
        assertEquals(new IqlCraftingStatement.Paused(true), change(lines.get(2)));
        assertEquals("Kiln 'A'", IqlCraftingStatement.parse(lines.get(0)).name());
        assertEquals(new IqlCraftingStatement.Route("Grass", "bone_meal", "North"),
                change(InterfaceScript.route("Kiln", "Grass", "bone_meal", "North")));
    }

    @Test
    void tryParse_givesTheStatementToTheEngine() {
        final IqlParseResult parsed = IqlParser.tryParse("PAUSE INTERFACE 'Kiln A'");

        assertTrue(parsed.ok() && parsed.isCrafting());
        assertFalse(IqlParser.tryParse("SET INTERFACE 'Kiln A' SPEED 2").ok());
    }

    private static IqlCraftingStatement.Change change(final String statement) {
        return IqlCraftingStatement.parse(statement).change();
    }
}
