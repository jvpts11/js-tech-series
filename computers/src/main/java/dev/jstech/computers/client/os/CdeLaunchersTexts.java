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

/** What CDE's Front Panel subpanels say: their headings, and the two rows the Files subpanel always shows. */
@TextHolder
final class CdeLaunchersTexts {

    static final TextKey FILES_HEADING = TextKey.of("jsc.cde.launchers.files_heading", "Files");
    static final TextKey EDITOR_HEADING =
            TextKey.of("jsc.cde.launchers.editor_heading", "Personal Applications");
    static final TextKey APPLICATIONS_HEADING =
            TextKey.of("jsc.cde.launchers.applications_heading", "Applications");
    static final TextKey HOME_ROW = TextKey.of("jsc.cde.launchers.home_row", "Home");
    static final TextKey DESKTOP_ROW = TextKey.of("jsc.cde.launchers.desktop_row", "Desktop");

    private CdeLaunchersTexts() {
    }
}
