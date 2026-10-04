/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.iql;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IqlScriptTest {

    @Test
    void split_readsEachStatementUpToItsSemicolon() {
        final List<IqlScript.Statement> statements =
                IqlScript.split("QUERY items;\nCRAFT 64 iron_ingot;\nQUERY servers");
        assertEquals(3, statements.size());
        assertEquals("QUERY items", statements.get(0).text());
        assertEquals("CRAFT 64 iron_ingot", statements.get(1).text());
        assertEquals("QUERY servers", statements.get(2).text());
    }

    @Test
    void split_keepsWhereEachStatementStands() {
        final String script = "  QUERY items;\n  CRAFT 4 torch;";
        final List<IqlScript.Statement> statements = IqlScript.split(script);
        assertEquals(2, statements.get(0).start());
        assertEquals(13, statements.get(0).end());
        assertEquals(script.indexOf("CRAFT"), statements.get(1).start());
    }

    @Test
    void split_blanksCommentsWithoutMovingAnything() {
        final String script = "-- what is short\nQUERY items -- every item\nWHERE qty < 5;";
        final List<IqlScript.Statement> statements = IqlScript.split(script);
        assertEquals(1, statements.size());
        final IqlScript.Statement statement = statements.get(0);
        assertFalse(statement.text().contains("--"));
        assertEquals(script.indexOf("WHERE"), statement.start() + statement.text().indexOf("WHERE"));
    }

    @Test
    void split_leavesOutStatementsThatAreOnlyComments() {
        assertEquals(1, IqlScript.split("-- nothing here;\n;\nQUERY items;").size());
    }

    @Test
    void split_ignoresSemicolonsInsideQuotes() {
        final List<IqlScript.Statement> statements = IqlScript.split("QUERY items WHERE name = \"a;b\"; QUERY disks");
        assertEquals(2, statements.size());
        assertEquals("QUERY items WHERE name = \"a;b\"", statements.get(0).text());
    }

    @Test
    void split_keepsAProcedureBodyWhole() {
        final List<IqlScript.Statement> statements =
                IqlScript.split("CREATE PROCEDURE restock AS { CRAFT 64 torch; CRAFT 8 chest }; QUERY items");
        assertEquals(2, statements.size());
        assertTrue(statements.get(0).text().endsWith("}"));
    }

    @Test
    void at_findsTheStatementTheCaretIsIn() {
        final String script = "QUERY items;\nQUERY servers;";
        assertEquals("QUERY servers", IqlScript.at(script, script.indexOf("servers")).text());
        assertEquals("QUERY items", IqlScript.at(script, 3).text());
    }

    @Test
    void at_isNullForAScriptWithNoStatements() {
        assertNull(IqlScript.at("-- empty", 2));
    }

    @Test
    void tokenSpan_pointsAtTheTokenWithItsQuotes() {
        final String statement = "QUERY items WHERE name = \"oak log\"";
        assertArrayEquals(new int[] {6, 11}, IqlScript.tokenSpan(statement, 1));
        assertArrayEquals(new int[] {statement.indexOf('"'), statement.length()}, IqlScript.tokenSpan(statement, 5));
    }

    @Test
    void tokenSpan_isNullPastTheLastToken() {
        assertNull(IqlScript.tokenSpan("QUERY items", 2));
        assertNull(IqlScript.tokenSpan("QUERY items", -1));
    }

    @Test
    void destructive_asksForDroppedItemsAndDroppedObjects() {
        assertTrue(IqlScript.destructive("DROP 64 dirt"));
        assertTrue(IqlScript.destructive("DROP VIEW low_stock"));
        assertTrue(IqlScript.destructive("DROP PROCEDURE restock"));
    }

    @Test
    void destructive_asksForEverythingOfAnItemSentOut() {
        assertTrue(IqlScript.destructive("DELETE ALL cobblestone TO Trash"));
        assertFalse(IqlScript.destructive("DELETE 64 cobblestone TO Trash"));
    }

    @Test
    void destructive_letsReadsAndCraftsThrough() {
        assertFalse(IqlScript.destructive("QUERY items"));
        assertFalse(IqlScript.destructive("CRAFT 64 iron_ingot"));
        assertFalse(IqlScript.destructive("not a statement"));
    }

    @Test
    void parameters_readsEachParameterOnce() {
        final List<IqlScript.Parameter> parameters =
                IqlScript.parameters("CRAFT <count, number, 64> <item, name, torch>; CRAFT <count, number, 64> chest");
        assertEquals(2, parameters.size());
        assertEquals(new IqlScript.Parameter("count", "number", "64"), parameters.get(0));
        assertEquals("item", parameters.get(1).name());
    }

    @Test
    void parameters_doesNotMistakeComparisonsForParameters() {
        assertTrue(IqlScript.parameters("QUERY items WHERE qty < 256 AND qty > 3").isEmpty());
    }

    @Test
    void fill_putsTheValuesInAndDefaultsWhereNoneIsGiven() {
        assertEquals("CRAFT 16 torch",
                IqlScript.fill("CRAFT <count, number, 64> <item, name, torch>", Map.of("count", "16")));
    }

    @Test
    void csv_quotesTheCellsThatNeedIt() {
        assertEquals("item,qty\n\"oak, log\",4\n\"say \"\"hi\"\"\",1", IqlScript.csv(List.of("item", "qty"),
                List.of(List.of("oak, log", "4"), List.of("say \"hi\"", "1"))));
    }

    @Test
    void textTable_padsEveryColumnToItsWidestCell() {
        final String expected = "item" + " ".repeat(10) + "qty\n"
                + "-".repeat(12) + "  ---\n"
                + "iron_ingot    198\n"
                + "copper_ingot  204";
        assertEquals(expected, IqlScript.textTable(List.of("item", "qty"),
                List.of(List.of("iron_ingot", "198"), List.of("copper_ingot", "204"))));
    }

    @Test
    void keyword_knowsTheVerbsAndTheClauses() {
        assertTrue(IqlScript.keyword("query"));
        assertTrue(IqlScript.keyword("WHERE"));
        assertFalse(IqlScript.keyword("iron_ingot"));
    }
}
