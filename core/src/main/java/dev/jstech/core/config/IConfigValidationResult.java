/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.config;

/**
 * Outcome of validating a config value against its declared {@link ConfigKey}.
 */
public sealed interface IConfigValidationResult<T>
        permits IConfigValidationResult.Valid,
        IConfigValidationResult.Clamped,
        IConfigValidationResult.Repaired,
        IConfigValidationResult.Rejected {

    T value();

    /**
     * Value passed validation as-is.
     */
    record Valid<T>(T value) implements IConfigValidationResult<T> {
    }

    /**
     * Numeric value was outside its range and was adjusted to the nearest bound.
     */
    record Clamped<T>(T value, T original) implements IConfigValidationResult<T> {
    }

    /**
     * Value was partly unreadable, such as one entry of a map or a list written wrongly: what could be read is kept,
     * and only the part that could not is left out.
     */
    record Repaired<T>(T value, String reason) implements IConfigValidationResult<T> {
    }

    /**
     * Value was rejected (wrong type, null, or other structural issue) and the default was substituted.
     */
    record Rejected<T>(T value, String reason) implements IConfigValidationResult<T> {
    }
}
