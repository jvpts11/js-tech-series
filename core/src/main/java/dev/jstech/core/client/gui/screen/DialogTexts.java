/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.screen;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * The words of the toolkit's dialogs. Kept apart from the dialogs, which exist only on a client, because every class
 * that declares sentences is read on the server as well.
 */
@TextHolder
final class DialogTexts {

    static final TextKey CONFIRM = TextKey.of("gui.jscore.confirm", "Confirm");
    static final TextKey CANCEL = TextKey.of("gui.jscore.cancel", "Cancel");

    private DialogTexts() {
    }
}
