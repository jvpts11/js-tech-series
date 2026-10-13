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

/** What a shell window is called when no desktop gives its terminal a name of its own. */
@TextHolder
final class ShellAppTexts {

    static final TextKey TITLE = TextKey.of("jsc.shell_app.title", "Command Prompt");

    private ShellAppTexts() {
    }
}
