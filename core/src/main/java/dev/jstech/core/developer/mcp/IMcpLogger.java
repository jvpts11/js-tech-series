/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer.mcp;

import org.jetbrains.annotations.Nullable;

/**
 * Where the MCP server writes what it does for the log: the game's developer log ({@link DeveloperMcpLogger}), or
 * nowhere, for a test with no log of its own.
 */
public interface IMcpLogger {

    /** Writes {@code message}. */
    void info(String message);

    /** Writes {@code message} as a warning, with {@code failed} when something threw. */
    void warn(String message, @Nullable Throwable failed);

    /** Nowhere. */
    static IMcpLogger quiet() {
        return new IMcpLogger() {
            @Override
            public void info(final String message) {
                // Written nowhere.
            }

            @Override
            public void warn(final String message, @Nullable final Throwable failed) {
                // Written nowhere.
            }
        };
    }
}
