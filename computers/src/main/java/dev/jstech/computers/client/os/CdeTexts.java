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

/** The words of CDE's own chrome: a window's menu and the Style Manager's pages. Key names are data. */
@TextHolder
final class CdeTexts {

    // A window's menu.
    static final TextKey RESTORE = TextKey.of("jsc.cde.window.restore", "Restore");
    static final TextKey MINIMIZE = TextKey.of("jsc.cde.window.minimize", "Minimize");
    static final TextKey MAXIMIZE = TextKey.of("jsc.cde.window.maximize", "Maximize");
    static final TextKey LOWER = TextKey.of("jsc.cde.window.lower", "Lower");
    static final TextKey OCCUPY_WORKSPACE = TextKey.of("jsc.cde.window.occupy_workspace", "Occupy Workspace...");
    static final TextKey OCCUPY_ALL = TextKey.of("jsc.cde.window.occupy_all", "Occupy All Workspaces");
    static final TextKey CLOSE = TextKey.of("jsc.cde.window.close", "Close");

    // The Style Manager's pages.
    static final TextKey FOR_WORKSPACE = TextKey.of("jsc.cde.style.for_workspace", "For workspace %s");
    static final TextKey APPLY = TextKey.of("jsc.cde.style.apply", "Apply");
    static final TextKey OK = TextKey.of("jsc.cde.style.ok", "OK");
    static final TextKey CANCEL = TextKey.of("jsc.cde.style.cancel", "Cancel");

    private CdeTexts() {
    }
}
