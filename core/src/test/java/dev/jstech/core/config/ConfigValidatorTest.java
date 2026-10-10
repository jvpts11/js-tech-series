/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;

class ConfigValidatorTest {

    private final ConfigValidator validator = new ConfigValidator(IConfigLogger.NOOP);

    @Test
    void validate_notANumberInARangedDouble_isRejectedToTheDefault() {
        final ConfigKey<Double> key = ConfigKey.number("share", 0.5).range(0.0, 1.0);

        final IConfigValidationResult<Double> result = validator.validate(key, Double.NaN);

        final IConfigValidationResult.Rejected<Double> rejected =
                assertInstanceOf(IConfigValidationResult.Rejected.class, result);
        assertEquals(0.5, rejected.value());
    }

    @Test
    void validate_aboveTheRange_isClamped() {
        final ConfigKey<Double> key = ConfigKey.number("share", 0.5).range(0.0, 1.0);

        final IConfigValidationResult<Double> result = validator.validate(key, 7.0);

        assertEquals(1.0, assertInstanceOf(IConfigValidationResult.Clamped.class, result).value());
    }
}
