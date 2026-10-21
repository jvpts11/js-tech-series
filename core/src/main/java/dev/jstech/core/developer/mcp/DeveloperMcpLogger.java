/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer.mcp;

import dev.jstech.core.log.CoreLogs;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/** The MCP server's lines in J's Core's developer log. */
public final class DeveloperMcpLogger implements IMcpLogger {

    private final Logger logger = CoreLogs.DEVELOPER.logger();

    @Override
    public void info(final String message) {
        this.logger.info(message);
    }

    @Override
    public void warn(final String message, @Nullable final Throwable failed) {
        this.logger.warn(message, failed);
    }
}
