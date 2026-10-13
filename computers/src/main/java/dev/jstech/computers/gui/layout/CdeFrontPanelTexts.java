/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** What resting the pointer on each of CDE's Front Panel controls says. */
@TextHolder
final class CdeFrontPanelTexts {

    static final TextKey CLOCK = TextKey.of("jsc.cde.front_panel.clock", "Clock");
    static final TextKey CALENDAR = TextKey.of("jsc.cde.front_panel.calendar", "Calendar");
    static final TextKey FILE_MANAGER = TextKey.of("jsc.cde.front_panel.file_manager", "File Manager");
    static final TextKey TEXT_EDITOR = TextKey.of("jsc.cde.front_panel.text_editor", "Text Editor");
    static final TextKey STYLE_MANAGER = TextKey.of("jsc.cde.front_panel.style_manager", "Style Manager");
    static final TextKey APPLICATIONS = TextKey.of("jsc.cde.front_panel.applications", "Applications");
    static final TextKey TRASH_CAN = TextKey.of("jsc.cde.front_panel.trash_can", "Trash Can");
    static final TextKey HELP_VIEWER = TextKey.of("jsc.cde.front_panel.help_viewer", "Help Viewer");

    private CdeFrontPanelTexts() {
    }
}
