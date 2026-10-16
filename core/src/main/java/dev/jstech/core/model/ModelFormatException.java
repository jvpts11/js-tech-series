/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

/** A model or animation file that cannot be read, saying what is wrong with it. */
public final class ModelFormatException extends RuntimeException {

    public ModelFormatException(final String problem) {
        super(problem);
    }

    public ModelFormatException(final String problem, final Throwable cause) {
        super(problem, cause);
    }
}
