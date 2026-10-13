/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.core.text.TextKey;
import java.util.List;

/**
 * The statements the IQL Server Management Studio writes for its player: the templates of its Template Explorer, and
 * what the menu of each Object Explorer node puts in a new query. They are the network's language, which reads the
 * same in every language; a template's parameters are written {@code <name, type, default>}, for Specify Values for
 * Template Parameters to fill.
 */
final class IsmsTemplates {

    /** The templates, in the order the Template Explorer lists them. */
    static final List<Template> ALL = List.of(
            new Template(IsmsTexts.TEMPLATE_QUERY, "QUERY <table, table, items>\n"
                    + "WHERE <column, column, qty> < <value, number, 64>\n"
                    + "ORDER BY <column, column, qty> DESC\nLIMIT <rows, number, 100>;"),
            new Template(IsmsTexts.TEMPLATE_COUNT, "COUNT <item, name, iron_ingot>;"),
            new Template(IsmsTexts.TEMPLATE_CRAFT, "CRAFT <quantity, number, 64> <item, name, iron_ingot>;"),
            new Template(IsmsTexts.TEMPLATE_VIEW, "CREATE VIEW <name, name, low_stock> AS\n"
                    + "QUERY items WHERE qty < <value, number, 64>;"),
            new Template(IsmsTexts.TEMPLATE_PROCEDURE, "CREATE PROCEDURE <name, name, restock> AS {\n"
                    + "    CRAFT <quantity, number, 64> <item, name, torch>;\n};"),
            new Template(IsmsTexts.TEMPLATE_JOB, "CREATE JOB <name, name, nightly_vacuum> AS VACUUM EVERY "
                    + "<interval, interval, 20m>;"),
            new Template(IsmsTexts.TEMPLATE_LOCK, "LOCK <quantity, number, 64> <item, name, diamond>;"),
            new Template(IsmsTexts.TEMPLATE_UPDATE,
                    "UPDATE <quantity, number, 1> <item, name, iron_sword> SET REPAIR;"));

    /** The IQL Reference: each statement's shape and what it does. */
    static final List<Reference> REFERENCE = List.of(
            new Reference("QUERY table [WHERE ...] [ORDER BY ...] [LIMIT n]", IsmsTexts.REF_QUERY),
            new Reference("COUNT item", IsmsTexts.REF_COUNT),
            new Reference("SELECT n item", IsmsTexts.REF_SELECT),
            new Reference("INSERT n item", IsmsTexts.REF_INSERT),
            new Reference("DELETE n item TO place", IsmsTexts.REF_DELETE),
            new Reference("MOVE n item FROM server TO server", IsmsTexts.REF_MOVE),
            new Reference("DROP n item", IsmsTexts.REF_DROP),
            new Reference("CRAFT n item", IsmsTexts.REF_CRAFT),
            new Reference("UPDATE n item SET action", IsmsTexts.REF_UPDATE),
            new Reference("LOCK n item / UNLOCK item", IsmsTexts.REF_LOCK),
            new Reference("ANALYZE / REINDEX / VACUUM", IsmsTexts.REF_MAINTENANCE),
            new Reference("CREATE VIEW name AS query", IsmsTexts.REF_VIEW),
            new Reference("CREATE PROCEDURE name AS { ... }", IsmsTexts.REF_PROCEDURE),
            new Reference("CREATE JOB name AS ... EVERY time", IsmsTexts.REF_JOB),
            new Reference("EXEC name", IsmsTexts.REF_EXEC),
            new Reference("WHERE / ORDER BY / LIMIT", IsmsTexts.REF_CLAUSES),
            new Reference("; --", IsmsTexts.REF_COMMENTS));

    /** How many rows a node's Query Top Rows reads. */
    static final int TOP_ROWS = 100;
    private static final String END = ";";

    private IsmsTemplates() {
    }

    /** The first rows of {@code table}. */
    static String topRows(final String table) {
        return "QUERY " + table + " LIMIT " + TOP_ROWS + END;
    }

    /** Every row of {@code table}, or what the view {@code table} reads. */
    static String query(final String table) {
        return "QUERY " + table + END;
    }

    /** What the server {@code name} holds. */
    static String queryServer(final String name) {
        return "QUERY items WHERE server = \"" + name + "\"" + END;
    }

    /** The Operations log, newest first. */
    static String queryLog() {
        return "QUERY operations LIMIT " + TOP_ROWS + END;
    }

    /** The statement that makes the view {@code name} again. */
    static String createView(final String name, final String body) {
        return "CREATE VIEW " + name + " AS " + body + END;
    }

    /** The statement that makes the procedure {@code name} again. */
    static String createProcedure(final String name, final String body) {
        return "CREATE PROCEDURE " + name + " AS " + body + END;
    }

    /** The statements that replace the procedure {@code name} with what its editor now holds. */
    static String modifyProcedure(final String name, final String body) {
        return "DROP PROCEDURE " + name + END + "\n" + createProcedure(name, body);
    }

    /** The statement that drops the view {@code name}. */
    static String dropView(final String name) {
        return "DROP VIEW " + name + END;
    }

    /** The statement that drops the procedure {@code name}. */
    static String dropProcedure(final String name) {
        return "DROP PROCEDURE " + name + END;
    }

    /** The statement that runs the procedure {@code name}. */
    static String exec(final String name) {
        return "EXEC " + name + END;
    }

    /** One of the index's three jobs: {@code ANALYZE}, {@code REINDEX} or {@code VACUUM}. */
    static String maintenance(final String verb) {
        return verb + END;
    }

    /** A template: its name in the Template Explorer, and the statement it opens. */
    record Template(TextKey name, String text) {
    }

    /** A line of the IQL Reference: a statement's shape as it is typed, and what it does. */
    record Reference(String syntax, TextKey meaning) {
    }
}
