/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * What the Command Prompt says on its own account: the banner a session opens with, the line naming the machine, and
 * the keys along its foot. Commands, file names and version numbers in these lines are data. Kept apart from the
 * screen so the language generator can read it on a server too, where screens do not exist.
 */
@TextHolder
final class CommandPromptTexts {

    /* What the typing field is called to a screen reader. */
    static final TextKey COMMAND = TextKey.of("jsc.console.command", "command");

    // The banners, one per family of system.
    static final TextKey DOS_VERSION = TextKey.of("jsc.console.dos_version", "%s  Version 1.0  [Network Build]");
    static final TextKey BASE_MEMORY = TextKey.of("jsc.console.base_memory", "%sK base memory");
    static final TextKey NO_SPACE = TextKey.of("jsc.console.no_space", "No operating space is installed.");
    static final TextKey PUTS_ONE_ON = TextKey.of("jsc.console.puts_one_on", "%s  puts one on.");
    static final TextKey SHELL_VERSION = TextKey.of("jsc.console.shell_version", "%s Shell v1.0");
    static final TextKey HELP_HINT =
            TextKey.of("jsc.console.help_hint", "type '%s' for commands, TAB to complete");

    // The machine a session is on.
    static final TextKey MACHINE = TextKey.of("jsc.console.machine", "machine: %s  ·  %s");
    /* A machine with no name of its own. */
    static final TextKey UNNAMED = TextKey.of("jsc.console.unnamed", "machine");
    static final TextKey NO_DRIVES = TextKey.of("jsc.console.no_drives", "no drives");
    static final TextKey ONE_DRIVE = TextKey.of("jsc.console.one_drive", "%s drive");
    static final TextKey DRIVES = TextKey.of("jsc.console.drives", "%s drives");

    // The window round the glass.
    static final TextKey TITLE = TextKey.of("jsc.console.title", "COMMAND PROMPT");
    static final TextKey PROGRAM = TextKey.of("jsc.console.program", "PROGRAM");
    static final TextKey SCROLLED = TextKey.of("jsc.console.scrolled", "scrolled +%s");
    static final TextKey KEYS_BUSY =
            TextKey.of("jsc.console.keys_busy", "CTRL+C interrupt    wheel scroll    ESC close");
    static final TextKey KEYS =
            TextKey.of("jsc.console.keys", "ENTER run    UP/DOWN history    wheel scroll    ESC close");

    private CommandPromptTexts() {
    }
}
