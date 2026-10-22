/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExpressionContextTest {

    private ExpressionContext context;
    private int reads;

    @BeforeEach
    void setUp() {
        context = new ExpressionContext();
        reads = 0;
    }

    @Test
    void readQuery_worksCodeOutOnceAFrame() {
        context.query("load", c -> ++reads);
        context.nextFrame();
        assertEquals(1.0, context.readQuery("load"));
        assertEquals(1.0, context.readQuery("load"));
        assertEquals(1, reads);
        context.nextFrame();
        assertEquals(2.0, context.readQuery("load"));
        assertEquals(2, reads);
    }

    @Test
    void value_holdsUntilGivenAnother() {
        context.value("ground_speed", 3.5);
        context.nextFrame();
        assertEquals(3.5, context.readQuery("ground_speed"));
        context.value("ground_speed", 0.25);
        assertEquals(0.25, context.readQuery("ground_speed"));
    }

    @Test
    void value_replacesCodeGivenBefore() {
        context.query("is_on_ground", c -> 1.0);
        context.value("is_on_ground", 0.0);
        assertEquals(0.0, context.readQuery("is_on_ground"));
    }

    @Test
    void readQuery_readsNothingGivenAsZero() {
        assertEquals(0.0, context.readQuery("limb_swing"));
    }

    @Test
    void parse_readsAValueHandedOverEachFrame() {
        final ModelExpression swing = ExpressionParser.parse("math.sin(query.limb_swing * 60) * 30");
        context.value("limb_swing", 1.5);
        assertEquals(30.0, swing.evaluate(context), 1e-9);
    }
}
