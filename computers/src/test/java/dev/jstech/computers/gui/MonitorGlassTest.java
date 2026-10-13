/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MonitorGlassTest {

    @Test
    void width_neverGoesBelowZeroInATinyWindow() {
        assertEquals(0, MonitorGlass.width(10));
        assertEquals(MonitorGlass.WIDTH, MonitorGlass.width(5000));
    }

    @Test
    void height_neverGoesBelowZeroInATinyWindow() {
        assertEquals(0, MonitorGlass.height(10));
        assertEquals(MonitorGlass.HEIGHT, MonitorGlass.height(5000));
    }
}
