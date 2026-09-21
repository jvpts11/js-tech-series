/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

/**
 * Fits a string to the length a payload's codec accepts.
 */
public final class WireStrings {

    private WireStrings() {
    }

    /** Cuts {@code s} to the {@code max} characters its wire field carries: sending more disconnects the player. */
    public static String wire(final String s, final int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }
}
