/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** What the Calculator's display says when a sum has no answer. */
@TextHolder
final class CalculatorTexts {

    static final TextKey ERROR = TextKey.of("jsc.calculator.error", "Error");

    private CalculatorTexts() {
    }
}
