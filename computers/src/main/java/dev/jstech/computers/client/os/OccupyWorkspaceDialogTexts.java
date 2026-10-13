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

/** Occupy Workspace's own window title, apart from what {@link OccupyWorkspaceTexts} covers. */
@TextHolder
final class OccupyWorkspaceDialogTexts {

    static final TextKey TITLE = TextKey.of("jsc.occupy_workspace_dialog.title", "Occupy Workspace");

    private OccupyWorkspaceDialogTexts() {
    }
}
