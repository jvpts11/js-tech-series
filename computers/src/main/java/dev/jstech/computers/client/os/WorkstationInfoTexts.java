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

/** The words of CDE's Workstation Info window around the facts, which carry their own. */
@TextHolder
final class WorkstationInfoTexts {

    static final TextKey TITLE = TextKey.of("jsc.workstation.title", "Workstation Info");
    static final TextKey CLOSE = TextKey.of("jsc.workstation.close", "Close");

    private WorkstationInfoTexts() {
    }
}
