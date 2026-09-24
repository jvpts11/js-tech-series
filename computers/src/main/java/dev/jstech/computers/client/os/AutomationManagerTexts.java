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
 * What the Automation Manager's own window says, apart from the job list and form covered by
 * {@link AutomationTexts}: its title and the delete mark on a job row.
 */
@TextHolder
final class AutomationManagerTexts {

    static final TextKey TITLE = TextKey.of("jsc.automation_manager.title", "Automation Manager");
    static final TextKey DELETE_MARK = TextKey.of("jsc.automation_manager.delete_mark", "x");

    private AutomationManagerTexts() {
    }
}
