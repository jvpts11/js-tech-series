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
 * What an effects page says around its rows: the way back to the page it is reached from, and the buttons of the
 * systems whose page was a dialog. Kept apart from the page so the language generator can read it on a server too.
 */
@TextHolder
final class EffectsPageTexts {

    static final TextKey BACK = TextKey.of("jsc.settings.effects.back", "Back");
    static final TextKey OK = TextKey.of("jsc.settings.effects.ok", "OK");
    static final TextKey CANCEL = TextKey.of("jsc.settings.effects.cancel", "Cancel");
    static final TextKey APPLY = TextKey.of("jsc.settings.effects.apply", "Apply");
    static final TextKey CLOSE = TextKey.of("jsc.settings.effects.close", "Close");

    private EffectsPageTexts() {
    }
}
