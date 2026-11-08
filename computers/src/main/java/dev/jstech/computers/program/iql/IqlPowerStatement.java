/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.iql;

import dev.jstech.computers.program.iql.IqlLexer.Token;
import dev.jstech.computers.program.iql.IqlLexer.Type;
import java.util.List;
import java.util.Locale;
import org.jetbrains.annotations.Nullable;

/**
 * One {@code UPDATE power} statement: it switches on or off the bays of the network's racks that have a PDU, those its
 * WHERE keeps, or every one without it (computers/docs/design/servers-and-racks.md). The rows are the {@code power}
 * table's, so the WHERE names its columns. Pure, so it is tested without the game; the Mainframe that runs it finds the
 * bays and switches them.
 *
 * <pre>
 * UPDATE power SET on = no WHERE server = 'VAULT-01'
 * UPDATE power SET on = yes
 * </pre>
 *
 * @param on    whether the bays are switched on; off otherwise
 * @param where which rows of the table it switches, or null for all of them
 */
public record IqlPowerStatement(boolean on, @Nullable IIqlCondition where) {

    /** The table it updates. */
    public static final String TABLE = "power";
    /** The one column it sets. */
    public static final String COLUMN = "on";

    /** Whether {@code input} is an {@code UPDATE power} statement, which this reads rather than the action parser. */
    public static boolean isPowerUpdate(final String input) {
        final List<Token> tokens = IqlLexer.lex(input == null ? "" : input);
        return tokens.size() >= 2 && word(tokens.get(0), "UPDATE") && word(tokens.get(1), TABLE);
    }

    /** Reads an {@code UPDATE power} statement. */
    public static IqlPowerStatement parse(final String input) {
        final List<Token> tokens = IqlLexer.lex(input == null ? "" : input);
        if (tokens.size() < 6 || !word(tokens.get(2), "SET") || !word(tokens.get(3), COLUMN)
                || !tokens.get(4).text().equals("=")) {
            throw IqlError.of(IqlError.POWER_NEEDS_ON);
        }
        final boolean on = switch (tokens.get(5).text().toLowerCase(Locale.ROOT)) {
            case "yes", "true", "on" -> true;
            case "no", "false", "off" -> false;
            default -> throw IqlError.of(IqlError.POWER_NEEDS_ON);
        };
        if (tokens.size() == 6) {
            return new IqlPowerStatement(on, null);
        }
        if (!word(tokens.get(6), "WHERE")) {
            throw IqlError.of(IqlError.UNEXPECTED_TOKEN, tokens.get(6).text());
        }
        final IqlConditionParser condition = new IqlConditionParser(tokens, 7);
        final IIqlCondition where = condition.parseCondition();
        if (condition.position() != tokens.size()) {
            throw IqlError.of(IqlError.UNEXPECTED_TOKEN, tokens.get(condition.position()).text());
        }
        return new IqlPowerStatement(on, where);
    }

    private static boolean word(final Token token, final String keyword) {
        return token.type() == Type.WORD && token.text().equalsIgnoreCase(keyword);
    }
}
