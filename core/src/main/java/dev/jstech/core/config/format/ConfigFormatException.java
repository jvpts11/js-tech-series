/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.config.format;

/** A settings file that is not a file of the format it was read as, with where it stops being one. */
public final class ConfigFormatException extends Exception {

    private static final long serialVersionUID = 1L;

    public ConfigFormatException(final String message) {
        super(message);
    }

    public ConfigFormatException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
