/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.format;

public enum Unit {
    FE(" FE"),

    FE_PER_TICK(" FE/t"),

    MB(" MB"),

    IT_PER_TICK(" it/t");

    private final String suffix;

    Unit(final String suffix) {
        this.suffix = suffix;
    }

    public String suffix() {
        return suffix;
    }
}
