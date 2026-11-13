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
import org.jetbrains.annotations.Nullable;

/**
 * One {@code UPDATE create_kinetics} statement: it sets the speed the Rotation Speed Controllers the network's computers
 * read aim at, those its WHERE keeps, or every one without it (computers/docs/design/implementation.md, "Create"). The
 * rows are the {@code create_kinetics} table's, so the WHERE names its columns. Pure, so it is tested without the game;
 * the Mainframe that runs it finds the controllers and sets them.
 *
 * <pre>
 * UPDATE create_kinetics SET target_speed = 64 WHERE at = '12, 64, -3'
 * UPDATE create_kinetics SET target_speed = -32
 * </pre>
 *
 * @param speed the speed to aim at, in RPM, negative to turn the other way
 * @param where which rows it sets; none for every one
 */
public record IqlKineticsStatement(double speed, @Nullable IIqlCondition where) {

    /** The table it updates. */
    public static final String TABLE = "create_kinetics";
    /** The one column it sets. */
    public static final String COLUMN = "target_speed";

    /** Whether {@code input} is an {@code UPDATE create_kinetics} statement, which this reads. */
    public static boolean isKineticsUpdate(final String input) {
        final List<Token> tokens = IqlLexer.lex(input == null ? "" : input);
        return tokens.size() >= 2 && word(tokens.get(0), "UPDATE") && word(tokens.get(1), TABLE);
    }

    /** Reads an {@code UPDATE create_kinetics} statement. */
    public static IqlKineticsStatement parse(final String input) {
        final List<Token> tokens = IqlLexer.lex(input == null ? "" : input);
        if (tokens.size() < 6 || !word(tokens.get(2), "SET") || !word(tokens.get(3), COLUMN)
                || !tokens.get(4).text().equals("=")) {
            throw IqlError.of(IqlError.KINETICS_NEEDS_SPEED);
        }
        int at = 5;
        final boolean minus = tokens.get(at).text().equals("-");
        if (minus) {
            at++;
        }
        if (at >= tokens.size()) {
            throw IqlError.of(IqlError.KINETICS_NEEDS_SPEED);
        }
        final double speed;
        try {
            speed = Double.parseDouble(tokens.get(at).text()) * (minus ? -1 : 1);
        } catch (final NumberFormatException notANumber) {
            throw IqlError.of(IqlError.KINETICS_NEEDS_SPEED);
        }
        // NaN and Infinity read as numbers to Java, and are no speed a controller can be set to.
        if (!Double.isFinite(speed)) {
            throw IqlError.of(IqlError.KINETICS_NEEDS_SPEED);
        }
        at++;
        if (at == tokens.size()) {
            return new IqlKineticsStatement(speed, null);
        }
        if (!word(tokens.get(at), "WHERE")) {
            throw IqlError.of(IqlError.UNEXPECTED_TOKEN, tokens.get(at).text());
        }
        final IqlConditionParser condition = new IqlConditionParser(tokens, at + 1);
        final IIqlCondition where = condition.parseCondition();
        if (condition.position() != tokens.size()) {
            throw IqlError.of(IqlError.UNEXPECTED_TOKEN, tokens.get(condition.position()).text());
        }
        return new IqlKineticsStatement(speed, where);
    }

    private static boolean word(final Token token, final String keyword) {
        return token.type() == Type.WORD && token.text().equalsIgnoreCase(keyword);
    }
}
