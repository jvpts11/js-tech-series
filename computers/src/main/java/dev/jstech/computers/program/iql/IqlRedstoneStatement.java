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

/**
 * One {@code SET REDSTONE} statement: the Redstone Interface it names, by the name the interface answers to among those
 * of the computer the statement runs on, and whether it reads or emits, with the strength it emits. Pure, so it is
 * tested without the game; the computer that runs it finds the interface and sets it.
 *
 * <pre>
 * SET REDSTONE 'Gate' IN
 * SET REDSTONE 'Gate' OUT 15
 * </pre>
 *
 * @param target   the name the interface answers to
 * @param emits    whether it emits; it reads otherwise
 * @param strength the strength it emits, from 0 to 15; 0 when it reads
 */
public record IqlRedstoneStatement(String target, boolean emits, int strength) {

    /** The strongest signal redstone carries. */
    public static final int MAX_STRENGTH = 15;

    /** Whether {@code input} is a {@code SET REDSTONE} statement, which this reads rather than the action parser. */
    public static boolean isSetRedstone(final String input) {
        final List<Token> tokens = IqlLexer.lex(input == null ? "" : input);
        return tokens.size() >= 2 && word(tokens.get(0), "SET") && word(tokens.get(1), "REDSTONE");
    }

    /** Reads a {@code SET REDSTONE} statement. */
    public static IqlRedstoneStatement parse(final String input) {
        final List<Token> tokens = IqlLexer.lex(input == null ? "" : input);
        if (tokens.size() < 3) {
            throw IqlError.of(IqlError.REDSTONE_NEEDS_NAME);
        }
        final String name = tokens.get(2).text();
        if (tokens.size() < 4) {
            throw IqlError.of(IqlError.REDSTONE_NEEDS_MODE);
        }
        final Token mode = tokens.get(3);
        final IqlRedstoneStatement statement = switch (mode.text().toUpperCase(Locale.ROOT)) {
            case "IN" -> new IqlRedstoneStatement(name, false, 0);
            case "OUT" -> new IqlRedstoneStatement(name, true, strength(tokens));
            default -> throw IqlError.of(IqlError.REDSTONE_NEEDS_MODE);
        };
        final int used = statement.emits() ? 5 : 4;
        if (tokens.size() > used) {
            throw IqlError.of(IqlError.UNEXPECTED_TOKEN, tokens.get(used).text());
        }
        return statement;
    }

    /* The strength after OUT: a whole number from 0 to 15. */
    private static int strength(final List<Token> tokens) {
        if (tokens.size() < 5) {
            throw IqlError.of(IqlError.REDSTONE_NEEDS_MODE);
        }
        final Token token = tokens.get(4);
        if (token.type() == Type.NUMBER) {
            try {
                final int strength = Integer.parseInt(token.text());
                if (strength >= 0 && strength <= MAX_STRENGTH) {
                    return strength;
                }
            } catch (final NumberFormatException e) {
                // Falls through to the refusal below.
            }
        }
        throw IqlError.of(IqlError.NOT_A_STRENGTH, token.text());
    }

    private static boolean word(final Token token, final String keyword) {
        return token.type() == Type.WORD && token.text().equalsIgnoreCase(keyword);
    }
}
