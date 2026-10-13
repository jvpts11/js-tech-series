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

/**
 * What the window that asks which program should open a file says: its question, the line under it and its two
 * buttons. File names, extensions and program names are data. Kept apart from the window so the language generator
 * can read it on a server too, where windows do not exist.
 */
@TextHolder
final class OpenWithTexts {

    static final TextKey QUESTION = TextKey.of("jsc.open_with.question", "How do you want to open %s?");
    static final TextKey NOTHING_OPENS =
            TextKey.of("jsc.open_with.nothing_opens", "Nothing on this computer opens this file yet.");
    static final TextKey NOTHING_OPENS_KIND =
            TextKey.of("jsc.open_with.nothing_opens_kind", "Nothing on this computer opens .%s files yet.");
    static final TextKey OPENS_IN = TextKey.of("jsc.open_with.opens_in", "This file opens in %s.");
    static final TextKey KIND_OPENS_IN = TextKey.of("jsc.open_with.kind_opens_in", ".%s files open in %s.");
    static final TextKey ONLY_THIS_TIME = TextKey.of("jsc.open_with.only_this_time", "Only this time");
    static final TextKey ALWAYS = TextKey.of("jsc.open_with.always", "Always");

    private OpenWithTexts() {
    }
}
