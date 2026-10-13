/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.language;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** What every language says the same way, whichever compiler found it. */
@TextHolder
final class LanguageTexts {

    /** A compiler's line: the file, the line, the column, the code and the message. */
    static final TextKey COMPLAINT = TextKey.of("jscore.language.complaint", "%s(%s,%s): error %s: %s");

    private LanguageTexts() {
    }
}
