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

/** What Exceed's own chrome says, apart from what {@link ExceedTexts} covers: the formula bar's mark. */
@TextHolder
final class ExceedAppTexts {

    static final TextKey FORMULA_MARK = TextKey.of("jsc.exceed_app.formula_mark", "fx");

    private ExceedAppTexts() {
    }
}
