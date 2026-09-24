/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.program;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * What the machine says straight to a terminal when a line cannot reach its shell, or asks for a program the prompt
 * cannot open. Program names are data. Kept apart from the handlers, which reach the client's windows, so the
 * language generator can read it on a server.
 */
@TextHolder
final class ConsoleTexts {

    static final TextKey NOT_ATTACHED =
            TextKey.of("jsc.console.not_attached", "This terminal is no longer attached to that computer.");
    static final TextKey NO_SUCH_PROGRAM = TextKey.of("jsc.console.no_such_program", "no such program: %s");
    static final TextKey NOT_INSTALLED =
            TextKey.of("jsc.console.not_installed", "%s is not installed - try: install %s");
    static final TextKey CANNOT_RUN =
            TextKey.of("jsc.console.cannot_run", "%s cannot run on this computer's OS or hardware");
    static final TextKey CANNOT_RUN_MESSAGE =
            TextKey.of("jsc.console.cannot_run_message", "The %s cannot run on this computer's OS or hardware.");
    static final TextKey NMS_FROM_ICON =
            TextKey.of("jsc.console.nms_from_icon", "open the NMS from its desktop icon on a Frames computer");
    static final TextKey NMS_FROM_ICON_MESSAGE =
            TextKey.of("jsc.console.nms_from_icon_message", "Open the NMS from its desktop icon.");
    static final TextKey ALREADY_OPEN = TextKey.of("jsc.console.already_open", "the %s is already open");
    // How far a program's setup has got, on the line that stands in for the prompt while it runs.
    static final TextKey SETTING_UP =
            TextKey.of("jsc.console.setting_up", "Setting up %s  %s%%  (Ctrl+C to cancel)");
    static final TextKey REMOVING = TextKey.of("jsc.console.removing", "Removing %s  %s%%  (Ctrl+C to cancel)");

    private ConsoleTexts() {
    }
}
