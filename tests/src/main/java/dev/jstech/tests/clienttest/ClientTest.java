/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks one of the series' client tests: a {@code public static void name(ClientTestContext ctx)} method that builds a
 * sequence of steps driven on the real client (world, integrated server, screens, input), with the series' own
 * helper rather than the Core's test kit's. Read from the classes listed in {@link ClientTestSuite}, which hands them
 * to the kit's runner.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ClientTest {

    /** Ticks the whole test may take before it is failed as timed out. */
    int timeoutTicks() default 1200;
}
