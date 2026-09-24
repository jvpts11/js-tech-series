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

/** The words of CDE's Occupy Workspace dialog around the program's name, which is data. */
@TextHolder
final class OccupyWorkspaceTexts {

    static final TextKey IS_ON = TextKey.of("jsc.occupy_workspace.is_on", "%s is on:");
    static final TextKey OK = TextKey.of("jsc.occupy_workspace.ok", "OK");
    static final TextKey CANCEL = TextKey.of("jsc.occupy_workspace.cancel", "Cancel");

    private OccupyWorkspaceTexts() {
    }
}
