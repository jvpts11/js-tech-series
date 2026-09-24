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

/** What a Sigma program's own window says when the program itself has not. */
@TextHolder
final class SigmaWindowTexts {

    static final TextKey DEFAULT_TITLE = TextKey.of("jsc.sigma_window.default_title", "Window");

    private SigmaWindowTexts() {
    }
}
