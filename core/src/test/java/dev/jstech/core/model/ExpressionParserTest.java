/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ExpressionParserTest {

    private static final double EPSILON = 1.0e-9;

    @Test
    void parse_keepsArithmeticsOrder() {
        assertEquals(7, value("1 + 2 * 3"), EPSILON);
        assertEquals(9, value("(1 + 2) * 3"), EPSILON);
        assertEquals(-4, value("-2 * 2"), EPSILON);
        assertEquals(0, value("5 / 0"), EPSILON, "a division by nothing is nothing, as Molang has it");
    }

    @Test
    void parse_readsQueriesInEitherSpelling() {
        final ExpressionContext context = new ExpressionContext().animTime(2).query("speed", 3);
        assertEquals(6, ExpressionParser.parse("query.anim_time * q.speed").evaluate(context), EPSILON);
        assertEquals(0, ExpressionParser.parse("query.unknown").evaluate(context), EPSILON, "a query not given is 0");
    }

    @Test
    void parse_mathFunctionsCountInDegrees() {
        assertEquals(1, value("math.sin(90)"), EPSILON);
        assertEquals(-1, value("math.cos(180)"), EPSILON);
        assertEquals(3, value("math.clamp(5, 0, 3)"), EPSILON);
        assertEquals(2, value("math.floor(2.7)"), EPSILON);
    }

    @Test
    void parse_comparesChoosesAndFallsBack() {
        assertEquals(1, value("2 > 1"), EPSILON);
        assertEquals(10, value("1 < 2 && 3 >= 3 ? 10 : 20"), EPSILON);
        assertEquals(4, value("v.unset ?? 4"), EPSILON, "a variable never set falls back");
    }

    @Test
    void parse_setsVariablesThatLaterStatementsRead() {
        final ExpressionContext context = new ExpressionContext();
        assertEquals(6, ExpressionParser.parse("v.a = 2; t.b = v.a * 3; return t.b;").evaluate(context), EPSILON);
        assertEquals(2, context.variable("a"), EPSILON, "and the variable stays");
    }

    @Test
    void parse_aNumberAloneIsConstant() {
        assertTrue(ExpressionParser.parse("12.5").constant());
        assertThrows(ModelFormatException.class, () -> ExpressionParser.parse("1 +"));
    }

    private static double value(final String source) {
        return ExpressionParser.parse(source).evaluate(new ExpressionContext());
    }
}
